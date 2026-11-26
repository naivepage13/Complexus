package com.complexus.operacoes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.complexus.auditoria.ServicoAuditoria;
import com.complexus.comum.RegraDeNegocioException;
import com.complexus.jogador.Jogador;
import com.complexus.jogador.ServicoJogador;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/** Forcas militares: carga inicial, ordens, passagem do tempo e API. */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:operacoes;DB_CLOSE_DELAY=-1",
        "jogo.turno.processamento-automatico=false"
})
@AutoConfigureMockMvc
class OperacoesTest {

    private static final Ponto GOIANIA = new Ponto(-49.2643, -16.6864);
    private static final Ponto MAR_ABERTO = new Ponto(-36.0, -20.0);

    @Autowired
    private ServicoOperacoes servico;
    @Autowired
    private RepositorioUnidadeMilitar repositorio;
    @Autowired
    private SeedForcas seed;
    @Autowired
    private CartografiaMundo mundo;
    @Autowired
    private RelogioOperacoes relogio;
    @Autowired
    private ServicoJogador servicoJogador;
    @Autowired
    private ServicoAuditoria auditoria;
    @Autowired
    private MockMvc mockMvc;

    private Jogador jogador;

    @BeforeEach
    void reiniciarForcas() {
        repositorio.deleteAll();
        seed.semear();
        jogador = servicoJogador.registrar("op" + UUID.randomUUID().toString().substring(0, 8),
                "Comandante", "senha123");
    }

    private ServicoOperacoes.Situacao situacaoDe(String nome) {
        return servico.situacao().stream()
                .filter(s -> s.unidade().getNome().equals(nome))
                .findFirst().orElseThrow();
    }

    private Long idDe(String nome) {
        return situacaoDe(nome).unidade().getId();
    }

    /** Finge que a ordem foi dada ha {@code horas} horas de jogo. */
    private void adiantarPartida(Long unidadeId, double horas) {
        UnidadeMilitar unidade = repositorio.findById(unidadeId).orElseThrow();
        long millis = Math.round(horas / relogio.horasJogoPorSegundo() * 1000);
        unidade.setPartidaEm(Instant.now().minus(Duration.ofMillis(millis)));
        repositorio.save(unidade);
    }

    @Test
    @DisplayName("Uma hora real equivale a um mes de jogo no deslocamento")
    void ritmoDoRelogio() {
        assertEquals(720.0 / 3600.0, relogio.horasJogoPorSegundo(), 1e-9);
    }

    @Test
    @DisplayName("As forcas iniciais entram paradas nas suas bases")
    void cargaInicial() {
        assertEquals(mundo.unidadesIniciais().size(), servico.situacao().size());
        assertTrue(servico.situacao().stream().noneMatch(ServicoOperacoes.Situacao::emMovimento));
        seed.semear();
        assertEquals(mundo.unidadesIniciais().size(), repositorio.count(), "carga repetida nao duplica");
    }

    @Test
    @DisplayName("Ordem coloca a unidade em movimento a partir de onde esta e fica auditada")
    void emitirOrdem() {
        Ponto base = situacaoDe("Brigada Planalto").posicao();

        ServicoOperacoes.Situacao depois = servico.emitirOrdem(jogador.getId(), idDe("Brigada Planalto"), GOIANIA);

        assertTrue(depois.emMovimento());
        assertEquals(GOIANIA, depois.trajeto().destino());
        assertTrue(depois.posicao().distanciaKm(base) < 5);
        assertTrue(auditoria.ultimos(10).stream().anyMatch(e -> e.getAcao().equals("ORDEM_MOVIMENTO")
                && e.getAtor().equals(jogador.getUsuario())));
    }

