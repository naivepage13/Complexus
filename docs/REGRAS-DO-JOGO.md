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
| Elasticidade-preço | 1,60 | 0,90 | 1,20 |
| Capital mínimo | R$ 250 mil | R$ 1,2 mi | R$ 800 mil |
| Capital mínimo por unidade | R$ 62,5 mil | R$ 300 mil | R$ 200 mil |

### 2.2 Fundação
- O capital sai do caixa do jogador: **70% vira patrimônio, 30% vira caixa** da empresa.
- Nome único, capital acima do mínimo do setor, ao menos um funcionário.
- A empresa nasce com uma **unidade sede** no município escolhido, que recebe
  todo o patrimônio e toda a equipe.

### 2.2.1 Unidades (`Unidade`)

Quem disputa mercado é a unidade, não a empresa. Patrimônio e equipe ficam
alocados em unidades; os totais da empresa são sempre a soma delas.

| Operação | Custo | Efeito |
|---|---|---|
| Abrir unidade | capital + 8% de instalação + meio salário por admissão | nova unidade em outro município, com a produtividade média da empresa |
| Fechar unidade | um salário de rescisão por funcionário | devolve 70% dos ativos ao caixa e tira 3 pontos de reputação |
| Transferir capital | 8% do valor movido | patrimônio muda de unidade sem passar pelo caixa |
| Transferir equipe | 30% do salário por pessoa | funcionários mudam de unidade |

Regras: uma unidade por município por empresa; a última unidade ativa não pode
ser fechada (para sair do mercado, encerra-se a empresa).

### 2.2.2 Linhas de produto (`LinhaProduto`)

O mix define o preço praticado e o custo do insumo. A fatia **não declarada**
do mix fica no posicionamento médio, então declarar 50% premium move o preço
médio para 1,15 e não para 1,30.

| Posicionamento | Preço | Custo de insumo |
|---|---|---|
| Popular | 0,85x | 0,93x |
| Médio | 1,00x | 1,00x |
| Premium | 1,30x | 1,12x |

A soma das fatias nunca passa de 100%.

### 2.2.3 Departamentos (`Departamento`)

Orçamento mensal fixo que compra vantagem com retorno decrescente:

```
intensidade = orcamento / (orcamento + referenciaDePorte)
efeito      = efeitoMaximo x intensidade
referenciaDePorte = 2% da capacidade mensal instalada (mínimo R$ 5 mil)
```

| Área | Efeito máximo |
|---|---|
| Pesquisa e desenvolvimento | +0,06 de produtividade por turno em todas as unidades |
| Qualidade | +3,0 pontos de reputação por turno |
| Comercial | +35% de competitividade |
| Logística | −12% no custo de insumo |

Com orçamento igual à referência de porte, o efeito é metade do teto. O
orçamento total sai do resultado todo turno, tenha havido venda ou não.

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
competitividade   = raiz(patrimonio) x produtividade x (0,5 + reputacao/100)
                    x fatorMarketing x (1 + bonusComercial) x atratividadeDePreco
participacao_i    = competitividade_i / soma das competitividades do grupo
```

O grupo que disputa um mercado é o conjunto de **unidades** do mesmo setor no
mesmo município. Duas filiais da mesma empresa em cidades diferentes não
competem entre si.

A elasticidade-preço age em duas metades: `atratividadeDePreco = preco^(-e/2)`
na disputa por cliente do concorrente, e outro `preco^(-e/2)` no tamanho da
demanda capturada. Quem baixa o preço tira cliente do vizinho e amplia o
próprio mercado — e quem sobe o preço perde nas duas pontas.

O mercado de uma cidade é muito maior que as empresas simuladas; o restante é
atendido por empresas não modeladas. Limitar a disputa à capacidade instalada
do grupo é o que faz a concorrência importar: quem perde participação fica com
capacidade ociosa e prejuízo.

### 2.5 Resultado do mês (`MotorSimulacao.simularOperacao` + `consolidar`)

O mês tem duas etapas. A **operação** acontece por unidade: cada filial produz,
vende e recolhe tributo indireto no estado e no município onde está. O
**fechamento** acontece uma vez por empresa: juros da dívida, orçamento dos
departamentos e imposto sobre o lucro entram no consolidado, nunca por filial.
É o que permite abrir uma unidade que opera no vermelho sem que ela seja
tributada como empresa separada.


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

### 2.7.1 Crédito e dívida (`ServicoCredito`)

Toda dívida é um **contrato** com prazo, taxa travada na contratação e
amortização constante (SAC): `amortização = saldo / turnos restantes`,
`juros = saldo × taxa mensal`. A parcela começa alta e cai a cada turno.

**Nota de crédito.** Um score de 0 a 100 define a faixa de risco:

| Componente | Peso | Como pontua |
|---|---|---|
| Alavancagem | 35% | 100 pontos sem dívida, 0 com dívida em 2,5x o patrimônio líquido |
| Cobertura de juros | 30% | 100 pontos quando o lucro anual cobre 6x os juros |
| Lastro patrimonial | 15% | o índice de lastro da empresa |
| Histórico de pagamento | 20% | −25 pontos por parcela em atraso |

| Nota | Score | Spread de risco | Fator de limite |
|---|---|---|---|
| A | ≥ 75 | +2,0% a.a. | 100% |
| B | ≥ 55 | +5,0% a.a. | 80% |
| C | ≥ 35 | +9,0% a.a. | 50% |
| D | < 35 | +15,0% a.a. | 25% |

**Linhas.** `taxa = Selic do turno + spread da linha + spread da nota`:

| Linha | Spread | Prazo | Limite | Garantia |
|---|---|---|---|---|
| Capital de giro | +4,0% | 3 a 18 turnos | 30% do PL | não |
| Investimento | +2,0% | 12 a 60 turnos | 70% do PL | 1,3x em patrimônio |
| Antecipação de recebíveis | +7,0% | 1 a 6 turnos | 25% do PL, teto de 3x a receita | não |
| Crédito rotativo | +22,0% | 6 turnos | 40% do PL | não (só automático) |

O limite é agregado: toda a dívida já contratada consome o espaço, em qualquer
linha. Endividar demais não bloqueia o crédito — encarece.

**Ordem no turno.** Os juros entram como despesa no resultado; depois o lucro
cai no caixa; então a **amortização sai do caixa antes do dividendo** (credor
antes de sócio); por último, caixa negativo vira crédito rotativo automático.

**Inadimplência.** Sem caixa para a amortização, a parcela vira atraso: multa de
2% mais mora de 1% sobre o saldo, reputação −2 por contrato atrasado. Na
terceira parcela seguida, a linha com garantia executa o patrimônio dado em
garantia (perda rateada entre as unidades, reputação −8). Na quarta, a empresa
vai à falência.

**Alívio.** Amortização antecipada reduz o saldo e, com ele, os juros futuros.
Renegociar alonga o prazo, soma 1% de comissão ao saldo, acrescenta 3% a.a. à
taxa e zera a contagem de atraso.

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
