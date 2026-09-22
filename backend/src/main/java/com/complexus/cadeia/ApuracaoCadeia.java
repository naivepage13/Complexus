package com.complexus.cadeia;

import com.complexus.economia.EfeitoCadeia;

/**
 * O que os contratos de fornecimento significam para uma empresa em um turno,
 * ja considerando o que o fornecedor conseguiu entregar de fato.
 *
 * As duas pontas saem da mesma apuracao, feita antes de qualquer empresa
 * produzir: se o fornecedor entrega menos do que prometeu, o comprador recebe
 * menos insumo contratado no mesmo turno, e nao no seguinte.
 *
 * @param capacidadeReservada capacidade que sai do mercado aberto para honrar contratos
 * @param insumoContratado    insumo que chega por contrato, a preco de referencia
 * @param precoInsumoContratado preco medio pago por esse insumo
 * @param receitaContratos    faturamento das entregas do turno
 * @param custoContratos      insumo consumido para produzir o que foi entregue
 */
public record ApuracaoCadeia(double capacidadeReservada,
                             double insumoContratado,
                             double precoInsumoContratado,
                             double receitaContratos,
                             double custoContratos) {

    public static final ApuracaoCadeia NENHUMA = new ApuracaoCadeia(0, 0, 1.0, 0, 0);

    public EfeitoCadeia efeito() {
        return new EfeitoCadeia(capacidadeReservada, insumoContratado, precoInsumoContratado);
    }

    public boolean temMovimento() {
        return receitaContratos > 0 || insumoContratado > 0;
    }
}
