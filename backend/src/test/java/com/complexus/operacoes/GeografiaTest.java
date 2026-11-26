package com.complexus.operacoes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Pecas puras de geografia e movimento, com mundos sinteticos pequenos. */
class GeografiaTest {

    private static Ponto p(double lon, double lat) {
        return new Ponto(lon, lat);
    }

    /** Quadrado de terra entre as longitudes e latitudes -1 e 1. */
    private static TerritorioGeografico ilhaQuadrada() {
        return new TerritorioGeografico(List.of(List.of(
                List.of(p(-1, -1), p(1, -1), p(1, 1), p(-1, 1), p(-1, -1)))));
    }

    @Test
    @DisplayName("Distancia entre Sao Paulo e Rio de Janeiro fica perto de 360 km")
    void distanciaEmEscalaReal() {
        double km = p(-46.63, -23.55).distanciaKm(p(-43.17, -22.91));

        assertEquals(360, km, 10);
    }

    @Test
    @DisplayName("Trajeto acumula o tempo de cada perna conforme a velocidade")
    void trajetoAcumulaHoras() {
        Ponto inicio = p(0, 0);
        Ponto meio = p(1, 0);
        Ponto fim = p(2, 0);
        double perna = inicio.distanciaKm(meio);

        Trajeto trajeto = Trajeto.aPartirDe(inicio).ate(meio, perna).ate(fim, perna / 3).construir();

        assertEquals(4.0, trajeto.duracaoHoras(), 0.001);
        assertEquals(fim, trajeto.destino());
    }

    @Test
    @DisplayName("Posicao no trajeto interpola a perna corrente e para nas pontas")
    void posicaoNoTrajeto() {
        Ponto inicio = p(0, 0);
        Ponto fim = p(2, 0);
        Trajeto trajeto = Trajeto.aPartirDe(inicio).ate(fim, inicio.distanciaKm(fim) / 10).construir();

        assertEquals(1.0, trajeto.posicaoEm(5).lon(), 0.001);
        assertEquals(inicio, trajeto.posicaoEm(-3));
        assertEquals(fim, trajeto.posicaoEm(99));
    }

    @Test
    @DisplayName("Territorio reconhece o que e terra, inclusive buracos")
    void pontoEmTerra() {
        TerritorioGeografico comBuraco = new TerritorioGeografico(List.of(List.of(
                List.of(p(-2, -2), p(2, -2), p(2, 2), p(-2, 2), p(-2, -2)),
                List.of(p(-1, -1), p(1, -1), p(1, 1), p(-1, 1), p(-1, -1)))));

        assertTrue(comBuraco.contem(p(1.5, 0)));
        assertFalse(comBuraco.contem(p(0, 0)));
        assertFalse(comBuraco.contem(p(3, 0)));
    }

    @Test
    @DisplayName("Territorio detecta o segmento que cruza a costa")
    void segmentoCruzaTerra() {
        assertTrue(ilhaQuadrada().cruza(p(-3, 0), p(3, 0)));
        assertFalse(ilhaQuadrada().cruza(p(-3, 2), p(3, 2)));
    }

    @Test
    @DisplayName("Malha viaria emenda trechos que se tocam e pequenas falhas do dado")
    void malhaEmendaTrechos() {
        // O segundo trecho comeca a cerca de 3 km do fim do primeiro.
        MalhaViaria malha = new MalhaViaria(List.of(
                List.of(p(0, 0), p(0.5, 0)),
                List.of(p(0.527, 0), p(1, 0))), 10);

        List<Ponto> caminho = malha.caminho(p(0, 0), p(1, 0));

        assertEquals(List.of(p(0, 0), p(0.5, 0), p(0.527, 0), p(1, 0)), caminho);
    }

    @Test
    @DisplayName("Malha viaria emenda uma falha mesmo havendo desvio longo entre os dois lados")
    void malhaEmendaFalhaComDesvio() {
        MalhaViaria malha = new MalhaViaria(List.of(
                List.of(p(0, 0), p(0.5, 0)),
                List.of(p(0.527, 0), p(1, 0)),
                List.of(p(0, 0), p(0, 1), p(1, 1), p(1, 0))), 10);

        List<Ponto> caminho = malha.caminho(p(0, 0), p(1, 0));

        assertEquals(List.of(p(0, 0), p(0.5, 0), p(0.527, 0), p(1, 0)), caminho);
    }

    @Test
    @DisplayName("Malha viaria escolhe o caminho mais curto entre duas alternativas")
    void malhaEscolheMaisCurto() {
        MalhaViaria malha = new MalhaViaria(List.of(
                List.of(p(0, 0), p(1, 1), p(2, 0)),
                List.of(p(0, 0), p(1, 0), p(2, 0))), 10);

        assertEquals(List.of(p(0, 0), p(1, 0), p(2, 0)), malha.caminho(p(0, 0), p(2, 0)));
    }

    @Test
    @DisplayName("Malha viaria ignora trechos isolados ao encostar origem e destino")
    void malhaIgnoraIlhas() {
        MalhaViaria malha = new MalhaViaria(List.of(
                List.of(p(0, 0), p(1, 0), p(2, 0)),
                List.of(p(2, 3), p(2.01, 3))), 10);

        List<Ponto> caminho = malha.caminho(p(0, 0), p(2, 3));

        assertEquals(p(2, 0), caminho.get(caminho.size() - 1));
    }

    @Test
    @DisplayName("Malha viaria sai da estrada onde a viagem fica mais rapida, nao no no mais proximo do destino")
    void malhaEscolheOndeSair() {
        // O no colado no destino so se alcanca por uma volta enorme.
        MalhaViaria malha = new MalhaViaria(List.of(
                List.of(p(0, 0), p(1, 0), p(2, 0), p(3, 0)),
                List.of(p(0, 0), p(0, 6), p(2, 6), p(2, 0.8))), 10);

        List<Ponto> caminho = malha.caminhoMaisRapido(p(0, 0), p(2, 0.5), 2.0);

        assertEquals(List.of(p(0, 0), p(1, 0), p(2, 0)), caminho);
    }

    @Test
    @DisplayName("Rota maritima vai direto quando nao ha terra no meio")
    void rotaMaritimaDireta() {
        RoteadorMaritimo roteador = new RoteadorMaritimo(ilhaQuadrada(), List.of(p(-2, -2), p(2, -2)));

        Optional<List<Ponto>> rota = roteador.rota(p(-3, 2), p(3, 2));

        assertEquals(List.of(p(-3, 2), p(3, 2)), rota.orElseThrow());
    }

    @Test
    @DisplayName("Rota maritima contorna a terra pelo corredor")
    void rotaMaritimaContorna() {
        TerritorioGeografico terra = ilhaQuadrada();
        RoteadorMaritimo roteador = new RoteadorMaritimo(terra, List.of(p(-2, -2), p(2, -2)));

        List<Ponto> rota = roteador.rota(p(-3, 0), p(3, 0)).orElseThrow();

        assertTrue(rota.size() > 2);
        for (int i = 1; i < rota.size(); i++) {
            assertFalse(terra.cruza(rota.get(i - 1), rota.get(i)), "perna " + i + " cruza terra");
        }
    }

    @Test
    @DisplayName("Rota maritima nao existe para destino cercado de terra")
    void rotaMaritimaImpossivel() {
        RoteadorMaritimo roteador = new RoteadorMaritimo(ilhaQuadrada(), List.of(p(-2, -2), p(2, -2)));

        assertTrue(roteador.rota(p(-3, 0), p(0, 0)).isEmpty());
    }
}
