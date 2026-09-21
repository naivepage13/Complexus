package com.complexus.politica;

/** Nivel federativo em que um cargo atua e em que uma lei vigora. */
public enum Esfera {
    FEDERAL("Federal"),
    ESTADUAL("Estadual"),
    MUNICIPAL("Municipal");

    private final String rotulo;

    Esfera(String rotulo) {
        this.rotulo = rotulo;
    }

    public String getRotulo() {
        return rotulo;
    }
}
