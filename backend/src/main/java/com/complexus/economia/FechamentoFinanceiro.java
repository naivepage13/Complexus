package com.complexus.economia;

/**
 * O que entra no fechamento do mes por conta da companhia, e nao da operacao
 * de cada unidade.
 *
 * @param juros             juros do mes dos contratos de credito
 * @param custoEstrutura    orcamento dos departamentos
 * @param receitaContratos  faturamento dos contratos de fornecimento entregues
 * @param custoContratos    insumo consumido para entregar esses contratos
 * @param impostoContratos  tributo indireto sobre o faturamento dos contratos
 * @param aliquotaImposto   aliquota federal sobre o lucro
 */
public record FechamentoFinanceiro(double juros,
                                   double custoEstrutura,
                                   double receitaContratos,
                                   double custoContratos,
                                   double impostoContratos,
                                   double aliquotaImposto) {

    /** Fechamento de uma empresa sem divida, estrutura nem contratos. */
    public static FechamentoFinanceiro simples(double juros, double aliquotaImposto) {
        return new FechamentoFinanceiro(juros, 0, 0, 0, 0, aliquotaImposto);
    }
}
