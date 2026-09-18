package com.geohistoricalsim.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Protege as rotas administrativas (/api/admin/**).
 *
 * A linha de auditoria nao e material de jogador: ela expoe atores, hashes e
 * detalhes internos de todas as partidas. Este filtro exige o cabecalho
 * X-Admin-Token para qualquer rota administrativa, de modo que esconder o item
 * do menu nao seja a unica barreira.
 */
@Component
public class FiltroAdmin extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(FiltroAdmin.class);
    private static final String PREFIXO = "/api/admin/";
    private static final String CABECALHO = "X-Admin-Token";

    private final PropriedadesJogo propriedades;

    public FiltroAdmin(PropriedadesJogo propriedades) {
        this.propriedades = propriedades;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest requisicao) {
        return !requisicao.getRequestURI().startsWith(PREFIXO);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest requisicao, HttpServletResponse resposta,
                                    FilterChain cadeia) throws ServletException, IOException {
        String informado = requisicao.getHeader(CABECALHO);
        String esperado = propriedades.getAdmin().getToken();

        if (esperado == null || esperado.isBlank()) {
            negar(resposta, "Acesso administrativo desabilitado: nenhum token configurado.");
            return;
        }
        if (informado == null || !esperado.equals(informado)) {
            log.warn("Acesso administrativo negado em {}", requisicao.getRequestURI());
            negar(resposta, "Credencial administrativa ausente ou invalida.");
            return;
        }
        cadeia.doFilter(requisicao, resposta);
    }

    private void negar(HttpServletResponse resposta, String mensagem) throws IOException {
        resposta.setStatus(HttpStatus.FORBIDDEN.value());
        resposta.setContentType(MediaType.APPLICATION_JSON_VALUE);
        resposta.setCharacterEncoding("UTF-8");
        resposta.getWriter().write(String.format(
                "{\"momento\":\"%s\",\"status\":403,\"erro\":\"Forbidden\",\"mensagem\":\"%s\"}",
                Instant.now(), mensagem));
    }
}
