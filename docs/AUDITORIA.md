# Linha de auditoria

O projeto tem duas linhas de auditoria, com propósitos distintos:

1. **Auditoria do jogo (em execução)** — registra o que aconteceu dentro da
   partida, com prova de não adulteração.
2. **Auditoria do desenvolvimento** — registra o que mudou no projeto, em
   `CHANGELOG.md` e no [RELATÓRIO](RELATORIO.md).

---

## 1. Auditoria do jogo

### 1.1 Como funciona

Todo fato relevante vira um `EventoAuditoria`:

| Campo | Conteúdo |
|---|---|
| `id` | Sequência da cadeia |
| `momento` | Instante do registro, truncado a milissegundos |
| `turno` | Turno corrente da partida |
| `ator` | `usuario` do jogador, `NPC:<nome>`, `SISTEMA` ou `SANCAO_TACITA` |
| `acao` | Verbo do fato (ex.: `EMPRESA_FUNDADA`, `LEI_SANCIONADA`) |
| `entidade` / `entidadeId` | Objeto afetado |
| `descricao` | Frase legível |
| `detalhes` | JSON com os números do fato |
| `hashAnterior` | Hash do evento anterior |
| `hash` | SHA-256 dos campos acima |

```
hash_n = SHA256( hash_{n-1} | momento | turno | ator | acao | entidade
                 | entidadeId | descricao | detalhes )
```

Alterar qualquer campo de um evento antigo muda o hash dele e quebra o elo com
todos os seguintes. `GET /api/auditoria/integridade` recalcula a cadeia inteira
e devolve o primeiro elo divergente.

### 1.2 Garantias de ordem

`ServicoAuditoria` grava cada evento em **transação própria** (`REQUIRES_NEW`)
e com método **sincronizado**: o evento é confirmado antes que o próximo leia o
último hash. Por isso os atalhos (`registrar` de 5 argumentos e
`registrarSistema`) repetem a anotação em vez de chamar o método principal —
uma chamada interna não passaria pelo proxy transacional do Spring e o evento
ficaria pendurado na transação de negócio, furando a ordem da cadeia.

Limitação: a garantia vale para **um processo**. Com vários servidores seria
preciso uma trava distribuída ou uma fila única de escrita (RNF-03).

### 1.3 Ações registradas

| Ação | Quando |
|---|---|
| `MUNDO_INICIALIZADO` | Carga inicial |
| `JOGADOR_REGISTRADO` | Novo cadastro |
| `EMPRESA_FUNDADA`, `EMPRESA_CAPEX`, `EMPRESA_CONTRATACAO`, `EMPRESA_DEMISSAO`, `EMPRESA_GESTAO_AJUSTADA`, `EMPRESA_IPO`, `EMPRESA_FALENCIA` | Ciclo de vida da empresa |
| `OBRA_INICIADA`, `OBRA_ATRASADA`, `OBRA_CONCLUIDA` | Empreendimentos |
| `INVESTIMENTO_COMPRA`, `INVESTIMENTO_VENDA` | Mercado de ações |
| `MANDATO_ASSUMIDO`, `MANDATO_ENCERRADO` | Cargos públicos |
| `PROJETO_PROPOSTO`, `PROJETO_PAUTADO`, `PROJETO_VOTO`, `PROJETO_APROVADO`, `PROJETO_REJEITADO`, `PROJETO_VETADO`, `LEI_SANCIONADA`, `VETO_DERRUBADO` | Tramitação legislativa |
| `TURNO_PROCESSADO` | Fechamento de cada turno, com o relatório completo |

### 1.4 Livro-razão

A auditoria registra o **fato**; o `LancamentoFinanceiro` registra o **valor**.
Todo dinheiro que se move no jogo gera um lançamento com origem, destino, tipo
e turno. Cruzar as duas fontes permite reconstruir o caixa de qualquer empresa
ou jogador e conferir se o fato registrado bate com o dinheiro movimentado.

Tipos: `APORTE_FUNDACAO`, `COMPRA_ACOES`, `VENDA_ACOES`, `DIVIDENDO`,
`RECEITA_OPERACIONAL`, `CUSTO_OPERACIONAL`, `IMPOSTO`, `SUBSIDIO`, `CAPEX`,
`OBRA`, `ENTREGA_OBRA`, `JUROS`, `TRANSFERENCIA_TESOURO`.

### 1.5 Consultas

```bash
# ultimos 100 eventos
curl -s localhost:8080/api/auditoria/eventos?limite=100

# tudo o que aconteceu no turno 7
curl -s 'localhost:8080/api/auditoria/eventos?turno=7'

# historico de uma empresa
curl -s 'localhost:8080/api/auditoria/eventos?entidade=Empresa&entidadeId=8'

# verificar se a cadeia foi adulterada
curl -s localhost:8080/api/auditoria/integridade

# livro-razao de um jogador
curl -s 'localhost:8080/api/auditoria/razao?jogadorId=1&limite=50'
```

A página `auditoria.html` expõe tudo isso com um botão de verificação.

---

## 2. Auditoria do desenvolvimento

Regra de trabalho do projeto: **nenhuma mudança entra sem deixar rastro**.

1. Todo commit descreve o que mudou e por quê.
2. Toda entrega atualiza o [CHANGELOG](../CHANGELOG.md) com versão e data.
3. Toda entrega atualiza o [RELATÓRIO](RELATORIO.md): seção de entregas,
   decisões, limitações e histórico de versões.
4. Decisão de arquitetura relevante vira ADR em [ARQUITETURA.md](ARQUITETURA.md).
5. Mudança de regra de jogo vira linha em [REGRAS-DO-JOGO.md](REGRAS-DO-JOGO.md).
6. Mudança de contrato de API vira linha em [API.md](API.md).

Assim o histórico do Git, a documentação e o estado do código contam a mesma
história — que é o que permite escalar o time e manter o jogo.
