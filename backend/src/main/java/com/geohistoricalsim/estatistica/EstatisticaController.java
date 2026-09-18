package com.geohistoricalsim.estatistica;

import com.geohistoricalsim.comum.Mapeadores;
import com.geohistoricalsim.core.EstadoJogo;
import com.geohistoricalsim.core.ServicoEstadoJogo;
import com.geohistoricalsim.economia.Setor;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Estatisticas gerais, series historicas e rankings. */
@RestController
@RequestMapping("/api/estatisticas")
public class EstatisticaController {

    private final ServicoEstatistica servico;
    private final ServicoEstadoJogo estadoJogo;

    public EstatisticaController(ServicoEstatistica servico, ServicoEstadoJogo estadoJogo) {
        this.servico = servico;
        this.estadoJogo = estadoJogo;
    }

    /** Painel geral: ultimo fechamento mais o recorte por setor. */
    @GetMapping("/gerais")
    public Map<String, Object> gerais() {
        EstadoJogo estado = estadoJogo.obter();
        SnapshotTurno ultimo = servico.ultimoSnapshot();

        Map<String, Object> resposta = new LinkedHashMap<>();
        resposta.put("estadoJogo", Mapeadores.estadoJogo(estado));
        resposta.put("ultimoFechamento", ultimo == null ? null : Mapeadores.snapshot(ultimo));
        resposta.put("setores", ultimo == null
                ? List.of()
                : Mapeadores.lista(servico.setoresNoTurno(ultimo.getTurno()), Mapeadores::estatisticaSetor));
        return resposta;
    }

    @GetMapping("/serie")
    public List<Map<String, Object>> serie(@RequestParam(defaultValue = "24") int limite) {
        return Mapeadores.lista(servico.serieHistorica(limite), Mapeadores::snapshot);
    }

    @GetMapping("/setores")
    public List<Map<String, Object>> setores(@RequestParam(required = false) Setor setor,
                                             @RequestParam(defaultValue = "24") int limite) {
        if (setor != null) {
            return Mapeadores.lista(servico.serieDoSetor(setor, limite), Mapeadores::estatisticaSetor);
        }
        SnapshotTurno ultimo = servico.ultimoSnapshot();
        if (ultimo == null) {
            return List.of();
        }
        return Mapeadores.lista(servico.setoresNoTurno(ultimo.getTurno()), Mapeadores::estatisticaSetor);
    }

    @GetMapping("/ranking")
    public List<Map<String, Object>> ranking(@RequestParam(defaultValue = "20") int limite) {
        return servico.ranking(limite);
    }
}
