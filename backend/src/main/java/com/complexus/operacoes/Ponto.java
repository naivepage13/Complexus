package com.complexus.operacoes;

/** Coordenada geografica em graus decimais. */
public record Ponto(double lon, double lat) {

    private static final double RAIO_TERRA_KM = 6371.0;

    /** Distancia sobre a superficie da Terra (formula de haversine). */
    public double distanciaKm(Ponto outro) {
        double dLat = Math.toRadians(outro.lat - lat);
        double dLon = Math.toRadians(outro.lon - lon);
        double a = Math.pow(Math.sin(dLat / 2), 2)
                + Math.cos(Math.toRadians(lat)) * Math.cos(Math.toRadians(outro.lat))
                * Math.pow(Math.sin(dLon / 2), 2);
        return 2 * RAIO_TERRA_KM * Math.asin(Math.min(1, Math.sqrt(a)));
    }

    /** Ponto intermediario em linha reta; {@code fracao} vai de 0 (aqui) a 1 (destino). */
    public Ponto interpolar(Ponto destino, double fracao) {
        return new Ponto(lon + (destino.lon - lon) * fracao, lat + (destino.lat - lat) * fracao);
    }
}
