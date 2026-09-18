# Changelog

Formato baseado em [Keep a Changelog](https://keepachangelog.com/pt-BR/1.1.0/).
Este arquivo é a linha de auditoria do desenvolvimento: nenhuma entrega entra
sem uma linha aqui. Ver também [docs/RELATORIO.md](docs/RELATORIO.md).

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
