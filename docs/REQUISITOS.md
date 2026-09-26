# Relatório de requisitos

<!-- Arquivo gerado por ferramentas/gerar_documentacao.py. Nao edite a mao: mude docs/requisitos.toml ou o codigo e rode o gerador. -->

Projeto **Complexus**, versão `0.7.0`. 21 de 38 requisitos entregues (55%).

Cada requisito declara o critério de aceite, os arquivos que o implementam e os testes que o cobrem. O gerador falha se um arquivo declarado não existir, então a rastreabilidade não envelhece em silêncio.

## Panorama

| Situação | Funcionais | Não funcionais | Total |
|---|---|---|---|
| Entregue | 14 | 7 | 21 |
| Parcial | 1 | 0 | 1 |
| Planejado | 11 | 5 | 16 |

17 dos 21 requisitos entregues têm teste automatizado declarado.

**Lacunas de cobertura** — entregues sem teste declarado:

| Requisito | Título | Situação |
|---|---|---|
| RF-24 | Mapa hierárquico com zoom semântico | entregue |
| RF-26 | Mapa alimentado pelo backend | entregue |
| RNF-08 | Resiliência ao serviço analítico | entregue |
| RNF-11 | Frontend sem dependência externa | entregue |

## Requisitos funcionais

| ID | Título | Situação | Prioridade | Versão | Testes |
|---|---|---|---|---|---|
| RF-01 | Cadastro e identificação de jogadores | entregue | Alta | 0.2.0 | 1 |
| RF-02 | Administração de empresas | entregue | Alta | 0.2.0 | 2 |
| RF-03 | Simulação econômica por turno | entregue | Alta | 0.2.0 | 4 |
| RF-04 | Empreendimentos dos setores imobiliário e construção | entregue | Media | 0.2.0 | 2 |
| RF-05 | Ocupação de cargos políticos nas três esferas | entregue | Alta | 0.2.0 | 1 |
| RF-06 | Tramitação de projetos de lei | entregue | Alta | 0.2.0 | 2 |
| RF-07 | Efeito das leis sobre a economia | entregue | Alta | 0.2.0 | 1 |
| RF-08 | Mercado de ações com investimento lastreado | entregue | Alta | 0.2.0 | 1 |
| RF-09 | Turno automático de uma hora | entregue | Alta | 0.2.0 | 1 |
| RF-10 | Eleições com candidatura e campanha | planejado | Alta | - | 0 |
| RF-11 | Quarto setor econômico | planejado | Media | - | 0 |
| RF-12 | Contratos entre empresas | planejado | Media | - | 0 |
| RF-13 | Eventos de mundo | planejado | Media | - | 0 |
| RF-14 | Notificações de turno para o jogador | planejado | Baixa | - | 0 |
| RF-15 | Mais municípios e estados | planejado | Baixa | - | 0 |
| RF-16 | Comércio exterior entre países | planejado | Baixa | - | 0 |
| RF-17 | Fusões e aquisições | planejado | Baixa | - | 0 |
| RF-18 | Impeachment e cassação | planejado | Baixa | - | 0 |
| RF-19 | Ranking de jogadores | planejado | Baixa | - | 0 |
| RF-20 | Projeções de investimento na interface | parcial | Baixa | - | 1 |
| RF-21 | Estatísticas gerais e séries históricas | entregue | Alta | 0.2.0 | 1 |
| RF-22 | Canal de atualizações do jogador | entregue | Alta | 0.2.1 | 2 |
| RF-23 | Motor de combate por fases | entregue | Alta | 0.5.0 | 1 |
| RF-24 | Mapa hierárquico com zoom semântico | entregue | Media | 0.5.0 | 0 |
| RF-25 | Backend consome o motor de combate | planejado | Alta | - | 0 |
| RF-26 | Mapa alimentado pelo backend | entregue | Media | 0.7.0 | 0 |

### RF-01 — Cadastro e identificação de jogadores

**Situação:** entregue · **Prioridade:** Alta · **Entregue em:** `0.2.0`

Criar conta, entrar na partida e manter um caixa pessoal que financia empresas e investimentos.

**Critério de aceite:** Um usuário novo se registra, recebe caixa inicial e consegue autenticar em seguida.

**Implementação:**

- `backend/src/main/java/com/complexus/jogador/ServicoJogador.java`
- `backend/src/main/java/com/complexus/jogador/JogadorController.java`
- `backend/src/main/resources/static/login.html`

