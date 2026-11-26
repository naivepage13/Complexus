package com.complexus.operacoes;

import com.complexus.auditoria.ServicoAuditoria;
import com.complexus.comum.RecursoNaoEncontradoException;
import com.complexus.comum.RegraDeNegocioException;
import com.complexus.config.PropriedadesJogo;
import com.complexus.jogador.Jogador;
import com.complexus.jogador.ServicoJogador;
import com.complexus.politica.CargoPolitico;
import com.complexus.politica.RepositorioMandato;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Ordens de movimento das forcas militares e a situacao delas no mapa. */
@Service
public class ServicoOperacoes {

    /**
     * Retrato de uma unidade em um instante.
     *
     * @param trajeto nulo quando a unidade esta parada
     * @param horasDecorridas horas de jogo desde a partida (zero se parada)
     */
    public record Situacao(UnidadeMilitar unidade, Ponto posicao, Trajeto trajeto, double horasDecorridas) {
        public boolean emMovimento() {
            return trajeto != null;
        }
    }

    private final RepositorioUnidadeMilitar repositorio;
    private final CartografiaMundo cartografia;
    private final RelogioOperacoes relogio;
    private final ServicoJogador servicoJogador;
    private final RepositorioMandato repositorioMandato;
    private final ServicoAuditoria auditoria;
    private final PropriedadesJogo propriedades;
    private final ObjectMapper json;

    public ServicoOperacoes(RepositorioUnidadeMilitar repositorio, CartografiaMundo cartografia,
                            RelogioOperacoes relogio, ServicoJogador servicoJogador,
                            RepositorioMandato repositorioMandato, ServicoAuditoria auditoria,
                            PropriedadesJogo propriedades, ObjectMapper json) {
        this.repositorio = repositorio;
        this.cartografia = cartografia;
        this.relogio = relogio;
        this.servicoJogador = servicoJogador;
        this.repositorioMandato = repositorioMandato;
        this.auditoria = auditoria;
        this.propriedades = propriedades;
        this.json = json;
    }

    @Transactional(readOnly = true)
    public List<Situacao> situacao() {
        Instant agora = Instant.now();
        return repositorio.findAll().stream()
                .sorted(Comparator.comparing(UnidadeMilitar::getId))
                .map(unidade -> situacaoEm(unidade, agora))
                .toList();
    }

    @Transactional(readOnly = true)
    public boolean podeComandar(Long jogadorId) {
        if (!propriedades.getOperacoes().isComandoExigePresidencia()) {
            return true;
        }
        return repositorioMandato.findByJogadorIdAndAtivoTrue(jogadorId).stream()
                .anyMatch(mandato -> mandato.getCargo() == CargoPolitico.PRESIDENTE);
    }

    /** Manda a unidade a um ponto, partindo de onde ela estiver agora. */
    @Transactional
    public Situacao emitirOrdem(Long jogadorId, Long unidadeId, Ponto destino) {
        Jogador comandante = exigirComandante(jogadorId);
        UnidadeMilitar unidade = buscar(unidadeId);
        Instant agora = Instant.now();
        Ponto partida = situacaoEm(unidade, agora).posicao();

        Trajeto trajeto = cartografia.planejador().planejar(unidade.getTipo(), partida, destino);
        if (trajeto.duracaoHoras() <= 0) {
            throw new RegraDeNegocioException("A unidade ja esta nesse ponto.");
        }
        unidade.setPonto(partida);
        unidade.setTrajeto(gravar(trajeto));
        unidade.setPartidaEm(agora);
        repositorio.save(unidade);

        Map<String, Object> detalhes = new LinkedHashMap<>();
        detalhes.put("destinoLongitude", destino.lon());
        detalhes.put("destinoLatitude", destino.lat());
        detalhes.put("horasDeJogo", trajeto.duracaoHoras());
        auditoria.registrar(comandante.getUsuario(), "ORDEM_MOVIMENTO", "UnidadeMilitar", unidade.getId(),
                unidade.getNome() + " recebeu ordem de movimento", detalhes);
        return new Situacao(unidade, partida, trajeto, 0);
    }

    /** Cancela o deslocamento: a unidade fica onde estiver. */
    @Transactional
    public Situacao parar(Long jogadorId, Long unidadeId) {
        Jogador comandante = exigirComandante(jogadorId);
        UnidadeMilitar unidade = buscar(unidadeId);
        Situacao atual = situacaoEm(unidade, Instant.now());
        if (!atual.emMovimento()) {
            throw new RegraDeNegocioException("A unidade ja esta parada.");
        }
        unidade.setPonto(atual.posicao());
        unidade.setTrajeto(null);
        unidade.setPartidaEm(null);
        repositorio.save(unidade);
        auditoria.registrar(comandante.getUsuario(), "ORDEM_CANCELADA", "UnidadeMilitar", unidade.getId(),
                unidade.getNome() + " interrompeu o deslocamento");
        return new Situacao(unidade, atual.posicao(), null, 0);
    }

    private Jogador exigirComandante(Long jogadorId) {
        Jogador jogador = servicoJogador.buscar(jogadorId);
        if (!podeComandar(jogadorId)) {
            throw new RegraDeNegocioException("Somente o Presidente da Republica comanda as forcas.");
        }
        return jogador;
    }

    private UnidadeMilitar buscar(Long id) {
        return repositorio.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Unidade militar", id));
    }

    private Situacao situacaoEm(UnidadeMilitar unidade, Instant agora) {
        if (unidade.getTrajeto() == null) {
            return new Situacao(unidade, unidade.getPonto(), null, 0);
        }
        Trajeto trajeto = ler(unidade.getTrajeto());
        double horas = relogio.horasDeJogoEntre(unidade.getPartidaEm(), agora);
        if (horas >= trajeto.duracaoHoras()) {
            return new Situacao(unidade, trajeto.destino(), null, 0);
        }
        return new Situacao(unidade, trajeto.posicaoEm(horas), trajeto, horas);
    }

    private String gravar(Trajeto trajeto) {
        List<double[]> marcos = trajeto.marcos().stream()
                .map(m -> new double[] {m.ponto().lon(), m.ponto().lat(), m.horas()})
                .toList();
        try {
            return json.writeValueAsString(marcos);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Falha ao gravar o trajeto.", ex);
        }
    }

    private Trajeto ler(String gravado) {
        try {
            List<Trajeto.Marco> marcos = new ArrayList<>();
            for (double[] m : json.readValue(gravado, double[][].class)) {
                marcos.add(new Trajeto.Marco(new Ponto(m[0], m[1]), m[2]));
            }
            return Trajeto.deMarcos(marcos);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Trajeto gravado ilegivel.", ex);
        }
    }
}
