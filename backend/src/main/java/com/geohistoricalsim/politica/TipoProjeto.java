package com.geohistoricalsim.politica;

import java.util.Set;

/**
 * Catalogo de instrumentos legais que um mandato pode propor.
 *
 * Cada tipo define em que esferas pode tramitar, o intervalo valido do
 * parametro numerico da lei e se ela precisa apontar um setor economico alvo.
 * O efeito de cada tipo e aplicado por {@code ServicoPolitica#aplicarEfeito}.
 */
public enum TipoProjeto {

    IMPOSTO_EMPRESARIAL("Aliquota federal sobre lucro das empresas",
            Set.of(Esfera.FEDERAL), 0.0, 0.45, false, "fracao"),

    IMPOSTO_ESTADUAL("Aliquota estadual sobre circulacao de bens",
            Set.of(Esfera.ESTADUAL), 0.0, 0.30, false, "fracao"),

    IMPOSTO_MUNICIPAL("Aliquota municipal sobre servicos",
            Set.of(Esfera.MUNICIPAL), 0.0, 0.15, false, "fracao"),

    SUBSIDIO_SETORIAL("Subsidio mensal a um setor economico",
            Set.of(Esfera.FEDERAL, Esfera.ESTADUAL), 0.0, 0.25, true, "fracao da receita"),

    REGULACAO_SETORIAL("Regulacao que eleva custo e reduz risco do setor",
            Set.of(Esfera.FEDERAL, Esfera.ESTADUAL), 0.0, 0.20, true, "fracao do custo"),

    INVESTIMENTO_INFRAESTRUTURA("Investimento publico em infraestrutura",
            Set.of(Esfera.FEDERAL, Esfera.ESTADUAL), 0.0, 5_000_000_000.0, false, "R$ por turno"),

    PROGRAMA_SOCIAL("Transferencia de renda a populacao",
            Set.of(Esfera.FEDERAL, Esfera.ESTADUAL, Esfera.MUNICIPAL), 0.0, 5_000_000_000.0, false, "R$ por turno"),

    ZONEAMENTO_URBANO("Permissividade do zoneamento urbano",
            Set.of(Esfera.MUNICIPAL), 0.0, 100.0, false, "indice 0-100");

    private final String descricao;
    private final Set<Esfera> esferasPermitidas;
    private final double parametroMinimo;
    private final double parametroMaximo;
    private final boolean exigeSetor;
    private final String unidadeParametro;

    TipoProjeto(String descricao, Set<Esfera> esferasPermitidas, double parametroMinimo,
                double parametroMaximo, boolean exigeSetor, String unidadeParametro) {
        this.descricao = descricao;
        this.esferasPermitidas = esferasPermitidas;
        this.parametroMinimo = parametroMinimo;
        this.parametroMaximo = parametroMaximo;
        this.exigeSetor = exigeSetor;
        this.unidadeParametro = unidadeParametro;
    }

    public String getDescricao() { return descricao; }
    public Set<Esfera> getEsferasPermitidas() { return esferasPermitidas; }
    public double getParametroMinimo() { return parametroMinimo; }
    public double getParametroMaximo() { return parametroMaximo; }
    public boolean isExigeSetor() { return exigeSetor; }
    public String getUnidadeParametro() { return unidadeParametro; }

    public boolean permite(Esfera esfera) {
        return esferasPermitidas.contains(esfera);
    }
}
