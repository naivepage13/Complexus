package com.complexus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.complexus.jogador.Jogador;
import com.complexus.jogador.ServicoJogador;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Trava o formato do hash de senha.
 *
 * O sal entra no hash gravado no banco: se alguem troca-lo (por exemplo em uma
 * substituicao de texto durante uma renomeacao do projeto), todas as contas
 * existentes param de conseguir entrar. Este teste falha antes disso chegar ao
 * repositorio.
 */
@SpringBootTest
class CredencialEstavelTest {

    /** SHA-256 de "geohistoricalsim::v1::senha123", o sal em vigor desde a 0.2.0. */
    private static final String HASH_ESPERADO =
            "089bd93d149656626ad73bbaeb36c9a5da115412dc8a315280598359ba115227";

    @Autowired
    private ServicoJogador servicoJogador;

    @Test
    @DisplayName("O hash de senha nao muda entre versoes")
    void hashDeSenhaEstavel() {
        String usuario = "credencial" + UUID.randomUUID().toString().substring(0, 8);

        Jogador jogador = servicoJogador.registrar(usuario, "Teste de credencial", "senha123");

        assertEquals(HASH_ESPERADO, jogador.getSenhaHash(),
                "o sal do hash mudou: contas ja existentes deixariam de entrar");
    }

    @Test
    @DisplayName("Quem registra consegue autenticar em seguida")
    void registroEAutenticacao() {
        String usuario = "login" + UUID.randomUUID().toString().substring(0, 8);
        servicoJogador.registrar(usuario, "Teste de login", "outra-senha");

        Jogador autenticado = servicoJogador.autenticar(usuario, "outra-senha");

        assertNotNull(autenticado.getId());
        assertEquals(usuario, autenticado.getUsuario());
    }
}
