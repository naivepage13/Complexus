"""Logistica: suprimento e financiamento da guerra.

Evolucao da regra original de ``engine_combate.py`` (custo de 1% do poder
militar). Agora o suprimento depende de duas restricoes independentes:

* **transporte** - caminhoes e aeronaves de carga sobreviventes, divididos pela
  distancia ate a frente. Destruir a logistica inimiga na fase de golpe
  profundo derruba este numero de verdade.
* **verba** - o tesouro tem que cobrir o custo operacional da rodada.

A restricao mais apertada e a que manda. O suprimento se move gradualmente ate
o alvo: um exercito nao passa de abastecido a faminto em uma rodada.
"""

from __future__ import annotations

from dataclasses import dataclass

from .unidades import ForcaArmada

SUPRIMENTO_MINIMO = 0.25
VELOCIDADE_AJUSTE = 0.5  # fracao da diferenca absorvida por rodada


@dataclass
class EstadoLogistico:
    demanda: float
    oferta_transporte: float
    razao_transporte: float
    custo_rodada: float  # em bilhoes
    razao_verba: float
    suprimento_alvo: float
    suprimento_efetivo: float
    falta_verba: bool

    def resumo(self) -> str:
        alerta = " (sem verba)" if self.falta_verba else ""
        return (
            f"suprimento {self.suprimento_efetivo:.0%}{alerta} | "
            f"transporte {self.razao_transporte:.0%} | "
            f"custo ${self.custo_rodada:.2f}B"
        )


def avaliar(
    forca: ForcaArmada, tesouro: float, distancia: float = 1.0
) -> EstadoLogistico:
    """Calcula o estado logistico da rodada sem aplicar nada."""
    demanda = forca.consumo_total()
    oferta = forca.capacidade_logistica() / max(distancia, 0.1)
    razao_transporte = (oferta / demanda) if demanda > 0 else 1.0

    custo = forca.custo_operacional() / 1000.0  # $M -> $B
    razao_verba = (tesouro / custo) if custo > 0 else 1.0

    alvo = max(SUPRIMENTO_MINIMO, min(1.0, razao_transporte, razao_verba))
    atual = forca.suprimento_global
    efetivo = atual + (alvo - atual) * VELOCIDADE_AJUSTE

    return EstadoLogistico(
        demanda=demanda,
        oferta_transporte=oferta,
        razao_transporte=razao_transporte,
        custo_rodada=custo,
        razao_verba=razao_verba,
        suprimento_alvo=alvo,
        suprimento_efetivo=efetivo,
        falta_verba=razao_verba < 1.0,
    )


def aplicar(forca: ForcaArmada, estado: EstadoLogistico) -> None:
    """Propaga o suprimento calculado para cada unidade da forca."""
    forca.suprimento_global = estado.suprimento_efetivo
    for unidade in forca.unidades:
        if unidade.ativa:
            unidade.suprimento = estado.suprimento_efetivo
