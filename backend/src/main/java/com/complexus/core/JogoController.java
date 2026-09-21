package com.complexus.core;

import com.complexus.comum.Mapeadores;
import com.complexus.config.PropriedadesJogo;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Estado da partida e controle manual de turnos. */
@RestController
@RequestMapping("/api/jogo")
public class JogoController {

    private final ServicoEstadoJogo estadoJogo;
    private final ServicoTurno servicoTurno;
    private final PropriedadesJogo propriedades;

    public JogoController(ServicoEstadoJogo estadoJogo, ServicoTurno servicoTurno,
                          PropriedadesJogo propriedades) {
        this.estadoJogo = estadoJogo;
        this.servicoTurno = servicoTurno;
        this.propriedades = propriedades;
    }

    @GetMapping("/estado")
    public Map<String, Object> estado() {
        EstadoJogo estado = estadoJogo.obter();
        Map<String, Object> resposta = new LinkedHashMap<>(Mapeadores.estadoJogo(estado));
        resposta.put("duracaoTurnoMinutos", propriedades.getTurno().getDuracaoMinutos());
        resposta.put("processamentoAutomatico", propriedades.getTurno().isProcessamentoAutomatico());
        resposta.put("segundosParaProximoTurno", segundosRestantes(estado));
        return resposta;
    }

    /**
     * Avanca o turno manualmente.
     *
     * Util para testes e para mestres de partida. A origem informada e gravada
     * na linha de auditoria.
     */
    @PostMapping("/turno/avancar")
    public Map<String, Object> avancarTurno(@RequestParam(defaultValue = "MANUAL") String origem) {
        return servicoTurno.processarTurno(origem);
    }

    private long segundosRestantes(EstadoJogo estado) {
        if (estado.getProximoProcessamento() == null) {
            return propriedades.getTurno().getDuracaoMinutos() * 60L;
        }
        long segundos = Duration.between(Instant.now(), estado.getProximoProcessamento()).getSeconds();
        return Math.max(segundos, 0);
    }
}
