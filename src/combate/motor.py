"""Orquestracao da guerra: junta ordem de batalha, logistica, fases e relatorio.

Ponto de entrada unico do sistema de combate:

    >>> from combate import carregar_paises, resolver_guerra
    >>> paises = carregar_paises()
    >>> rel = resolver_guerra(paises["Brasil"], paises["Argentina"], semente=7)
    >>> print(rel.narrar())

O motor resolve sozinho: nao existe arbitragem manual. Para reproduzir uma
batalha basta a mesma semente e os mesmos dados de entrada.
"""

from __future__ import annotations

import json
from dataclasses import dataclass
from pathlib import Path
from typing import Mapping

from . import logistica
from .catalogo import Fase
from .dados import Dados
from .fases import (
    ResultadoRodada,
    controle_maritimo,
    densidade_antiaerea,
    engajar,
    fase_inteligencia,
    multiplicador_de_supremacia,
    poder_aereo,
)
from .relatorio import BalancoNacional, RelatorioBatalha
from .unidades import ForcaArmada, criar_forca

CAMINHO_PADRAO = Path(__file__).resolve().parents[2] / "data" / "paises.json"

RODADAS_PADRAO = 5
DISTANCIA_ATACANTE = 1.8  # campanha fora de casa custa mais logistica
VANTAGEM_PARA_SURPRESA = 0.25
BONUS_SURPRESA = 1.30
MOVIMENTO_MAXIMO_POR_RODADA = 12.0
DESGASTE_MORAL = 0.15
MORAL_MINIMA = 0.40


@dataclass
class Beligerante:
    """Estado mutavel de uma nacao durante a guerra."""

    nome: str
    forca: ForcaArmada
    tesouro: float
    estabilidade: float
    distancia: float
    tesouro_inicial: float
    estabilidade_inicial: float
    efetivos_iniciais: int
    unidades_iniciais: int
    gasto_acumulado: float = 0.0

    @classmethod
    def criar(cls, pais: Mapping, distancia: float) -> "Beligerante":
        forca = criar_forca(pais)
        tesouro = float(pais.get("tesouro", 0.0))
        estabilidade = float(pais.get("estabilidade", 100.0))
        return cls(
            nome=pais["nome"],
            forca=forca,
            tesouro=tesouro,
            estabilidade=estabilidade,
            distancia=distancia,
            tesouro_inicial=tesouro,
            estabilidade_inicial=estabilidade,
            efetivos_iniciais=forca.efetivos_vivos(),
            unidades_iniciais=sum(1 for u in forca.unidades if u.ativa),
        )


def carregar_paises(caminho: Path | str = CAMINHO_PADRAO) -> dict[str, dict]:
    """Le ``data/paises.json`` e devolve os paises indexados por nome."""
    dados = json.loads(Path(caminho).read_text(encoding="utf-8"))
    return {p["nome"]: p for p in dados["paises"]}