**Testes:**

- `CredencialEstavelTest#registroEAutenticacao`

> Autenticação simplificada: ver RNF-01.

### RF-02 — Administração de empresas

**Situação:** entregue · **Prioridade:** Alta · **Entregue em:** `0.2.0`

Fundar empresa nos setores disponíveis e administrá-la: aporte de capital, contratação, demissão, marketing, salário, política de dividendos e abertura de capital.

**Critério de aceite:** Fundar debita o caixa do jogador, cria a empresa com patrimônio e caixa, e cada comando de gestão altera o estado e gera lançamento e evento.

**Implementação:**

- `backend/src/main/java/com/complexus/economia/ServicoEmpresa.java`
- `backend/src/main/java/com/complexus/economia/EmpresaController.java`
- `backend/src/main/resources/static/empresa.html`

**Testes:**

- `FluxoDoJogoTest#fundarEmpresaRegistraTudo`
- `FluxoDoJogoTest#capitalMinimoEValidado`

### RF-03 — Simulação econômica por turno

**Situação:** entregue · **Prioridade:** Alta · **Entregue em:** `0.2.0`

Calcular a cada turno mercado, concorrência, receita, custos, tributos, lucro, valor de mercado e preço da ação de todas as empresas ativas, além dos indicadores macroeconômicos.

**Critério de aceite:** Um turno processado atualiza o resultado de todas as empresas, grava histórico e recalcula PIB, desemprego, inflação, juros e aprovação.

**Implementação:**

- `backend/src/main/java/com/complexus/economia/MotorSimulacao.java`
- `backend/src/main/java/com/complexus/core/ServicoTurno.java`

**Testes:**

- `MotorSimulacaoTest#receitaLimitadaPelaCapacidade`
- `MotorSimulacaoTest#efeitoDeSubsidioEregulacao`
- `MotorSimulacaoTest#valuationRespeitaOLastro`
- `FluxoDoJogoTest#turnoCompleto`

> A produção é limitada pelo menor teto entre equipe e patrimônio (decisão D-02).

### RF-04 — Empreendimentos dos setores imobiliário e construção

**Situação:** entregue · **Prioridade:** Media · **Entregue em:** `0.2.0`

Tocar obras de longo prazo que consomem caixa em parcelas, atrasam quando falta caixa e viram patrimônio ao concluir.

**Critério de aceite:** Obra iniciada consome uma parcela por turno; sem caixa o prazo escorrega; ao concluir o valor estimado entra no patrimônio.

**Implementação:**

- `backend/src/main/java/com/complexus/economia/Empreendimento.java`
- `backend/src/main/java/com/complexus/core/ServicoTurno.java`

**Testes:**

- `FluxoDoJogoTest#cicloDeEmpreendimento`
- `FluxoDoJogoTest#alimenticioNaoTemObra`

### RF-05 — Ocupação de cargos políticos nas três esferas

**Situação:** entregue · **Prioridade:** Alta · **Entregue em:** `0.2.0`

Assumir cargos federais, estaduais e municipais, de presidente a vereador, com mandato por prazo e cadeiras ocupadas por NPCs quando não há jogador.

**Critério de aceite:** Havendo vaga a posse é direta; o mandato tem início e fim em turnos; ao vencer, a cadeira é renovada por NPC.

**Implementação:**

- `backend/src/main/java/com/complexus/politica/CargoPolitico.java`
- `backend/src/main/java/com/complexus/politica/ServicoPolitica.java`
- `backend/src/main/resources/static/politica.html`

**Testes:**

- `FluxoDoJogoTest#mundoInicialCarregado`

### RF-06 — Tramitação de projetos de lei

**Situação:** entregue · **Prioridade:** Alta · **Entregue em:** `0.2.0`

Propor, pautar, votar (jogadores e NPCs), apurar, sancionar, vetar, sancionar tacitamente e derrubar veto por dois terços.

**Critério de aceite:** Um projeto percorre rascunho, votação e apuração, e termina sancionado, rejeitado ou vetado sem intervenção manual.

**Implementação:**

- `backend/src/main/java/com/complexus/politica/ProjetoDeLei.java`
- `backend/src/main/java/com/complexus/politica/ServicoPolitica.java`

**Testes:**

- `FluxoDoJogoTest#tramitacaoDeProjeto`
- `FluxoDoJogoTest#votoForaDaCasaERecusado`

### RF-07 — Efeito das leis sobre a economia

