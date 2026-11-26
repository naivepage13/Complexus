package com.complexus.operacoes;

import com.complexus.config.PropriedadesJogo;
import java.time.Duration;
import java.time.Instant;
import org.springframework.stereotype.Component;

/**
 * Converte tempo real em tempo de jogo para o deslocamento no mapa, no mesmo
 * ritmo do calendario: um turno vale um mes de 30 dias (720 horas).
 *
 * O movimento segue o relogio de parede, entao continua mesmo com o jogo
 * fechado e nao depende de turno processado.
 */
@Component
public class RelogioOperacoes {

    private static final double HORAS_POR_TURNO = 30 * 24;

    private final PropriedadesJogo propriedades;

    public RelogioOperacoes(PropriedadesJogo propriedades) {
        this.propriedades = propriedades;
    }

    public double horasJogoPorSegundo() {
        return HORAS_POR_TURNO / (Math.max(propriedades.getTurno().getDuracaoMinutos(), 1) * 60.0);
    }

    public double horasDeJogoEntre(Instant inicio, Instant fim) {
        return Duration.between(inicio, fim).toMillis() / 1000.0 * horasJogoPorSegundo();
    }
}
