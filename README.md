# Complexus

Simulador de administração de **empresas** e de **países**. O jogador funda e
administra companhias nos setores alimentício, imobiliário e de construção,
disputa cargos públicos nas três esferas do modelo brasileiro, legisla sobre
impostos e subsídios, e investe no mercado de ações do jogo.

**A cada hora de tempo real o mundo avança um mês.** O turno recalcula todas as
empresas, aplica as leis sancionadas, distribui dividendos, atualiza os
indicadores macroeconômicos e fecha as estatísticas do período.

## Tecnologias

| Camada | Stack |
|---|---|
| Núcleo de regras e API | Java 21 + Spring Boot 3.5 (principal) |
| Persistência | Spring Data JPA + H2 |
| Serviço analítico | Python 3 (somente biblioteca padrão) |
| Interface | HTML + CSS + JavaScript, sem framework |

## Como rodar

Pré-requisitos: JDK 21 ou superior, Maven 3.9+, Python 3.10+.

```bash
python analytics/servico_analitico.py
```

```bash
cd backend && mvn spring-boot:run
```

Abra <http://localhost:8080> e entre com `demo` / `demo1234`.
O serviço Python é opcional: sem ele o backend usa o cálculo local equivalente.

## Testes

```bash
cd backend && mvn test
```

```bash
cd analytics && python -m unittest discover -p "testes_*.py"
```

## O que dá para fazer no jogo

- **Empresas** — fundar, aportar capital, contratar, demitir, ajustar marketing,
  salário e política de dividendos, abrir capital e tocar empreendimentos.
  A produção é limitada pelo menor teto entre equipe e patrimônio, então crescer
  exige as duas frentes.
- **Política** — assumir cadeiras de presidente a vereador, propor projetos de
  lei, votar, sancionar, vetar e derrubar veto. As leis mudam de verdade a
  economia das empresas.
- **Investimentos** — comprar ações de empresas de capital aberto, receber
  dividendos mensais lastreados no lucro e acompanhar o retorno da carteira.
- **Estatísticas** — PIB, inflação, juros, desemprego, aprovação do governo,
  índice de mercado e séries históricas por turno e por setor.
- **Atualizações** — canal com o que acontece no mundo: fechamentos de turno,
  leis, empresas e cargos, mais as suas próprias ações.

Nos bastidores, toda ação vira um evento encadeado por hash com livro-razão
financeiro. Essa linha de auditoria é administrativa: fica em
`/api/admin/auditoria` (cabeçalho `X-Admin-Token`) e na página
`admin/auditoria.html`, fora do menu do jogo.

## Documentação

| Documento | Conteúdo |
|---|---|
| [docs/RELATORIO.md](docs/RELATORIO.md) | **Documento vivo**: estado do projeto, entregas, decisões e limitações |
| [docs/ARQUITETURA.md](docs/ARQUITETURA.md) | Camadas, modelo de dados e decisões de arquitetura |
| [docs/REGRAS-DO-JOGO.md](docs/REGRAS-DO-JOGO.md) | Fórmulas e regras de economia, política e investimento |
| [docs/API.md](docs/API.md) | Endpoints REST |
| [docs/AUDITORIA.md](docs/AUDITORIA.md) | Linha de auditoria do jogo e do desenvolvimento |
| [docs/ROADMAP.md](docs/ROADMAP.md) | Próximos ciclos e dívidas técnicas |
| [CHANGELOG.md](CHANGELOG.md) | Histórico de versões |

## Estrutura

```
backend/     nucleo Java (Spring Boot) + frontend estatico
analytics/   servico analitico em Python
docs/        documentacao viva do projeto
legado/      prototipo original, preservado para referencia
```

## Contribuindo

Regra do projeto: nenhuma mudança entra sem deixar rastro. Toda entrega
atualiza o `CHANGELOG.md` e o `docs/RELATORIO.md`; mudança de regra atualiza
`docs/REGRAS-DO-JOGO.md`; mudança de contrato atualiza `docs/API.md`.
