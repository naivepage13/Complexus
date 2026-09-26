# Changelog

Formato baseado em [Keep a Changelog](https://keepachangelog.com/pt-BR/1.1.0/).
Este arquivo é a linha de auditoria do desenvolvimento: nenhuma entrega entra
sem uma linha aqui. Ver também [docs/RELATORIO.md](docs/RELATORIO.md).

## [0.8.0] - 2026-09-26

Exclusão de conta pelo jogador, com confirmação em duas etapas.

### Adicionado
- **Zona de risco** no final da página de Perfil (`perfil.html`), isolada por
  borda vermelha, com o botão destrutivo "Excluir conta".
- Modal de confirmação: o botão final começa desabilitado e só habilita
  quando o jogador digita a própria senha atual ou a palavra "EXCLUIR"
  (`js/perfil.js`); nenhuma das duas hipóteses dispara a exclusão sem passar
  pelo modal.
- `ServicoJogador.excluirConta` e `POST /api/jogadores/{id}/excluir`: exigem
  senha atual válida ou a palavra "EXCLUIR" (verificado de novo no servidor),
  desativam a conta (`Jogador.ativo = false`) e registram `CONTA_EXCLUIDA` na
  auditoria. A conta desativada deixa de autenticar.
- Após o sucesso, o navegador limpa o `localStorage` inteiro e redireciona
  para `login.html`.
- `ContaDoJogadorTest#excluirContaComSenha` e `#excluirContaComPalavraChave`.

### Observação
Exclusão lógica, não física: apagar a linha do jogador quebraria a cadeia de
auditoria (ADR-06) e as referências de empresas, carteira e mandatos que
apontam para ele.

## [0.7.0] - 2026-09-26

Menu de configurações e páginas de conta do jogador.

### Adicionado
- Ícone de engrenagem no cabeçalho de todas as páginas do jogador, com menu
  suspenso para Perfil, Novidades, Tutorial e Termos de uso
  (`Interface.montarMenuConfiguracoes` em `js/app.js`).
- **Página Perfil** (`perfil.html`): avatar (upload restrito a PNG/JPEG,
  guardado só no navegador), data de cadastro e horas de partida em campos
  somente leitura, formulário de redefinir senha (exige a senha atual) e
  formulário de alterar e-mail (valida o formato), ambos com estado de
  carregamento no botão de salvar.
- **Página Novidades** (`novidades.html`): histórico de versões do jogo com
  estado de leitura por card (`localStorage`), envelope fechado e com brilho
  quando não lida, aberto e neutro depois de aberta.
- **Página Tutorial** (`tutorial.html`): guia em accordion por categoria
  macro (primeiros passos, empresas, investimentos, política, mapa).
- **Página Termos de uso** (`termos.html`): PDF estático embutido na página
  (`termos-de-uso.pdf`), com botão de download.
- `Jogador.email` e os endpoints `POST /api/jogadores/{id}/senha` e
  `POST /api/jogadores/{id}/email`; `GET /api/jogadores/{id}` passou a
  devolver `email`, `criadoEm` e `horasEmJogo`.
- `ContaDoJogadorTest`, cobrindo redefinição de senha (senha atual incorreta
  recusada) e alteração de e-mail (formato inválido recusado).

### Observação
Avatar e estado de leitura de Novidades ficam só no navegador: não há upload
de arquivo nem tabela de notificações no backend ainda (ver L-08 em
[docs/RELATORIO.md](docs/RELATORIO.md)).

## [0.6.0] - 2026-09-25

Organização do repositório: descarta o que não é mais necessário e corrige uma
sincronia de versão que ficou pra trás. A pasta raiz do projeto também foi
renomeada de `desenvolvimentoGeoHistoricalSim` para `Complexus`.

### Removido
- Pasta `legado/` inteira: os quatro HTML e o CSS do protótipo original, e os
  três arquivos Python já documentados como obsoletos ou quebrados
  (`engine_combate.py`, `pais.py` — instanciava `Pais` com o número errado de
  argumentos — e `main.py` — rodava o loop do jogo só por ser importado).
  Nada no código ativo referenciava esses arquivos; a história continua no
  Git para quem precisar consultar.
- Seção "Arquivos legados" de `docs/REGRAS-DE-COMBATE.md`, que descrevia
  exatamente os três arquivos acima e dizia que deveriam ser removidos.

### Corrigido
- `backend/pom.xml` estava preso em `0.4.1` desde antes do merge da 0.5.0: o
  commit que integrou combate e mapa esqueceu de bumpar a versão do artefato.
  `docs/RELATORIO.md` tinha o mesmo atraso — cabeçalho e tabela de histórico
  não registravam a 0.5.0. Os dois foram sincronizados.
- `.claude/launch.json` apontava para `python -m http.server 8123`, o
  servidor estático que o mapa usava antes de ser servido pelo backend Spring
  Boot. Atualizado para subir o backend de verdade (`mvn spring-boot:run`).

### Alterado
- Pasta raiz do projeto renomeada para `Complexus`. O atalho da área de
  trabalho e as referências de caminho foram atualizados junto.
- `ferramentas/inventario.py` não exclui mais `legado` do inventário (a pasta
  não existe mais); `ferramentas/testes_gerador.py` perdeu o teste que
  verificava essa exclusão, que não tinha mais o que verificar.

## [0.5.0] - 2026-09-24

Integracao das duas linhas de trabalho que corriam em paralelo. Ate aqui o
nucleo Spring Boot vivia em branch, enquanto a `main` recebia o motor de
combate e o mapa interativo, feitos em outras sessoes. Agora e um repositorio
so.

### Adicionado
- **Motor de combate** (`src/combate/`) vindo da `main`: resolucao por fases,
  cada plataforma como entidade individual, logistica e gate nuclear. Com a
  CLI `src/simular_batalha.py`, `data/paises.json`, `docs/REGRAS-DE-COMBATE.md`
  e 19 testes em `tests/test_combate.py`.
- **Mapa hierarquico** vindo da `main`, agora servido pelo proprio backend em
  `/mapa.html` e acessivel pelo menu do jogo.
- Leaflet passou a ser servido de `static/vendor/leaflet/` em vez de CDN: o
  jogo roda offline, que e uma propriedade do projeto (H2 local, Python so com
  biblioteca padrao, frontend sem build).
- Requisitos RF-23 (combate) e RF-24 (mapa) no catalogo, com rastreabilidade
  para os arquivos e testes correspondentes.

### Alterado
- O mapa foi trazido do tema escuro para a paleta clara do Complexus: as cores
  de estado, cidade e estrada agora saem da mesma paleta de `css/app.css`, e o
  filtro que invertia os ladrilhos do OpenStreetMap saiu, porque o mapa base ja
  e claro.
- Dados do mapa foram de `data/mapa/` para
  `backend/src/main/resources/static/dados/mapa/`, onde o backend os serve.
  `data/` segue existindo para o `paises.json`, que e lido pelo Python.
- `.gitignore` deixou de ignorar `data/` inteiro: a regra apagava da vista o
  `paises.json` do combate. Agora ignora so o banco H2.

### Removido
- Arquivos `__pycache__` que estavam versionados. Bytecode nao entra no
  repositorio.

### Conflitos resolvidos
- O Git queria mover `src/combate/` para `legado/`, por causa da renomeacao de
  `src/` feita neste branch. Recusado: o combate e codigo vivo, o `legado/`
  guarda o prototipo antigo.
- `README.md` e `CHANGELOG.md` foram fundidos a mao, preservando os dois lados.

### Pendente
- O backend Java ainda nao consome o motor de combate, e o mapa ainda nao le
  dados do backend. As duas ligacoes estao no roadmap como RF-25 e RF-26.

## [0.4.1] - 2026-09-24

### Adicionado
- `IniciarComplexus.bat`: sobe o servico analitico e o backend e abre o
  navegador sozinho quando a porta responde. Rodando com o servidor ja no ar,
  apenas abre o navegador.
- O script procura um JDK 21 ou superior por conta propria e usa so na propria
  janela, entao funciona com o JAVA_HOME da maquina apontando para outra
  versao. Sem JDK compativel, explica o que falta em vez de despejar erro do
  compilador.

### Observacao
A maquina de desenvolvimento estava com `JAVA_HOME` no JDK 17 e o projeto
compila com release 21: `mvn` na mao falha com "release version 21 not
supported". O README passou a registrar isso e o .bat contorna sozinho.

## [0.4.0] - 2026-09-21

Documentacao que se mantem sozinha. Os relatorios deixam de depender de alguem
lembrar de atualiza-los: passam a ser gerados do codigo e de um catalogo de
requisitos, e o commit e recusado quando estao atrasados.

### Adicionado
- **Catalogo de requisitos** em `docs/requisitos.toml`: fonte unica com 34
  requisitos (22 funcionais e 12 nao funcionais), cada um com situacao,
  prioridade, criterio de aceite, arquivos que o implementam e testes que o
  cobrem.
- **Gerador** `ferramentas/gerar_documentacao.py`, com `ferramentas/inventario.py`
  (le o codigo: versao, endpoints, entidades, servicos, paginas, testes, linhas)
  e `ferramentas/requisitos.py` (le e valida o catalogo).
- **Relatorio de requisitos** `docs/REQUISITOS.md`, gerado por inteiro: panorama
  por situacao, lacunas de cobertura de teste e ficha de cada requisito.
- **Inventario tecnico** `docs/INVENTARIO.md`, gerado por inteiro: endpoints
  separados entre jogador e administracao, entidades, servicos e paginas.
- **Blocos automaticos** no README (estado do projeto e indice da documentacao),
  no RELATORIO (numeros e situacao dos requisitos) e no ROADMAP (pendentes).
- **Hook de pre-commit** em `.githooks/pre-commit`: regera os relatorios, inclui
  no commit o que mudou e recusa o commit se o catalogo estiver invalido.
- Modo `--verificar`, que nao escreve e falha quando a documentacao esta
  desatualizada: serve para CI.
- 12 testes do proprio gerador, incluindo um que falha se a documentacao do
  repositorio estiver atrasada em relacao ao codigo.
- Testes do ciclo de empreendimentos (`cicloDeEmpreendimento` e
  `alimenticioNaoTemObra`), fechando a lacuna que o novo relatorio apontou.

### Alterado
- README reestruturado: estado do projeto e indice passam a sair do gerador, e
  a secao de contribuicao diz o que e manual e o que e automatico.
- ROADMAP: a tabela de pendencias virou projecao do catalogo, entao situacao de
  requisito muda em um lugar so.
- Versao do backend para 0.4.0.

### Observacao
A saida do gerador e deterministica de proposito: depende do codigo e do
catalogo, nunca do relogio nem do hash do commit. Sem isso, `--verificar`
acusaria diferenca a cada execucao.

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

---

## Linha paralela, incorporada na 0.5.0

Estas entradas vieram da `main` e descrevem o trabalho feito em outras sessoes.
A numeracao e a original delas e corre em paralelo a de cima.

## [main 0.3.0] - 2026-09-23

### Adicionado
- Mapa interativo hierárquico (`mapa.html` + `mapa.js`) com zoom semântico em
  três níveis — Estados, Cidades e Estradas — usando Leaflet.
- Lazy loading espacial client-side: cada camada só materializa as features
  cuja bbox cruza a viewport atual e cujo zoom está na faixa configurada
  (`criarCamadaLazy` em `mapa.js`); ao sair da vista a layer é removida, não
  só escondida.
- Drill-down: clicar num estado dá `flyTo` para o aglomerado de cidades
  filhas (não o centro geométrico do polígono) e revela a camada de cidades;
  clicar numa cidade abre o Painel de Decisão com PIB municipal,
  infraestrutura e tropas guarnecidas. Zoom máximo travado no nível de
  Cidades (sem zoom de rua).
- Camada logística de estradas (`LineString`) com hover (espessura) e clique
  para abrir decisões de "Expandir Rodovia" e "Bloquear Suprimentos".
- Herança de estado: alíquota estadual e investimento em infraestrutura são
  sempre recalculados a partir do estado pai (nunca guardados na cidade) —
  mudar a alíquota de um estado atualiza o PIB efetivo de todas as cidades
  filhas em tempo real; bloquear uma rodovia reduz a infraestrutura efetiva
  das cidades conectadas.
- Dados geográficos ilustrativos (4 estados do Sudeste, 8 cidades, 6 rodovias)
  — geometrias simplificadas para prototipar a arquitetura, não para uso
  cartográfico preciso (ver nota `_nota` em cada arquivo).

### Notas
- A lógica de herança roda inteiramente no cliente (sem persistência); mover
  as regras de estado/cidade para o backend Java é o próximo passo natural.

## [main 0.2.0] - 2026-09-21

### Adicionado
- Sistema de combate em `src/combate/`: resolução por fases (inteligência, ar,
  golpe profundo, mar, terra) com cada plataforma como entidade individual.
- `data/paises.json` como fonte única do estado das nações, com os campos novos
  `defesas.baterias_antiaereas` e `doutrina`.
- Catálogo de tipos de unidade (`catalogo.py`) mapeado campo a campo para o
  inventário militar de `stats.html`.
- Logística com duas restrições (transporte e verba) acopladas às unidades
  sobreviventes.
- Gate nuclear: ogivas só entram na ordem de batalha com
  `doutrina.permitir_nuclear`, e o uso custa 25 pontos de estabilidade.
- CLI `src/simular_batalha.py` com saída narrada ou JSON.
- `docs/REGRAS-DE-COMBATE.md` com a especificação completa.
- 19 testes em `tests/test_combate.py`.

### Notas
- `src/engine_combate.py`, `src/main.py` e `src/pais.py` seguem no repositório
  como legado e foram substituídos pelo novo motor.
- O frontend ainda não consome o motor.
