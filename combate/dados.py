"""Aleatoriedade do combate.

Regra de ouro: o dado nunca decide a batalha, so desloca o resultado dentro de
uma faixa. Quanto melhor a inteligencia, mais estreita a faixa - informacao boa
compra previsibilidade, nao poder de fogo.
"""

from __future__ import annotations

import random
from .unidades import Unidade

# Amplitude maxima do dado quando a nacao esta cega (intel = 0).
AMPLITUDE_BASE = 0.45
# Fracao da amplitude que a inteligencia perfeita consegue eliminar.
REDUCAO_MAXIMA = 0.70
# Quantos alvos o atacante consegue comparar antes de escolher.
CANDIDATOS_POR_ATAQUE = 5


class Dados:
    def __init__(self, semente: int | None = None) -> None:
        self._rng = random.Random(semente)

    def fator(self, intel: float = 0.0) -> float:
        """Multiplicador de dano em torno de 1.0, modulado pela inteligencia."""
        amplitude = AMPLITUDE_BASE * (1 - REDUCAO_MAXIMA * _clamp(intel))
        return 1.0 + self._rng.uniform(-amplitude, amplitude)

    def chance(self, probabilidade: float) -> bool:
        return self._rng.random() < _clamp(probabilidade)

    def escolher_alvo(
        self, alvos: list[Unidade], intel: float = 0.0
    ) -> Unidade | None:
        """Sorteia candidatos e escolhe o mais visivel/ameacador entre eles.

        Com intel alta o atacante compara mais candidatos, entao acerta com
        mais frequencia o alvo que importa.

        A lista e limpa no caminho: unidades ja destruidas sao removidas em
        O(1) ao serem sorteadas. Sem isso, um campo cheio de destrocos faz
        quase todo ataque cair no vazio.
        """
        candidatos = 1 + int(CANDIDATOS_POR_ATAQUE * _clamp(intel) * 2)
        melhor: Unidade | None = None
        melhor_valor = -1.0
        avaliados = 0
        while avaliados < candidatos and alvos:
            indice = self._rng.randrange(len(alvos))
            candidato = alvos[indice]
            if not candidato.ativa:
                alvos[indice] = alvos[-1]
                alvos.pop()
                continue
            avaliados += 1
            valor = _valor_do_alvo(candidato)
            if valor > melhor_valor:
                melhor, melhor_valor = candidato, valor
        return melhor

    def embaralhar(self, sequencia: list) -> None:
        self._rng.shuffle(sequencia)


def _valor_do_alvo(unidade: Unidade) -> float:
    ameaca = max(unidade.tipo.ataque.values(), default=0.0)
    return unidade.tipo.assinatura * (1.0 + ameaca / 40.0) * unidade.integridade


def _clamp(valor: float, minimo: float = 0.0, maximo: float = 1.0) -> float:
    return max(minimo, min(maximo, valor))
