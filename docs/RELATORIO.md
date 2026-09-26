# Relatório de desenvolvimento — Complexus

> **Documento vivo.** As seções de métricas e de requisitos são reescritas por
> `ferramentas/gerar_documentacao.py` a partir do código e de
> [`requisitos.toml`](requisitos.toml). O restante é análise escrita à mão e
> muda a cada entrega, junto com o [CHANGELOG](../CHANGELOG.md).

| Campo | Valor |
|---|---|
| Versão atual | **0.6.0** |
| Data da última atualização | 25/09/2026 |
| Repositório | [naivepage13/Complexus](https://github.com/naivepage13/Complexus) |
| Branch | `claude/business-country-management-game-4bbe6e` |
| Estado | Núcleo jogável: economia, política, investimentos, turnos e auditoria |
| Cobertura de testes | 24 testes Java + 21 testes Python, todos verdes |

## Números do projeto

<!-- auto:inicio:metricas -->
Versão `0.8.0` · 48 testes · 47 endpoints · 16 entidades · 10 serviços · 14 páginas.

Linhas de código não vazias, sem contar artefatos de build:

| Linguagem | Linhas |
|---|---|
| Java | 6.402 |
| Python | 2.389 |
| JavaScript | 2.167 |
| HTML | 1.581 |
| CSS | 1.294 |
| Configuração | 647 |
| **Total** | **14.480** |
<!-- auto:fim:metricas -->

## Situação dos requisitos

<!-- auto:inicio:requisitos -->
| Situação | Funcionais | Não funcionais | Total |
|---|---|---|---|
| Entregue | 15 | 7 | 22 |
| Parcial | 1 | 0 | 1 |
| Planejado | 12 | 5 | 17 |

19 dos 22 requisitos entregues têm teste automatizado declarado.

**Lacunas de cobertura** — entregues sem teste declarado:

| Requisito | Título | Situação |
|---|---|---|
| RF-24 | Mapa hierárquico com zoom semântico | entregue |
| RNF-08 | Resiliência ao serviço analítico | entregue |
| RNF-11 | Frontend sem dependência externa | entregue |
<!-- auto:fim:requisitos -->

Detalhamento requisito a requisito, com critério de aceite e rastreabilidade,
em [REQUISITOS.md](REQUISITOS.md).

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
- Motor de simulação puro (`MotorSimulacao`) com mercado potencial por
  município, competitividade relativa, **dois tetos de produção** (equipe e
  patrimônio), custos, tributos, subsídios e valuation lastreado.
- Ciclo completo de gestão: fundar, aportar capital, contratar, demitir,
  ajustar marketing/salário/payout, abrir capital, tocar empreendimentos.
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

### 3.9 Documentação viva (entregue na 0.4.0)
- Catálogo de requisitos em [`requisitos.toml`](requisitos.toml) como fonte
  única: 34 requisitos com critério de aceite, implementação e testes.
- Gerador em `ferramentas/` que lê o código e o catálogo e reescreve
  [REQUISITOS.md](REQUISITOS.md), [INVENTARIO.md](INVENTARIO.md) e os blocos
  automáticos do README, deste relatório e do roadmap.
- Hook de pré-commit que regenera e inclui no commit, e modo `--verificar` que
  falha quando a documentação está atrasada.
- A rastreabilidade é verificada: requisito apontando para arquivo inexistente
  quebra a geração em vez de virar referência morta.
- O mecanismo já se provou: o primeiro relatório apontou três requisitos
  entregues sem teste, e um deles (RF-04, empreendimentos) foi coberto na mesma
  entrega.

### 3.10 Menu de configurações e páginas de conta (entregue na 0.7.0)
- Ícone de engrenagem no cabeçalho de todas as páginas do jogador, com menu
  suspenso para Perfil, Novidades, Tutorial e Termos de uso.
- **Perfil**: avatar (guardado só no navegador, via `localStorage`), data de
  cadastro e horas de partida em campos somente leitura, e dois formulários
  independentes — redefinir senha (exige a senha atual) e alterar e-mail
  (valida o formato) — cada um com estado de carregamento no botão.
- **Novidades**: histórico de versões do jogo (fonte: `CHANGELOG.md`) com
  estado de leitura por versão, também em `localStorage`: envelope fechado e
  com brilho quando não lida, aberto e neutro depois que o card é aberto.
- **Tutorial**: guia em accordion, uma categoria macro por painel (primeiros
  passos, empresas, investimentos, política, mapa).
- **Termos de uso**: PDF estático embutido na página, com botão de download.
- `Jogador` ganhou o campo `email`; `GET /api/jogadores/{id}` passou a expor
  `email`, `criadoEm` e `horasEmJogo` (horas reais desde o cadastro).

### 3.11 Exclusão de conta (entregue na 0.8.0)
- **Zona de risco** na página de Perfil, isolada por borda vermelha sutil, com
  o botão destrutivo "Excluir conta".
- O clique nunca exclui direto: abre um **modal de confirmação** com um campo
  de texto único. O botão final ("Excluir definitivamente") nasce desabilitado
  e só habilita quando o jogador digita a própria senha atual ou a palavra
  **EXCLUIR** — a mesma regra é checada de novo no servidor, então digitar
  qualquer coisa no cliente não basta sem uma das duas condições.
- `ServicoJogador.excluirConta` desativa a conta (`Jogador.ativo = false`),
  registra `CONTA_EXCLUIDA` na auditoria e nunca apaga a linha: apagar de
  verdade quebraria a cadeia de auditoria (ADR-06) e as referências de
  empresas, carteira e mandatos do jogador.
- Depois do sucesso, o navegador limpa o `localStorage` inteiro (sessão,
  avatar, estado de leitura de Novidades) e redireciona para `login.html`; a
  conta desativada deixa de autenticar (`ServicoJogador.autenticar`).

### 3.8 Identidade do projeto (entregue na 0.3.0)
- O jogo passou a se chamar **Complexus**, em todas as camadas: pacote Java
  (`com.complexus`), artefato Maven (`complexus-backend`), banco local,
  títulos das páginas, serviço Python e documentação.
- O banco existente foi renomeado em vez de recriado, então partidas em
  andamento continuam de onde estavam.
- Mantido de propósito: o sal do hash de senha (`geohistoricalsim::v1::`),
  que entra no hash gravado e derrubaria o login de todas as contas se mudasse;
  e o prefixo `jogo.*` das propriedades, que descreve o domínio e não a marca.
  A pasta `legado/` foi mantida até a 0.5.0 e removida na 0.6.0, quando os
  três arquivos que ela guardava (`engine_combate.py`, `main.py`, `pais.py`
  — a primeira versão do combate, já substituída por `src/combate/`) deixaram
  de ter qualquer valor de referência.
- O sal chegou a ser renomeado junto e quebrou o login das contas existentes.
  Foi revertido e agora há teste (`CredencialEstavelTest`) travando o hash.

## 4. Verificações executadas

| Verificação | Resultado |
|---|---|
| `mvn test` (backend) | 22 testes, 0 falhas |
| `python -m unittest` (analytics) | 9 testes, 0 falhas |
| Subida da aplicação + carga do mundo | OK |
| Fluxo ponta a ponta pela API | Empresa → turno → IPO → compra → dividendo → lei sancionada → auditoria íntegra |
| Integração Java ↔ Python | `fonteModificadores: PYTHON` com o serviço no ar; `FALLBACK_JAVA` com ele desligado |
| Documentação viva | Gerador roda, `--verificar` acusa atraso, 12 testes do gerador verdes e hook regenera no commit |
| Renomeação para Complexus | Build, 22 testes, login de conta anterior e cadeia de auditoria íntegra após o rename |
| Separação de visibilidade | `/api/admin/auditoria` responde 403 sem token e 200 com token; canal de atualizações sem hash, ator ou detalhe interno |

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
| D-12 | Relatórios gerados do código, não escritos à mão | Documentação manual envelhece em silêncio; gerada, ela quebra o build quando mente |
| D-13 | Saída do gerador sem data nem hash de commit | Determinismo é o que permite o modo `--verificar` acusar atraso de verdade |

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
| L-08 | Avatar do perfil e estado de leitura de Novidades vivem só em `localStorage` | Trocar de navegador ou de máquina perde o avatar e volta todas as versões a "não lida" | Precisaria de upload real (armazenamento de arquivo) e de uma tabela de notificações no backend |

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
| 0.6.0 | 25/09/2026 | Limpeza e organização: pasta `legado/` removida (incluindo os três arquivos Python já documentados como quebrados/obsoletos), correção de versão desalinhada entre pom.xml e changelog, renomeação da pasta raiz do projeto para `Complexus` |
| 0.5.0 | 24/09/2026 | Integração do motor de combate e do mapa interativo, que corriam em paralelo na `main` |
| 0.4.1 | 24/09/2026 | Atalho `IniciarComplexus.bat` para Windows, que escolhe o JDK compatível sozinho |
| 0.4.0 | 21/09/2026 | Documentação viva: catálogo de requisitos, gerador de relatórios, inventário técnico e hook que mantém tudo sincronizado |
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
