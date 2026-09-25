# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this is

Complexus is a business/country management simulation game. Players found and
run companies (food, real estate, construction sectors), hold political office
across Brazil's three government tiers (federal/state/municipal), pass and
vote on legislation that has real economic effects, and invest in a stock
market backed by company performance. One real-world hour = one in-game month;
a scheduler advances turns automatically and recomputes every company, applies
enacted laws, pays dividends, and refreshes macro indicators.

Repository language is Portuguese throughout — code comments, docs, commit
messages, identifiers. Match that when editing.

## Three independent subsystems

The repo holds three things that don't share code and are at different
maturity levels:

1. **`backend/`** — the actual game (Java/Spring Boot + static frontend). This
   is what `IniciarComplexus.bat` and `mvn spring-boot:run` start.
2. **`src/combate/`** — a standalone Python war-simulation engine (phase-based
   combat: intel, air, deep strike, sea, land), invoked only via CLI
   (`src/simular_batalha.py`). **Not wired into the backend yet** — see
   RF-25 in `docs/requisitos.toml`.
3. **`analytics/`** — a small Python HTTP service the backend calls for
   sector-shock calculations. This one *is* integrated (see Python
   integration pattern below).

Don't assume combat and the backend talk to each other — they don't, on
purpose, until RF-25 is picked up.

## Commands

### Run the game
```bash
python analytics/servico_analitico.py    # optional analytics service, port 8100
cd backend && mvn spring-boot:run         # backend + static frontend, port 8080
```
On Windows, `IniciarComplexus.bat` does both and opens the browser once the
server responds; it also hunts for a JDK 21+ across common install locations
if `JAVA_HOME` points somewhere older (a real, previously-hit problem).

Login at `http://localhost:8080` with `demo` / `demo1234`. The Python
analytics service is optional — the backend falls back to an equivalent local
calculation and records which source it used (`PYTHON` vs `FALLBACK_JAVA`) in
the turn report.

### Tests
```bash
cd backend && mvn test                                    # Java: JUnit
cd analytics && python -m unittest discover -p "testes_*.py"
python -m unittest discover -s ferramentas -p "testes_*.py"  # doc-generator self-tests
python -m unittest discover -s tests                       # combat engine tests
```
Run a single Java test class: `mvn test -Dtest=FluxoDoJogoTest`.
Run a single Python test: `python -m unittest tests.test_combate.NomeDoTeste`.

### Combat engine CLI (standalone, not backend-connected)
```bash
python src/simular_batalha.py Brasil Argentina --semente 42
```

### Documentation generator
```bash
python ferramentas/gerar_documentacao.py              # rewrite generated docs
python ferramentas/gerar_documentacao.py --verificar   # check only, no writes; exits 1 if stale
```

## Architecture

### Request flow
```
browser (HTML+CSS+JS, no framework) → fetch /api/...
  → Controller (HTTP translation only, no logic)
    → Servico* (business logic + transactions; only layer that writes to DB or audits)
      → Repositorio* (Spring Data, query/persist only)
      → MotorSimulacao (pure functions, no DB, no state — economic math lives here)
    → Mapeadores (entity → response map; API never serializes entities directly)
```

### Package layout (`backend/src/main/java/com/complexus/`)
`auditoria` (hash-chained audit log) · `atualizacoes` (player-facing feed —
see visibility split below) · `comum` (exceptions, error handler, response
mappers) · `config` (game properties, world seed data) · `core` (game clock,
turn engine, scheduler) · `economia` (sectors, companies, ventures, the
simulation engine) · `estatistica` (per-turn snapshots) · `integracao`
(Python analytics HTTP client) · `investimento` (stocks, portfolio,
dividends, ledger) · `jogador` (player accounts) · `politica` (territories,
offices, mandates, bills, votes).

### The turn engine — `ServicoTurno.processarTurno`
Synchronous, transactional, and order-dependent — laws enacted in step 3 take
effect in the same turn's step 5:
1. advance clock (turn+1, +1 month), publish new turn
2. fetch sector shocks from Python (local fallback on failure)
3. political cycle: vote tallying, pending sanctions, mandate renewal
4. recurring public spending
5. simulate all companies (grouped by municipality+sector), pay dividends
6. advance construction ventures
7. recompute macro indicators
8. consolidate stats, record `TURNO_PROCESSADO` audit event

### Visibility split (deliberate, load-bearing pattern)
Two audiences, two contracts — **do not merge them**:
- Players see `/api/atualizacoes` (translated, filtered feed) and a public
  company projection (`Mapeadores.empresaPublica` — no profit/cost/cash/net
  worth). Company financials live only on that company's own page.
