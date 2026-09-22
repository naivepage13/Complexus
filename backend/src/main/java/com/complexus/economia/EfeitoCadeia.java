package com.complexus.economia;

/**
 * Efeito dos contratos de fornecimento sobre a operacao de uma empresa no turno.
 *
 * O record vive aqui, e nao no pacote da cadeia produtiva, porque quem consome
 * e a montagem do perfil operacional. A cadeia calcula, a operacao aplica.
 *
 * @param capacidadeReservada    capacidade ja comprometida com entregas contratadas
 * @param insumoContratado       insumo coberto por contrato, a preco de referencia
 * @param precoInsumoContratado  preco medio pago por esse insumo sobre a referencia
 */
public record EfeitoCadeia(double capacidadeReservada,
                           double insumoContratado,
                           double precoInsumoContratado) {

    public static final EfeitoCadeia NENHUM = new EfeitoCadeia(0, 0, 1.0);
}