def resolver_guerra(
    atacante: Mapping,
    defensor: Mapping,
    rodadas: int = RODADAS_PADRAO,
    semente: int | None = None,
    distancia: float = DISTANCIA_ATACANTE,
) -> RelatorioBatalha:
    """Resolve uma guerra completa e devolve o relatorio de batalha."""
    dados = Dados(semente)
    a = Beligerante.criar(atacante, distancia)
    d = Beligerante.criar(defensor, 1.0)

    frente = 50.0
    registros: list[ResultadoRodada] = []
    nuclear: list[str] = []
    perdas_a: dict[str, int] = {}
    perdas_d: dict[str, int] = {}

    for numero in range(1, rodadas + 1):
        rodada = ResultadoRodada(numero=numero)

        # --- logistica (antes de tudo: define a eficacia da rodada) --------
        for lado in (a, d):
            estado = logistica.avaliar(lado.forca, lado.tesouro, lado.distancia)
            logistica.aplicar(lado.forca, estado)
            lado.tesouro = max(0.0, lado.tesouro - estado.custo_rodada)
            lado.gasto_acumulado += estado.custo_rodada

        # --- fase 0: inteligencia ------------------------------------------
        intel_a, intel_d = fase_inteligencia(a.forca, d.forca)
        rodada.intel = {a.nome: intel_a, d.nome: intel_d}

        bonus_a = bonus_d = 1.0
        if intel_a - intel_d >= VANTAGEM_PARA_SURPRESA:
            rodada.surpresa, bonus_a = a.nome, BONUS_SURPRESA
        elif intel_d - intel_a >= VANTAGEM_PARA_SURPRESA:
            rodada.surpresa, bonus_d = d.nome, BONUS_SURPRESA

        # --- fase 1: campanha aerea ----------------------------------------
        # Quem tem melhor inteligencia abre a fase: ve primeiro, atira primeiro.
        ordem = [(a, d, intel_a, bonus_a), (d, a, intel_d, bonus_d)]
        if intel_d > intel_a:
            ordem.reverse()
        for ofensor, alvo, intel, bonus in ordem:
            rodada.engajamentos.append(
                engajar(ofensor.forca, alvo.forca, Fase.AR, dados, intel, bonus)
            )

        pa, pd = poder_aereo(a.forca), poder_aereo(d.forca)
        total_ar = pa + pd
        sup_a = pa / total_ar if total_ar > 0 else 0.5
        sup_d = 1.0 - sup_a if total_ar > 0 else 0.5
        rodada.supremacia_aerea = {a.nome: sup_a, d.nome: sup_d}
        mult_a = multiplicador_de_supremacia(sup_a) * bonus_a
        mult_d = multiplicador_de_supremacia(sup_d) * bonus_d

        # --- fase 2: golpe profundo ----------------------------------------
        aa_a = densidade_antiaerea(a.forca)
        aa_d = densidade_antiaerea(d.forca)
        rodada.engajamentos.append(
            engajar(a.forca, d.forca, Fase.PROFUNDO, dados, intel_a, mult_a, aa_d)
        )
        rodada.engajamentos.append(
            engajar(d.forca, a.forca, Fase.PROFUNDO, dados, intel_d, mult_d, aa_a)
        )
        _registrar_nuclear(a.forca, nuclear)
        _registrar_nuclear(d.forca, nuclear)

        # --- fase 3: mar ----------------------------------------------------
        rodada.engajamentos.append(
            engajar(a.forca, d.forca, Fase.MAR, dados, intel_a, mult_a)
        )
        rodada.engajamentos.append(
            engajar(d.forca, a.forca, Fase.MAR, dados, intel_d, mult_d)
        )
        rodada.bloqueio = controle_maritimo(a.forca, d.forca)

        # --- fase 4: terra --------------------------------------------------
        terra_a = engajar(a.forca, d.forca, Fase.TERRA, dados, intel_a, mult_a)
        terra_d = engajar(d.forca, a.forca, Fase.TERRA, dados, intel_d, mult_d)
        rodada.engajamentos.extend((terra_a, terra_d))

        movimento = _movimento_de_frente(terra_a.dano_causado, terra_d.dano_causado)
        frente = max(0.0, min(100.0, frente + movimento))
        rodada.movimento_frente = movimento

        # --- desgaste -------------------------------------------------------
        for lado, adversario in ((a, d), (d, a)):
            perdidas = adversario_causou(rodada, adversario.nome)
            _desgastar_moral(lado, perdidas)

        _acumular_perdas(rodada, causadas_por=d.nome, destino=perdas_a)
        _acumular_perdas(rodada, causadas_por=a.nome, destino=perdas_d)
        registros.append(rodada)

        if a.forca.aniquilada() or d.forca.aniquilada():
            break

    balanco_a = _fechar_balanco(a, perdas_a)
    balanco_d = _fechar_balanco(d, perdas_d)
    veredito, detalhe = _julgar(a, d, frente)
    _aplicar_estabilidade(balanco_a, balanco_d, a, d, veredito, nuclear)

    return RelatorioBatalha(
        atacante=balanco_a,
        defensor=balanco_d,
        rodadas=registros,
        frente_final=frente,
        veredito=veredito,
        detalhe=detalhe,
        nuclear_empregado=nuclear,
        semente=semente,
    )


# ---------------------------------------------------------------------------
# Auxiliares
# ---------------------------------------------------------------------------

def _movimento_de_frente(dano_atacante: float, dano_defensor: float) -> float:
    """Movimento da linha de frente pela razao de dano, nao pelo valor bruto.

    Usar a razao mantem a escala coerente entre uma escaramuca de fronteira e
    uma guerra continental: o que move a frente e a vantagem relativa.
    """
    total = dano_atacante + dano_defensor
    if total <= 0:
        return 0.0
    vantagem = dano_atacante / total - 0.5
    return max(
        -MOVIMENTO_MAXIMO_POR_RODADA,
        min(MOVIMENTO_MAXIMO_POR_RODADA, vantagem * 2 * MOVIMENTO_MAXIMO_POR_RODADA),
    )


