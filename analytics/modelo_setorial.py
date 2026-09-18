"""Modelo de choques setoriais do GeoHistoricalSim.

Funcoes puras e deterministicas: o mesmo turno sempre produz os mesmos
choques, o que torna o resultado de uma partida reproduzivel e auditavel.

O calculo combina tres componentes:

1. sazonalidade anual (ciclo de 12 turnos, ja que 1 turno = 1 mes de jogo);
2. ruido pseudoaleatorio com semente derivada do turno e do setor;
3. reacao macro a inflacao e juros, especifica de cada setor.

Somente biblioteca padrao: o servico roda sem instalar dependencias.
"""

from __future__ import annotations

import math
import random
from dataclasses import dataclass, asdict

#: Parametros por setor, espelhando o enum Setor do backend Java.
SETORES: dict[str, dict[str, float]] = {
    "ALIMENTICIO": {
        "volatilidade": 0.05,
        "sazonalidade": 0.35,
        "sensibilidade_inflacao": 0.40,
        "sensibilidade_juros": 0.10,
    },
    "IMOBILIARIO": {
        "volatilidade": 0.11,
        "sazonalidade": 0.20,
        "sensibilidade_inflacao": 0.25,
        "sensibilidade_juros": 0.85,
    },
    "CONSTRUCAO": {
        "volatilidade": 0.09,
        "sazonalidade": 0.45,
        "sensibilidade_inflacao": 0.55,
        "sensibilidade_juros": 0.65,
    },
}

INFLACAO_NEUTRA = 0.04
JUROS_NEUTRO = 0.1075


@dataclass(frozen=True)
class Modificador:
    """Choque de um setor em um turno."""

    choqueDemanda: float
    choqueCusto: float
    confianca: float

    def como_dicionario(self) -> dict[str, float]:
        return asdict(self)


def _semente(turno: int, setor: str) -> random.Random:
    """Gerador estavel por turno e setor."""
    return random.Random(f"{turno}:{setor}")


def calcular_setor(
    setor: str,
    turno: int,
    inflacao_anual: float = INFLACAO_NEUTRA,
    taxa_juros: float = JUROS_NEUTRO,
) -> Modificador:
    """Calcula o choque de um setor em um turno."""
    if setor not in SETORES:
        raise ValueError(f"Setor desconhecido: {setor}")

    parametros = SETORES[setor]
    aleatorio = _semente(turno, setor)

    mes = turno % 12
    sazonal = math.sin(mes / 12 * 2 * math.pi) * parametros["volatilidade"] * parametros["sazonalidade"]
    ruido = (aleatorio.random() - 0.5) * 2 * parametros["volatilidade"]

    desvio_inflacao = inflacao_anual - INFLACAO_NEUTRA
    desvio_juros = taxa_juros - JUROS_NEUTRO

    choque_demanda = sazonal + ruido - desvio_juros * parametros["sensibilidade_juros"] * 0.8
    choque_custo = (
        (aleatorio.random() - 0.5) * parametros["volatilidade"]
        + desvio_inflacao * parametros["sensibilidade_inflacao"]
    )
    confianca = _limitar(1.0 + choque_demanda * 1.5 - desvio_juros * 0.5, 0.5, 1.5)

    return Modificador(
        choqueDemanda=round(choque_demanda, 6),
        choqueCusto=round(choque_custo, 6),
        confianca=round(confianca, 6),
    )


def calcular_todos(
    turno: int,
    inflacao_anual: float = INFLACAO_NEUTRA,
    taxa_juros: float = JUROS_NEUTRO,
) -> dict[str, dict[str, float]]:
    """Choques de todos os setores em um turno."""
    return {
        setor: calcular_setor(setor, turno, inflacao_anual, taxa_juros).como_dicionario()
        for setor in SETORES
    }


def projetar_investimento(
    preco_acao: float,
    lucro_mensal: float,
    payout: float,
    acoes_totais: int,
    crescimento_mensal: float,
    turnos: int = 12,
) -> dict[str, object]:
    """Projeta o retorno de uma posicao acionaria ao longo de N turnos.

    O preco acompanha o crescimento do lucro com amortecimento de 60 por cento,
    representando a convergencia gradual do mercado ao valor justo.
    """
    if acoes_totais <= 0:
        raise ValueError("acoes_totais deve ser positivo")

    preco = float(preco_acao)
    lucro = float(lucro_mensal)
    dividendos_acumulados = 0.0
    serie: list[dict[str, float]] = []

    for turno in range(1, max(turnos, 1) + 1):
        lucro *= 1 + crescimento_mensal
        dividendo_por_acao = max(lucro, 0.0) * payout / acoes_totais
        dividendos_acumulados += dividendo_por_acao
        preco *= 1 + crescimento_mensal * 0.6
        serie.append(
            {
                "turno": turno,
                "precoProjetado": round(preco, 6),
                "dividendoPorAcao": round(dividendo_por_acao, 6),
                "dividendosAcumulados": round(dividendos_acumulados, 6),
            }
        )

    retorno_total = 0.0
    if preco_acao > 0:
        retorno_total = (preco - preco_acao + dividendos_acumulados) / preco_acao

    return {
        "precoInicial": preco_acao,
        "precoFinal": round(preco, 6),
        "dividendosAcumulados": round(dividendos_acumulados, 6),
        "retornoTotal": round(retorno_total, 6),
        "serie": serie,
    }


def _limitar(valor: float, minimo: float, maximo: float) -> float:
    return max(minimo, min(maximo, valor))
