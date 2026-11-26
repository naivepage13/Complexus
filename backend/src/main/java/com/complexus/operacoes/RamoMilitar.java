package com.complexus.operacoes;

/** Forca a que uma unidade pertence; define por onde ela pode se deslocar. */
public enum RamoMilitar {
    EXERCITO("Exercito"),
    MARINHA("Marinha"),
    AERONAUTICA("Forca Aerea");

    private final String rotulo;

    RamoMilitar(String rotulo) {
        this.rotulo = rotulo;
    }

    public String getRotulo() {
        return rotulo;
    }
}
