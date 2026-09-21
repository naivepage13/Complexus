package com.complexus.economia;

/**
 * Resultado de um mes de operacao de uma empresa, antes de qualquer
 * persistencia. Todos os valores em R$ de jogo.
 *
 * @param demandaCapturada demanda que a empresa conquistou no mercado
 * @param capacidade       quanto a empresa conseguiria produzir e vender
 * @param receita          menor valor entre demanda e capacidade
 * @param custoOperacional folha, insumos, marketing, depreciacao e juros
 * @param impostoIndireto  tributos estaduais e municipais sobre receita
 * @param impostoRenda     tributo federal sobre o lucro
 * @param subsidioRecebido transferencia publica creditada no turno
 * @param lucro            resultado liquido do mes
 * @param ocupacao         uso da capacidade instalada (0 a 1)
 */
public record ResultadoMensal(double demandaCapturada,
                              double capacidade,
                              double receita,
                              double custoOperacional,
                              double impostoIndireto,
                              double impostoRenda,
                              double subsidioRecebido,
                              double lucro,
                              double ocupacao) {
}
