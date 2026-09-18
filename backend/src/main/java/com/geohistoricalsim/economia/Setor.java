package com.geohistoricalsim.economia;

/**
 * Setores economicos disponiveis na versao atual do jogo.
 *
 * Cada setor carrega os parametros que a simulacao de turno usa para calcular
 * receita, custo e valor de mercado. Novos setores entram aqui sem alterar o
 * motor de simulacao.
 *
 * <ul>
 *   <li>{@code margemBase} - margem bruta sobre insumos: o que sobra da receita
 *       antes de folha, tributos, depreciacao e juros;</li>
 *   <li>{@code volatilidade} - amplitude do choque aleatorio mensal;</li>
 *   <li>{@code giroAtivoMensal} - receita mensal que cada real de patrimonio
 *       consegue sustentar (teto de producao pelo lado do capital);</li>
 *   <li>{@code receitaPorFuncionario} - receita mensal que um funcionario
 *       entrega (teto de producao pelo lado da equipe);</li>
 *   <li>{@code elasticidadeRenda} - sensibilidade da demanda a renda media;</li>
 *   <li>{@code elasticidadeJuros} - sensibilidade da demanda a taxa de juros;</li>
 *   <li>{@code multiploValuation} - multiplo de lucro anual usado no valuation;</li>
 *   <li>{@code capitalMinimo} - capital exigido para abrir a empresa.</li>
 * </ul>
 */
public enum Setor {

    ALIMENTICIO("Alimenticio", 0.42, 0.05, 0.22, 18_000.0, 0.35, 0.10, 9.0, 250_000.0),
    IMOBILIARIO("Imobiliario", 0.34, 0.11, 0.06, 45_000.0, 0.70, 0.85, 12.0, 1_200_000.0),
    CONSTRUCAO("Construcao", 0.33, 0.09, 0.12, 30_000.0, 0.45, 0.65, 8.0, 800_000.0);

    private final String rotulo;
    private final double margemBase;
    private final double volatilidade;
    private final double giroAtivoMensal;
    private final double receitaPorFuncionario;
    private final double elasticidadeRenda;
    private final double elasticidadeJuros;
    private final double multiploValuation;
    private final double capitalMinimo;

    Setor(String rotulo, double margemBase, double volatilidade, double giroAtivoMensal,
          double receitaPorFuncionario, double elasticidadeRenda, double elasticidadeJuros,
          double multiploValuation, double capitalMinimo) {
        this.rotulo = rotulo;
        this.margemBase = margemBase;
        this.volatilidade = volatilidade;
        this.giroAtivoMensal = giroAtivoMensal;
        this.receitaPorFuncionario = receitaPorFuncionario;
        this.elasticidadeRenda = elasticidadeRenda;
        this.elasticidadeJuros = elasticidadeJuros;
        this.multiploValuation = multiploValuation;
        this.capitalMinimo = capitalMinimo;
    }

    public String getRotulo() { return rotulo; }
    public double getMargemBase() { return margemBase; }
    public double getVolatilidade() { return volatilidade; }
    public double getGiroAtivoMensal() { return giroAtivoMensal; }
    public double getReceitaPorFuncionario() { return receitaPorFuncionario; }

    /** Equipe coerente com um dado patrimonio: evita folha ociosa ou capacidade parada. */
    public int equipeSugerida(double patrimonio) {
        return Math.max((int) Math.round(patrimonio * giroAtivoMensal / receitaPorFuncionario), 1);
    }
    public double getElasticidadeRenda() { return elasticidadeRenda; }
    public double getElasticidadeJuros() { return elasticidadeJuros; }
    public double getMultiploValuation() { return multiploValuation; }
    public double getCapitalMinimo() { return capitalMinimo; }
}
