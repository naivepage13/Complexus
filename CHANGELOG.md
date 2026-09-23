# Changelog

Formato baseado em [Keep a Changelog](https://keepachangelog.com/pt-BR/1.1.0/).

## [0.2.0] — 2026-09-22

### Adicionado
- Sistema de combate em `combate/`: resolução por fases (inteligência, ar,
  golpe profundo, mar, terra) com cada plataforma como entidade individual.
- **Serviço HTTP** `combate/servico.py` no molde do `analytics/`: só biblioteca
  padrão, sem estado, porta 8200, com `/saude`, `/batalha` e `/ordem-de-batalha`.
  Sem fallback em Java — batalha pendente resolve no turno seguinte, para não
  existirem duas regras divergentes.
- `combate/paises.json` como fixture para uso standalone, com os campos novos
  `defesas.baterias_antiaereas` e `doutrina`. Fora de `data/`, que o
  `.gitignore` do projeto ignora.
- Catálogo de tipos de unidade (`catalogo.py`) mapeado campo a campo para o
  inventário militar de `stats.html`.
- Logística com duas restrições (transporte e verba) acopladas às unidades
  sobreviventes.
- Gate nuclear: ogivas só entram na ordem de batalha com
  `doutrina.permitir_nuclear`, e o uso custa 25 pontos de estabilidade.
- CLI `python -m combate.cli` com saída narrada ou JSON.
- `docs/REGRAS-DE-COMBATE.md` com a especificação completa.
- 31 testes em `tests/`, incluindo o contrato de fio do serviço HTTP
  exercitado contra um servidor real em porta efêmera.

### Alterado
- Chaves do relatório JSON passam a **camelCase** (`frenteFinal`,
  `baixasHumanas`), acompanhando o contrato que o backend Java já usa com o
  serviço analítico.
- `engine_combate.py`, `main.py` e `pais.py` movidos para `legado/`, no mesmo
  caminho e byte a byte iguais aos da branch do backend, para o merge não
  conflitar.
- `.gitignore` copiado da branch do backend; `__pycache__` deixou de ser
  rastreado.

### Notas
- O frontend ainda não consome o serviço.
- Falta o lado Java: `ClienteCombate` e a fila de batalhas pendentes.
