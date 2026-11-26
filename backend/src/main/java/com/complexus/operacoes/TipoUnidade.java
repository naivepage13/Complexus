package com.complexus.operacoes;

/**
 * Tipos de unidade militar e suas velocidades de deslocamento em km/h.
 * Navios e aeronaves tem uma velocidade so; tropas andam mais devagar fora da
 * rodovia.
 */
public enum TipoUnidade {
    BRIGADA_INFANTARIA(RamoMilitar.EXERCITO, "Brigada de infantaria", 60, 25),
    BRIGADA_BLINDADA(RamoMilitar.EXERCITO, "Brigada blindada", 45, 20),
    FRAGATA(RamoMilitar.MARINHA, "Fragata", 50, 50),
    SUBMARINO(RamoMilitar.MARINHA, "Submarino", 30, 30),
    NAVIO_PATRULHA(RamoMilitar.MARINHA, "Navio-patrulha", 40, 40),
    CACA(RamoMilitar.AERONAUTICA, "Esquadrao de caca", 900, 900),
    TRANSPORTE_AEREO(RamoMilitar.AERONAUTICA, "Esquadrao de transporte", 700, 700);

    private final RamoMilitar ramo;
    private final String rotulo;
    private final double velocidadeEstrada;
    private final double velocidadeForaDaEstrada;

    TipoUnidade(RamoMilitar ramo, String rotulo, double velocidadeEstrada, double velocidadeForaDaEstrada) {
        this.ramo = ramo;
        this.rotulo = rotulo;
        this.velocidadeEstrada = velocidadeEstrada;
        this.velocidadeForaDaEstrada = velocidadeForaDaEstrada;
    }

    public RamoMilitar ramo() { return ramo; }
    public String rotulo() { return rotulo; }
    public double velocidadeEstrada() { return velocidadeEstrada; }
    public double velocidadeForaDaEstrada() { return velocidadeForaDaEstrada; }
}
