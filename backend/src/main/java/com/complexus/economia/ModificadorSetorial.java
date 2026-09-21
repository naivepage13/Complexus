package com.complexus.economia;

/**
 * Choques setoriais do turno, normalmente calculados pelo servico analitico em
 * Python. Valores em fracao: 0.03 significa 3 por cento acima do normal.
 *
 * @param choqueDemanda variacao da demanda do setor no turno
 * @param choqueCusto   variacao dos custos de insumo do setor no turno
 * @param confianca     confianca do mercado no setor (0.5 a 1.5), afeta valuation
 */
public record ModificadorSetorial(double choqueDemanda, double choqueCusto, double confianca) {

    public static ModificadorSetorial neutro() {
        return new ModificadorSetorial(0.0, 0.0, 1.0);
    }
}
