"""Relatorio de batalha: o que o motor entrega para o jogo.

Duas saidas do mesmo objeto:

* ``narrar()`` - texto para o console de [index.html] e para o log do jogo.
* ``para_dicionario()`` - JSON para a API e para o frontend. As chaves saem
  em camelCase para acompanhar o contrato ja usado entre o backend Java e o
  servico analitico (``choqueDemanda``, ``retornoTotal``).
"""

from __future__ import annotations

from dataclasses import dataclass, field

from .catalogo import CATALOGO
from .fases import ResultadoRodada


@dataclass
class BalancoNacional:
    nome: str
    efetivos_iniciais: float = 0.0
    efetivos_finais: float = 0.0
    unidades_iniciais: int = 0
    unidades_finais: int = 0
    perdas_por_tipo: dict[str, int] = field(default_factory=dict)
    custo_total: float = 0.0
    tesouro_inicial: float = 0.0
    tesouro_final: float = 0.0
    estabilidade_inicial: float = 0.0
    estabilidade_final: float = 0.0
    moral_final: float = 1.0
    suprimento_final: float = 1.0

    @property
    def baixas_humanas(self) -> int:
        return int(max(0.0, self.efetivos_iniciais - self.efetivos_finais))

    @property
    def unidades_perdidas(self) -> int:
        return max(0, self.unidades_iniciais - self.unidades_finais)

    def para_dicionario(self) -> dict:
        return {
            "nome": self.nome,
            "baixasHumanas": self.baixas_humanas,
            "unidadesPerdidas": self.unidades_perdidas,
            "perdasPorTipo": dict(self.perdas_por_tipo),
            "efetivos": {
                "inicial": int(self.efetivos_iniciais),
                "final": int(self.efetivos_finais),
            },
            "unidades": {
                "inicial": self.unidades_iniciais,
                "final": self.unidades_finais,
            },
            "custoTotal": round(self.custo_total, 3),
            "tesouro": {
                "inicial": round(self.tesouro_inicial, 3),
                "final": round(self.tesouro_final, 3),
            },
            "estabilidade": {
                "inicial": round(self.estabilidade_inicial, 1),
                "final": round(self.estabilidade_final, 1),
            },
            "moralFinal": round(self.moral_final, 3),
            "suprimentoFinal": round(self.suprimento_final, 3),
        }


@dataclass
class RelatorioBatalha:
    atacante: BalancoNacional
    defensor: BalancoNacional
    rodadas: list[ResultadoRodada] = field(default_factory=list)
    frente_final: float = 50.0
    veredito: str = "impasse"
    detalhe: str = ""
    nuclear_empregado: list[str] = field(default_factory=list)
    semente: int | None = None

    # -- texto -------------------------------------------------------------
    def narrar(self) -> str:
        linhas: list[str] = []
        ata, defe = self.atacante.nome, self.defensor.nome
        linhas.append(f"=== {ata} x {defe} ===")
        if self.semente is not None:
            linhas.append(f"semente: {self.semente}")

        for rodada in self.rodadas:
            linhas.append("")
            linhas.append(f"-- Rodada {rodada.numero} --")
            linhas.append(
                f"inteligencia: {ata} {rodada.intel.get(ata, 0):.0%} | "
                f"{defe} {rodada.intel.get(defe, 0):.0%}"
            )
            if rodada.surpresa:
                linhas.append(f"surpresa operacional para {rodada.surpresa}")
            linhas.append(
                f"supremacia aerea: {ata} {rodada.supremacia_aerea.get(ata, 0):.0%} | "
                f"{defe} {rodada.supremacia_aerea.get(defe, 0):.0%}"
            )
            for eng in rodada.engajamentos:
                if eng.ataques == 0 and eng.interceptados == 0:
                    continue
                perdas = _formatar_perdas(eng.destruidas)
                intercept = (
                    f", {eng.interceptados} interceptados"
                    if eng.interceptados else ""
                )
                linhas.append(
                    f"  [{eng.fase.value}] {eng.atacante}: {eng.ataques} ataques"
                    f"{intercept}, {_milhar(round(eng.dano_causado))} de dano"
                    f" -> {perdas}"
                )
            if rodada.bloqueio:
                linhas.append(f"bloqueio naval imposto por {rodada.bloqueio}")
            linhas.append(f"linha de frente: {self._barra(rodada)}")

        linhas.append("")
        if self.nuclear_empregado:
            linhas.append(
                "ARMA NUCLEAR EMPREGADA POR: " + ", ".join(self.nuclear_empregado)
            )
        linhas.append(f"=== {self.veredito.upper()} ===")
        linhas.append(self.detalhe)
        linhas.append("")
        linhas.append(self._bloco_balanco(self.atacante))
        linhas.append(self._bloco_balanco(self.defensor))
        return "\n".join(linhas)

    def _barra(self, rodada: ResultadoRodada) -> str:
        return f"{rodada.movimento_frente:+.1f} pontos"

    def _bloco_balanco(self, b: BalancoNacional) -> str:
        perdas = _formatar_perdas(b.perdas_por_tipo, limite=6)
        return (
            f"{b.nome}: {_milhar(b.baixas_humanas)} baixas humanas | "
            f"{_milhar(b.unidades_perdidas)} unidades perdidas | "
            f"custo ${b.custo_total:.2f}B | "
            f"estabilidade {b.estabilidade_inicial:.0f} -> {b.estabilidade_final:.0f}\n"
            f"  perdas: {perdas}"
        )

    # -- dados -------------------------------------------------------------
    def para_dicionario(self) -> dict:
        return {
            "veredito": self.veredito,
            "detalhe": self.detalhe,
            "frenteFinal": round(self.frente_final, 1),
            "nuclearEmpregado": list(self.nuclear_empregado),
            "semente": self.semente,
            "atacante": self.atacante.para_dicionario(),
            "defensor": self.defensor.para_dicionario(),
            "rodadas": [
                {
                    "numero": r.numero,
                    "intel": {k: round(v, 3) for k, v in r.intel.items()},
                    "supremaciaAerea": {
                        k: round(v, 3) for k, v in r.supremacia_aerea.items()
                    },
                    "surpresa": r.surpresa,
                    "bloqueio": r.bloqueio,
                    "movimentoFrente": round(r.movimento_frente, 2),
                    "engajamentos": [
                        {
                            "fase": e.fase.value,
                            "atacante": e.atacante,
                            "ataques": e.ataques,
                            "interceptados": e.interceptados,
                            "dano": round(e.dano_causado, 1),
                            "destruidas": dict(e.destruidas),
                        }
                        for e in r.engajamentos
                    ],
                }
                for r in self.rodadas
            ],
        }


def _milhar(valor: int) -> str:
    """Separador de milhar no padrao pt-BR."""
    return f"{valor:,}".replace(",", ".")


def _formatar_perdas(perdas: dict[str, int], limite: int = 4) -> str:
    if not perdas:
        return "nenhuma perda"
    itens = sorted(perdas.items(), key=lambda kv: kv[1], reverse=True)
    partes = [
        f"{quantidade}x {CATALOGO[chave].nome}" if chave in CATALOGO
        else f"{quantidade}x {chave}"
        for chave, quantidade in itens[:limite]
    ]
    if len(itens) > limite:
        partes.append(f"+{len(itens) - limite} tipos")
    return ", ".join(partes)
