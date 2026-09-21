"""As fases de uma rodada de guerra.

A ordem importa: cada fase entrega um modificador para a seguinte.

    0. Inteligencia  -> qualidade de informacao (estreita o dado, da surpresa)
    1. Ar            -> supremacia aerea (multiplica as fases 2, 3 e 4)
    2. Profundo      -> destroi logistica e defesa aerea na retaguarda
    3. Mar           -> controle maritimo e bloqueio
    4. Terra         -> movimento da linha de frente

Nenhuma fase decide a guerra sozinha. Perder o ar nao mata o exercito, mas
faz cada fase seguinte custar muito mais caro.
"""

from __future__ import annotations

from dataclasses import dataclass, field
from math import ceil

from .catalogo import Dominio, Fase, Papel
from .dados import Dados
from .unidades import ForcaArmada, Unidade

# Saturacao da defesa antiaerea: quanto de poder AA equivale a 50% de
# interceptacao. Valor alto = misseis passam com mais facilidade.
SATURACAO_ANTIAEREA = 3000.0
# Fracao maxima de um estoque de consumiveis empregada por rodada. Sem isso
# o arsenal inteiro de misseis e disparado na primeira rodada.
SALVA_MAXIMA = 0.35
# Peso da assinatura inimiga ao calcular cobertura de inteligencia.
PESO_ASSINATURA_INIMIGA = 0.5


@dataclass
class ResultadoEngajamento:
    fase: Fase
    atacante: str
    ataques: int = 0
    interceptados: int = 0
    dano_causado: float = 0.0
    destruidas: dict[str, int] = field(default_factory=dict)

    def registrar_baixa(self, unidade: Unidade) -> None:
        chave = unidade.tipo.chave
        self.destruidas[chave] = self.destruidas.get(chave, 0) + 1

    @property
    def total_destruidas(self) -> int:
        return sum(self.destruidas.values())


@dataclass
class ResultadoRodada:
    numero: int
    intel: dict[str, float] = field(default_factory=dict)
    supremacia_aerea: dict[str, float] = field(default_factory=dict)
    surpresa: str | None = None
    engajamentos: list[ResultadoEngajamento] = field(default_factory=list)
    movimento_frente: float = 0.0
    bloqueio: str | None = None

    def dano_de(self, dono: str) -> float:
        return sum(e.dano_causado for e in self.engajamentos if e.atacante == dono)

    def baixas_de(self, dono: str) -> int:
        return sum(e.total_destruidas for e in self.engajamentos if e.atacante == dono)


# ---------------------------------------------------------------------------
# Selecao de alvos
# ---------------------------------------------------------------------------

def alvos_da_fase(forca: ForcaArmada, fase: Fase) -> list[Unidade]:
    """O que pode ser atingido em cada fase.

    Fases diferentes atacam camadas diferentes: a fase aerea disputa o ceu e
    as defesas que o protegem; a fase profunda vai atras da retaguarda.
    """
    if fase is Fase.AR:
        return [
            u for u in forca.unidades
            if u.ativa and not u.tipo.consumivel
            and (u.tipo.dominio is Dominio.AR or u.tipo.papel is Papel.DEFESA_AEREA)
        ]
    if fase is Fase.PROFUNDO:
        return [
            u for u in forca.unidades
            if u.ativa and not u.tipo.consumivel
            and (
                u.tipo.papel in (Papel.LOGISTICA, Papel.DEFESA_AEREA)
                or u.tipo.dominio is Dominio.MAR
            )
        ]
    if fase is Fase.MAR:
        return forca.alvos((Dominio.MAR,))
    if fase is Fase.TERRA:
        # Comboio logistico nao esta na linha de frente: so e atingido na
        # fase de golpe profundo. Sem isso os caminhoes absorvem a guerra
        # inteira e nenhuma unidade de combate morre.
        return [
            u for u in forca.alvos((Dominio.TERRA,))
            if u.tipo.papel is not Papel.LOGISTICA
        ]
    return []


def _indexar_por_dominio(alvos: list[Unidade]) -> dict[Dominio, list[Unidade]]:
    indice: dict[Dominio, list[Unidade]] = {}
    for alvo in alvos:
        indice.setdefault(alvo.tipo.dominio, []).append(alvo)
    return indice


# ---------------------------------------------------------------------------
# Motor de engajamento
# ---------------------------------------------------------------------------

