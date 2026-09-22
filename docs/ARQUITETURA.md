# Arquitetura

## 1. Visão geral

```
navegador (HTML + CSS + JS)
        |  fetch /api/...
        v
+---------------------------------------------------+
|  Backend Java 21 + Spring Boot 3.5                 |
|                                                    |
|  controllers  ->  services  ->  repositories       |
|                      |                             |
|                      +-> MotorSimulacao (puro)     |
|                      +-> ServicoAuditoria (hash)   |
|                      +-> ServicoRazao (livro-razao)|
+----------------------+-----------------------------+
        |                          |
        | HTTP (choques)           | JPA
        v                          v
+---------------------+   +----------------------+
| Servico analitico   |   | H2 (arquivo local)   |
| Python 3 (stdlib)   |   | 12 tabelas           |
+---------------------+   +----------------------+
```

O Java concentra as regras. O Python calcula os choques setoriais de cada turno
e projeções de investimento. O frontend é estático, servido pelo próprio Spring
Boot a partir de `backend/src/main/resources/static`.

## 2. Estrutura de pastas

```
backend/           nucleo Java (Spring Boot)
  src/main/java/com/complexus/
    auditoria/     linha de auditoria encadeada por hash
    comum/         excecoes, tratador de erros, mapeadores de resposta
    config/        propriedades do jogo e carga inicial do mundo
    core/          relogio, motor de turnos, agendador
    economia/      setores, empresas, empreendimentos, motor de simulacao
    estatistica/   snapshots por turno e consolidacao por setor
    integracao/    cliente do servico analitico em Python
    investimento/  acoes, carteira, dividendos e livro-razao
    jogador/       contas de jogador
    politica/      territorios, cargos, mandatos, projetos de lei e votos
  src/main/resources/
    application.yml
    static/        frontend (html, css/app.css, js/*.js)
      admin/       console administrativo, fora do menu do jogo
  src/test/        testes unitarios e de integracao
analytics/         servico analitico em Python + testes
docs/              esta documentacao
legado/            prototipo original preservado
```

## 3. Camadas

| Camada | Regra |
|---|---|
| `*Controller` | Só traduz HTTP. Recebe `record` de requisição, chama o serviço, devolve mapa montado por `Mapeadores`. Não contém regra. |
| `Servico*` | Guarda a regra de negócio e a transação. É o único lugar que escreve no banco e que registra auditoria. |
| `Repositorio*` | Spring Data. Só consulta e grava. |
| `MotorSimulacao` | Funções puras de cálculo. Sem banco, sem estado, testável isoladamente. |
| Entidades | Estado persistente. Métodos apenas para cálculos derivados simples (ex.: `indiceLastro()`). |

## 4. Modelo de dados

| Tabela | Conteúdo |
|---|---|
| `jogador` | Contas, caixa pessoal |
| `pais`, `estado_federativo`, `municipio` | Territórios e seus indicadores fiscais e sociais |
| `mandato` | Ocupação de cargo por jogador ou NPC, com prazo e aprovação |
| `projeto_lei`, `voto_projeto` | Tramitação legislativa e votos nominais |
| `empresa` | Balanço, operação e posição de mercado (totais = soma das unidades) |
| `unidade` | Filial: patrimônio, equipe e produtividade em um município |
| `linha_produto` | Mix de posicionamento que define preço e custo de insumo |
| `departamento` | Orçamento mensal por área administrativa |
| `historico_empresa` | Resultado de cada empresa em cada turno |
| `empreendimento` | Obras em andamento dos setores imobiliário e construção, ligadas à unidade que as toca |
| `financiamento` | Contratos de dívida: saldo, taxa travada, prazo, garantia e atraso |
| `contrato_fornecimento` | Contratos entre empresas: insumo, volume, preço, prazo e entregas |
| `investimento` | Posição acionária de cada jogador |
| `lancamento_financeiro` | Livro-razão: toda movimentação de dinheiro |
| `evento_auditoria` | Linha de auditoria encadeada por hash |
| `snapshot_turno`, `estatistica_setor` | Estatísticas consolidadas por turno |
| `estado_jogo` | Relógio da partida (linha única) |

Relações territoriais: `municipio -> estado_federativo -> pais`.
Mandatos e leis apontam para o território por `esfera` + `territorioId`, o que
evita três colunas de chave estrangeira e mantém o mesmo código para as três esferas.

