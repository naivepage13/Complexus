# Relatório de requisitos

<!-- Arquivo gerado por ferramentas/gerar_documentacao.py. Nao edite a mao: mude docs/requisitos.toml ou o codigo e rode o gerador. -->

Projeto **Complexus**, versão `0.6.0`. 20 de 38 requisitos entregues (53%).

Cada requisito declara o critério de aceite, os arquivos que o implementam e os testes que o cobrem. O gerador falha se um arquivo declarado não existir, então a rastreabilidade não envelhece em silêncio.

## Panorama

| Situação | Funcionais | Não funcionais | Total |
|---|---|---|---|
| Entregue | 13 | 7 | 20 |
| Parcial | 1 | 0 | 1 |
| Planejado | 12 | 5 | 17 |

17 dos 20 requisitos entregues têm teste automatizado declarado.

**Lacunas de cobertura** — entregues sem teste declarado:

| Requisito | Título | Situação |
|---|---|---|
| RF-24 | Mapa hierárquico com zoom semântico | entregue |
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
| RF-26 | Mapa alimentado pelo backend | planejado | Media | - | 0 |

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
Sem teste automatizado: a herança de estado roda só no cliente. 
Renderização verificada (camadas, paleta, dados servidos pelo backend); o zoom e o drill-down NÃO puderam ser 
verificados aqui, porque a animação de zoom do Leaflet não completa no navegador embutido usado nos testes — 
reproduzido com uma instância limpa do Leaflet, e com `zoomAnimation: false` o zoom volta a funcionar. 
Conferir num navegador comum; se também travar, a correção é criar o mapa com `zoomAnimation: false`.

### RF-25 — Backend consome o motor de combate

**Situação:** planejado · **Prioridade:** Alta

Expor a resolução de guerra pela API do backend Java, chamando o motor Python no mesmo padrão do serviço analítico, com fallback e registro na auditoria.

**Critério de aceite:** Uma guerra disparada pela API gera evento de auditoria, lançamentos no razão e efeito sobre a estabilidade do país.

**Testes:** nenhum declarado.

> Hoje o motor só roda por CLI. É a ligação que falta entre os dois lados do projeto.

### RF-26 — Mapa alimentado pelo backend

**Situação:** planejado · **Prioridade:** Media

Trocar os dados ilustrativos do mapa pelos territórios reais da partida, servidos pela API, e persistir as decisões tomadas no painel.

**Critério de aceite:** Municípios e estados do mapa vêm de /api/politica/territorios e as decisões mudam o estado da partida.

**Testes:** nenhum declarado.

> Hoje o mapa lê GeoJSON estático e a herança de estado vive só no navegador.

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