**Situação:** entregue · **Prioridade:** Alta · **Entregue em:** `0.2.0`

Lei sancionada altera alíquotas, subsídios, custo regulatório, investimento em infraestrutura, programa social ou zoneamento, e isso entra no cálculo das empresas.

**Critério de aceite:** Sancionar uma lei tributária muda a alíquota do território e o efeito aparece no resultado do turno seguinte.

**Implementação:**

- `backend/src/main/java/com/complexus/politica/TipoProjeto.java`
- `backend/src/main/java/com/complexus/politica/ServicoPolitica.java`

**Testes:**

- `MotorSimulacaoTest#efeitoDeSubsidioEregulacao`

### RF-08 — Mercado de ações com investimento lastreado

**Situação:** entregue · **Prioridade:** Alta · **Entregue em:** `0.2.0`

Abrir capital, comprar e vender ações e receber dividendos mensais proporcionais ao lucro da empresa.

**Critério de aceite:** A compra credita o caixa da empresa (mercado primário) e o investidor recebe dividendo no turno seguinte.

**Implementação:**

- `backend/src/main/java/com/complexus/investimento/ServicoInvestimento.java`
- `backend/src/main/resources/static/investimentos.html`

**Testes:**

- `FluxoDoJogoTest#mercadoDeAcoes`

### RF-09 — Turno automático de uma hora

**Situação:** entregue · **Prioridade:** Alta · **Entregue em:** `0.2.0`

O mundo avança sozinho a cada hora de tempo real, equivalente a um mês de jogo, com avanço manual disponível pela API.

**Critério de aceite:** O agendador dispara no intervalo configurado e a API expõe o turno corrente e a contagem para o próximo.

**Implementação:**

- `backend/src/main/java/com/complexus/core/AgendadorTurno.java`
- `backend/src/main/java/com/complexus/core/JogoController.java`

**Testes:**

- `FluxoDoJogoTest#turnoCompleto`

### RF-10 — Eleições com candidatura e campanha

**Situação:** planejado · **Prioridade:** Alta

Disputa de cargos por candidatura, campanha e apuração por aprovação, substituindo a posse simplificada.

**Critério de aceite:** Ao fim do mandato, candidatos concorrem e o vencedor sai da apuração, não da ordem de chegada.

**Testes:** nenhum declarado.

> Hoje a posse é direta em vaga livre ou desafio a NPC com aprovação abaixo de 45.

### RF-11 — Quarto setor econômico

**Situação:** planejado · **Prioridade:** Media

Acrescentar um setor novo (varejo ou energia) para validar que o motor aceita setores apenas com parâmetros.

**Critério de aceite:** O setor entra alterando somente o enum Setor e a carga inicial, sem tocar no motor de simulação.

**Testes:** nenhum declarado.

### RF-12 — Contratos entre empresas

**Situação:** planejado · **Prioridade:** Media

Fornecimento entre empresas, ligando construção e imobiliário em cadeia produtiva.

**Critério de aceite:** Uma empresa compra insumo de outra e o custo de uma vira receita da outra no mesmo turno.

**Testes:** nenhum declarado.

### RF-13 — Eventos de mundo

**Situação:** planejado · **Prioridade:** Media

Crises, secas, booms setoriais e outros choques narrativos que quebrem a previsibilidade do ciclo.

**Critério de aceite:** Um evento ativo altera os parâmetros do turno e aparece no canal de atualizações.

**Testes:** nenhum declarado.

### RF-14 — Notificações de turno para o jogador

**Situação:** planejado · **Prioridade:** Baixa

Avisar o jogador do que aconteceu no fechamento sem exigir que ele abra a tela.

**Critério de aceite:** O jogador recebe o resumo do turno por um canal fora da interface.

**Testes:** nenhum declarado.

### RF-15 — Mais municípios e estados

**Situação:** planejado · **Prioridade:** Baixa

Ampliar o mapa para aumentar a disputa local e a variedade de mercados.

**Critério de aceite:** A carga inicial cria os novos territórios e as empresas disputam mercados distintos.

**Testes:** nenhum declarado.

### RF-16 — Comércio exterior entre países

**Situação:** planejado · **Prioridade:** Baixa

Trocas comerciais entre países, exigindo um segundo país jogável.

**Critério de aceite:** Exportação e importação alteram PIB e balança dos países envolvidos.

**Testes:** nenhum declarado.

### RF-17 — Fusões e aquisições

**Situação:** planejado · **Prioridade:** Baixa