## 5. Ordem do turno

`ServicoTurno.processarTurno` é síncrono, transacional e sempre segue esta ordem:

1. avança relógio (turno + 1, data + 1 mês) e publica o novo turno;
2. busca choques setoriais no Python (fallback local em caso de falha);
3. rotina política: apuração, sanções pendentes, renovação de mandatos;
4. gastos públicos recorrentes (programa social, infraestrutura);
5. apuração dos contratos de fornecimento (o que cada fornecedor consegue
   entregar neste turno), antes de qualquer empresa produzir;
6. simulação das **unidades** agrupadas por município e setor, seguida do
   fechamento de cada empresa: juros dos contratos e estrutura entram como
   despesa, o imposto incide sobre o lucro, a amortização sai do caixa, o
   dividendo vem depois e o caixa negativo vira crédito rotativo;
7. andamento das obras;
8. recálculo macroeconômico;
9. consolidação das estatísticas e registro do evento `TURNO_PROCESSADO`.

A ordem importa: leis sancionadas no passo 3 valem já no passo 5 do mesmo turno.

## 6. Decisões de arquitetura (ADR resumido)

### ADR-01 — Java + Spring Boot como núcleo
Todas as regras que afetam o estado do jogo ficam no backend Java. Frontend e
Python são periféricos. Motivo: uma única fonte de verdade transacional.

### ADR-02 — Python como serviço opcional
O serviço analítico entrega valor (modelo estocástico legível e testável em
Python) sem se tornar ponto único de falha: o Java tem a mesma fórmula como
fallback e registra qual origem usou em cada turno.

### ADR-03 — Dinheiro em `double`
É uma simulação, não contabilidade. `double` mantém o motor legível e rápido.
Se o jogo passar a ter economia entre servidores ou valores auditados por
terceiros, migrar para `BigDecimal` (impacto concentrado em `MotorSimulacao`,
`ServicoEmpresa`, `ServicoInvestimento` e `ServicoRazao`).

### ADR-04 — Associações `EAGER` nas entidades
`open-in-view` está desligado. Como os grafos são pequenos (empresa → município
→ estado → país), carregar junto evita `LazyInitializationException` na
serialização. Se algum grafo crescer, trocar por consultas com `join fetch`.

### ADR-05 — Respostas montadas por `Mapeadores`
A API não serializa entidades diretamente. Isso dá liberdade para evoluir o
banco sem quebrar o frontend e evita expor campos internos.

### ADR-06 — Auditoria em transação própria
Cada gravação de auditoria abre transação nova e é sincronizada, garantindo que
o hash anterior lido já esteja confirmado. Consequência: o evento sobrevive
mesmo que a operação de negócio falhe depois — desejável para auditoria.

### ADR-07 — Visibilidade separada por publico
O que o jogador ve e o que a administracao ve sao contratos diferentes:

- o jogador consome `/api/atualizacoes`, um feed traduzido e filtrado;
- a administracao consome `/api/admin/auditoria/**`, protegido por token.

As listagens de mercado tambem usam uma projecao publica da empresa
(`Mapeadores.empresaPublica`), sem lucro, custo, caixa nem patrimonio. Resultado
de empresa e assunto da pagina da propria empresa, exibido ali em grafico.
Excecao deliberada: empresa de capital aberto publica o resultado na vitrine de
investimentos, porque o investidor precisa dele para decidir.

### ADR-08 — Frontend sem framework
Uma página por arquivo, um CSS único, um JS por página. Sem build, sem
dependência externa: abrir o backend já entrega a interface pronta.

## 7. Configuração

`backend/src/main/resources/application.yml`:

| Chave | Padrão | Efeito |
|---|---|---|
| `jogo.turno.duracao-minutos` | 60 | Intervalo real entre turnos |
| `jogo.turno.processamento-automatico` | true | Liga/desliga o agendador |
| `jogo.analitico.habilitado` | true | Usa o serviço Python |
| `jogo.analitico.url` | http://localhost:8100 | Endereço do serviço |
| `jogo.analitico.timeout-ms` | 1500 | Tempo máximo de espera |
| `jogo.admin.token` | `admin-local` (env `JOGO_ADMIN_TOKEN`) | Credencial das rotas `/api/admin/**` |

Em teste (`src/test/resources/application.yml`) o banco é em memória, o
agendador fica desligado e o serviço Python é dispensado.
