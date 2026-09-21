# Changelog

Formato baseado em [Keep a Changelog](https://keepachangelog.com/pt-BR/1.1.0/).

## [0.2.0] — 2026-09-21

### Adicionado
- Sistema de combate em `src/combate/`: resolução por fases (inteligência, ar,
  golpe profundo, mar, terra) com cada plataforma como entidade individual.
- `data/paises.json` como fonte única do estado das nações, com os campos novos
  `defesas.baterias_antiaereas` e `doutrina`.
- Catálogo de tipos de unidade (`catalogo.py`) mapeado campo a campo para o
  inventário militar de `stats.html`.
- Logística com duas restrições (transporte e verba) acopladas às unidades
  sobreviventes.
- Gate nuclear: ogivas só entram na ordem de batalha com
  `doutrina.permitir_nuclear`, e o uso custa 25 pontos de estabilidade.
- CLI `src/simular_batalha.py` com saída narrada ou JSON.
- `docs/REGRAS-DE-COMBATE.md` com a especificação completa.
- 19 testes em `tests/test_combate.py`.

### Notas
- `src/engine_combate.py`, `src/main.py` e `src/pais.py` seguem no repositório
  como legado e foram substituídos pelo novo motor.
- O frontend ainda não consome o motor.
