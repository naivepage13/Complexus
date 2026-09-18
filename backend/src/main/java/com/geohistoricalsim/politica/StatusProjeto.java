package com.geohistoricalsim.politica;

/**
 * Tramitacao de um projeto de lei:
 *
 * RASCUNHO -> EM_VOTACAO -> (APROVADO | REJEITADO)
 * APROVADO -> (SANCIONADO | VETADO)
 * VETADO -> SANCIONADO quando o legislativo derruba o veto.
 */
public enum StatusProjeto {
    RASCUNHO("Rascunho"),
    EM_VOTACAO("Em votacao"),
    APROVADO("Aprovado pelo legislativo"),
    REJEITADO("Rejeitado"),
    SANCIONADO("Sancionado e em vigor"),
    VETADO("Vetado pelo executivo");

    private final String rotulo;

    StatusProjeto(String rotulo) {
        this.rotulo = rotulo;
    }

    public String getRotulo() {
        return rotulo;
    }

    public boolean encerrado() {
        return this == REJEITADO || this == SANCIONADO;
    }
}
