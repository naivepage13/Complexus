# Regras do jogo

Documento de referência das regras implementadas na versão 0.2.0. Cada regra
aponta para o arquivo que a implementa, para que regra e código não se separem.

---

## 1. Tempo

| Regra | Valor |
|---|---|
| 1 turno | 1 hora de tempo real |
| 1 turno no calendário do jogo | 1 mês |
| Mandato de 4 anos | 48 turnos |
| Mandato de senador (8 anos) | 96 turnos |

O turno avança sozinho (`AgendadorTurno`) ou por chamada manual
(`POST /api/jogo/turno/avancar`). O relógio vive em `estado_jogo`.

## 2. Empresas

### 2.1 Setores (`Setor`)

| Parâmetro | Alimentício | Imobiliário | Construção |
|---|---|---|---|
| Margem bruta sobre insumos | 42% | 34% | 33% |
| Volatilidade mensal | 5% | 11% | 9% |
| Giro do ativo por mês | 22% | 6% | 12% |
| Receita por funcionário/mês | R$ 18.000 | R$ 45.000 | R$ 30.000 |
| Múltiplo de valuation | 9x | 12x | 8x |
| Elasticidade à renda | 0,35 | 0,70 | 0,45 |
| Elasticidade a juros | 0,10 | 0,85 | 0,65 |
| Capital mínimo | R$ 250 mil | R$ 1,2 mi | R$ 800 mil |

### 2.2 Fundação
- O capital sai do caixa do jogador: **70% vira patrimônio, 30% vira caixa** da empresa.
- Nome único, capital acima do mínimo do setor, ao menos um funcionário.

### 2.3 Capacidade de produção (`MotorSimulacao.capacidadeProdutiva`)

```
capacidadePorEquipe  = funcionarios x receitaPorFuncionario x produtividade
capacidadePorCapital = patrimonio x giroAtivoMensal
capacidade           = min(capacidadePorEquipe, capacidadePorCapital)
```

É a regra central da gestão: contratar sem investir gera folha ociosa;
investir sem contratar deixa ativo parado. A tela da empresa mostra qual dos
dois tetos está limitando (`gargalo`) e a equipe sugerida para o patrimônio atual.

### 2.4 Mercado e concorrência

```
mercadoPotencial  = f(populacao, renda, desemprego, indicadores locais)   # por municipio e setor
mercadoDisputavel = min(mercadoPotencial, soma das capacidades do grupo)
competitividade   = raiz(patrimonio) x produtividade x (0,5 + reputacao/100) x fatorMarketing
participacao_i    = competitividade_i / soma das competitividades do grupo
```

O mercado de uma cidade é muito maior que as empresas simuladas; o restante é
atendido por empresas não modeladas. Limitar a disputa à capacidade instalada
do grupo é o que faz a concorrência importar: quem perde participação fica com
capacidade ociosa e prejuízo.

### 2.5 Resultado do mês (`MotorSimulacao.simularMes`)

```
demanda  = mercadoDisputavel x participacao x ajusteRenda x ajusteJuros
           x ajusteConfianca x (1 + choqueDemanda)
receita  = min(demanda, capacidade)
custo    = receita x (1 - margemBruta) x (1 + choqueCusto) x (1 + regulacao)
           + folha (salario x 1,32 de encargos)
           + marketing
           + depreciacao (0,4% do patrimonio)
           + juros da divida
impostoIndireto = receita x (aliquotaEstadual x baseEstadual + aliquotaMunicipal x baseMunicipal)
lucroAntesIR    = receita + subsidio - custo - impostoIndireto
impostoRenda    = max(0, lucroAntesIR) x aliquotaFederal
lucro           = lucroAntesIR - impostoRenda
```

Bases de cálculo dos tributos indiretos:

| Setor | Base estadual | Base municipal |
|---|---|---|
| Alimentício | 60% | 10% |
| Imobiliário | 15% | 55% |
| Construção | 45% | 40% |

### 2.6 Valor de mercado e ação

```
valuation  = max(patrimonioLiquido x 0,6,
                 patrimonioLiquido x 0,85 + max(lucro x 12, 0) x multiploSetor x confianca)
precoAcao  = precoAtual + (valuation / acoesTotais - precoAtual) x 0,4
indiceLastro = (patrimonio + caixa - divida) / valuation
```

O piso patrimonial é o lastro: mesmo em prejuízo a empresa não vale zero.
A suavização de 40% evita saltos de preço entre turnos.

### 2.7 Operação e risco
- Produtividade cai 0,5% por turno (depreciação). Aportes de capital a elevam,
  com retorno decrescente, até o teto de 2,0.
- Reputação sobe com ocupação alta e marketing, cai com ociosidade e prejuízo.
- Caixa negativo vira dívida automática com juros de mercado.
- Dívida acima de 2,5x o patrimônio encerra a empresa por insolvência.
- Contratar custa meio salário por pessoa; demitir custa um salário e desgasta
  a reputação.