Compra de participação relevante e troca de controle entre jogadores.

**Critério de aceite:** Ao ultrapassar o limite de participação, o comprador assume o controle da empresa.

**Testes:** nenhum declarado.

### RF-18 — Impeachment e cassação

**Situação:** planejado · **Prioridade:** Baixa

Responsabilização política, fechando o ciclo de poder com perda de mandato.

**Critério de aceite:** Um processo aprovado encerra o mandato do titular antes do prazo.

**Testes:** nenhum declarado.

### RF-19 — Ranking de jogadores

**Situação:** planejado · **Prioridade:** Baixa

Classificação por patrimônio e influência política entre partidas.

**Critério de aceite:** O ranking ordena jogadores por critério declarado e atualiza a cada turno.

**Testes:** nenhum declarado.

### RF-20 — Projeções de investimento na interface

**Situação:** parcial · **Prioridade:** Baixa

Mostrar ao investidor a projeção de retorno calculada pelo serviço analítico.

**Critério de aceite:** A tela de investimentos exibe a série projetada vinda de /projecao.

**Implementação:**

- `analytics/modelo_setorial.py`

**Testes:**

- `testes_modelo#test_projecao_acumula_dividendos`

> O cálculo existe e é testado no Python; falta a interface consumir o endpoint.

### RF-21 — Estatísticas gerais e séries históricas

**Situação:** entregue · **Prioridade:** Alta · **Entregue em:** `0.2.0`

Consolidar por turno os agregados do mundo e por setor, com séries históricas e ranking de empresas.

**Critério de aceite:** Cada turno grava um snapshot e uma linha por setor, consultáveis por série histórica.

**Implementação:**

- `backend/src/main/java/com/complexus/estatistica/ServicoEstatistica.java`
- `backend/src/main/resources/static/estatisticas.html`

**Testes:**

- `FluxoDoJogoTest#turnoCompleto`

### RF-22 — Canal de atualizações do jogador

**Situação:** entregue · **Prioridade:** Alta · **Entregue em:** `0.2.1`

Feed com os fatos públicos do mundo e as ações do próprio jogador, em linguagem de jogo.

**Critério de aceite:** O feed mostra fatos públicos a todos, movimentos privados só ao autor, e nunca hash, ator ou detalhe interno.

**Implementação:**

- `backend/src/main/java/com/complexus/atualizacoes/ServicoAtualizacoes.java`
- `backend/src/main/resources/static/atualizacoes.html`

**Testes:**

- `CanalEAcessoAdminTest#canalNaoExpoeDadoInterno`
- `CanalEAcessoAdminTest#movimentoPrivadoFicaComODono`

### RF-23 — Motor de combate por fases

**Situação:** entregue · **Prioridade:** Alta · **Entregue em:** `0.5.0`

Resolver guerras entre nações por fases (inteligência, ar, golpe profundo, mar, terra), com cada plataforma como entidade individual, logística por transporte e verba, e gate nuclear que exige doutrina permissiva.

**Critério de aceite:** Uma guerra entre duas nações do inventário resolve em rodadas e devolve relatório; a mesma semente produz sempre o mesmo resultado.

**Implementação:**

- `src/combate/motor.py`
- `src/combate/fases.py`
- `src/combate/unidades.py`
- `src/simular_batalha.py`
- `data/paises.json`

**Testes:**

- `test_combate`

> Veio da main, feito em outra sessão. O backend Java ainda não consome o motor: ver RF-25.

### RF-24 — Mapa hierárquico com zoom semântico

**Situação:** entregue · **Prioridade:** Media · **Entregue em:** `0.5.0`

Navegar Estados, Cidades e Estradas em três níveis de zoom, com carregamento por viewport, painel de decisão e herança de alíquota e infraestrutura do estado para as cidades filhas.

**Critério de aceite:** Aproximar revela cidades e estradas; clicar numa cidade abre o painel com PIB e infraestrutura já ajustados pelo estado pai.

**Implementação:**

- `backend/src/main/resources/static/mapa.html`
- `backend/src/main/resources/static/js/mapa.js`
- `backend/src/main/resources/static/dados/mapa/estados.geojson`

**Testes:** nenhum declarado.

