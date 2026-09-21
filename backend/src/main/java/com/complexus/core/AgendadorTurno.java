package com.complexus.core;

import com.complexus.config.PropriedadesJogo;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Dispara o turno automatico. A cada hora de tempo real o mundo avanca um mes.
 *
 * O intervalo vem de jogo.turno.duracao-minutos, o que permite acelerar o
 * relogio em ambiente de teste sem tocar no codigo.
 */
@Component
public class AgendadorTurno {

    private static final Logger log = LoggerFactory.getLogger(AgendadorTurno.class);

    private final ServicoTurno servicoTurno;
    private final PropriedadesJogo propriedades;

    public AgendadorTurno(ServicoTurno servicoTurno, PropriedadesJogo propriedades) {
        this.servicoTurno = servicoTurno;
        this.propriedades = propriedades;
    }

    @Scheduled(initialDelayString = "${jogo.turno.duracao-minutos}",
            fixedRateString = "${jogo.turno.duracao-minutos}",
            timeUnit = TimeUnit.MINUTES)
    public void executar() {
        if (!propriedades.getTurno().isProcessamentoAutomatico()) {
            log.debug("Processamento automatico desligado; turno nao avancou.");
            return;
        }
        try {
            servicoTurno.processarTurno("AGENDADOR");
        } catch (Exception ex) {
            log.error("Falha ao processar turno automatico: {}", ex.getMessage(), ex);
        }
    }
}