### 2.8 Empreendimentos (imobiliário e construção)
- Custo total dividido em parcelas iguais pelos turnos de obra.
- Sem caixa para a parcela, o prazo escorrega um turno.
- Ao concluir, o valor estimado (custo x multiplicador do tipo x fator de
  urbanização local) entra como patrimônio.

## 3. Política

### 3.1 Cargos (`CargoPolitico`)

| Esfera | Cargos | Vagas por território | Mandato |
|---|---|---|---|
| Federal | Presidente, Vice, Ministro (5), Senador (12), Deputado Federal (20) | conforme tabela | 48 turnos (senador: 96) |
| Estadual | Governador, Vice, Secretário (3), Deputado Estadual (12) | conforme tabela | 48 turnos |
| Municipal | Prefeito, Vice, Secretário (2), Vereador (9) | conforme tabela | 48 turnos |

- **Legislativo** (senador, deputados, vereador): propõe e vota.
- **Executivo** (presidente, governador, prefeito): sanciona ou veta.
- **Nomeados** (ministros, secretários): executam, não votam nem são assumidos
  diretamente pelo jogador.

### 3.2 Posse (eleição simplificada da 0.2)
- Havendo cadeira vaga, a posse é direta.
- Com todas ocupadas, o jogador desafia o NPC de menor aprovação e vence se
  essa aprovação estiver **abaixo de 45**.
- A carga inicial deixa **um terço das cadeiras legislativas vagas**.
- Mandato vencido encerra e um NPC assume até que alguém dispute.

### 3.3 Instrumentos legais (`TipoProjeto`)

| Instrumento | Esferas | Parâmetro | Efeito |
|---|---|---|---|
| `IMPOSTO_EMPRESARIAL` | Federal | 0 a 0,45 | Alíquota federal sobre o lucro |
| `IMPOSTO_ESTADUAL` | Estadual | 0 a 0,30 | Alíquota estadual sobre circulação |
| `IMPOSTO_MUNICIPAL` | Municipal | 0 a 0,15 | Alíquota municipal sobre serviços |
| `SUBSIDIO_SETORIAL` | Federal, Estadual | 0 a 0,25 | Percentual da receita creditado à empresa |
| `REGULACAO_SETORIAL` | Federal, Estadual | 0 a 0,20 | Acréscimo ao custo variável do setor |
| `INVESTIMENTO_INFRAESTRUTURA` | Federal, Estadual | R$ | Gasto mensal que eleva o desenvolvimento estadual |
| `PROGRAMA_SOCIAL` | Todas | R$ | Transferência que eleva a renda média |
| `ZONEAMENTO_URBANO` | Municipal | 0 a 100 | Muda custo do terreno e demanda imobiliária |

### 3.4 Tramitação

```
RASCUNHO --pautar--> EM_VOTACAO --apuracao--> APROVADO ou REJEITADO
APROVADO --executivo--> SANCIONADO ou VETADO
VETADO --2/3 do legislativo--> SANCIONADO
```

- A apuração acontece no processamento do turno seguinte à pauta.
- NPCs votam por heurística: apoiam gasto e subsídio, resistem a aumento de
  imposto e a regulação pesada; a aprovação do mandatário desloca a inclinação.
- Executivo NPC decide por regra fiscal (veta corte agressivo de receita).
- Executivo jogador que não se manifesta em 2 turnos sofre **sanção tácita**.
- Havendo mais de uma lei vigente do mesmo tipo e setor, vale a mais recente.

## 4. Investimentos

- Só empresas com capital aberto são negociáveis. Para abrir capital é preciso
  lucro acumulado positivo e ofertar entre 5% e 49% das ações.
- Negociação em lotes de 100 ações, com custo de transação de 0,5%.
- **Compra**: mercado primário — o valor entra no caixa da empresa.
- **Venda**: liquidada pelo mercado, sem drenar o caixa da empresa.
- **Dividendos**: a cada turno a empresa distribui `lucro x payout`, rateado
  pelas ações; o que não está com investidores vai ao controlador.
- Retorno do investidor = variação do preço + dividendos + resultado realizado.

## 5. Macroeconomia

```
PIB            = receita agregada x 12 x 0,65 + gasto social anualizado
desemprego     -> converge 25% ao alvo derivado dos empregos gerados
inflacao       -> converge 30% ao alvo (pressao fiscal - efeito dos juros)
juros          -> regra tipo Taylor: 0,02 + 1,5 x (inflacao - 0,03) + 0,03
aprovacao      -> reage a desemprego, inflacao e lucro agregado
estabilidade   -> segue a aprovacao com inercia
```

Todos os indicadores convergem devagar, evitando oscilação brusca entre turnos.

## 6. Choques setoriais (Python)

A cada turno o backend pede ao serviço analítico os choques de demanda, custo e
confiança por setor. O cálculo combina sazonalidade de 12 turnos, ruído com
semente derivada do turno (resultado reproduzível) e reação a inflação e juros.
Com o serviço indisponível, o Java aplica a mesma fórmula e registra
`fonteModificadores: FALLBACK_JAVA` no relatório do turno.
