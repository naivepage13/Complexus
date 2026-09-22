# Changelog

Formato baseado em [Keep a Changelog](https://keepachangelog.com/pt-BR/1.1.0/).
Este arquivo é a linha de auditoria do desenvolvimento: nenhuma entrega entra
sem uma linha aqui. Ver também [docs/RELATORIO.md](docs/RELATORIO.md).

## [0.6.0] - 2026-09-22

As empresas passam a comprar umas das outras. Terceira parte da administracao
de empresas.

### Adicionado
- **Contratos de fornecimento** (`ContratoFornecimento`) entre duas empresas,
  com insumo, volume mensal, preco relativo a referencia do setor e prazo.
- **Matriz de insumos** (`TipoInsumo`) ligando os tres setores: construcao
  entrega material e obra ao imobiliario, o imobiliario aluga espaco comercial
  ao alimenticio e a construcao, e o alimenticio abastece o proprio setor.
- **Efeito no motor**: o comprador troca insumo de mercado por insumo de preco
  travado, que nao sofre o choque de custo do turno; o fornecedor reserva
  capacidade, que sai do mercado aberto e do mercado disputavel do grupo.
- **Negociacao completa**: propor (dos dois lados), aceitar, recusar e romper
  com multa de 10 por cento do valor remanescente e perda de reputacao.
- **Resposta automatica das empresas do sistema**: aceitam fornecer com ate 5
  por cento de desconto e comprar pagando ate 5 por cento acima da referencia.
- **Falha de entrega**: fornecedor sem capacidade entrega menos, registra a
  falha, perde reputacao e o comprador recebe menos insumo no mesmo turno.
- Pagina `cadeia.html` com limites dos dois lados, propostas, contratos em
  vigor, historico e simulacao do efeito antes de enviar a proposta.
- Rotas `/api/empresas/{id}/fornecimento/**`.
- `CadeiaProdutivaTest` (9 testes) e 2 testes novos do motor para o insumo
  contratado e a capacidade reservada.

### Alterado
- `MotorSimulacao.consolidar` passou a receber um `FechamentoFinanceiro`, que
  reune juros, estrutura, contratos e aliquota em vez de quatro parametros.
- O turno ganhou uma etapa: a apuracao dos contratos acontece **antes** da
  producao, para que fornecedor e comprador enxerguem a mesma entrega.
- `PerfilOperacional` ganhou capacidade reservada, insumo contratado e preco do
  insumo contratado, rateados entre as unidades pelo patrimonio.
- Tributo indireto virou metodo publico do motor (`tributoIndireto`), usado
  tanto na venda ao mercado quanto no faturamento de contrato.

### Corrigido
- **Corrida na carga inicial**: em uma partida nova, uma requisicao que chegasse
  enquanto a carga do mundo rodava criava a linha do relogio em paralelo e
  derrubava a carga com violacao de chave primaria. O relogio passou a nascer na
  inicializacao dos beans, antes de o servidor abrir a porta. O defeito existia
  desde a 0.2.0 e so aparecia com requisicao nos primeiros segundos.

## [0.5.0] - 2026-09-21

Divida com contrato: a empresa passa a tomar credito, pagar parcela, renegociar
e responder por atraso. Segunda parte da administracao de empresas.

### Adicionado
- **Financiamentos** (`Financiamento`) com prazo, taxa travada na contratacao e
  amortizacao constante (SAC). Quatro linhas: capital de giro, investimento,
  antecipacao de recebiveis e o rotativo automatico.
- **Nota de credito** de A a D (`NotaCredito`), calculada por alavancagem (35%),
  cobertura de juros (30%), lastro (15%) e historico de pagamento (20%). A nota
  define o spread de risco e o fator de limite.
- **Limite agregado por empresa**: toda divida ja contratada consome o espaco
  das demais linhas, e a antecipacao tem teto adicional de 3x a receita mensal.
- **Amortizacao antecipada** e **renegociacao** (alonga o prazo, soma 1% de
  comissao ao saldo, acrescenta 3% a.a. a taxa e zera a contagem de atraso).
- **Inadimplencia**: multa de 2%, mora de 1% ao mes, reputacao em queda,
  execucao da garantia na terceira parcela seguida e falencia na quarta.
- **Garantia real** na linha de investimento: 1,3x o valor liberado, limitada ao
  patrimonio livre; quando executada, a perda e rateada entre as unidades.
- Pagina `financas.html` com nota, score decomposto em barras, vitrine de linhas,
  simulacao da parcela antes de assinar e gestao dos contratos.
- Rotas `/api/empresas/{id}/financas/**`.
- `FinancasDaEmpresaTest`: 9 testes cobrindo avaliacao, limite, garantia,
  cobranca no turno, quitacao antecipada, renegociacao e atraso.
- `MigracaoEsquema`: converte colunas de enum do tipo ENUM do H2 para varchar na
  subida da aplicacao.

### Alterado
- **Os juros do turno vem da taxa de cada contrato**, e nao da Selic corrente
  aplicada sobre um saldo solto.
- **Ordem do fechamento**: juros entram no resultado, o lucro cai no caixa, a
  amortizacao sai antes do dividendo e so entao o caixa negativo vira rotativo.
- `Empresa.divida` deixou de ser escrita direto: e sempre a soma dos contratos
  em aberto, recalculada por `ServicoCredito.sincronizarDivida`.
- Falencia passou a ter dois gatilhos: alavancagem acima de 2,5x o patrimonio ou
  quatro parcelas seguidas sem pagamento.
- A pagina da empresa ganhou o atalho para Financas e um resumo da alavancagem.

### Corrigido
- **Enum novo quebrava partida existente**: o H2 cria coluna de enum como tipo
  ENUM nativo e o `ddl-auto=update` nao acrescenta valores a ele. Gravar um
  lancamento `EMPRESTIMO` em um banco criado antes da 0.5.0 falhava com erro
  22030. A migracao de esquema converte essas colunas para varchar, o que
  tambem destrava qualquer enum novo daqui para frente. Reproduzido em uma
  partida criada na 0.4.0 e reaberta na 0.5.0.

### Migracao
- Divida que existia solta em `Empresa.divida` vira um contrato de credito
  rotativo com a taxa vigente, para que o servico da divida passe a ter origem
  visivel em vez de um numero sem contrato.

## [0.4.0] - 2026-09-21

A empresa deixa de ser uma caixa unica: passa a ter unidades, linhas de produto
e departamentos. Primeira parte da administracao de empresas.

### Adicionado
- **Unidades** (`Unidade`): patrimonio, equipe e produtividade por municipio.
  Quem disputa o mercado de uma cidade e a unidade instalada nela; os totais da
  empresa sao sempre a soma das unidades ativas.
  - abrir filial (capital + 8% de instalacao + meio salario por admissao);
  - fechar filial (liquida 70% dos ativos, paga rescisao, custa 3 pontos de
    reputacao) - a ultima unidade nao pode ser fechada;
  - transferir capital (8% de perda na mudanca) e equipe (30% do salario de
    ajuda de custo) entre unidades.
- **Linhas de produto** (`LinhaProduto`) com posicionamento popular, medio ou
  premium. O mix define o preco praticado e o custo de insumo; a fatia nao
  declarada fica no padrao do setor.
- **Departamentos** (`Departamento`): P&D, qualidade, comercial e logistica, com
  orcamento mensal, efeito saturante calibrado pelo porte da empresa e custo
  fixo cobrado todo turno.
- **Elasticidade-preco por setor** (`Setor.elasticidadePreco`), dividida entre a
  disputa por mercado e o tamanho da demanda capturada.
- Rotas `/api/empresas/{id}/estrutura/**` para unidades, linhas e departamentos.
- Secoes de unidades, linhas e departamentos na pagina da empresa, com
  simulacao do desembolso antes de abrir uma filial.
- `EstruturaDaEmpresaTest`: 9 testes cobrindo sede automatica, abertura e
  fechamento de filial, limite do mix, saturacao de departamento e a igualdade
  entre a receita da empresa e a soma das unidades.

### Alterado
- **Motor de simulacao separado em duas camadas**: `simularOperacao` fecha o mes
  de cada unidade e `consolidar` fecha o mes da empresa. Juros, estrutura
  administrativa e imposto sobre o lucro entram uma vez so, na companhia.
- **Tributo indireto fica onde a unidade opera**, e nao onde fica a sede.
- `POST /empresas/{id}/capital`, `/contratar`, `/demitir` e `/empreendimentos`
  aceitam `unidadeId`; sem ele, a operacao vai para a sede.
- Empreendimento passou a pertencer a uma unidade: a obra entregue vira
  patrimonio da filial que a tocou, mantendo o balanco igual a soma das unidades.
- Diagnostico de capacidade passou a somar o gargalo de cada unidade, em vez de
  comparar os totais da empresa - equipe sobrando em uma cidade nao produz em outra.
- `Empresa.exigirControleDe` centraliza a checagem de dono, usada por todos os
  servicos que administram a empresa.

### Migracao
- Empresas criadas antes das unidades recebem uma sede automatica na subida da
  aplicacao, carregando o patrimonio e a equipe que estavam na empresa. Nenhum
  numero do balanco muda e a partida em andamento continua de onde parou.

## [0.3.0] - 2026-09-21

O projeto passa a se chamar **Complexus**. Sem mudanca de comportamento: e
renomeacao de ponta a ponta, com testes rodados depois.

### Alterado
- Pacote Java `com.geohistoricalsim` -> `com.complexus`; classe principal
  `GeoHistoricalSimApplication` -> `ComplexusApplication`.
- Maven: `groupId` `com.complexus`, `artifactId` `complexus-backend`,
  versao `0.3.0`.
- Banco local H2: `./data/geohistoricalsim` -> `./data/complexus`
  (o arquivo existente foi renomeado, entao a partida em andamento continua).
- Titulos das paginas, marca do cabecalho, rodape e toda a documentacao.
- Servico analitico em Python: cabecalho `Server` e textos.
- Chaves do navegador: `ghs.jogador` -> `complexus.jogador` e
  `ghs.admin.token` -> `complexus.admin.token`.
- `origin` do Git apontando para o repositorio renomeado
  (`naivepage13/Complexus`).

### Adicionado
- `CredencialEstavelTest`: trava o hash de senha, para que uma substituicao de
  texto nao invalide de novo as contas ja existentes.

### Nao alterado
- **Sal do hash de senha** (`geohistoricalsim::v1::`): o valor entra no hash
  gravado no banco. A renomeacao chegou a troca-lo e derrubou o login das
  contas existentes; foi revertido, comentado no codigo e coberto por teste.
- Prefixo `jogo.*` das propriedades: descreve o dominio, nao a marca.
- Pasta `legado/`: e registro historico do prototipo e fica como estava.

## [0.2.1] - 2026-09-18

Ajuste de visibilidade: o que cada publico enxerga.

### Adicionado
- **Canal de atualizacoes** (`/api/atualizacoes`, pagina `atualizacoes.html`):
  feed com os fatos publicos do mundo mais as acoes do proprio jogador,
  com filtro por categoria e sem hash, ator ou detalhe interno.
- `ServicoAtualizacoes`, que traduz a linha de auditoria em noticia de jogo.
- `FiltroAdmin`: qualquer rota `/api/admin/**` exige o cabecalho
  `X-Admin-Token`, com valor em `jogo.admin.token` (env `JOGO_ADMIN_TOKEN`).
- `Mapeadores.empresaPublica`: visao de empresa sem lucro, custo, caixa e
  patrimonio, usada nas listagens de mercado.
- Graficos de receita e de margem liquida na pagina da empresa, ao lado do
  grafico de lucro, cada um com legenda de ultimo, maior e menor valor.
- 5 testes cobrindo a separacao: 403 sem credencial, 200 com credencial,
  feed sem dado interno e movimento privado restrito ao dono.

### Alterado
- **Painel inicial** deixou de ser relatorio: agora traz saudacao, turno, caixa,
  atalhos com contagem e o canal de atualizacoes. Sem lucro, carteira detalhada
  nem feed tecnico.
- **Lucro saiu das listagens** e vive na pagina da empresa, em grafico por turno.
  Tambem saiu do ranking de estatisticas e da resposta de `/api/empresas`.
- Rotas de auditoria migraram de `/api/auditoria/**` para
  `/api/admin/auditoria/**`.
- Pagina de auditoria saiu do menu e virou `admin/auditoria.html`, com campo de
  token guardado apenas na sessao do navegador.
- `Interface.grafico` passou a aceitar formato (dinheiro ou percentual),
  rotulos de turno e elemento de legenda.

## [0.2.0] - 2026-09-18

### Adicionado

**Núcleo (Java 21 + Spring Boot 3.5)**
- Motor de simulação econômica (`MotorSimulacao`) com mercado potencial por
  município, competitividade relativa, dois tetos de produção (equipe e
  patrimônio), custos, tributos, subsídios, valuation lastreado e preço de ação.
- Motor de turnos (`ServicoTurno`) e agendador de 1 hora (`AgendadorTurno`):
  1 turno = 1 hora real = 1 mês de jogo.
- Setores alimentício, imobiliário e construção, parametrizados em `Setor`.
- Gestão de empresas: fundação, aporte de capital, contratação, demissão,
  marketing, salário, payout, abertura de capital e empreendimentos.
- Empreendimentos com parcelas, atraso por falta de caixa e entrega em patrimônio.
- Endividamento automático e falência por alavancagem.
- Estrutura política brasileira simplificada: 13 cargos nas três esferas,
  mandatos, NPCs, oito instrumentos legais, tramitação completa com votação,
  sanção, veto, sanção tácita e derrubada de veto.
- Efeito real das leis sobre impostos, subsídios, regulação, infraestrutura,
  programa social e zoneamento.
- Mercado de ações lastreado: compra no primário, venda pelo mercado,
  dividendos mensais proporcionais e carteira com retorno total.
- Livro-razão (`LancamentoFinanceiro`) de toda movimentação financeira.
- Linha de auditoria encadeada por SHA-256 com verificação de integridade.
- Estatísticas por turno e por setor, com séries históricas e ranking.
- Macroeconomia: PIB, desemprego, inflação, juros (regra tipo Taylor),
  aprovação do governo e estabilidade.
- Carga inicial idempotente do mundo (1 país, 3 estados, 4 municípios,
  cadeiras políticas e 7 empresas concorrentes).
- API REST completa em `/api` com tratamento de erro padronizado.

**Serviço analítico (Python 3, biblioteca padrão)**
- Modelo de choques setoriais determinístico por turno (sazonalidade, ruído com
  semente e reação a inflação e juros).
- Projeção de retorno de investimento.
- Servidor HTTP com `/saude`, `/modificadores` e `/projecao`.
- Integração com fallback: o Java reproduz a fórmula se o serviço estiver fora.

**Frontend (HTML + CSS + JS)**
- Sete páginas, uma por arquivo: login, painel, empresas, empresa,
  investimentos, política, estatísticas e auditoria.
- Folha de estilo única (`css/app.css`) com paleta clara e sólida.
- Um arquivo JS por página mais `js/app.js` compartilhado.
- Relógio de turno no cabeçalho e gráficos de barras sem dependência externa.

**Documentação**
- `docs/RELATORIO.md` (documento vivo), `docs/ARQUITETURA.md`,
  `docs/REGRAS-DO-JOGO.md`, `docs/API.md`, `docs/AUDITORIA.md`,
  `docs/ROADMAP.md` e este changelog.

**Testes**
- 15 testes Java (7 do motor econômico, 8 de integração do ciclo do jogo).
- 9 testes Python do modelo setorial.

### Corrigido
- Cadeia de auditoria quebrava quando os atalhos de registro chamavam o método
  principal internamente, furando o proxy transacional do Spring: cada entrada
  passou a abrir a própria transação.
- Verificação de integridade falhava por diferença de precisão entre o instante
  em memória e o gravado no banco: o instante passou a ser truncado a
  milissegundos antes de assinar.
- Eventos gerados durante o processamento do turno recebiam o número do turno
  anterior: o relógio passou a publicar o turno corrente em memória.

### Alterado
- Margens dos setores recalibradas para margem bruta sobre insumos (42%, 34% e
  33%), de modo que folha, tributos e depreciação caibam no resultado.
- Concorrência passou a disputar apenas o mercado equivalente à capacidade
  instalada do grupo, e não o mercado potencial inteiro da cidade.
- Carga inicial passou a deixar um terço das cadeiras legislativas vagas, para
  que jogadores possam entrar na política desde o turno zero.
- Empresas concorrentes do mundo inicial passaram a ter equipe e marketing
  derivados do patrimônio, mantendo-se equilibradas a mudanças de parâmetro.
- Protótipo original (HTML solto e scripts Python) movido para `legado/`.

## [0.1.0] - anterior

### Adicionado
- Protótipo de interface em HTML/CSS/JS com países, ataques e estatísticas.
- Scripts Python iniciais de país e motor de combate.
