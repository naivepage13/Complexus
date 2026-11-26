package com.complexus.operacoes;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.ArrayList;
import java.util.List;

/**
 * Conjunto de poligonos em coordenadas geograficas. Cada poligono e uma lista
 * de aneis: o primeiro e o contorno e os demais sao buracos.
 */
public final class TerritorioGeografico {

    private record Poligono(List<List<Ponto>> aneis, double oeste, double sul, double leste, double norte) {
    }

    private final List<Poligono> poligonos = new ArrayList<>();

    public TerritorioGeografico(List<List<List<Ponto>>> definicao) {
        for (List<List<Ponto>> aneis : definicao) {
            double oeste = Double.MAX_VALUE;
            double sul = Double.MAX_VALUE;
            double leste = -Double.MAX_VALUE;
            double norte = -Double.MAX_VALUE;
            for (Ponto p : aneis.get(0)) {
                oeste = Math.min(oeste, p.lon());
                leste = Math.max(leste, p.lon());
                sul = Math.min(sul, p.lat());
                norte = Math.max(norte, p.lat());
            }
            poligonos.add(new Poligono(aneis, oeste, sul, leste, norte));
        }
    }

    /** Le um FeatureCollection com geometrias Polygon ou MultiPolygon. */
    public static TerritorioGeografico deGeoJson(JsonNode colecao) {
        List<List<List<Ponto>>> poligonos = new ArrayList<>();
        for (JsonNode feature : colecao.get("features")) {
            JsonNode geometria = feature.get("geometry");
            JsonNode coordenadas = geometria.get("coordinates");
            if ("Polygon".equals(geometria.get("type").asText())) {
                poligonos.add(aneis(coordenadas));
            } else {
                for (JsonNode poligono : coordenadas) {
                    poligonos.add(aneis(poligono));
                }
            }
        }
        return new TerritorioGeografico(poligonos);
    }

    private static List<List<Ponto>> aneis(JsonNode poligono) {
        List<List<Ponto>> aneis = new ArrayList<>();
        for (JsonNode anel : poligono) {
            List<Ponto> pontos = new ArrayList<>();
            for (JsonNode par : anel) {
                pontos.add(new Ponto(par.get(0).asDouble(), par.get(1).asDouble()));
            }
            aneis.add(pontos);
        }
        return aneis;
    }

    /** Regra do par-impar: conta quantas bordas um raio para leste atravessa. */
    public boolean contem(Ponto p) {
        for (Poligono poligono : poligonos) {
            if (p.lon() < poligono.oeste() || p.lon() > poligono.leste()
                    || p.lat() < poligono.sul() || p.lat() > poligono.norte()) {
                continue;
            }
            boolean dentro = false;
            for (List<Ponto> anel : poligono.aneis()) {
                for (int i = 0, j = anel.size() - 1; i < anel.size(); j = i++) {
                    Ponto a = anel.get(i);
                    Ponto b = anel.get(j);
                    if ((a.lat() > p.lat()) != (b.lat() > p.lat())
                            && p.lon() < (b.lon() - a.lon()) * (p.lat() - a.lat())
                            / (b.lat() - a.lat()) + a.lon()) {
                        dentro = !dentro;
                    }
                }
            }
            if (dentro) {
                return true;
            }
        }
        return false;
    }

    /** Verdadeiro quando o segmento atravessa alguma borda. */
    public boolean cruza(Ponto a, Ponto b) {
        double oeste = Math.min(a.lon(), b.lon());
        double leste = Math.max(a.lon(), b.lon());
        double sul = Math.min(a.lat(), b.lat());
        double norte = Math.max(a.lat(), b.lat());
        for (Poligono poligono : poligonos) {
            if (leste < poligono.oeste() || oeste > poligono.leste()
                    || norte < poligono.sul() || sul > poligono.norte()) {
                continue;
            }
            for (List<Ponto> anel : poligono.aneis()) {
                for (int i = 1; i < anel.size(); i++) {
                    if (intersectam(a, b, anel.get(i - 1), anel.get(i))) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private static boolean intersectam(Ponto a, Ponto b, Ponto c, Ponto d) {
        return lado(c, d, a) * lado(c, d, b) < 0 && lado(a, b, c) * lado(a, b, d) < 0;
    }

    private static double lado(Ponto a, Ponto b, Ponto p) {
        return (b.lon() - a.lon()) * (p.lat() - a.lat()) - (b.lat() - a.lat()) * (p.lon() - a.lon());
    }
}
