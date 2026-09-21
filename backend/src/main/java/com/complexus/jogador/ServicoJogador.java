package com.complexus.jogador;

import com.complexus.auditoria.ServicoAuditoria;
import com.complexus.comum.RecursoNaoEncontradoException;
import com.complexus.comum.RegraDeNegocioException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Cadastro e identificacao de jogadores.
 *
 * Limitacao conhecida da versao 0.2: a senha e guardada como SHA-256 com sal
 * fixo e nao ha emissao de token. A troca por BCrypt e Spring Security esta
 * registrada no roadmap (RNF-01).
 */
@Service
public class ServicoJogador {

    /**
     * Sal do hash de senha.
     *
     * NAO renomear: o valor entra no hash gravado no banco, entao troca-lo
     * invalida a senha de todas as contas existentes. Ele nasceu com o nome
     * antigo do projeto e continua assim de proposito. Se um dia precisar
     * mudar, sera com migracao de credenciais, nao com substituicao de texto.
     */
    private static final String SAL = "geohistoricalsim::v1::";
    private static final double SALDO_INICIAL = 5_000_000.0;

    private final RepositorioJogador repositorio;
    private final ServicoAuditoria auditoria;

    public ServicoJogador(RepositorioJogador repositorio, ServicoAuditoria auditoria) {
        this.repositorio = repositorio;
        this.auditoria = auditoria;
    }

    @Transactional
    public Jogador registrar(String usuario, String nome, String senha) {
        String login = usuario == null ? "" : usuario.trim().toLowerCase();
        if (login.length() < 3) {
            throw new RegraDeNegocioException("Usuario precisa de ao menos 3 caracteres.");
        }
        if (senha == null || senha.length() < 4) {
            throw new RegraDeNegocioException("Senha precisa de ao menos 4 caracteres.");
        }
        if (repositorio.existsByUsuario(login)) {
            throw new RegraDeNegocioException("Usuario ja cadastrado: " + login);
        }
        Jogador jogador = new Jogador();
        jogador.setUsuario(login);
        jogador.setNome(nome == null || nome.isBlank() ? login : nome.trim());
        jogador.setSenhaHash(hash(senha));
        jogador.setSaldo(SALDO_INICIAL);
        Jogador salvo = repositorio.save(jogador);

        auditoria.registrar(login, "JOGADOR_REGISTRADO", "Jogador", salvo.getId(),
                "Novo jogador cadastrado", Map.of("saldoInicial", SALDO_INICIAL));
        return salvo;
    }

    @Transactional(readOnly = true)
    public Jogador autenticar(String usuario, String senha) {
        String login = usuario == null ? "" : usuario.trim().toLowerCase();
        Jogador jogador = repositorio.findByUsuario(login)
                .orElseThrow(() -> new RegraDeNegocioException("Usuario ou senha invalidos."));
        if (!jogador.getSenhaHash().equals(hash(senha)) || !jogador.isAtivo()) {
            throw new RegraDeNegocioException("Usuario ou senha invalidos.");
        }
        return jogador;
    }

    @Transactional(readOnly = true)
    public Jogador buscar(Long id) {
        return repositorio.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Jogador", id));
    }

    @Transactional(readOnly = true)
    public List<Jogador> listar() {
        return repositorio.findAll();
    }

    @Transactional
    public Jogador salvar(Jogador jogador) {
        return repositorio.save(jogador);
    }

    /** Debita o caixa pessoal do jogador, recusando saldo insuficiente. */
    @Transactional
    public void debitar(Jogador jogador, double valor, String motivo) {
        if (valor < 0) {
            throw new RegraDeNegocioException("Valor de debito nao pode ser negativo.");
        }
        if (jogador.getSaldo() < valor) {
            throw new RegraDeNegocioException("Saldo insuficiente para " + motivo
                    + ". Disponivel: " + String.format("%.2f", jogador.getSaldo()));
        }
        jogador.setSaldo(jogador.getSaldo() - valor);
        repositorio.save(jogador);
    }

    @Transactional
    public void creditar(Jogador jogador, double valor) {
        jogador.setSaldo(jogador.getSaldo() + Math.max(valor, 0));
        repositorio.save(jogador);
    }

    private String hash(String senha) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest((SAL + senha).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(bytes);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 indisponivel na JVM", ex);
        }
    }
}
