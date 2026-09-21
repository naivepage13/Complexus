package com.complexus.auditoria;

import com.complexus.core.ServicoEstadoJogo;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Linha de auditoria do sistema.
 *
 * Cada fato relevante vira um {@link EventoAuditoria} encadeado por hash
 * SHA-256 (hash = SHA256(hashAnterior + campos canonicos)). A cadeia permite
 * demonstrar que o historico nao foi adulterado depois do registro.
 */
@Service
public class ServicoAuditoria {

    public static final String ATOR_SISTEMA = "SISTEMA";
    private static final String HASH_GENESE = "0000000000000000000000000000000000000000000000000000000000000000";

    private final RepositorioAuditoria repositorio;
    private final ServicoEstadoJogo estadoJogo;

    public ServicoAuditoria(RepositorioAuditoria repositorio, ServicoEstadoJogo estadoJogo) {
        this.repositorio = repositorio;
        this.estadoJogo = estadoJogo;
    }

    /**
     * Registra um fato.
     *
     * Cada entrada publica abre a sua propria transacao e e sincronizada: o
     * evento e gravado e confirmado antes que o proximo leia o ultimo hash,
     * o que mantem a cadeia sempre encadeada na ordem dos identificadores.
     * Por isso os atalhos abaixo repetem a anotacao em vez de chamar este
     * metodo diretamente - uma chamada interna nao passaria pelo proxy
     * transacional do Spring.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public synchronized EventoAuditoria registrar(String ator, String acao, String entidade,
                                                  Long entidadeId, String descricao,
                                                  Map<String, ?> detalhes) {
        return gravar(ator, acao, entidade, entidadeId, descricao, detalhes);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public synchronized EventoAuditoria registrar(String ator, String acao, String entidade,
                                                  Long entidadeId, String descricao) {
        return gravar(ator, acao, entidade, entidadeId, descricao, Map.of());
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public synchronized EventoAuditoria registrarSistema(String acao, String entidade, Long entidadeId,
                                                         String descricao, Map<String, ?> detalhes) {
        return gravar(ATOR_SISTEMA, acao, entidade, entidadeId, descricao, detalhes);
    }

    private EventoAuditoria gravar(String ator, String acao, String entidade,
                                   Long entidadeId, String descricao,
                                   Map<String, ?> detalhes) {
        EventoAuditoria evento = new EventoAuditoria();
        // O banco grava o instante com precisao de microssegundos. Truncar antes
        // de calcular o hash garante que a verificacao posterior encontre
        // exatamente o mesmo valor que foi assinado.
        evento.setMomento(Instant.now().truncatedTo(ChronoUnit.MILLIS));
        evento.setAtor(ator == null || ator.isBlank() ? ATOR_SISTEMA : ator);
        evento.setAcao(acao);
        evento.setEntidade(entidade);
        evento.setEntidadeId(entidadeId);
        evento.setDescricao(descricao);
        evento.setDetalhes(serializar(detalhes));
        evento.setTurno(estadoJogo.turnoAtual());

        String anterior = repositorio.findFirstByOrderByIdDesc()
                .map(EventoAuditoria::getHash)
                .orElse(HASH_GENESE);
        evento.setHashAnterior(anterior);
        evento.setHash(calcularHash(evento));
        return repositorio.save(evento);
    }

    @Transactional(readOnly = true)
    public List<EventoAuditoria> ultimos(int limite) {
        int tamanho = Math.min(Math.max(limite, 1), 500);
        return repositorio.findAllByOrderByIdDesc(PageRequest.of(0, tamanho));
    }

    @Transactional(readOnly = true)
    public List<EventoAuditoria> porEntidade(String entidade, Long entidadeId) {
        return repositorio.findByEntidadeAndEntidadeIdOrderByIdDesc(entidade, entidadeId);
    }

    @Transactional(readOnly = true)
    public List<EventoAuditoria> porTurno(int turno) {
        return repositorio.findByTurnoOrderByIdAsc(turno);
    }

    /** Recalcula a cadeia inteira e aponta os elos divergentes, se houver. */
    @Transactional(readOnly = true)
    public Map<String, Object> verificarIntegridade() {
        List<EventoAuditoria> eventos = repositorio.findAll(Sort.by("id").ascending());
        String anterior = HASH_GENESE;
        List<Map<String, Object>> falhas = new ArrayList<>();
        for (EventoAuditoria evento : eventos) {
            String esperado = calcularHash(evento);
            boolean elo = anterior.equals(evento.getHashAnterior());
            boolean integro = esperado.equals(evento.getHash());
            if (!elo || !integro) {
                Map<String, Object> falha = new LinkedHashMap<>();
                falha.put("id", evento.getId());
                falha.put("acao", evento.getAcao());
                falha.put("eloValido", elo);
                falha.put("hashValido", integro);
                falhas.add(falha);
            }
            anterior = evento.getHash();
        }
        Map<String, Object> resultado = new LinkedHashMap<>();
        resultado.put("totalEventos", eventos.size());
        resultado.put("cadeiaIntegra", falhas.isEmpty());
        resultado.put("falhas", falhas);
        resultado.put("ultimoHash", anterior);
        return resultado;
    }

    private String calcularHash(EventoAuditoria e) {
        String canonico = String.join("|",
                String.valueOf(e.getHashAnterior()),
                String.valueOf(e.getMomento()),
                String.valueOf(e.getTurno()),
                String.valueOf(e.getAtor()),
                String.valueOf(e.getAcao()),
                String.valueOf(e.getEntidade()),
                String.valueOf(e.getEntidadeId()),
                String.valueOf(e.getDescricao()),
                String.valueOf(e.getDetalhes()));
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(canonico.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 indisponivel na JVM", ex);
        }
    }

    /** Serializacao JSON minima e estavel (ordem de insercao preservada). */
    private String serializar(Map<String, ?> detalhes) {
        if (detalhes == null || detalhes.isEmpty()) {
            return "{}";
        }
        StringBuilder sb = new StringBuilder("{");
        boolean primeiro = true;
        for (Map.Entry<String, ?> entrada : detalhes.entrySet()) {
            if (!primeiro) {
                sb.append(',');
            }
            primeiro = false;
            sb.append('"').append(escapar(entrada.getKey())).append("\":");
            Object valor = entrada.getValue();
            if (valor == null) {
                sb.append("null");
            } else if (valor instanceof Number || valor instanceof Boolean) {
                sb.append(valor);
            } else {
                sb.append('"').append(escapar(String.valueOf(valor))).append('"');
            }
        }
        return sb.append('}').toString();
    }

    private String escapar(String valor) {
        return valor.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", " ");
    }
}