    @Test
    @DisplayName("A unidade avanca com o relogio e para ao chegar")
    void avancaEChega() {
        Long id = idDe("Brigada Planalto");
        Ponto base = situacaoDe("Brigada Planalto").posicao();
        double duracao = servico.emitirOrdem(jogador.getId(), id, GOIANIA).trajeto().duracaoHoras();

        adiantarPartida(id, duracao / 2);
        ServicoOperacoes.Situacao meio = situacaoDe("Brigada Planalto");
        assertTrue(meio.emMovimento());
        assertTrue(meio.posicao().distanciaKm(base) > 20);
        assertTrue(meio.posicao().distanciaKm(GOIANIA) > 20);

        adiantarPartida(id, duracao + 1);
        ServicoOperacoes.Situacao fim = situacaoDe("Brigada Planalto");
        assertFalse(fim.emMovimento());
        assertEquals(GOIANIA, fim.posicao());
    }

    @Test
    @DisplayName("Nova ordem no meio do caminho parte da posicao atual")
    void novaOrdemParteDaPosicaoAtual() {
        Long id = idDe("Brigada Planalto");
        Ponto base = situacaoDe("Brigada Planalto").posicao();
        double duracao = servico.emitirOrdem(jogador.getId(), id, GOIANIA).trajeto().duracaoHoras();
        adiantarPartida(id, duracao / 2);

        ServicoOperacoes.Situacao volta = servico.emitirOrdem(jogador.getId(), id, base);

        assertTrue(volta.trajeto().marcos().get(0).ponto().distanciaKm(base) > 20);
    }

    @Test
    @DisplayName("Parar congela a unidade no ponto em que ela estava")
    void parar() {
        Long id = idDe("Brigada Planalto");
        double duracao = servico.emitirOrdem(jogador.getId(), id, GOIANIA).trajeto().duracaoHoras();
        adiantarPartida(id, duracao / 2);
        Ponto antes = situacaoDe("Brigada Planalto").posicao();

        ServicoOperacoes.Situacao parada = servico.parar(jogador.getId(), id);

        assertFalse(parada.emMovimento());
        assertTrue(parada.posicao().distanciaKm(antes) < 5);
    }

    @Test
    @DisplayName("Ordem impossivel e recusada e a unidade continua onde estava")
    void ordemImpossivel() {
        Long id = idDe("Brigada Planalto");

        assertThrows(RegraDeNegocioException.class,
                () -> servico.emitirOrdem(jogador.getId(), id, MAR_ABERTO));

        assertFalse(situacaoDe("Brigada Planalto").emMovimento());
    }

    @Test
    @DisplayName("A API entrega a malha civil e a situacao das forcas")
    void apiDeConsulta() throws Exception {
        mockMvc.perform(get("/api/operacoes/malha"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.portos[0].posicao[0]").isNumber())
                .andExpect(jsonPath("$.rotas[0].marcos[0][2]").isNumber())
                .andExpect(jsonPath("$.veiculos[0].rotaId").isString());
        mockMvc.perform(get("/api/operacoes/situacao").param("jogadorId", jogador.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.agora").isNumber())
                .andExpect(jsonPath("$.horasJogoPorSegundo").value(0.2))
                .andExpect(jsonPath("$.podeComandar").value(true))
                .andExpect(jsonPath("$.unidades[0].posicao[1]").isNumber())
                .andExpect(jsonPath("$.unidades[0].emMovimento").value(false));
    }

    @Test
    @DisplayName("A API aceita uma ordem valida e explica a recusa da invalida")
    void apiDeOrdens() throws Exception {
        String corpo = "{\"jogadorId\":%d,\"unidadeId\":%d,\"longitude\":%s,\"latitude\":%s}";

        mockMvc.perform(post("/api/operacoes/ordens").contentType(MediaType.APPLICATION_JSON)
                        .content(corpo.formatted(jogador.getId(), idDe("Esquadrão Anápolis"), "-38.5", "-12.9")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.emMovimento").value(true))
                .andExpect(jsonPath("$.trajeto[1][2]").isNumber());
        mockMvc.perform(post("/api/operacoes/ordens").contentType(MediaType.APPLICATION_JSON)
                        .content(corpo.formatted(jogador.getId(), idDe("Fragata Guanabara"), "-47.9", "-15.8")))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.mensagem").isString());
        mockMvc.perform(post("/api/operacoes/unidades/" + idDe("Esquadrão Anápolis") + "/parar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"jogadorId\":" + jogador.getId() + "}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.emMovimento").value(false));
    }
}
