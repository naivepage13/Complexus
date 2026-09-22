package com.complexus.investimento;

/** Natureza de um lancamento no livro-razao do jogo. */
public enum TipoLancamento {
    APORTE_FUNDACAO("Aporte de fundacao da empresa"),
    COMPRA_ACOES("Compra de acoes"),
    VENDA_ACOES("Venda de acoes"),
    DIVIDENDO("Dividendo distribuido"),
    RECEITA_OPERACIONAL("Receita operacional"),
    CUSTO_OPERACIONAL("Custo operacional"),
    IMPOSTO("Recolhimento de imposto"),
    SUBSIDIO("Subsidio publico recebido"),
    CAPEX("Investimento em ativo"),
    OBRA("Aporte em empreendimento"),
    ENTREGA_OBRA("Entrega de empreendimento"),
    JUROS("Juros sobre divida"),
    EMPRESTIMO("Credito liberado"),
    AMORTIZACAO("Amortizacao de divida"),
    EXECUCAO_GARANTIA("Execucao de garantia"),
    FORNECIMENTO("Fornecimento entre empresas"),
    MULTA_CONTRATUAL("Multa por rompimento de contrato"),
    TRANSFERENCIA_TESOURO("Transferencia do tesouro");

    private final String rotulo;

    TipoLancamento(String rotulo) {
        this.rotulo = rotulo;
    }

    public String getRotulo() {
        return rotulo;
    }
}
