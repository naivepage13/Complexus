# Regras de Combate — GeoHistoricalSim

Especificação do sistema de combate. O código em `src/combate/` é a
implementação desta especificação; quando os dois divergirem, este documento
descreve a intenção e o código descreve o que acontece de fato.

## Decisões de projeto

| Decisão | Escolha | Consequência |
|---|---|---|
| Onde a regra vive | Python (`src/combate/`) | Fonte única da verdade. O frontend consome o resultado, nunca recalcula. |
| Granularidade | Unidade por unidade | Cada plataforma é uma entidade com resistência e suprimento próprios. |
| Arbitragem | Motor resolve sozinho | Sem mestre e sem override manual. Mesma semente + mesmos dados = mesmo resultado. |

### Por que infantaria é agrupada

A granularidade é "unidade por unidade" para **plataformas** — cada tanque,
caça, navio, drone e míssil existe como objeto. Infantaria é a exceção: a
entidade atômica é a **companhia de 100 homens** (`SOLDADOS_POR_COMPANHIA`).
Individualizar 220 mil soldados multiplicaria o custo por duas ordens de
grandeza sem mudar o resultado — um soldado sozinho não toma decisão tática no
modelo. As baixas continuam contadas por pessoa, proporcionais à integridade da
companhia.

Ordem de batalha do Brasil hoje: ~10 mil entidades ativas. Uma guerra de 5
rodadas resolve em ~0,5 s.

## Sequência de uma rodada

Cada rodada é um turno de guerra. As fases rodam em ordem e cada uma entrega um
modificador para a seguinte.

### 0 — Logística (antes de tudo)

Duas restrições independentes, e a mais apertada manda:

- **Transporte**: capacidade dos caminhões e aeronaves de carga sobreviventes,
  dividida pela distância até a frente. O atacante opera a `distancia = 1.8` por
  padrão; o defensor, em casa, a `1.0`.
- **Verba**: o tesouro precisa cobrir o custo operacional da rodada.

O suprimento se move gradualmente até o alvo (`VELOCIDADE_AJUSTE = 0.5`) e tem
piso em `0.25`. Um exército não passa de abastecido a faminto em uma rodada, e
mesmo cercado ainda resiste um pouco.

**Ponto de acoplamento importante:** destruir os caminhões inimigos na fase de
golpe profundo derruba a capacidade logística de verdade, porque ela é calculada
a partir dos caminhões ainda vivos. Não existe uma "penalidade de suprimento"
inventada — ela emerge.

### 1 — Inteligência

Cobertura de cada lado, de 0 a 1:

```
cobertura = sensores_próprios / (sensores_próprios + assinatura_inimiga × 0.5)
```

É uma medida **absoluta**, não uma divisão de 100% entre os dois. Perder o
reconhecimento inimigo não deixa ninguém onisciente — foi o que quebrou a
primeira versão, em que a inteligência subia para 99% depois de abater os
drones adversários.

A inteligência faz duas coisas:

- **Estreita o dado.** Amplitude = `0.45 × (1 − 0.70 × cobertura)`. Informação
  boa compra previsibilidade, não poder de fogo.
- **Melhora a escolha de alvo.** Mais candidatos comparados por ataque.

Vantagem de `0.25` ou mais sobre o adversário concede **surpresa operacional**:
multiplicador de `1.30` na rodada e iniciativa na fase aérea.

### 2 — Campanha aérea

Quem tem melhor inteligência abre a fase: vê primeiro, atira primeiro. Alvos:
aeronaves e baterias antiaéreas.

Ao fim da fase, calcula-se a **supremacia aérea** pela razão de poder aéreo
sobrevivente, somada à projeção dos porta-aviões. Ela vira um multiplicador
para todas as fases seguintes:

```
multiplicador = 1.0 + 1.2 × (supremacia − 0.5)     # limitado a [0.5, 1.6]
```

Perder o céu não mata o exército — faz cada fase seguinte custar o dobro.

### 3 — Golpe profundo

Mísseis, drones kamikaze, submarinos e caças atacam a **retaguarda**: logística,
defesa antiaérea e alvos navais. É a fase que estrangula o inimigo sem enfrentar
a linha de frente.

- **Interceptação**: `densidade_AA × (0.40 + 0.15 × assinatura)`. Um míssil
  hipersônico (assinatura 1.0) passa muito mais que um balístico (assinatura 4.0).
- **Racionamento**: no máximo `35%` de cada estoque de consumíveis por rodada.
  Sem isso o arsenal inteiro é disparado na rodada 1 e as rodadas seguintes
  ficam vazias.

Consumíveis são gastos mesmo quando interceptados.

### 4 — Mar

Combate naval. Quem sobrar com marinha e o adversário não, impõe **bloqueio**.

### 5 — Terra

A única fase que move a linha de frente. Alvos: unidades terrestres de combate —
**comboio logístico não está na linha de frente** e só é atingido no golpe
profundo.