> Veio da main. Integrado ao frontend do jogo na 0.5.0, com Leaflet servido localmente. 
A herança de estado só no cliente foi substituída por dados reais da API na RF-26 — os campos mostrados 
no painel (população, tesouro, alíquota etc.) agora vêm de /api/politica/territorios, e a decisão persiste 
de verdade via proposta de lei. Ver RF-26. 
O travamento de zoom/drill-down no navegador embutido de teste foi corrigido de vez: `zoomAnimation: false` 
no L.map() (aplicado na RF-26), como a observação anterior já indicava.

### RF-25 — Backend consome o motor de combate

**Situação:** planejado · **Prioridade:** Alta

Expor a resolução de guerra pela API do backend Java, chamando o motor Python no mesmo padrão do serviço analítico, com fallback e registro na auditoria.

**Critério de aceite:** Uma guerra disparada pela API gera evento de auditoria, lançamentos no razão e efeito sobre a estabilidade do país.

**Testes:** nenhum declarado.

> Hoje o motor só roda por CLI. É a ligação que falta entre os dois lados do projeto.

### RF-26 — Mapa alimentado pelo backend

**Situação:** entregue · **Prioridade:** Media · **Entregue em:** `0.7.0`

Trocar os dados ilustrativos do mapa pelos territórios reais da partida, servidos pela API, e persistir as decisões tomadas no painel.

**Critério de aceite:** Municípios e estados do mapa vêm de /api/politica/territorios e as decisões mudam o estado da partida.

**Implementação:**

- `backend/src/main/resources/static/mapa.html`
- `backend/src/main/resources/static/js/mapa.js`
- `backend/src/main/resources/static/css/app.css`
- `backend/src/main/resources/static/dados/mapa/estados.geojson`
- `backend/src/main/resources/static/dados/mapa/cidades.geojson`
- `backend/src/main/resources/static/dados/mapa/estradas.geojson`

**Testes:** nenhum declarado.

> O GeoJSON estático virou só geometria (id/nome/sigla), casada no cliente por sigla (estado) ou 
nome+sigla-do-estado (município) com o registro vivo de /api/politica/territorios — recortado pros 3 estados 
e 4 municípios que a carga inicial (SeedDados) realmente cria, então toda feature no mapa corresponde a um 
território de verdade. A "herança" de alíquota que rodava só no cliente na RF-24 foi removida: cada painel 
mostra os campos reais do território (população, tesouro, alíquota, índice etc.), sem recalcular nada — o 
efeito econômico de verdade já é do motor de simulação do backend, o mapa só exibe. 
Decisão persistente: em vez de mutar estado local, o painel de Estado/Município propõe um projeto de lei de 
verdade (POST /api/politica/projetos) quando o jogador tem mandato legislativo ou executivo naquele 
território — mesmo fluxo de rascunho → pautar → votar → sancionar da página Política, só com o mandato e o 
território já pré-selecionados. Sem mandato, o painel convida a assumir um cargo em vez de fingir a ação. 
Testado hoje contra o backend rodando de verdade (não só lido): entrei com a conta demo, assumi um mandato 
de Deputado Estadual em Minas Gerais via /api/politica/mandatos, propus uma alíquota estadual pelo mapa e 
confirmei via GET /api/politica/projetos que o rascunho foi persistido no banco (H2). Município e o painel 
somente-leitura de estrada (a malha viária ainda não é território — ver observação abaixo) também conferidos. 
Sem teste automatizado: é interação de mapa, mesma limitação da RF-24. 
Estradas continuam ilustrativas (contexto geográfico entre as cidades da carga inicial): o backend não modela 
infraestrutura viária como território, então a camada não tem painel de decisão, só um card informativo — 
não dá pra "persistir uma decisão" que não existe no domínio. 
De brinde, o travamento de zoom do Leaflet que a observação da RF-24 registrava foi corrigido (`zoomAnimation: 
false` no L.map()) — o drill-down completa normalmente agora. 

