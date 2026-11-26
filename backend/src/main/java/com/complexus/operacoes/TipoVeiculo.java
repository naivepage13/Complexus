package com.complexus.operacoes;

/** Trafego civil mostrado no mapa, com a velocidade de cruzeiro em km/h. */
public enum TipoVeiculo {
    CAMINHAO("Caminhao", 70, 3),
    NAVIO_CARGA("Navio cargueiro", 28, 2),
    AVIAO_COMERCIAL("Aviao comercial", 800, 2);

    private final String rotulo;
    private final double velocidade;
    private final int veiculosPorRota;

    TipoVeiculo(String rotulo, double velocidade, int veiculosPorRota) {
        this.rotulo = rotulo;
        this.velocidade = velocidade;
        this.veiculosPorRota = veiculosPorRota;
    }

    public String rotulo() { return rotulo; }
    public double velocidade() { return velocidade; }
    public int veiculosPorRota() { return veiculosPorRota; }
}