- `/api/admin/**` (full audit log, ledger) requires header `X-Admin-Token`
  matching `jogo.admin.token` (env `JOGO_ADMIN_TOKEN`); returns 403 without
  it. This is a server-side gate, not just a hidden menu item.
- Exception made on purpose: publicly-traded companies show financials on the
  investment marketplace, because investors need that to decide.

### Python integration pattern (`ClienteAnalitico` + `analytics/servico_analitico.py`)
Any new Python service must follow this shape: stdlib-only HTTP server (no
install step), Java client calls with a timeout and **falls back to an
equivalent Java implementation** on failure so a turn never fails to process,
and the source used (`PYTHON`/`FALLBACK_JAVA`) is recorded in the turn report.

### Data model gotchas
- Territory hierarchy: `municipio → estado_federativo → pais`. Mandates and
  bills reference territory via `esfera` + `territorioId` (not three FK
  columns) so the same code handles all three government tiers.
- `open-in-view` is off; associations are `EAGER` because the object graphs
  are small (company → municipality → state → country) — see ADR-04 in
  `docs/ARQUITETURA.md` before changing this.
- Money is `double`, not `BigDecimal` — a deliberate simulation-not-accounting
  choice (ADR-03). If you touch money math, it's concentrated in
  `MotorSimulacao`, `ServicoEmpresa`, `ServicoInvestimento`, `ServicoRazao`.
- Audit events (`ServicoAuditoria`) write in a fresh, synchronized transaction
  each time so the previous hash read is always already committed (ADR-06) —
  don't refactor this into a shared transaction without understanding why.

### Frontend
One HTML page per file, one shared `css/app.css`, one JS file per page, no
build step, no framework, no CDN dependencies (Leaflet for the map is vendored
under `static/vendor/`, not loaded from a CDN — this app must run offline).

## The self-updating documentation system

This is the project's defining convention — **don't bypass it**.

`docs/requisitos.toml` is the single source of truth for what's built. Each
requirement (`RF-nn`/`RNF-nn`) declares status, acceptance criterion, the
files that implement it, and the tests that cover it.
`ferramentas/gerar_documentacao.py` reads that catalog plus the actual code
(`ferramentas/inventario.py`: endpoints, entities, services, pages, test
counts) and regenerates:
- `docs/REQUISITOS.md`, `docs/INVENTARIO.md` — fully generated, carry a
  "don't hand-edit" banner
- blocks inside `README.md`, `docs/RELATORIO.md`, `docs/ROADMAP.md` between
  `<!-- auto:inicio:NOME -->` / `<!-- auto:fim:NOME -->` markers — text
  outside those markers is never touched

**The generator fails the build if a requirement's `implementacao` path
doesn't exist** — no dead references allowed. Generation is deterministic
(no timestamps, no commit hashes) so `--verificar` can reliably detect staleness.

A pre-commit hook (`.githooks/pre-commit`, enabled via
`git config core.hooksPath .githooks`) regenerates docs and auto-stages the
generated files on every commit; it hard-fails the commit if the catalog is
invalid (e.g., a requirement pointing at a deleted file).

**When you add or change a requirement-level feature**: update
`docs/requisitos.toml` (status/tests/implementacao), then run
`python ferramentas/gerar_documentacao.py` — don't hand-edit the generated
files or the auto-blocks.

**Other things each kind of change should update** (per `docs/AUDITORIA.md`'s
own rule — nothing ships without a trail):
| Changed | Also update |
|---|---|
| Any delivery | `CHANGELOG.md` + the manual parts of `docs/RELATORIO.md` |
| Game rule | `docs/REGRAS-DO-JOGO.md` (economy/politics/investment formulas) |
| API contract | `docs/API.md` |
| Architecture decision | `docs/ARQUITETURA.md` (add an ADR) |
| Combat engine rule | `docs/REGRAS-DE-COMBATE.md` |

## Things that look like bugs but aren't (yet)

- `src/combate/` and `data/paises.json` are fully functional but **not called
  from the backend** — this is intentional pending RF-25, not an oversight.
- `mapa.html` reads static GeoJSON under `static/dados/mapa/`, not live
  territory data from the API — intentional pending RF-26.
- `GET /api/empresas/{id}` returns any company's full financials regardless
  of who's asking (no real auth yet — `jogadorId` travels in the request
  body). Known and tracked as L-02/L-07 in `docs/RELATORIO.md`, blocked on
  RNF-01 (real authentication).
- `.gitignore` deliberately does **not** ignore `data/` wholesale — only
  `backend/data/` (the H2 file) — because `data/paises.json` is real,
  tracked combat-engine input. Don't "clean up" that pattern.