Segunda rodada (mesma versão, antes do commit): a hierarquia semântica ganhou um nível — País > Estado > 
Cidade, renderização mutuamente exclusiva entre os três (cada camada só existe montada em Leaflet dentro da 
sua própria faixa de zoom; ao cruzar de nível a anterior é desmontada, não só escondida). País não tem 
geometria própria no jogo, então é um único marcador clicável no centro do Brasil, com painel alimentado 
pelos campos reais de Pais (população, tesouro, PIB, alíquota federal, gasto social, estabilidade, aprovação, 
desemprego, renda média) e o mesmo formulário de proposta de lei, agora também pra esfera FEDERAL (IMPOSTO_ 
EMPRESARIAL entrou no conjunto de tipos sem setor). estados.geojson trocou a geometria ilustrativa de 3 
estados por fronteiras reais dos 27 estados brasileiros — dataset público codeforamerica/click_that_hood 
(brazil-states.geojson), simplificado com Douglas-Peucker (tolerância ~1,3km, 85585 → 12203 pontos) porque 
não há mapshaper/turf offline neste projeto; arquivo caiu de ~3,3MB pra ~235KB. Dos 27, só os 3 que o 
SeedDados cria têm registro vivo — os outros 24 aparecem só como contexto geográfico, e o painel deixa isso 
explícito ("este estado ainda não tem território de partida") em vez de fingir dado que não existe. Estradas 
passaram a aparecer a partir do nível Estado (antes só em Cidade), pra serem o destaque visual da malha 
logística contra um território agora estilizado com cor neutra e baixa opacidade — estilo sutil de propósito, 
ver js/mapa.js. O painel de estrada ganhou uma estrutura de métricas com placeholders explícitos (índice de 
desenvolvimento, pavimentação, capacidade, todos null no geojson) já pronta pra receber dado real do motor de 
simulação se um domínio de infraestrutura viária for modelado no futuro — não é dado inventado, é null 
exibido como "—". Testado de novo ao vivo: painel de País (dado real), Goiás sem dado (fallback correto), 
Minas Gerais com o formulário de novo funcional, e o painel de estrada com os placeholders. 

Terceira rodada (mesma versão, antes do commit): pedido explícito do usuário — "demarque todas as capitais 
do país e as principais rodovias, seguindo a malha real". cidades.geojson ganhou as 27 capitais estaduais 
(coordenadas reais, fato geográfico público, não precisou de dataset externo); só São Paulo, Rio de Janeiro 
e Belo Horizonte têm município seedado, então o painel de cidade também ganhou o mesmo fallback "sem 
território de partida" que o de estado já tinha. estradas.geojson trocou as 3 rotas retas ilustrativas por 
geometria real de 10 rodovias federais principais (BR-101, BR-116, BR-040, BR-153, BR-364, BR-230, BR-070, 
BR-060, BR-050, BR-262) — extraída do OpenStreetMap via Overpass API (dados sob licença ODbL, atribuição já 
existe no mapa via o crédito do tile OSM), filtrada pelas classes motorway/trunk/primary (e _link) que 
carregam a ref BR-xxx, e simplificada com o mesmo Douglas-Peucker (tolerância ~2,2km — escala de país/estado, 
não navegação turn-by-turn): caiu de ~279 mil pra ~49 mil pontos, arquivo final ~1MB pras 10 rodovias juntas. 
Cada rodovia virou um MultiLineString (os segmentos do Overpass não vêm ordenados ponta-a-ponta, mas o 
Leaflet desenha cada um independente, então isso não afeta o resultado visual). bboxDeGeometria precisou 
aprender a profundidade de MultiLineString (mesma do Polygon) e aoRenderizarEstrada a desenhar um array de 
segmentos em vez de uma linha só — sem isso o Overpass API do overpass-api.de recusou a conexão (406) e um 
mirror (osm.ch) provou ser regional (só Suíça); o mirror openstreetmap.fr funcionou, mas bloqueou o 
User-Agent padrão do urllib (403) até eu forçar um header de navegador. O campo cidadesConectadas, que só 
fazia sentido pras 3 rotas antigas ligando 2 cidades específicas, saiu das rodovias novas — o painel omite a 
linha "Conecta" quando o campo não existe. Testado ao vivo: as 10 rodovias renderizam com traçado real 
(visivelmente sinuoso, não mais linha reta) a partir do nível Estado, painel de rodovia sem a linha "Conecta" 
e com os placeholders, capital sem dado (Goiânia) com o fallback correto — via fetch com cache:no-store pra 
contornar cache HTTP do navegador de teste (mesma limitação de ferramenta já documentada nas rodadas 
anteriores, não um bug do app). 

