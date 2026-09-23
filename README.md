# GeoHistoricalSim

Projeto pessoal para desenvolvimento de uma plataforma voltada para RPG geopolítico e simulações históricas.

## Objetivo
Criar uma ferramenta para gerenciamento de jogos estratégicos baseados em geopolítica e história.

## Tecnologias
- Python
- Java
- Git

## Sistema de combate

O motor de combate fica em `src/combate/` e é a fonte única das regras de
guerra — o frontend consome o resultado, nunca recalcula.

```
python src/simular_batalha.py Brasil Argentina --semente 42
python -m unittest discover -s tests
```

Especificação completa das regras: [docs/REGRAS-DE-COMBATE.md](docs/REGRAS-DE-COMBATE.md).

## Mapa interativo

`mapa.html` traz o mapa hierárquico (Estados → Cidades → Estradas) com zoom
semântico e lazy loading espacial, feito com [Leaflet](https://leafletjs.com/).
Os dados geográficos (ilustrativos) ficam em `data/mapa/`. Para testar
localmente, sirva a pasta por HTTP (o `fetch()` dos GeoJSON não funciona em
`file://`):

```
python -m http.server 8123
```