A frente começa em 50/100 e se move pela **razão** de dano, não pelo valor
bruto:

```
movimento = (dano_atacante / dano_total − 0.5) × 2 × 12     # máx ±12/rodada
```

Usar a razão mantém a escala coerente entre uma escaramuça de fronteira e uma
guerra continental.

## Resolução de um ataque

Para cada unidade atuante na fase:

```
poder = ataque_contra(domínio_do_alvo)
      × integridade × suprimento × moral × treinamento × bônus_de_tipo
      × multiplicador_de_supremacia
      × sorte_da_fase

dano  = poder × dado_da_unidade / (1 + defesa_do_alvo)
```

**Dois dados, não um.** Com dez mil unidades, os dados individuais se cancelam
pela lei dos grandes números e toda batalha dá o mesmo resultado. A `sorte_da_fase`
é um único dado para a fase inteira — é ela que faz duas guerras idênticas
terminarem diferente.

### Saída de combate

Uma unidade é retirada abaixo de `20%` de integridade (`LIMIAR_INEFICACIA`).
Nenhuma formação continua lutando com 5% da capacidade; ela quebra bem antes de
ser aniquilada.

### Escolha de alvo

O atacante sorteia candidatos e escolhe o mais valioso entre eles, por
`assinatura × (1 + ameaça/40) × integridade`. Quanto melhor a inteligência,
mais candidatos ele compara.

## Escada de escalada

Ogivas nucleares **não são mais uma arma**. Ficam atrás de um gate:

- `doutrina.permitir_nuclear` precisa ser `true`, senão as ogivas nem entram na
  ordem de batalha.
- Área de efeito de 80 alvos por ogiva.
- Custo político: **−25 de estabilidade** para quem empregou, **−5** para o
  outro lado (reação internacional).

## Consequências

| Efeito | Cálculo |
|---|---|
| Baixas humanas | `efetivos × (1 − integridade)`, somado — feridos contam, não só unidades destruídas |
| Custo | Custo operacional acumulado, debitado do tesouro a cada rodada |
| Moral | Cai com perdas relativas; penalidade extra abaixo de 60% de suprimento; piso em 0.40 |
| Estabilidade | Ajuste político pelo veredito − sangria (`baixas/efetivos × 80`) − penalidade nuclear |

### Vereditos

| Frente final | Veredito |
|---|---|
| ≥ 75 | vitória decisiva |
| 60–75 | vitória tática |
| 40–60 | impasse |
| 25–40 | ofensiva contida |
| < 25 | derrota decisiva |

Aniquilação da capacidade de combate de um lado encerra a guerra antes das
rodadas previstas.

## Balanceamento observado

Com os dados atuais de `data/paises.json`:

- **Brasil → Argentina**: vitória decisiva. Razão de forças de 2,4:1 somada a
  supremacia aérea e ISR total. A lei quadrática de Lanchester amplifica a
  vantagem — o resultado assimétrico é esperado, não um bug.
- **Brasil → Brasil (espelho)**: impasse na maioria das sementes, com a frente
  entre 44 e 70 conforme a sorte da campanha. O atacante leva pequena vantagem
  de iniciativa.

## Dados de entrada

`data/paises.json` é a fonte única. O inventário militar veio de `stats.html`,
com dois campos novos:

- `defesas.baterias_antiaereas` — não existia e a interceptação precisa dele.
- `doutrina` — `mobilizacao_reserva`, `agressividade`, `permitir_nuclear`,
  `qualidade_treinamento`.

`terrestre.equipamento_infantaria` não vira entidade: é um **modificador**. A
cobertura (`equipamento / soldados_ativos`) multiplica o ataque da infantaria
por `0.5 + 0.5 × cobertura`. O Brasil, com 85 mil equipamentos para 220 mil
soldados, luta a 69% da capacidade nominal de infantaria.

## Como rodar

```
python src/simular_batalha.py Brasil Argentina --semente 42
python src/simular_batalha.py Brasil Argentina --json
python -m unittest discover -s tests
```

## Limitações conhecidas

- Só Brasil e Argentina têm inventário militar. França e Egito aparecem em
  `src/main.py` sem dados — precisam ser preenchidos para entrar em guerra.
- Não há geografia: `distancia` é um único número, sem terreno, fronteira ou
  teatro. Uma guerra naval contra um país sem litoral não é impedida.
- Não há alianças: os acordos em `paises.json` são decorativos no combate.
- Sem reposição: nenhuma unidade é reconstruída entre rodadas. A guerra só
  consome.
- O frontend ainda não consome o motor. `index.html` tem sua própria cópia dos
  países e a função `atacar()` só escreve no console.

## Arquivos legados

`src/engine_combate.py`, `src/main.py` e `src/pais.py` são a primeira versão do
combate e foram substituídos por `src/combate/`. `src/pais.py` está quebrado
(instancia `Pais` com 4 argumentos para 5 parâmetros) e `src/main.py` executa o
loop do jogo no import. Devem ser removidos quando o frontend migrar.
