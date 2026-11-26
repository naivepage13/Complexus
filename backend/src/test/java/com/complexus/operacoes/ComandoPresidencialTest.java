package com.complexus.operacoes;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.complexus.comum.RegraDeNegocioException;
import com.complexus.jogador.Jogador;
import com.complexus.jogador.ServicoJogador;
import com.complexus.politica.CargoPolitico;
import com.complexus.politica.Mandato;
import com.complexus.politica.RepositorioMandato;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/** Regra opcional: so o Presidente comanda as forcas. */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:comando;DB_CLOSE_DELAY=-1",
        "jogo.turno.processamento-automatico=false",
        "jogo.operacoes.comando-exige-presidencia=true"
})
class ComandoPresidencialTest {

    private static final Ponto GOIANIA = new Ponto(-49.2643, -16.6864);

    @Autowired
    private ServicoOperacoes servico;
    @Autowired
    private ServicoJogador servicoJogador;
    @Autowired
    private RepositorioMandato repositorioMandato;

    @Test
    @DisplayName("Sem a presidencia a ordem e recusada; com ela, aceita")
    void soOPresidenteComanda() {
        Jogador jogador = servicoJogador.registrar("cmd" + UUID.randomUUID().toString().substring(0, 8),
                "Candidato", "senha123");
        Long unidade = servico.situacao().stream()
                .filter(s -> s.unidade().getTipo().ramo() == RamoMilitar.EXERCITO)
                .findFirst().orElseThrow().unidade().getId();

        assertFalse(servico.podeComandar(jogador.getId()));
        assertThrows(RegraDeNegocioException.class,
                () -> servico.emitirOrdem(jogador.getId(), unidade, GOIANIA));
        assertThrows(RegraDeNegocioException.class, () -> servico.parar(jogador.getId(), unidade));

        Mandato presidencia = repositorioMandato.findAll().stream()
                .filter(m -> m.getCargo() == CargoPolitico.PRESIDENTE && m.isAtivo())
                .findFirst().orElseThrow();
        presidencia.setJogador(jogador);
        presidencia.setNpc(false);
        repositorioMandato.save(presidencia);

        assertTrue(servico.podeComandar(jogador.getId()));
        assertTrue(servico.emitirOrdem(jogador.getId(), unidade, GOIANIA).emMovimento());
    }
}
