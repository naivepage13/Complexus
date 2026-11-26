package com.complexus.operacoes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.complexus.comum.RegraDeNegocioException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Regras de deslocamento por ramo, sobre a geografia real do jogo. */
class PlanejadorMovimentoTest {

    private static final Ponto SAO_PAULO = new Ponto(-46.6333, -23.5505);
    private static final Ponto BRASILIA = new Ponto(-47.8828, -15.7939);
    private static final Ponto MAR_DO_RIO = new Ponto(-43.05, -23.30);
    private static final Ponto MAR_DO_RECIFE = new Ponto(-34.50, -8.10);
    private static final Ponto PACIFICO = new Ponto(-85.0, -15.0);
    private static final Ponto LISBOA = new Ponto(-9.14, 38.72);

    private static CartografiaMundo mundo;
    private static PlanejadorMovimento planejador;

    @BeforeAll
    static void carregar() {
        mundo = new CartografiaMundo(new ObjectMapper());
        planejador = mundo.planejador();
    }

    @Test
    @DisplayName("Tropa em viagem longa usa a rodovia e chega ao ponto pedido")
    void tropaUsaRodovia() {
        Trajeto trajeto = planejador.planejar(TipoUnidade.BRIGADA_INFANTARIA, SAO_PAULO, BRASILIA);

        double foraDaEstrada = SAO_PAULO.distanciaKm(BRASILIA) / 25;
        assertEquals(BRASILIA, trajeto.destino());
        assertTrue(trajeto.marcos().size() > 10, "deveria seguir os pontos da rodovia");
        assertTrue(trajeto.duracaoHoras() < foraDaEstrada);
    }

    @Test
    @DisplayName("Tropa rumo a area sem rodovia aproveita a estrada ate onde ela ajuda")
    void tropaUsaRodoviaAteOndeAjuda() {
        Ponto cuiaba = new Ponto(-56.0979, -15.6014);
        Ponto sulDoAmazonas = new Ponto(-61.096, -7.06);

        Trajeto trajeto = planejador.planejar(TipoUnidade.BRIGADA_BLINDADA, cuiaba, sulDoAmazonas);

        double foraDaEstrada = cuiaba.distanciaKm(sulDoAmazonas) / 20;
        assertTrue(trajeto.marcos().size() > 10, "deveria seguir os pontos da rodovia");
        assertTrue(trajeto.duracaoHoras() < foraDaEstrada);
    }

    @Test
    @DisplayName("Tropa em deslocamento curto vai direto")
    void tropaVaiDiretoQuandoCompensa() {
        Ponto perto = new Ponto(-47.90, -15.76);

        Trajeto trajeto = planejador.planejar(TipoUnidade.BRIGADA_INFANTARIA, BRASILIA, perto);

        assertEquals(2, trajeto.marcos().size());
    }

    @Test
    @DisplayName("Tropa nao aceita destino no mar")
    void tropaNaoVaiAoMar() {
        assertThrows(RegraDeNegocioException.class,
                () -> planejador.planejar(TipoUnidade.BRIGADA_BLINDADA, SAO_PAULO, MAR_DO_RIO));
    }

    @Test
    @DisplayName("Navio contorna a costa sem passar por terra")
    void navioContornaCosta() {
        Trajeto trajeto = planejador.planejar(TipoUnidade.FRAGATA, MAR_DO_RIO, MAR_DO_RECIFE);

        List<Trajeto.Marco> marcos = trajeto.marcos();
        assertEquals(MAR_DO_RECIFE, trajeto.destino());
        for (int i = 1; i < marcos.size(); i++) {
            assertFalse(mundo.terra().cruza(marcos.get(i - 1).ponto(), marcos.get(i).ponto()));
        }
    }

    @Test
    @DisplayName("Navio alcanca o alto-mar que aparece no mapa ao redor do pais")
    void navioAlcancaAltoMar() {
        for (Ponto altoMar : List.of(new Ponto(-35.2, -25.35), new Ponto(-30.0, -10.0),
                new Ponto(-44.0, -33.0), new Ponto(-42.0, 3.0))) {
            assertEquals(altoMar,
                    planejador.planejar(TipoUnidade.FRAGATA, MAR_DO_RIO, altoMar).destino());
        }
    }

    @Test
    @DisplayName("Navio nao aceita destino em terra nem fora da area de operacoes")
    void navioRecusaDestinoInvalido() {
        assertThrows(RegraDeNegocioException.class,
                () -> planejador.planejar(TipoUnidade.FRAGATA, MAR_DO_RIO, BRASILIA));
        assertThrows(RegraDeNegocioException.class,
                () -> planejador.planejar(TipoUnidade.FRAGATA, MAR_DO_RIO, PACIFICO));
    }

    @Test
    @DisplayName("Aeronave voa em linha reta na propria velocidade")
    void aeronaveEmLinhaReta() {
        Trajeto trajeto = planejador.planejar(TipoUnidade.CACA, SAO_PAULO, MAR_DO_RECIFE);

        assertEquals(2, trajeto.marcos().size());
        assertEquals(SAO_PAULO.distanciaKm(MAR_DO_RECIFE) / 900, trajeto.duracaoHoras(), 0.001);
    }

    @Test
    @DisplayName("Aeronave nao sai do teatro de operacoes")
    void aeronaveDentroDoTeatro() {
        assertThrows(RegraDeNegocioException.class,
                () -> planejador.planejar(TipoUnidade.CACA, SAO_PAULO, LISBOA));
    }
}
