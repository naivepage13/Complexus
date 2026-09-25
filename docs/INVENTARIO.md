# Inventário técnico

<!-- Arquivo gerado por ferramentas/gerar_documentacao.py. Nao edite a mao: mude docs/requisitos.toml ou o codigo e rode o gerador. -->

Retrato do código na versão `0.6.0`, lido direto dos fontes.

## Endpoints da API

41 públicos e 3 administrativos (estes exigem o cabeçalho `X-Admin-Token`).

| Método | Rota | Controlador | Acesso |
|---|---|---|---|
| GET | `/api/admin/auditoria/eventos` | AuditoriaController | administrativo |
| GET | `/api/admin/auditoria/integridade` | AuditoriaController | administrativo |
| GET | `/api/admin/auditoria/razao` | AuditoriaController | administrativo |
| GET | `/api/atualizacoes` | AtualizacaoController | jogador |
| GET | `/api/empresas` | EmpresaController | jogador |
| POST | `/api/empresas` | EmpresaController | jogador |
| GET | `/api/empresas/setores` | EmpresaController | jogador |
| GET | `/api/empresas/{id}` | EmpresaController | jogador |
| POST | `/api/empresas/{id}/capital` | EmpresaController | jogador |
| POST | `/api/empresas/{id}/contratar` | EmpresaController | jogador |
| POST | `/api/empresas/{id}/demitir` | EmpresaController | jogador |
| GET | `/api/empresas/{id}/empreendimentos` | EmpresaController | jogador |
| POST | `/api/empresas/{id}/empreendimentos` | EmpresaController | jogador |
| POST | `/api/empresas/{id}/gestao` | EmpresaController | jogador |
| GET | `/api/empresas/{id}/historico` | EmpresaController | jogador |
| POST | `/api/empresas/{id}/ipo` | EmpresaController | jogador |
| GET | `/api/estatisticas/gerais` | EstatisticaController | jogador |
| GET | `/api/estatisticas/ranking` | EstatisticaController | jogador |
| GET | `/api/estatisticas/serie` | EstatisticaController | jogador |
| GET | `/api/estatisticas/setores` | EstatisticaController | jogador |
| GET | `/api/investimentos/carteira` | InvestimentoController | jogador |
| POST | `/api/investimentos/comprar` | InvestimentoController | jogador |
| GET | `/api/investimentos/mercado` | InvestimentoController | jogador |
| POST | `/api/investimentos/vender` | InvestimentoController | jogador |
| POST | `/api/jogadores/login` | JogadorController | jogador |
| POST | `/api/jogadores/registrar` | JogadorController | jogador |
| GET | `/api/jogadores/{id}` | JogadorController | jogador |
| GET | `/api/jogadores/{id}/painel` | JogadorController | jogador |
| GET | `/api/jogo/estado` | JogoController | jogador |
| POST | `/api/jogo/turno/avancar` | JogoController | jogador |
| GET | `/api/politica/cargos` | PoliticaController | jogador |
| GET | `/api/politica/leis` | PoliticaController | jogador |
| GET | `/api/politica/mandatos` | PoliticaController | jogador |
| POST | `/api/politica/mandatos` | PoliticaController | jogador |
| GET | `/api/politica/mandatos/jogador/{jogadorId}` | PoliticaController | jogador |
| GET | `/api/politica/projetos` | PoliticaController | jogador |
| POST | `/api/politica/projetos` | PoliticaController | jogador |
| GET | `/api/politica/projetos/{id}` | PoliticaController | jogador |
| POST | `/api/politica/projetos/{id}/derrubar-veto` | PoliticaController | jogador |
| POST | `/api/politica/projetos/{id}/pautar` | PoliticaController | jogador |
| POST | `/api/politica/projetos/{id}/sancionar` | PoliticaController | jogador |
| POST | `/api/politica/projetos/{id}/votar` | PoliticaController | jogador |
| GET | `/api/politica/territorios` | PoliticaController | jogador |
| GET | `/api/politica/tipos-projeto` | PoliticaController | jogador |

## Entidades persistidas

| Classe | Tabela |
|---|---|
| Empreendimento | `empreendimento` |
| Empresa | `empresa` |
| Estado | `estado_federativo` |
| EstadoJogo | `estado_jogo` |
| EstatisticaSetor | `estatistica_setor` |
| EventoAuditoria | `evento_auditoria` |
| HistoricoEmpresa | `historico_empresa` |
| Investimento | `investimento` |
| Jogador | `jogador` |
| LancamentoFinanceiro | `lancamento_financeiro` |
| Mandato | `mandato` |
| Municipio | `municipio` |
| Pais | `pais` |
| ProjetoDeLei | `projeto_lei` |
| SnapshotTurno | `snapshot_turno` |
| VotoProjeto | `voto_projeto` |

## Serviços

| Serviço |
|---|
| `ServicoAtualizacoes` |
| `ServicoAuditoria` |
| `ServicoEmpresa` |
| `ServicoEstadoJogo` |
| `ServicoEstatistica` |
| `ServicoInvestimento` |
| `ServicoJogador` |
| `ServicoPolitica` |
| `ServicoRazao` |
| `ServicoTurno` |

## Páginas do frontend

| Arquivo |
|---|
| `backend/src/main/resources/static/admin/auditoria.html` |
| `backend/src/main/resources/static/atualizacoes.html` |
| `backend/src/main/resources/static/empresa.html` |
| `backend/src/main/resources/static/empresas.html` |
| `backend/src/main/resources/static/estatisticas.html` |
| `backend/src/main/resources/static/index.html` |
| `backend/src/main/resources/static/investimentos.html` |
| `backend/src/main/resources/static/login.html` |
| `backend/src/main/resources/static/mapa.html` |
| `backend/src/main/resources/static/politica.html` |

## Testes automatizados

| Origem | Quantidade |
|---|---|
| Java (JUnit) | 24 |
| Python (unittest) | 20 |
| **Total** | **44** |
