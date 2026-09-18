# API REST

Base: `http://localhost:8080/api`. Todo corpo é JSON.

Erros seguem um formato único:

```json
{ "momento": "...", "status": 422, "erro": "Unprocessable Entity", "mensagem": "Saldo insuficiente..." }
```

| Status | Significado |
|---|---|
| 400 | Dados inválidos na requisição |
| 404 | Recurso inexistente |
| 422 | Regra de jogo violada (ex.: capital abaixo do mínimo) |

> **Identificação do jogador (limitação conhecida L-02):** nesta versão o
> `jogadorId` viaja no corpo da requisição. A troca por autenticação real está
> no roadmap como RNF-01.

---

## Jogo

| Método | Rota | Descrição |
|---|---|---|
| GET | `/jogo/estado` | Turno atual, data do jogo, índice de mercado, segundos até o próximo turno |
| POST | `/jogo/turno/avancar?origem=MANUAL` | Processa um turno e devolve o relatório completo |

## Jogadores

| Método | Rota | Corpo |
|---|---|---|
| POST | `/jogadores/registrar` | `{usuario, nome, senha}` |
| POST | `/jogadores/login` | `{usuario, senha}` |
| GET | `/jogadores/{id}` | — |
| GET | `/jogadores/{id}/painel` | — (empresas, carteira, mandatos e patrimônio consolidado) |

## Empresas

| Método | Rota | Corpo |
|---|---|---|
| GET | `/empresas/setores` | — (catálogo com parâmetros de cada setor) |
| GET | `/empresas?setor=&jogadorId=` | — |
| GET | `/empresas/{id}` | — (inclui histórico, obras e diagnóstico de capacidade) |
| GET | `/empresas/{id}/historico?limite=36` | — |
| POST | `/empresas` | `{jogadorId, nome, setor, municipioId, capitalInicial, funcionarios}` |
| POST | `/empresas/{id}/capital` | `{jogadorId, valor}` |
| POST | `/empresas/{id}/contratar` | `{jogadorId, quantidade}` |
| POST | `/empresas/{id}/demitir` | `{jogadorId, quantidade}` |
| POST | `/empresas/{id}/gestao` | `{jogadorId, marketingMensal, salarioMedio, payout}` |
| POST | `/empresas/{id}/ipo` | `{jogadorId, fracaoOfertada}` |
| GET | `/empresas/{id}/empreendimentos` | — |
| POST | `/empresas/{id}/empreendimentos` | `{jogadorId, nome, tipo, custoTotal, turnosTotais}` |

## Investimentos

| Método | Rota | Corpo |
|---|---|---|
| GET | `/investimentos/mercado` | — (empresas negociáveis com preço, lastro e dividendo projetado) |
| GET | `/investimentos/carteira?jogadorId=` | — |
| POST | `/investimentos/comprar` | `{jogadorId, empresaId, quantidade}` |
| POST | `/investimentos/vender` | `{jogadorId, empresaId, quantidade}` |

## Política

| Método | Rota | Corpo |
|---|---|---|
| GET | `/politica/cargos` | — |
| GET | `/politica/tipos-projeto` | — |
| GET | `/politica/territorios` | — (países, estados e municípios) |
| GET | `/politica/mandatos?esfera=&territorioId=` | — |
| GET | `/politica/mandatos/jogador/{jogadorId}` | — |
| POST | `/politica/mandatos` | `{jogadorId, cargo, territorioId, partido}` |
| GET | `/politica/projetos?esfera=&territorioId=` | — |
| GET | `/politica/projetos/{id}` | — |
| GET | `/politica/leis` | — (leis sancionadas em vigor) |
| POST | `/politica/projetos` | `{jogadorId, mandatoId, titulo, ementa, tipo, setorAlvo, parametro}` |
| POST | `/politica/projetos/{id}/pautar` | `{jogadorId}` |
| POST | `/politica/projetos/{id}/votar` | `{jogadorId, mandatoId, opcao}` — opção: `SIM`, `NAO`, `ABSTENCAO` |
| POST | `/politica/projetos/{id}/sancionar` | `{jogadorId, sancionar, justificativa}` |
| POST | `/politica/projetos/{id}/derrubar-veto` | `{jogadorId, mandatoId}` |

## Estatísticas

| Método | Rota | Descrição |
|---|---|---|
| GET | `/estatisticas/gerais` | Último fechamento + recorte por setor |
| GET | `/estatisticas/serie?limite=24` | Série histórica de turnos |
| GET | `/estatisticas/setores?setor=&limite=24` | Série de um setor ou fotografia do último turno |
| GET | `/estatisticas/ranking?limite=20` | Ranking de empresas por valor de mercado |

## Auditoria

| Método | Rota | Descrição |
|---|---|---|
| GET | `/auditoria/eventos?limite=&turno=&entidade=&entidadeId=` | Eventos da linha de auditoria |
| GET | `/auditoria/integridade` | Recalcula a cadeia de hashes e aponta elos divergentes |
| GET | `/auditoria/razao?limite=&empresaId=&jogadorId=&turno=` | Livro-razão financeiro |

---

## Serviço analítico (Python, porta 8100)

| Método | Rota | Corpo | Resposta |
|---|---|---|---|
| GET | `/saude` | — | `{status, servico, versao}` |
| POST | `/modificadores` | `{turno, inflacaoAnual, taxaJuros}` | choques por setor |
| POST | `/projecao` | `{precoAcao, lucroMensal, payout, acoesTotais, crescimentoMensal, turnos}` | série projetada e retorno total |

## Exemplo de sessão

```bash
# entrar
curl -s -X POST localhost:8080/api/jogadores/login \
  -H 'Content-Type: application/json' \
  -d '{"usuario":"demo","senha":"demo1234"}'

# fundar empresa
curl -s -X POST localhost:8080/api/empresas \
  -H 'Content-Type: application/json' \
  -d '{"jogadorId":1,"nome":"Padaria Aurora","setor":"ALIMENTICIO","municipioId":1,"capitalInicial":900000,"funcionarios":8}'

# rodar um turno e ler o relatorio
curl -s -X POST 'localhost:8080/api/jogo/turno/avancar?origem=MANUAL'

# conferir a linha de auditoria
curl -s localhost:8080/api/auditoria/integridade
```
