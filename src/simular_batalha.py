"""CLI de simulacao de batalha.

    python src/simular_batalha.py Brasil Argentina
    python src/simular_batalha.py Brasil Argentina --rodadas 8 --semente 42
    python src/simular_batalha.py Brasil Argentina --json > batalha.json
"""

from __future__ import annotations

import argparse
import json
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))

from combate import carregar_paises, resolver_guerra  # noqa: E402


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description="Simula uma guerra entre duas nacoes.")
    parser.add_argument("atacante")
    parser.add_argument("defensor")
    parser.add_argument("--rodadas", type=int, default=5)
    parser.add_argument("--semente", type=int, default=None)
    parser.add_argument(
        "--distancia", type=float, default=1.8,
        help="Multiplicador logistico do atacante (1.0 = guerra na fronteira).",
    )
    parser.add_argument("--json", action="store_true", help="Saida em JSON.")
    args = parser.parse_args(argv)

    if hasattr(sys.stdout, "reconfigure"):
        sys.stdout.reconfigure(encoding="utf-8")

    paises = carregar_paises()
    faltando = [n for n in (args.atacante, args.defensor) if n not in paises]
    if faltando:
        print(
            f"Pais nao encontrado: {', '.join(faltando)}. "
            f"Disponiveis: {', '.join(sorted(paises))}",
            file=sys.stderr,
        )
        return 1

    relatorio = resolver_guerra(
        paises[args.atacante],
        paises[args.defensor],
        rodadas=args.rodadas,
        semente=args.semente,
        distancia=args.distancia,
    )

    if args.json:
        print(json.dumps(relatorio.para_dicionario(), ensure_ascii=False, indent=2))
    else:
        print(relatorio.narrar())
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
