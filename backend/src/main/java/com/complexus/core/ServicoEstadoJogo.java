package com.complexus.core;

import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Acesso ao relogio da partida. Isolado em um servico proprio para que
 * qualquer modulo consulte o turno corrente sem depender do motor de turnos.
 *
 * O turno corrente tambem fica em memoria porque a auditoria grava seus
 * eventos em transacoes proprias: sem o cache, um evento produzido durante o
 * processamento do turno leria do banco o numero do turno anterior, ainda nao
 * substituido pelo commit.
 */
@Service
public class ServicoEstadoJogo {

    private static final int NAO_CARREGADO = -1;

    private final RepositorioEstadoJogo repositorio;
    private final AtomicInteger turnoCorrente = new AtomicInteger(NAO_CARREGADO);

    public ServicoEstadoJogo(RepositorioEstadoJogo repositorio) {
        this.repositorio = repositorio;
    }

    /** Devolve o estado da partida, criando a linha inicial se ainda nao existir. */
    @Transactional
    public EstadoJogo obter() {
        EstadoJogo estado = repositorio.findById(EstadoJogo.ID_UNICO)
                .orElseGet(() -> repositorio.save(new EstadoJogo()));
        turnoCorrente.set(estado.getTurnoAtual());
        return estado;
    }

    /** Turno corrente, servido do cache em memoria quando disponivel. */
    @Transactional(readOnly = true)
    public int turnoAtual() {
        int emMemoria = turnoCorrente.get();
        if (emMemoria != NAO_CARREGADO) {
            return emMemoria;
        }
        int doBanco = repositorio.findById(EstadoJogo.ID_UNICO).map(EstadoJogo::getTurnoAtual).orElse(0);
        turnoCorrente.set(doBanco);
        return doBanco;
    }

    @Transactional
    public EstadoJogo salvar(EstadoJogo estado) {
        turnoCorrente.set(estado.getTurnoAtual());
        return repositorio.save(estado);
    }
}
