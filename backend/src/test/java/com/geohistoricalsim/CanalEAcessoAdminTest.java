package com.geohistoricalsim;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.geohistoricalsim.atualizacoes.ServicoAtualizacoes;
import com.geohistoricalsim.economia.ServicoEmpresa;
import com.geohistoricalsim.economia.Setor;
import com.geohistoricalsim.jogador.Jogador;
import com.geohistoricalsim.jogador.ServicoJogador;
import com.geohistoricalsim.politica.Municipio;
import com.geohistoricalsim.politica.ServicoPolitica;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * Garante as duas regras de exposicao de informacao:
 *
 * <ul>
 *   <li>o jogador so enxerga o canal de atualizacoes;</li>
 *   <li>a linha de auditoria exige credencial administrativa.</li>
 * </ul>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "jogo.admin.token=token-de-teste")
class CanalEAcessoAdminTest {

    @Autowired
    private TestRestTemplate rest;
    @Autowired
    private ServicoAtualizacoes servicoAtualizacoes;
    @Autowired
    private ServicoEmpresa servicoEmpresa;
    @Autowired
    private ServicoJogador servicoJogador;
    @Autowired
    private ServicoPolitica servicoPolitica;

    @Test
    @DisplayName("Auditoria sem credencial responde 403")
    void auditoriaExigeCredencial() {
        ResponseEntity<String> resposta = rest.getForEntity("/api/admin/auditoria/eventos", String.class);
        assertEquals(HttpStatus.FORBIDDEN, resposta.getStatusCode());
    }

    @Test
    @DisplayName("Auditoria com credencial correta responde normalmente")
    void auditoriaComCredencial() {
        HttpHeaders cabecalhos = new HttpHeaders();
        cabecalhos.set("X-Admin-Token", "token-de-teste");

        ResponseEntity<String> resposta = rest.exchange("/api/admin/auditoria/integridade",
                HttpMethod.GET, new HttpEntity<>(cabecalhos), String.class);

        assertEquals(HttpStatus.OK, resposta.getStatusCode());
        assertTrue(resposta.getBody().contains("cadeiaIntegra"));
    }

    @Test
    @DisplayName("Credencial invalida tambem responde 403")
    void credencialInvalida() {
        HttpHeaders cabecalhos = new HttpHeaders();
        cabecalhos.set("X-Admin-Token", "chute");

        ResponseEntity<String> resposta = rest.exchange("/api/admin/auditoria/eventos",
                HttpMethod.GET, new HttpEntity<>(cabecalhos), String.class);

        assertEquals(HttpStatus.FORBIDDEN, resposta.getStatusCode());
    }

    @Test
    @DisplayName("O canal de atualizacoes nao expoe hash nem ator do evento")
    void canalNaoExpoeDadoInterno() {
        List<Map<String, Object>> feed = servicoAtualizacoes.feed(null, 20);

        assertFalse(feed.isEmpty(), "a carga inicial ja produz pelo menos um fato publico");
        for (Map<String, Object> item : feed) {
            assertFalse(item.containsKey("hash"));
            assertFalse(item.containsKey("hashAnterior"));
            assertFalse(item.containsKey("ator"));
            assertFalse(item.containsKey("detalhes"));
            assertTrue(item.containsKey("titulo"));
            assertTrue(item.containsKey("categoria"));
        }
    }

    @Test
    @DisplayName("Movimento privado so aparece no canal do proprio jogador")
    void movimentoPrivadoFicaComODono() {
        Jogador dono = servicoJogador.registrar("dono" + sufixo(), "Dono", "senha123");
        Jogador outro = servicoJogador.registrar("outro" + sufixo(), "Outro", "senha123");
        Municipio municipio = servicoPolitica.listarMunicipios().get(0);

        var empresa = servicoEmpresa.fundar(dono.getId(), "Cantina " + sufixo(),
                Setor.ALIMENTICIO, municipio.getId(), 400_000, 5);
        servicoEmpresa.investirCapital(empresa.getId(), dono.getId(), 50_000);

        boolean donoVeOAporte = servicoAtualizacoes.feed(dono.getId(), 50).stream()
                .anyMatch(item -> "Aporte de capital".equals(item.get("titulo")));
        boolean outroVeOAporte = servicoAtualizacoes.feed(outro.getId(), 50).stream()
                .anyMatch(item -> "Aporte de capital".equals(item.get("titulo")));
        boolean todosVeemAFundacao = servicoAtualizacoes.feed(outro.getId(), 50).stream()
                .anyMatch(item -> "Nova empresa no mercado".equals(item.get("titulo")));

        assertTrue(donoVeOAporte, "o dono acompanha as proprias movimentacoes");
        assertFalse(outroVeOAporte, "aporte de capital e movimento privado");
        assertTrue(todosVeemAFundacao, "a fundacao de uma empresa e fato publico");
    }

    private String sufixo() {
        return UUID.randomUUID().toString().substring(0, 8);
    }
}
