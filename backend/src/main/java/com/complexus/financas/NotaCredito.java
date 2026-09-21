package com.complexus.financas;

/**
 * Nota de credito da empresa.
 *
 * Define quanto o mercado cobra a mais pelo risco e quanto esta disposto a
 * emprestar. E o que faz alavancagem ter preco: a empresa que se endivida demais
 * nao e proibida de tomar credito, ela passa a pagar mais caro por ele.
 *
 * @param spreadAnual    acrescimo de risco sobre a taxa basica
 * @param fatorLimite    multiplicador do limite da modalidade
 * @param scoreMinimo    score a partir do qual a nota vale
 */
public enum NotaCredito {

    A("A", "Risco baixo", 0.020, 1.00, 75),
    B("B", "Risco moderado", 0.050, 0.80, 55),
    C("C", "Risco alto", 0.090, 0.50, 35),
    D("D", "Risco severo", 0.150, 0.25, 0);

    private final String rotulo;
    private final String descricao;
    private final double spreadAnual;
    private final double fatorLimite;
    private final int scoreMinimo;

    NotaCredito(String rotulo, String descricao, double spreadAnual, double fatorLimite, int scoreMinimo) {
        this.rotulo = rotulo;
        this.descricao = descricao;
        this.spreadAnual = spreadAnual;
        this.fatorLimite = fatorLimite;
        this.scoreMinimo = scoreMinimo;
    }

    public String getRotulo() { return rotulo; }
    public String getDescricao() { return descricao; }
    public double getSpreadAnual() { return spreadAnual; }
    public double getFatorLimite() { return fatorLimite; }
    public int getScoreMinimo() { return scoreMinimo; }

    /** Nota correspondente a um score de 0 a 100. */
    public static NotaCredito porScore(double score) {
        for (NotaCredito nota : values()) {
            if (score >= nota.scoreMinimo) {
                return nota;
            }
        }
        return D;
    }
}
