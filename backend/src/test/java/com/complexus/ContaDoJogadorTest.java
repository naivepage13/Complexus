package com.complexus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.complexus.comum.RegraDeNegocioException;
import com.complexus.jogador.Jogador;
import com.complexus.jogador.ServicoJogador;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/** Cobre a pagina de conta: redefinir senha e alterar e-mail. */
@SpringBootTest
class ContaDoJogadorTest {

    @Autowired
    private ServicoJogador servicoJogador;

    @Test
    @DisplayName("Redefinir senha exige a senha atual correta e aceita login com a nova")
    void redefinirSenha() {
        String usuario = "conta" + UUID.randomUUID().toString().substring(0, 8);
        Jogador jogador = servicoJogador.registrar(usuario, "Teste de conta", "senha-antiga");

        assertThrows(RegraDeNegocioException.class,
                () -> servicoJogador.alterarSenha(jogador.getId(), "senha-errada", "senha-nova"));

        servicoJogador.alterarSenha(jogador.getId(), "senha-antiga", "senha-nova");

        Jogador autenticado = servicoJogador.autenticar(usuario, "senha-nova");
        assertEquals(jogador.getId(), autenticado.getId());
    }

    @Test
    @DisplayName("Alterar e-mail recusa formato invalido e grava o valor valido")
    void alterarEmail() {
        String usuario = "conta" + UUID.randomUUID().toString().substring(0, 8);
        Jogador jogador = servicoJogador.registrar(usuario, "Teste de conta", "senha123");

        assertThrows(RegraDeNegocioException.class,
                () -> servicoJogador.alterarEmail(jogador.getId(), "nao-e-email"));

        Jogador salvo = servicoJogador.alterarEmail(jogador.getId(), "jogador@example.com");
        assertEquals("jogador@example.com", salvo.getEmail());
    }

    @Test
    @DisplayName("Excluir conta recusa sem senha nem confirmacao, e desativa o login depois")
    void excluirContaComSenha() {
        String usuario = "conta" + UUID.randomUUID().toString().substring(0, 8);
        Jogador jogador = servicoJogador.registrar(usuario, "Teste de conta", "senha123");

        assertThrows(RegraDeNegocioException.class,
                () -> servicoJogador.excluirConta(jogador.getId(), "senha-errada", null));

        servicoJogador.excluirConta(jogador.getId(), "senha123", null);

        assertThrows(RegraDeNegocioException.class,
                () -> servicoJogador.autenticar(usuario, "senha123"));
    }

    @Test
    @DisplayName("Excluir conta aceita a palavra EXCLUIR como alternativa a senha")
    void excluirContaComPalavraChave() {
        String usuario = "conta" + UUID.randomUUID().toString().substring(0, 8);
        Jogador jogador = servicoJogador.registrar(usuario, "Teste de conta", "senha123");

        servicoJogador.excluirConta(jogador.getId(), null, "excluir");

        assertThrows(RegraDeNegocioException.class,
                () -> servicoJogador.autenticar(usuario, "senha123"));
    }
}
