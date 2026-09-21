package com.complexus.economia;

/**
 * Resultado de um mes de uma unidade, antes das despesas da companhia
 * (juros, estrutura administrativa e imposto sobre o lucro).
 *
 * @param demandaCapturada demanda conquistada, a preco de referencia
 * @param capacidade       quanto a unidade conseguiria produzir, a preco de referencia
 * @param volume           o que de fato foi produzido e vendido, a preco de referencia
 * @param receita          volume ja convertido pelo preco praticado
 * @param custoOperacional insumos, folha, marketing e depreciacao da unidade
 * @param impostoIndireto  tributos estaduais e municipais sobre a receita
 * @param subsidioRecebido transferencia publica creditada no turno
 * @param ocupacao         uso da capacidade instalada (0 a 1)
 */
public record ResultadoOperacional(double demandaCapturada,
                                   double capacidade,
                                   double volume,
                                   double receita,
                                   double custoOperacional,
                                   double impostoIndireto,
                                   double subsidioRecebido,
                                   double ocupacao) {

    /** Sobra da operacao antes de juros, estrutura e imposto de renda. */
    public double margemOperacional() {
        return receita + subsidioRecebido - custoOperacional - impostoIndireto;
    }
}
