package com.complexus.economia;

/**
 * Retrato de uma unidade no momento do turno, do jeito que o motor precisa.
 *
 * A unidade e a operacao: produz, vende, emprega e ocupa um mercado. A empresa
 * e a camada financeira: divida, tributo sobre lucro e estrutura administrativa.
 * Separar os dois permite simular filiais que competem em cidades diferentes sem
 * duplicar o resultado financeiro da companhia.
 *
 * @param fatorPreco         preco praticado sobre o preco de referencia do setor
 *                           (vem do mix de linhas de produto)
 * @param fatorCustoVariavel custo de insumo sobre o padrao do setor (posicionamento
 *                           das linhas e ganho de logistica)
 * @param bonusComercial     acrescimo de competitividade vindo do departamento
 *                           comercial, alem do marketing
 * @param capacidadeReservada parte da capacidade ja comprometida com contratos
 *                           de fornecimento, que nao disputa o mercado aberto
 * @param insumoContratado   insumo coberto por contrato, a preco de referencia
 * @param precoInsumoContratado preco pago por esse insumo sobre a referencia
 */
public record PerfilOperacional(Setor setor,
                                double patrimonio,
                                int funcionarios,
                                double produtividade,
                                double salarioMedio,
                                double marketingMensal,
                                double reputacao,
                                double fatorPreco,
                                double fatorCustoVariavel,
                                double bonusComercial,
                                double capacidadeReservada,
                                double insumoContratado,
                                double precoInsumoContratado) {

    /** Perfil de uma empresa sem estrutura declarada: preco e custo de referencia. */
    public static PerfilOperacional neutro(Empresa empresa) {
        return new PerfilOperacional(empresa.getSetor(), empresa.getPatrimonio(), empresa.getFuncionarios(),
                empresa.getProdutividade(), empresa.getSalarioMedio(), empresa.getMarketingMensal(),
                empresa.getReputacao(), 1.0, 1.0, 0.0, 0.0, 0.0, 1.0);
    }
}