def engajar(
    atacante: ForcaArmada,
    defensor: ForcaArmada,
    fase: Fase,
    dados: Dados,
    intel: float,
    multiplicador: float = 1.0,
    interceptacao: float = 0.0,
) -> ResultadoEngajamento:
    """Resolve uma fase, unidade por unidade, de um lado contra o outro."""
    resultado = ResultadoEngajamento(fase=fase, atacante=atacante.dono)
    indice = _indexar_por_dominio(alvos_da_fase(defensor, fase))
    if not indice:
        return resultado

    atuantes = _racionar(atacante.atuantes(fase))
    dados.embaralhar(atuantes)

    # Um unico dado para a fase inteira, alem do dado por unidade. Com dez mil
    # unidades os dados individuais se cancelam pela lei dos grandes numeros e
    # toda batalha vira o mesmo resultado; a sorte do dia precisa existir no
    # nivel da campanha para o combate ainda ter historia.
    sorte_da_fase = dados.fator(intel)

    for unidade in atuantes:
        dominios = [d for d in indice if unidade.tipo.poder_contra(d) > 0]
        if not dominios:
            continue

        if unidade.tipo.consumivel:
            unidade.gasta = True
            if interceptacao > 0 and dados.chance(
                _chance_de_interceptacao(unidade, interceptacao)
            ):
                resultado.interceptados += 1
                continue

        dominio = dominios[0] if len(dominios) == 1 else _melhor_dominio(
            unidade, dominios
        )
        alvo = dados.escolher_alvo(indice[dominio], intel)
        if alvo is None:
            del indice[dominio]
            if not indice:
                break
            continue

        eficacia = unidade.eficacia(
            atacante.moral, atacante.treinamento, atacante.bonus_unidade(unidade)
        )
        poder = (
            unidade.tipo.poder_contra(dominio)
            * eficacia * multiplicador * sorte_da_fase
        )
        if poder <= 0:
            continue

        resultado.ataques += 1
        atingidos = [alvo]
        for _ in range(unidade.tipo.alvos_atingidos - 1):
            extra = dados.escolher_alvo(indice.get(dominio, []), intel)
            if extra is None:
                break
            atingidos.append(extra)

        for atingido in atingidos:
            dano = poder * dados.fator(intel) / (1.0 + atingido.tipo.defesa)
            resultado.dano_causado += min(dano, atingido.resistencia_atual)
            if atingido.receber_dano(dano):
                resultado.registrar_baixa(atingido)

    return resultado


def _racionar(atuantes: list[Unidade]) -> list[Unidade]:
    """Limita quanto de cada estoque de consumivel entra nesta rodada."""
    permanentes = [u for u in atuantes if not u.tipo.consumivel]
    por_tipo: dict[str, list[Unidade]] = {}
    for u in atuantes:
        if u.tipo.consumivel:
            por_tipo.setdefault(u.tipo.chave, []).append(u)
    for estoque in por_tipo.values():
        limite = max(1, ceil(len(estoque) * SALVA_MAXIMA))
        permanentes.extend(estoque[:limite])
    return permanentes


def _melhor_dominio(unidade: Unidade, dominios: list[Dominio]) -> Dominio:
    return max(dominios, key=unidade.tipo.poder_contra)


def _chance_de_interceptacao(unidade: Unidade, densidade: float) -> float:
    """Alvo mais furtivo (assinatura baixa) e mais dificil de abater."""
    exposicao = 0.40 + 0.15 * unidade.tipo.assinatura
    return max(0.0, min(0.95, densidade * exposicao))


# ---------------------------------------------------------------------------
# Fases
# ---------------------------------------------------------------------------

def fase_inteligencia(a: ForcaArmada, b: ForcaArmada) -> tuple[float, float]:
    """Cobertura de inteligencia de cada lado (0..1).

    Medida absoluta: sensores proprios contra o tamanho do que precisa ser
    vigiado do outro lado. Nao e uma divisao de 100% entre os dois - perder
    o reconhecimento inimigo nao deixa ninguem onisciente.
    """
    def cobertura(observador: ForcaArmada, observado: ForcaArmada) -> float:
        sensores = observador.deteccao_total()
        alvo = observado.assinatura_total() * PESO_ASSINATURA_INIMIGA
        if sensores + alvo <= 0:
            return 0.5
        return sensores / (sensores + alvo)

    return cobertura(a, b), cobertura(b, a)


def poder_aereo(forca: ForcaArmada) -> float:
    """Capacidade de disputar o ceu, somando a projecao dos porta-avioes."""
    bruto = sum(
        u.tipo.poder_contra(Dominio.AR)
        * u.eficacia(forca.moral, forca.treinamento)
        for u in forca.disponiveis()
    )
    return bruto * (1.0 + forca.projecao_aerea())


def multiplicador_de_supremacia(supremacia: float) -> float:
    """0.5 quando o ceu e do inimigo, 1.0 em paridade, 1.6 com dominio total."""
    return max(0.5, min(1.6, 1.0 + 1.2 * (supremacia - 0.5)))


def densidade_antiaerea(forca: ForcaArmada) -> float:
    poder = sum(
        u.tipo.poder_contra(Dominio.AR)
        * u.eficacia(forca.moral, forca.treinamento)
        for u in forca.disponiveis()
        if u.tipo.papel is Papel.DEFESA_AEREA or u.tipo.dominio is Dominio.MAR
    )
    return poder / (poder + SATURACAO_ANTIAEREA)


def controle_maritimo(a: ForcaArmada, b: ForcaArmada) -> str | None:
    """Devolve o nome de quem impoe bloqueio, se alguem impuser."""
    navais_a = [u for u in a.disponiveis() if u.tipo.dominio is Dominio.MAR]
    navais_b = [u for u in b.disponiveis() if u.tipo.dominio is Dominio.MAR]
    if navais_a and not navais_b:
        return a.dono
    if navais_b and not navais_a:
        return b.dono
    return None
