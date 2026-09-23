# GeoHistoricalSim

Projeto pessoal para desenvolvimento de uma plataforma voltada para RPG geopolítico e simulações históricas.

## Objetivo
Criar uma ferramenta para gerenciamento de jogos estratégicos baseados em geopolítica e história.

## Tecnologias
- Python
- Java
- Git

## Sistema de combate

O motor de combate fica em `combate/` e é a fonte única das regras de guerra —
o frontend consome o resultado, nunca recalcula. É exposto ao backend Java por
um serviço HTTP sem estado, no mesmo molde do `analytics/`.

```
python -m combate.cli Brasil Argentina --semente 42
python -m combate.servico
python -m unittest discover -s tests
```

Especificação completa das regras: [docs/REGRAS-DE-COMBATE.md](docs/REGRAS-DE-COMBATE.md).
