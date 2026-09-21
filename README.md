# Complexus

Simulador de administração de **empresas** e de **países**. O jogador funda e
administra companhias nos setores alimentício, imobiliário e de construção,
disputa cargos públicos nas três esferas do modelo brasileiro, legisla sobre
impostos e subsídios, e investe no mercado de ações do jogo.

**A cada hora de tempo real o mundo avança um mês.** O turno recalcula todas as
empresas, aplica as leis sancionadas, distribui dividendos, atualiza os
indicadores macroeconômicos e fecha as estatísticas do período.

## Estado do projeto

<!-- auto:inicio:estado -->
| Indicador | Valor |
|---|---|
| Versão | `0.4.0` |
| Requisitos entregues | 18 de 34 (53%) |
| Requisitos funcionais | 22 |
| Requisitos não funcionais | 12 |
| Testes automatizados | 45 (24 Java + 21 Python) |
| Endpoints da API | 41 públicos + 3 administrativos |
| Entidades persistidas | 16 |
| Páginas do frontend | 9 |

Situação por requisito em [docs/REQUISITOS.md](docs/REQUISITOS.md); mapa do código em [docs/INVENTARIO.md](docs/INVENTARIO.md).
<!-- auto:fim:estado -->

## Tecnologias

| Camada | Stack |
|---|---|
| Núcleo de regras e API | Java 21 + Spring Boot 3.5 (principal) |
| Persistência | Spring Data JPA + H2 |
| Serviço analítico | Python 3 (somente biblioteca padrão) |
| Interface | HTML + CSS + JavaScript, sem framework |

## Como rodar

Pré-requisitos: JDK 21 ou superior, Maven 3.9+, Python 3.11+.

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

```bash
python -m unittest discover -s ferramentas -p "testes_*.py"
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

<!-- auto:inicio:documentacao -->
| Documento | Conteúdo | Origem |
|---|---|---|
| [docs/RELATORIO.md](docs/RELATORIO.md) | Relatório geral: estado, entregas, decisões e limitações | Misto |
| [docs/REQUISITOS.md](docs/REQUISITOS.md) | Requisitos funcionais e não funcionais, com rastreabilidade | Gerado |
| [docs/INVENTARIO.md](docs/INVENTARIO.md) | Mapa técnico: endpoints, entidades, serviços e páginas | Gerado |
| [docs/ARQUITETURA.md](docs/ARQUITETURA.md) | Camadas, modelo de dados e decisões de arquitetura | Manual |
| [docs/REGRAS-DO-JOGO.md](docs/REGRAS-DO-JOGO.md) | Fórmulas e regras de economia, política e investimento | Manual |
| [docs/API.md](docs/API.md) | Contrato dos endpoints REST | Manual |
| [docs/AUDITORIA.md](docs/AUDITORIA.md) | Linha de auditoria do jogo e do desenvolvimento | Manual |
| [docs/ROADMAP.md](docs/ROADMAP.md) | Próximos ciclos e dívidas técnicas | Misto |
| [CHANGELOG.md](CHANGELOG.md) | Histórico de versões | Manual |
| [docs/requisitos.toml](docs/requisitos.toml) | Catálogo de requisitos: fonte única dos relatórios | Manual |
<!-- auto:fim:documentacao -->

## Como a documentação se mantém em dia

A documentação não depende de alguém lembrar de atualizá-la. Duas fontes
alimentam os relatórios:

1. **O código**, lido por `ferramentas/inventario.py`: versão, endpoints,
   entidades, serviços, páginas, testes e tamanho por linguagem.
2. **O catálogo de requisitos** em [`docs/requisitos.toml`](docs/requisitos.toml),
   escrito à mão, onde cada requisito declara critério de aceite, arquivos que o
   implementam e testes que o cobrem.

O gerador junta as duas e reescreve os relatórios:

```bash
python ferramentas/gerar_documentacao.py
```

```bash
python ferramentas/gerar_documentacao.py --verificar
```

O modo `--verificar` não escreve nada e falha quando a documentação está
atrasada em relação ao código — é o que roda no hook de pré-commit e o que
impede o repositório de aceitar documentação desatualizada.

Para ligar o hook na sua cópia (uma vez só):

```bash
git config core.hooksPath .githooks
```

Arquivos gerados por inteiro (`docs/REQUISITOS.md`, `docs/INVENTARIO.md`) trazem
aviso no topo e não devem ser editados à mão. Nos demais, só muda o conteúdo
entre marcadores `<!-- auto:inicio:... -->` e `<!-- auto:fim:... -->`; o texto
escrito por pessoas fora dos marcadores nunca é tocado.

A rastreabilidade é verificada: se um requisito apontar para um arquivo que não
existe mais, o gerador falha em vez de publicar uma referência morta.

## Estrutura

```
backend/     nucleo Java (Spring Boot) + frontend estatico
analytics/   servico analitico em Python
ferramentas/ gerador da documentacao viva
docs/        documentacao do projeto e catalogo de requisitos
.githooks/   hook que mantem a documentacao sincronizada
legado/      prototipo original, preservado para referencia
```

## Contribuindo

Regra do projeto: nenhuma mudança entra sem deixar rastro.

| Mudou | Atualize |
|---|---|
| Qualquer entrega | `CHANGELOG.md` e a parte manual de `docs/RELATORIO.md` |
| Escopo (novo requisito, mudança de situação) | `docs/requisitos.toml` |
| Regra de jogo | `docs/REGRAS-DO-JOGO.md` |
| Contrato da API | `docs/API.md` |
| Decisão de arquitetura | `docs/ARQUITETURA.md` |

Os relatórios de requisitos, o inventário técnico e os números do README saem
do gerador — não precisam de edição manual.
