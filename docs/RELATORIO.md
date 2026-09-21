# Relatório de desenvolvimento — Complexus

> **Documento vivo.** É atualizado a cada entrega. Sempre que algo muda no
> projeto, esta página e o [CHANGELOG](../CHANGELOG.md) mudam junto.

| Campo | Valor |
|---|---|
| Versão atual | **0.5.0** |
| Data da última atualização | 21/09/2026 |
| Repositório | [naivepage13/Complexus](https://github.com/naivepage13/Complexus) |
| Branch | `claude/game-company-admin-system-331d1c` |
| Estado | Núcleo jogável: economia, política, investimentos, turnos e auditoria, com administração de empresas por unidade |
| Cobertura de testes | 40 testes Java + 9 testes Python, todos verdes |

---

## 1. O que o jogo é

Simulador de administração de **empresas** e de **países**. O jogador:

1. funda e administra empresas nos setores **alimentício, imobiliário e construção**;
2. disputa cargos públicos (presidente, governador, prefeito, senadores,
   deputados e vereadores) e legisla sobre impostos, subsídios e zoneamento;
3. investe em empresas de capital aberto e recebe dividendos lastreados no
   lucro real das companhias;
4. acompanha o mundo evoluir **a cada hora de tempo real = 1 mês de jogo**.

Toda ação relevante entra em uma **linha de auditoria encadeada por hash**, o
que permite reconstruir e provar o histórico da partida. Essa linha é material
administrativo: o jogador acompanha o mundo pelo **canal de atualizações**.

## 2. Arquitetura entregue

| Camada | Tecnologia | Papel |
|---|---|---|
| Núcleo de regras | **Java 21 + Spring Boot 3.5** | Economia, política, investimentos, turnos, auditoria, API REST |
| Persistência | Spring Data JPA + H2 (arquivo) | Modelo relacional, histórico e livro-razão |
| Serviço analítico | **Python 3 (biblioteca padrão)** | Choques setoriais do turno e projeções de investimento |
| Frontend | **HTML + CSS + JS (sem framework)** | Painéis minimalistas, uma página por arquivo, um CSS único |

Detalhamento em [ARQUITETURA.md](ARQUITETURA.md).
Regras e fórmulas em [REGRAS-DO-JOGO.md](REGRAS-DO-JOGO.md).
Endpoints em [API.md](API.md).
Linha de auditoria em [AUDITORIA.md](AUDITORIA.md).

## 3. Entregue até aqui

### 3.1 Economia e empresas
- Três setores parametrizados (`Setor`): margem bruta, volatilidade, giro do
  ativo, receita por funcionário, múltiplo de valuation e capital mínimo.
- Motor de simulação puro (`MotorSimulacao`) em duas camadas: `simularOperacao`
  fecha o mês de cada unidade (mercado potencial, competitividade relativa,
  **dois tetos de produção**, preço praticado, custos e tributo indireto) e
  `consolidar` fecha o mês da empresa (juros, estrutura e imposto sobre o lucro).
- Ciclo completo de gestão: fundar, aportar capital, contratar, demitir,
  ajustar marketing/salário/payout, abrir capital, tocar empreendimentos.
- **Crédito e dívida ativa** (0.5.0): quatro linhas com prazo, taxa travada na
  contratação e amortização constante; nota de crédito de A a D calculada por
  alavancagem, cobertura de juros, lastro e histórico; amortização antecipada,
  renegociação, inadimplência com multa e mora, execução de garantia e falência
  por default. O crédito rotativo automático substituiu o endividamento solto.
- **Estrutura interna da empresa** (0.4.0):
  - **unidades** por município, com patrimônio, equipe e produtividade próprios;
    é a unidade que disputa mercado, e os totais da empresa são a soma delas;
  - abertura, fechamento com deságio e transferência de capital ou equipe entre
    unidades, cada movimento com o seu custo;
  - **linhas de produto** com posicionamento popular, médio ou premium, que
    definem o preço praticado e o custo de insumo pelo mix;
  - **departamentos** (P&D, qualidade, comercial e logística) com orçamento
    mensal e retorno decrescente calibrado pelo porte da empresa.
- Empreendimentos (obras) para imobiliário e construção, com prazo, parcelas,
  atraso por falta de caixa e entrega que vira patrimônio.
- Endividamento automático com juros e falência por alavancagem excessiva.

### 3.2 Política
- 13 cargos nas três esferas, com mandato, poder de legislar e de sancionar.
- Cadeiras ocupadas por NPCs, com **um terço do legislativo reservado** para
  jogadores entrarem sem precisar derrubar ninguém.
- Oito instrumentos legais (`TipoProjeto`), da alíquota federal ao zoneamento.
- Tramitação completa: proposta → pauta → votação (jogador + NPCs) → apuração
  → sanção, veto, sanção tácita ou derrubada de veto por dois terços.
- Efeito real das leis sobre os territórios e sobre o cálculo das empresas.

### 3.3 Investimentos
- Abertura de capital com free float definido pelo dono.
- Compra no mercado primário (o dinheiro entra no caixa da empresa, mantendo o
  lastro) e venda liquidada pelo mercado.
- Dividendos mensais proporcionais à participação, creditados no turno.
- Carteira com preço médio, resultado não realizado, dividendos e retorno total.

### 3.4 Turnos e estatísticas
- `AgendadorTurno` dispara o turno a cada 60 minutos (configurável) e a API
  permite avanço manual.
- Rotina do turno: relógio → choques do Python → política → gastos públicos →
  empresas → obras → macroeconomia → estatísticas → auditoria.
- Macroeconomia com PIB, desemprego, inflação, juros (regra tipo Taylor),
  aprovação do governo e estabilidade.
- Snapshot por turno e consolidação por setor, com séries históricas na API.

### 3.5 Auditoria
- Todo fato vira `EventoAuditoria` encadeado por SHA-256.
- Livro-razão (`LancamentoFinanceiro`) com a contrapartida financeira de cada fato.
- Endpoint de verificação de integridade que aponta o elo divergente.

### 3.6 Frontend
- Oito páginas, cada uma em seu arquivo: login, painel, empresas, empresa,
  investimentos, política, estatísticas e atualizações; mais o console
  administrativo em `admin/auditoria.html`, fora do menu.
- Um único arquivo de estilo (`css/app.css`) com paleta clara e sólida.
- Um arquivo JS por página mais um compartilhado (`js/app.js`).

### 3.7 Visibilidade da informacao (entregue na 0.2.1)
- **Painel inicial enxuto**: saudação, turno, caixa, atalhos com contagem e o
  canal de atualizações. Deixou de repetir lucro, carteira e feed técnico.
- **Resultado de empresa vive na página da empresa**, em três gráficos por
  turno (lucro, receita e margem líquida), com legenda de último, maior e menor.
  Lucro saiu das listagens, do ranking e da resposta de `/api/empresas`.
- **Canal de atualizações** substitui a auditoria no menu: fatos públicos do
  mundo mais as ações do próprio jogador, com filtro por categoria.
- **Auditoria virou área administrativa**: rotas em `/api/admin/auditoria/**`
  exigem `X-Admin-Token` e a página saiu para `admin/auditoria.html`.

### 3.8 Identidade do projeto (entregue na 0.3.0)
- O jogo passou a se chamar **Complexus**, em todas as camadas: pacote Java
  (`com.complexus`), artefato Maven (`complexus-backend`), banco local,
  títulos das páginas, serviço Python e documentação.
- O banco existente foi renomeado em vez de recriado, então partidas em
  andamento continuam de onde estavam.
- Mantidos de propósito: o sal do hash de senha (`geohistoricalsim::v1::`),
  que entra no hash gravado e derrubaria o login de todas as contas se mudasse;
  o prefixo `jogo.*` das propriedades, que descreve o domínio e não a marca; e
  a pasta `legado/`, registro do protótipo original.
- O sal chegou a ser renomeado junto e quebrou o login das contas existentes.
  Foi revertido e agora há teste (`CredencialEstavelTest`) travando o hash.

## 4. Verificações executadas

| Verificação | Resultado |
|---|---|
| `mvn test` (backend) | 40 testes, 0 falhas |
| `python -m unittest` (analytics) | 9 testes, 0 falhas |
| Subida da aplicação + carga do mundo | OK |
| Fluxo ponta a ponta pela API | Empresa → turno → IPO → compra → dividendo → lei sancionada → auditoria íntegra |
| Integração Java ↔ Python | `fonteModificadores: PYTHON` com o serviço no ar; `FALLBACK_JAVA` com ele desligado |
| Renomeação para Complexus | Build, 22 testes, login de conta anterior e cadeia de auditoria íntegra após o rename |
| Separação de visibilidade | `/api/admin/auditoria` responde 403 sem token e 200 com token; canal de atualizações sem hash, ator ou detalhe interno |
| Estrutura por unidade (0.4.0) | Partida nova: empresa fundada com 2 unidades, turno processado com 8 empresas e 9 unidades, receita da empresa igual à soma das filiais (R$ 308 mil + R$ 66 mil) |
| Migração da partida em andamento | Empresas anteriores às unidades recebem sede automática na subida, sem alterar nenhum número do balanço |
| Interface da empresa | Abertura de filial pela página: caixa R$ 600 mil → R$ 262 mil, patrimônio R$ 1,4 mi → R$ 1,7 mi, equipe 40 → 50 |
| Crédito ponta a ponta (0.5.0) | Contrato de R$ 200 mil em 12 turnos: o turno seguinte amortizou R$ 16.666,67 e cobrou R$ 2.408 de juros, prazo 12 → 11, nota caiu de A para B |
| Atualização de banco existente | Partida criada na 0.4.0 e reaberta na 0.5.0: 17 colunas de enum convertidas, contrato gravado e turno processado sem erro |
| Página de finanças | Nota, score decomposto, limites por linha, simulação da parcela e taxa travada (contrato a 14,45% enquanto o mercado oferecia 16,09%) |

Balanceamento observado após a calibragem (7 empresas do mundo inicial):
receita agregada ≈ R$ 3,9 mi/mês, lucro agregado ≈ R$ 150–235 mil/mês,
margem líquida de 4% a 6% e retorno sobre o capital investido entre 8% e 13% ao ano.

## 5. Decisões que moldam o jogo

| # | Decisão | Motivo |
|---|---|---|
| D-01 | 1 turno = 1 hora real = 1 mês de jogo | Pedido do produto; permite lucros mensais e mandatos em anos |
| D-02 | Produção limitada pelo **menor** teto entre equipe e patrimônio | Sem isso, contratar seria a única alavanca e o capital não teria função |
| D-03 | Mercado disputável limitado à capacidade instalada do grupo | O mercado de uma cidade é gigante perto das empresas simuladas; sem o limite a concorrência não existiria |
| D-04 | Compra de ações no mercado primário | Garante lastro: todo aporte vira caixa e capacidade da empresa |
| D-05 | Cadeiras legislativas parcialmente vagas | Garante entrada de jogadores na política desde o turno zero |
| D-06 | Auditoria em transação própria e sincronizada | Mantém a cadeia de hashes encadeada na ordem dos identificadores |
| D-07 | Valores monetários em `double` | Simulação de jogo, não contabilidade real; simplifica o motor. Revisar se houver economia entre servidores |
| D-08 | Serviço Python opcional com fallback em Java | O turno nunca deixa de ser processado por indisponibilidade de um serviço |
| D-09 | Jogador vê o canal de atualizações; auditoria é administrativa | A linha de auditoria expõe ator, hash e detalhes internos de todas as partidas |
| D-10 | Resultado de empresa só na página da empresa, em gráfico | Evita transformar painel e listagens em relatório e mantém o número no contexto certo |
| D-11 | Empresa de capital aberto publica resultado na vitrine de investimentos | Quem vende ação divulga balanço; sem isso o investidor decide no escuro |
| D-12 | Unidade opera, empresa fecha o mês | Juros, estrutura e imposto de renda são da companhia; cobrá-los por filial tributaria cada unidade como se fosse uma empresa separada |
| D-13 | Totais da empresa recalculados a partir das unidades | Um único lugar define patrimônio, equipe e produtividade, então nenhuma operação faz o balanço divergir da estrutura |
| D-14 | Elasticidade-preço dividida entre disputa e demanda | Só na demanda, baixar preço não tiraria cliente do concorrente; só na disputa, o mercado inteiro seria insensível a preço |
| D-15 | Efeito de departamento satura pelo porte | Sem isso, orçamento grande em empresa pequena compraria vantagem infinita |
| D-16 | Taxa travada na contratação | Faz do momento de tomar crédito uma decisão: quem pegou barato continua pagando barato quando a Selic sobe |
| D-17 | Amortização antes do dividendo | Credor antes de sócio; sem isso a empresa distribuiria lucro e daria calote no mesmo turno |
| D-18 | Só o principal sai do caixa na parcela | Os juros já foram deduzidos no resultado; cobrar a parcela inteira os descontaria duas vezes |
| D-19 | Risco encarece em vez de bloquear | Alavancagem alta muda a nota, a taxa e o limite, não a permissão de tomar crédito |
| D-20 | Colunas de enum em `varchar`, não no tipo ENUM do H2 | `ddl-auto=update` não acrescenta valor a um tipo ENUM existente: toda constante nova quebraria a gravação nas partidas já criadas |

## 6. Limitações conhecidas

| # | Limitação | Impacto | Encaminhamento |
|---|---|---|---|
| L-01 | Autenticação simplificada (SHA-256 com sal fixo, sem token) | Não serve para produção aberta | RNF-01 no [ROADMAP](ROADMAP.md) |
| L-02 | Identificação do jogador por `jogadorId` no corpo da requisição | Um cliente malicioso pode agir por outro | RNF-01 |
| L-03 | H2 em arquivo | Um processo por vez | RNF-02 (PostgreSQL) |
| L-04 | Eleições simplificadas (posse por vaga ou desafio a NPC fraco) | Sem campanha nem urna | RF-10 |
| L-05 | Cadeia de auditoria depende de escrita em processo único | Vários servidores exigiriam trava distribuída | RNF-03 |
| L-06 | Balanceamento é inicial | Pode exigir ajuste com jogadores reais | Parâmetros centralizados em `Setor` e `MotorSimulacao` |
| L-07 | `GET /api/empresas/{id}` devolve o balanço completo de qualquer empresa | Um jogador curioso pode consultar o detalhe de um concorrente pela API | Depende de RNF-01: com autenticação, o detalhe completo fica restrito ao dono |
| L-08 | Marketing e esforço comercial são rateados entre unidades pelo patrimônio | O jogador não escolhe onde concentrar a verba | Verba por unidade, se a estrutura mostrar que faz diferença no jogo |
| L-09 | O mix de linhas vale para a empresa inteira | Não dá para vender premium em uma cidade e popular em outra | Mix por unidade, se a demanda local justificar |
| L-10 | A garantia executada é rateada entre as unidades | O jogador não escolhe qual ativo perde | Garantia por unidade, junto com o mix por unidade |
| L-11 | Empresas do sistema não tomam crédito por decisão própria | Só recorrem ao rotativo automático | Política de crédito para NPC, quando houver estratégia de NPC |

## 7. Como rodar

```bash
# 1. serviço analítico (opcional, o jogo funciona sem ele)
python analytics/servico_analitico.py

# 2. backend + frontend
cd backend && mvn spring-boot:run
```

Interface em <http://localhost:8080>. Conta de demonstração: `demo` / `demo1234`.
Console administrativo em <http://localhost:8080/admin/auditoria.html>, com o
token de `jogo.admin.token` (padrão `admin-local`).

## 8. Histórico de versões

| Versão | Data | Entrega |
|---|---|---|
| 0.5.0 | 21/09/2026 | Crédito e dívida ativa: quatro linhas, nota de risco, amortização, renegociação, inadimplência e execução de garantia |
| 0.4.0 | 21/09/2026 | Estrutura interna da empresa: unidades por município, linhas de produto e departamentos, com o motor separado em operação (unidade) e fechamento (empresa) |
| 0.3.0 | 21/09/2026 | Renomeação do projeto para Complexus em todas as camadas, com a partida local preservada |
| 0.2.1 | 18/09/2026 | Separação de visibilidade: painel inicial enxuto, resultado de empresa em gráfico na própria página, canal de atualizações para o jogador e auditoria restrita à administração com token |
| 0.2.0 | 18/09/2026 | Núcleo completo em Spring Boot: economia dos três setores, política das três esferas, investimentos lastreados, turnos automáticos, estatísticas, linha de auditoria e sete painéis novos |
| 0.1.0 | — | Protótipo em HTML/CSS/JS e scripts Python soltos (preservado em `legado/`) |

## 9. Próximo ciclo

Prioridades no [ROADMAP.md](ROADMAP.md). As três primeiras:

1. **RNF-01** — autenticação real (Spring Security + BCrypt + token de sessão).
2. **RF-10** — eleições com candidatura, campanha e apuração por aprovação.
3. **RF-11** — quarto setor (varejo ou energia), validando que o motor aceita
   setores novos apenas com parâmetros.
