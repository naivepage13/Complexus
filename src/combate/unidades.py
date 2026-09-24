"""Entidades individuais de combate e montagem da ordem de batalha (ORBAT).

Cada plataforma (tanque, caca, navio, drone, missil) vira um objeto
``Unidade`` proprio, com resistencia e suprimento rastreados separadamente.
Infantaria e agrupada em companhias (ver ``SOLDADOS_POR_COMPANHIA``).
"""

from __future__ import annotations

from dataclasses import dataclass, field
from itertools import count
from typing import Iterable, Iterator, Mapping, Sequence

from .catalogo import (
    CATALOGO,
    MAPA_INVENTARIO,
    SOLDADOS_POR_COMPANHIA,
    Dominio,
    Fase,
    Papel,
    TipoUnidade,
)

# Abaixo desta integridade a unidade e retirada de combate. Nenhuma formacao
# continua lutando com 5% da capacidade - ela quebra muito antes disso.
LIMIAR_INEFICACIA = 0.20

_proximo_id = count(1)


@dataclass(slots=True)
class Unidade:
    """Uma entidade individual no campo de batalha."""

    id: int
    tipo: TipoUnidade
    dono: str
    resistencia_atual: float
    suprimento: float = 1.0
    ativa: bool = True
    gasta: bool = False  # consumiveis ja empregados

    @property
    def integridade(self) -> float:
        if self.tipo.resistencia <= 0:
            return 0.0
        return max(0.0, self.resistencia_atual / self.tipo.resistencia)

    @property
    def disponivel(self) -> bool:
        return self.ativa and not self.gasta

    def eficacia(self, moral: float, treinamento: float, bonus: float = 1.0) -> float:
        """Fracao da capacidade nominal que a unidade consegue entregar."""
        return self.integridade * self.suprimento * moral * treinamento * bonus

    def receber_dano(self, dano: float) -> bool:
        """Aplica dano. Devolve ``True`` se a unidade saiu de combate agora."""
        if not self.ativa:
            return False
        self.resistencia_atual -= dano
        if self.integridade <= LIMIAR_INEFICACIA:
            self.resistencia_atual = max(0.0, self.resistencia_atual)
            self.ativa = False
            return True
        return False


@dataclass
class ForcaArmada:
    """Conjunto de unidades de uma nacao, com os indices usados pelas fases."""

    dono: str
    unidades: list[Unidade] = field(default_factory=list)
    cobertura_equipamento: float = 1.0
    moral: float = 1.0
    treinamento: float = 1.0
    suprimento_global: float = 1.0

    # -- consultas ---------------------------------------------------------
    def disponiveis(self) -> Iterator[Unidade]:
        return (u for u in self.unidades if u.disponivel)

    def atuantes(self, fase: Fase) -> list[Unidade]:
        """Unidades que participam ativamente desta fase."""
        return [u for u in self.unidades if u.disponivel and fase in u.tipo.fases]

    def alvos(self, dominios: Sequence[Dominio]) -> list[Unidade]:
        """Unidades que podem ser atingidas nos dominios informados.

        Consumiveis nao sao alvo: nao existem como plataforma no campo.
        """
        permitidos = set(dominios)
        return [
            u for u in self.unidades
            if u.ativa and not u.tipo.consumivel and u.tipo.dominio in permitidos
        ]

    def bonus_unidade(self, unidade: Unidade) -> float:
        """Modificador especifico do tipo (hoje so cobertura de equipamento)."""
        if unidade.tipo.chave.startswith("infantaria"):
            return 0.5 + 0.5 * self.cobertura_equipamento
        return 1.0

    # -- agregados ---------------------------------------------------------
    def deteccao_total(self) -> float:
        return sum(
            u.tipo.deteccao * u.eficacia(self.moral, self.treinamento)
            for u in self.disponiveis()
        )

    def capacidade_logistica(self) -> float:
        return sum(
            u.tipo.capacidade_logistica * u.integridade
            for u in self.unidades
            if u.ativa and u.tipo.papel is Papel.LOGISTICA
        )

    def consumo_total(self) -> float:
        return sum(u.tipo.consumo for u in self.unidades if u.ativa)

    def custo_operacional(self) -> float:
        return sum(u.tipo.custo_operacional for u in self.unidades if u.ativa)

    def projecao_aerea(self) -> float:
        return sum(
            u.tipo.projecao_aerea * u.integridade
            for u in self.unidades if u.ativa
        )

    def assinatura_total(self) -> float:
        """Quanto desta forca existe para ser visto pelo inimigo."""
        return sum(u.tipo.assinatura for u in self.unidades if u.ativa)

    def efetivos_vivos(self) -> float:
        """Efetivos ainda em combate, ponderados pela integridade.

        Uma companhia a 40% de integridade nao perdeu 0 homens nem 100:
        perdeu 60. Baixas graduadas em vez de so contar unidades destruidas.
        """
        return sum(u.tipo.efetivos * u.integridade for u in self.unidades)

    def contagem(self) -> dict[str, int]:
        saida: dict[str, int] = {}
        for u in self.unidades:
            if u.ativa and not u.gasta:
                saida[u.tipo.chave] = saida.get(u.tipo.chave, 0) + 1
        return saida

    def aniquilada(self) -> bool:
        return not any(
            u.disponivel and u.tipo.papel is Papel.COMBATE for u in self.unidades
        )


def _valor(inventario: Mapping, caminho: str) -> int:
    grupo, campo = caminho.split(".")
    return int(inventario.get(grupo, {}).get(campo, 0) or 0)


def criar_forca(pais: Mapping) -> ForcaArmada:
    """Monta a ordem de batalha de um pais a partir de ``data/paises.json``."""
    inventario = pais.get("militar", {})
    doutrina = pais.get("doutrina", {})
    mobilizacao = float(doutrina.get("mobilizacao_reserva", 0.30))

    ativos = _valor(inventario, "terrestre.soldados_ativos")
    equipamento = _valor(inventario, "terrestre.equipamento_infantaria")
    cobertura = min(1.0, equipamento / ativos) if ativos else 1.0

    forca = ForcaArmada(
        dono=pais["nome"],
        cobertura_equipamento=cobertura,
        moral=float(pais.get("moral", 1.0)),
        treinamento=float(doutrina.get("qualidade_treinamento", 1.0)),
    )

    for caminho, chave in MAPA_INVENTARIO.items():
        tipo = CATALOGO[chave]
        bruto = _valor(inventario, caminho)
        if bruto <= 0:
            continue

        if tipo.efetivos == SOLDADOS_POR_COMPANHIA:
            quantidade = bruto // SOLDADOS_POR_COMPANHIA
        else:
            quantidade = bruto

        if tipo.mobilizavel:
            quantidade = int(quantidade * mobilizacao)

        if tipo.gate_nuclear and not doutrina.get("permitir_nuclear", False):
            continue

        forca.unidades.extend(
            Unidade(
                id=next(_proximo_id),
                tipo=tipo,
                dono=forca.dono,
                resistencia_atual=float(tipo.resistencia),
            )
            for _ in range(quantidade)
        )

    return forca


def resumo_por_tipo(unidades: Iterable[Unidade]) -> dict[str, int]:
    saida: dict[str, int] = {}
    for u in unidades:
        saida[u.tipo.chave] = saida.get(u.tipo.chave, 0) + 1
    return saida