Quarta rodada (mesma versão, antes do commit): usuário reportou (com print) trecho picotado da BR-230 perto 
da represa de Boa Esperança (MA/PI). Diagnóstico: não era filtro de classe de via (já tinha sido alargado pra 
"sem filtro de highway, só ref" na rodada anterior) — era match exato de "ref". Vários trechos de rodovia 
brasileira são cossinalizados no OSM (a mesma pista carrega duas BRs) e o valor do ref vem combinado, tipo 
"BR-230;BR-135" — a query `way["ref"="BR-230"]` não casa essa string, então o trecho combinado simplesmente 
não vinha, abrindo um buraco visível na malha renderizada. Corrigido trocando pra `way["ref"~"(^|;)BR-230($|;)"]` 
(regex com fronteira de ";", não precisa escapar "-" fora de [...]) e refazendo o download das 10 rodovias — 
saltou de ~19 mil pra ~26,5 mil vias no total, confirmando que não era só a BR-230: BR-116 quase dobrou (5559 
→ 8285 vias), BR-050 e BR-364 mais que dobraram também. Escrevi um checador de conectividade à parte 
(distância euclidiana da ponta de cada segmento até a ponta mais próxima de outro segmento da mesma rodovia, 
via grade espacial pra não ser O(n²)) pra achar pontas soltas (>1,1km sem vizinho): caiu de dezenas por 
rodovia pra 1-11, o que sobrou são majoritariamente pontas de rota de verdade (litoral, fronteira, onde o 
ref muda). Arquivo final ~1,5MB pras 10 rodovias. Reproduzi a visão exata do print do usuário (mesma represa, 
mesmas cidades — Pastos Bons, São João dos Patos, Guadalupe) e confirmei ao vivo que o traçado agora é 
contínuo ali.

## Requisitos não funcionais

| ID | Título | Situação | Prioridade | Versão | Testes |
|---|---|---|---|---|---|
| RNF-01 | Autenticação real | planejado | Alta | - | 0 |
| RNF-02 | Banco PostgreSQL com migração versionada | planejado | Alta | - | 0 |
| RNF-03 | Trava distribuída para a auditoria | planejado | Media | - | 0 |
| RNF-04 | Valores monetários em BigDecimal | planejado | Baixa | - | 0 |
| RNF-05 | Cache das consultas de estatísticas | planejado | Baixa | - | 0 |
| RNF-06 | Integridade verificável do histórico | entregue | Alta | 0.2.0 | 1 |
| RNF-07 | Isolamento do acesso administrativo | entregue | Alta | 0.2.1 | 2 |
| RNF-08 | Resiliência ao serviço analítico | entregue | Media | 0.2.0 | 0 |
| RNF-09 | Reprodutibilidade da simulação | entregue | Media | 0.2.0 | 2 |
| RNF-10 | Estabilidade das credenciais entre versões | entregue | Alta | 0.3.0 | 1 |
| RNF-11 | Frontend sem dependência externa | entregue | Media | 0.2.0 | 0 |
| RNF-12 | Documentação viva e rastreável | entregue | Alta | 0.4.0 | 3 |

### RNF-01 — Autenticação real

**Situação:** planejado · **Prioridade:** Alta

Spring Security com BCrypt e token de sessão, substituindo o jogadorId que viaja no corpo da requisição.

**Critério de aceite:** Nenhuma rota aceita agir em nome de um jogador sem credencial verificada.

**Testes:** nenhum declarado.

> Bloqueia também a correção da limitação L-07 (detalhe de empresa de terceiros).

### RNF-02 — Banco PostgreSQL com migração versionada

**Situação:** planejado · **Prioridade:** Alta

Trocar o H2 em arquivo por PostgreSQL com Flyway, pré-requisito para mais de um processo.

**Critério de aceite:** O esquema é criado por migração versionada, não por ddl-auto.

**Testes:** nenhum declarado.

### RNF-03 — Trava distribuída para a auditoria

**Situação:** planejado · **Prioridade:** Media

Garantir a ordem da cadeia de hashes com mais de um servidor gravando.

**Critério de aceite:** Dois processos gravando em paralelo produzem uma cadeia íntegra.

**Testes:** nenhum declarado.

### RNF-04 — Valores monetários em BigDecimal

**Situação:** planejado · **Prioridade:** Baixa

Migrar de double para BigDecimal caso o jogo passe a exigir precisão contábil.

**Critério de aceite:** Somas de lançamentos fecham sem erro de arredondamento.

**Testes:** nenhum declarado.

> Decisão ADR-03: double é suficiente enquanto for simulação de jogo.

### RNF-05 — Cache das consultas de estatísticas

**Situação:** planejado · **Prioridade:** Baixa

Evitar recalcular séries longas a cada requisição quando a partida passar de centenas de turnos.

**Critério de aceite:** A consulta de série histórica responde em tempo constante com o crescimento do histórico.

**Testes:** nenhum declarado.

