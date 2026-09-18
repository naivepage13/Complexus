package com.geohistoricalsim.politica;

/**
 * Numero de cadeiras de cada cargo por territorio, na versao simplificada.
 *
 * Os valores sao uma reducao proporcional da estrutura brasileira: mantem a
 * hierarquia e a assimetria entre as casas sem exigir centenas de mandatos.
 */
public final class VagasCargo {

    private VagasCargo() {
    }

    public static int vagas(CargoPolitico cargo) {
        return switch (cargo) {
            case PRESIDENTE, VICE_PRESIDENTE -> 1;
            case MINISTRO -> 5;
            case SENADOR -> 12;
            case DEPUTADO_FEDERAL -> 20;
            case GOVERNADOR, VICE_GOVERNADOR -> 1;
            case SECRETARIO_ESTADUAL -> 3;
            case DEPUTADO_ESTADUAL -> 12;
            case PREFEITO, VICE_PREFEITO -> 1;
            case SECRETARIO_MUNICIPAL -> 2;
            case VEREADOR -> 9;
        };
    }
}