def adversario_causou(rodada: ResultadoRodada, nome_adversario: str) -> int:
    return rodada.baixas_de(nome_adversario)


def _acumular_perdas(
    rodada: ResultadoRodada, causadas_por: str, destino: dict[str, int]
) -> None:
    """Perdas sofridas por um lado = baixas causadas pelo adversario."""
    for eng in rodada.engajamentos:
        if eng.atacante != causadas_por:
            continue
        for chave, quantidade in eng.destruidas.items():
            destino[chave] = destino.get(chave, 0) + quantidade


def _desgastar_moral(lado: Beligerante, unidades_perdidas: int) -> None:
    vivas = max(1, sum(1 for u in lado.forca.unidades if u.ativa))
    fracao = unidades_perdidas / (vivas + unidades_perdidas)
    penalidade = DESGASTE_MORAL * fracao
    if lado.forca.suprimento_global < 0.6:
        penalidade += 0.05
    lado.forca.moral = max(MORAL_MINIMA, lado.forca.moral - penalidade)


def _registrar_nuclear(forca: ForcaArmada, registro: list[str]) -> None:
    if forca.dono in registro:
        return
    if any(u.tipo.gate_nuclear and u.gasta for u in forca.unidades):
        registro.append(forca.dono)


def _fechar_balanco(lado: Beligerante, perdas: dict[str, int]) -> BalancoNacional:
    return BalancoNacional(
        nome=lado.nome,
        efetivos_iniciais=lado.efetivos_iniciais,
        efetivos_finais=lado.forca.efetivos_vivos(),
        unidades_iniciais=lado.unidades_iniciais,
        unidades_finais=sum(1 for u in lado.forca.unidades if u.ativa),
        perdas_por_tipo=perdas,
        custo_total=lado.gasto_acumulado,
        tesouro_inicial=lado.tesouro_inicial,
        tesouro_final=lado.tesouro,
        estabilidade_inicial=lado.estabilidade_inicial,
        estabilidade_final=lado.estabilidade_inicial,
        moral_final=lado.forca.moral,
        suprimento_final=lado.forca.suprimento_global,
    )


def _julgar(a: Beligerante, d: Beligerante, frente: float) -> tuple[str, str]:
    if d.forca.aniquilada():
        return "vitoria decisiva", f"{d.nome} perdeu a capacidade de combate."
    if a.forca.aniquilada():
        return "derrota decisiva", f"{a.nome} perdeu a capacidade de combate."
    if frente >= 75:
        return "vitoria decisiva", f"{a.nome} rompeu a frente ({frente:.0f}/100)."
    if frente >= 60:
        return "vitoria tatica", f"{a.nome} ganhou terreno ({frente:.0f}/100)."
    if frente > 40:
        return "impasse", f"Guerra de atrito, frente estavel em {frente:.0f}/100."
    if frente > 25:
        return "ofensiva contida", f"{d.nome} conteve o avanco ({frente:.0f}/100)."
    return "derrota decisiva", f"{d.nome} contra-atacou ({frente:.0f}/100)."


def _aplicar_estabilidade(
    balanco_a: BalancoNacional,
    balanco_d: BalancoNacional,
    a: Beligerante,
    d: Beligerante,
    veredito: str,
    nuclear: list[str],
) -> None:
    """Custo politico interno da guerra, ja com a reacao ao uso nuclear."""
    ajuste = {
        "vitoria decisiva": (5.0, -18.0),
        "vitoria tatica": (2.0, -10.0),
        "impasse": (-6.0, -4.0),
        "ofensiva contida": (-12.0, 2.0),
        "derrota decisiva": (-20.0, 4.0),
    }[veredito]

    for balanco, politico in ((balanco_a, ajuste[0]), (balanco_d, ajuste[1])):
        sangria = 0.0
        if balanco.efetivos_iniciais:
            sangria = balanco.baixas_humanas / balanco.efetivos_iniciais * 80.0
        penalidade_nuclear = 25.0 if balanco.nome in nuclear else (
            5.0 if nuclear else 0.0
        )
        balanco.estabilidade_final = max(
            0.0,
            min(
                100.0,
                balanco.estabilidade_inicial + politico - sangria - penalidade_nuclear,
            ),
        )