### RNF-06 — Integridade verificável do histórico

**Situação:** entregue · **Prioridade:** Alta · **Entregue em:** `0.2.0`

Cada fato do jogo entra em uma cadeia encadeada por SHA-256, com verificação que aponta o elo adulterado.

**Critério de aceite:** Alterar um evento antigo faz a verificação apontar exatamente onde a cadeia quebrou.

**Implementação:**

- `backend/src/main/java/com/complexus/auditoria/ServicoAuditoria.java`

**Testes:**

- `FluxoDoJogoTest#cadeiaDeAuditoriaIntegra`

### RNF-07 — Isolamento do acesso administrativo

**Situação:** entregue · **Prioridade:** Alta · **Entregue em:** `0.2.1`

Rotas administrativas exigem credencial no servidor, não apenas ausência de link no menu.

**Critério de aceite:** Chamada a /api/admin sem o cabeçalho correto responde 403.

**Implementação:**

- `backend/src/main/java/com/complexus/config/FiltroAdmin.java`

**Testes:**

- `CanalEAcessoAdminTest#auditoriaExigeCredencial`
- `CanalEAcessoAdminTest#credencialInvalida`

### RNF-08 — Resiliência ao serviço analítico

**Situação:** entregue · **Prioridade:** Media · **Entregue em:** `0.2.0`

O turno é processado mesmo com o serviço Python fora do ar, usando a fórmula equivalente em Java.

**Critério de aceite:** Com o serviço desligado o turno fecha e o relatório registra a origem FALLBACK_JAVA.

**Implementação:**

- `backend/src/main/java/com/complexus/integracao/ClienteAnalitico.java`

**Testes:** nenhum declarado.

> Verificado manualmente; os testes rodam sempre com o serviço desabilitado.

### RNF-09 — Reprodutibilidade da simulação

**Situação:** entregue · **Prioridade:** Media · **Entregue em:** `0.2.0`

Os choques setoriais derivam de semente ligada ao turno, então o mesmo turno produz sempre o mesmo resultado.

**Critério de aceite:** Calcular duas vezes o mesmo turno devolve valores idênticos.

**Implementação:**

- `analytics/modelo_setorial.py`

**Testes:**

- `testes_modelo#test_resultado_e_deterministico`
- `testes_modelo#test_turnos_diferentes_geram_choques_diferentes`

### RNF-10 — Estabilidade das credenciais entre versões

**Situação:** entregue · **Prioridade:** Alta · **Entregue em:** `0.3.0`

O hash de senha não pode mudar de formato sem migração: contas existentes continuam entrando.

**Critério de aceite:** O hash de uma senha conhecida permanece igual entre versões.

**Implementação:**

- `backend/src/main/java/com/complexus/jogador/ServicoJogador.java`

**Testes:**

- `CredencialEstavelTest#hashDeSenhaEstavel`

> Criado depois que a renomeação do projeto trocou o sal e derrubou o login de todas as contas.

### RNF-11 — Frontend sem dependência externa

**Situação:** entregue · **Prioridade:** Media · **Entregue em:** `0.2.0`

Interface em HTML, CSS e JavaScript puros, uma página por arquivo e uma única folha de estilo, sem build e sem depender de rede. A única biblioteca externa, o Leaflet do mapa, é servida da pasta vendor.

**Critério de aceite:** Subir o backend entrega a interface pronta, sem etapa de compilação e sem buscar nada em CDN.

**Implementação:**

- `backend/src/main/resources/static/css/app.css`
- `backend/src/main/resources/static/vendor/leaflet/leaflet.js`

**Testes:** nenhum declarado.

### RNF-12 — Documentação viva e rastreável

**Situação:** entregue · **Prioridade:** Alta · **Entregue em:** `0.4.0`

README, relatórios e catálogo de requisitos se atualizam a partir do código a cada commit, com rastreabilidade de requisito para arquivo e teste.

**Critério de aceite:** O gerador falha quando um requisito aponta para arquivo inexistente ou quando a documentação está desatualizada em relação ao código.

**Implementação:**

- `ferramentas/gerar_documentacao.py`
- `ferramentas/inventario.py`
- `ferramentas/requisitos.py`
- `.githooks/pre-commit`

**Testes:**

- `testes_gerador#test_requisitos_tem_id_unico`
- `testes_gerador#test_implementacao_aponta_para_arquivo_existente`
- `testes_gerador#test_documentacao_esta_atualizada`
