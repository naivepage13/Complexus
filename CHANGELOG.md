# Changelog

Formato baseado em [Keep a Changelog](https://keepachangelog.com/pt-BR/1.1.0/).

## [0.3.0] — 2026-09-23

### Adicionado
- Mapa interativo hierárquico (`mapa.html` + `mapa.js`) com zoom semântico em
  três níveis — Estados, Cidades e Estradas — usando Leaflet.
- Lazy loading espacial client-side: cada camada só materializa as features
  cuja bbox cruza a viewport atual e cujo zoom está na faixa configurada
  (`criarCamadaLazy` em `mapa.js`); ao sair da vista a layer é removida, não
  só escondida.
- Drill-down: clicar num estado dá `flyTo` para o aglomerado de cidades
  filhas (não o centro geométrico do polígono) e revela a camada de cidades;
  clicar numa cidade abre o Painel de Decisão com PIB municipal,
  infraestrutura e tropas guarnecidas. Zoom máximo travado no nível de
  Cidades (sem zoom de rua).
- Camada logística de estradas (`LineString`) com hover (espessura) e clique
  para abrir decisões de "Expandir Rodovia" e "Bloquear Suprimentos".
- Herança de estado: alíquota estadual e investimento em infraestrutura são
  sempre recalculados a partir do estado pai (nunca guardados na cidade) —
  mudar a alíquota de um estado atualiza o PIB efetivo de todas as cidades
  filhas em tempo real; bloquear uma rodovia reduz a infraestrutura efetiva
  das cidades conectadas.
- Dados geográficos ilustrativos em `data/mapa/` (4 estados do Sudeste, 8
  cidades, 6 rodovias) — geometrias simplificadas para prototipar a
  arquitetura, não para uso cartográfico preciso (ver nota `_nota` em cada
  arquivo).

### Notas
- A lógica de herança roda inteiramente no cliente (sem persistência); mover
  as regras de estado/cidade para o backend Java é o próximo passo natural,
  seguindo o padrão de `Estado`/`Pais` já existente nas branches do backend.

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
