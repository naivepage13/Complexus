package com.complexus.operacoes;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

/**
 * Confere o mundo de verdade (mundo/operacoes.json e os GeoJSON do mapa): um
 * ponto digitado errado ali vira navio em terra ou rodovia que da a volta no
 * pais, e so estes testes percebem.
 */
class CartografiaMundoTest {

    private static CartografiaMundo mundo;

    @BeforeAll
    static void carregar() {
        mundo = new CartografiaMundo(new ObjectMapper());
    }

    @Test
    @DisplayName("O corredor maritimo passa inteiro pelo mar")
    void corredorNoMar() {
        List<Ponto> corredor = mundo.corredorMaritimo();

        assertAll(java.util.stream.IntStream.range(1, corredor.size()).mapToObj(i -> (Executable) () -> {
            Ponto a = corredor.get(i - 1);
            Ponto b = corredor.get(i);
            assertFalse(mundo.terra().contem(b), "ponto em terra: " + b);
            assertFalse(mundo.terra().cruza(a, b), "perna cruza terra: " + a + " -> " + b);
        }));
    }

    @Test
    @DisplayName("Todo ancoradouro fica no mar, dentro da area de operacoes")
    void ancoradourosNoMar() {
        assertFalse(mundo.portos().isEmpty());
        assertAll(mundo.portos().stream().map(porto -> (Executable) () -> {
            assertFalse(mundo.terra().contem(porto.ancoradouro()), porto.id() + " em terra");
            assertTrue(mundo.areaMaritima().contem(porto.ancoradouro()), porto.id() + " fora da area");
        }));
    }

    @Test
    @DisplayName("Cada forca comeca no ambiente que consegue ocupar")
    void forcasNoAmbienteCerto() {
        assertTrue(mundo.unidadesIniciais().size() >= 12);
        assertAll(mundo.unidadesIniciais().stream().map(unidade -> (Executable) () -> {
            boolean emTerra = mundo.terra().contem(unidade.posicao());
            switch (unidade.tipo().ramo()) {
                case EXERCITO, AERONAUTICA -> assertTrue(emTerra, unidade.nome() + " no mar");
                case MARINHA -> {
                    assertFalse(emTerra, unidade.nome() + " em terra");
                    assertTrue(mundo.areaMaritima().contem(unidade.posicao()), unidade.nome() + " fora da area");
                }
            }
        }));
    }

    @Test
    @DisplayName("Rotas de caminhao seguem a malha sem voltas absurdas")
    void rotasRodoviariasPlausiveis() {
        List<CartografiaMundo.RotaCivil> rodovias = mundo.rotasCivis().stream()
                .filter(rota -> rota.tipo() == TipoVeiculo.CAMINHAO).toList();

        assertTrue(rodovias.size() >= 12);
        assertAll(rodovias.stream().map(rota -> (Executable) () -> {
            List<Trajeto.Marco> marcos = rota.trajeto().marcos();
            double percorrido = 0;
            for (int i = 1; i < marcos.size(); i++) {
                percorrido += marcos.get(i - 1).ponto().distanciaKm(marcos.get(i).ponto());
            }
            double reta = marcos.get(0).ponto().distanciaKm(rota.trajeto().destino());
            assertTrue(percorrido <= reta * 1.9,
                    rota.id() + ": " + Math.round(percorrido) + " km para " + Math.round(reta) + " km em reta");
        }));
    }

    @Test
    @DisplayName("Ha rotas dos tres tipos e cada veiculo aponta para uma delas")
    void veiculosEmRotasExistentes() {
        for (TipoVeiculo tipo : TipoVeiculo.values()) {
            assertTrue(mundo.rotasCivis().stream().anyMatch(rota -> rota.tipo() == tipo), "sem rota " + tipo);
        }
        List<String> rotas = mundo.rotasCivis().stream().map(CartografiaMundo.RotaCivil::id).toList();
        assertTrue(mundo.veiculos().stream().allMatch(veiculo -> rotas.contains(veiculo.rotaId())));
        assertTrue(mundo.veiculos().size() > rotas.size());
    }
}
