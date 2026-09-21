# Roadmap

Prioridades para os próximos ciclos. `RF` = requisito funcional,
`RNF` = requisito não funcional. Itens concluídos ficam registrados no
[CHANGELOG](../CHANGELOG.md) e no [RELATÓRIO](RELATORIO.md).

## O que ainda não foi entregue

A tabela abaixo é gerada a partir de [`requisitos.toml`](requisitos.toml):
mudar a situação de um requisito lá muda este roadmap e o relatório junto.

<!-- auto:inicio:planejados -->
| Prioridade | Requisito | Título | Situação | Por quê |
|---|---|---|---|---|
| Alta | RF-10 | Eleições com candidatura e campanha | planejado | Disputa de cargos por candidatura, campanha e apuração por aprovação, substituindo a posse simplificada. |
| Alta | RNF-01 | Autenticação real | planejado | Spring Security com BCrypt e token de sessão, substituindo o jogadorId que viaja no corpo da requisição. |
| Alta | RNF-02 | Banco PostgreSQL com migração versionada | planejado | Trocar o H2 em arquivo por PostgreSQL com Flyway, pré-requisito para mais de um processo. |
| Media | RF-11 | Quarto setor econômico | planejado | Acrescentar um setor novo (varejo ou energia) para validar que o motor aceita setores apenas com parâmetros. |
| Media | RF-12 | Contratos entre empresas | planejado | Fornecimento entre empresas, ligando construção e imobiliário em cadeia produtiva. |
| Media | RF-13 | Eventos de mundo | planejado | Crises, secas, booms setoriais e outros choques narrativos que quebrem a previsibilidade do ciclo. |
| Media | RNF-03 | Trava distribuída para a auditoria | planejado | Garantir a ordem da cadeia de hashes com mais de um servidor gravando. |
| Baixa | RF-14 | Notificações de turno para o jogador | planejado | Avisar o jogador do que aconteceu no fechamento sem exigir que ele abra a tela. |
| Baixa | RF-15 | Mais municípios e estados | planejado | Ampliar o mapa para aumentar a disputa local e a variedade de mercados. |
| Baixa | RF-16 | Comércio exterior entre países | planejado | Trocas comerciais entre países, exigindo um segundo país jogável. |
| Baixa | RF-17 | Fusões e aquisições | planejado | Compra de participação relevante e troca de controle entre jogadores. |
| Baixa | RF-18 | Impeachment e cassação | planejado | Responsabilização política, fechando o ciclo de poder com perda de mandato. |
| Baixa | RF-19 | Ranking de jogadores | planejado | Classificação por patrimônio e influência política entre partidas. |
| Baixa | RF-20 | Projeções de investimento na interface | parcial | Mostrar ao investidor a projeção de retorno calculada pelo serviço analítico. |
| Baixa | RNF-04 | Valores monetários em BigDecimal | planejado | Migrar de double para BigDecimal caso o jogo passe a exigir precisão contábil. |
| Baixa | RNF-05 | Cache das consultas de estatísticas | planejado | Evitar recalcular séries longas a cada requisição quando a partida passar de centenas de turnos. |
<!-- auto:fim:planejados -->

## Dívidas técnicas conhecidas

| Item | Risco | Quando tratar |
|---|---|---|
| Balanceamento inicial dos setores | Economia pode ficar fácil ou impossível demais | Após a primeira partida com jogadores reais |
| Sem paginação nas listagens de empresas e projetos | Lentidão com muitos registros | Antes de abrir para muitos jogadores |
| Frontend sem testes automatizados | Regressão silenciosa na interface | Quando o time crescer |
| Serviço Python sem empacotamento (sem `requirements`, roda com stdlib) | Aceitável hoje; vira problema se ganhar dependências | Ao adicionar numpy/pandas |
