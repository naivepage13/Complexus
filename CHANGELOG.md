# Changelog

Formato baseado em [Keep a Changelog](https://keepachangelog.com/pt-BR/1.1.0/).
Este arquivo é a linha de auditoria do desenvolvimento: nenhuma entrega entra
sem uma linha aqui. Ver também [docs/RELATORIO.md](docs/RELATORIO.md).

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
