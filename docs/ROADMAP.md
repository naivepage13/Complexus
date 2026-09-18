# Roadmap

Prioridades para os próximos ciclos. `RF` = requisito funcional,
`RNF` = requisito não funcional. Itens concluídos ficam registrados no
[CHANGELOG](../CHANGELOG.md) e no [RELATÓRIO](RELATORIO.md).

## Próximo ciclo (0.3.0)

| # | Item | Por quê | Onde mexe |
|---|---|---|---|
| RNF-01 | Autenticação real: Spring Security, BCrypt e token de sessão | Hoje o `jogadorId` vem no corpo e qualquer cliente pode agir por outro (L-01, L-02) | `jogador/`, todos os controllers, `js/app.js` |
| RF-10 | Eleições com candidatura, campanha e apuração | A posse simplificada não representa disputa política | `politica/ServicoPolitica`, nova entidade `Candidatura` |
| RF-11 | Quarto setor (varejo ou energia) | Valida que o motor aceita setor novo só com parâmetros | `economia/Setor` e seed |

## Ciclo seguinte (0.4.0)

| # | Item | Por quê |
|---|---|---|
| RNF-02 | PostgreSQL + Flyway | H2 em arquivo aceita um processo só; migração versionada é pré-requisito para produção |
| RF-12 | Contratos entre empresas (fornecimento construção → imobiliário) | Cria cadeia produtiva entre setores |
| RF-13 | Eventos de mundo (crise, seca, boom imobiliário) | Dá narrativa e quebra a previsibilidade do ciclo |
| RF-14 | Notificações de turno para o jogador | Hoje é preciso abrir a tela para saber o que aconteceu |

## Backlog

| # | Item | Observação |
|---|---|---|
| RNF-03 | Trava distribuída para a auditoria | Necessária ao rodar mais de um servidor (L-05) |
| RNF-04 | Migrar dinheiro para `BigDecimal` | Só se o jogo passar a exigir precisão contábil (ADR-03) |
| RNF-05 | Cache das consultas de estatísticas | Quando a série passar de algumas centenas de turnos |
| RF-15 | Mais municípios e estados | Amplia o mapa e a disputa local |
| RF-16 | Comércio exterior entre países | Exige um segundo país jogável |
| RF-17 | Fusões e aquisições | Compra de participação relevante e controle |
| RF-18 | Impeachment e cassação | Fecha o ciclo de responsabilização política |
| RF-19 | Ranking de jogadores por patrimônio e influência | Motiva a competição entre partidas |
| RF-20 | Projeções de investimento na tela usando `/projecao` do Python | O endpoint já existe, falta consumir no frontend |

## Dívidas técnicas conhecidas

| Item | Risco | Quando tratar |
|---|---|---|
| Balanceamento inicial dos setores | Economia pode ficar fácil ou impossível demais | Após a primeira partida com jogadores reais |
| Sem paginação nas listagens de empresas e projetos | Lentidão com muitos registros | Antes de abrir para muitos jogadores |
| Frontend sem testes automatizados | Regressão silenciosa na interface | Quando o time crescer |
| Serviço Python sem empacotamento (sem `requirements`, roda com stdlib) | Aceitável hoje; vira problema se ganhar dependências | Ao adicionar numpy/pandas |
