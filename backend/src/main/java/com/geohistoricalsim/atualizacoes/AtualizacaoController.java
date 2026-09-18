package com.geohistoricalsim.atualizacoes;

import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Canal de atualizacoes do jogo.
 *
 * E o unico feed exposto ao jogador. A linha de auditoria completa vive em
 * /api/admin/auditoria e exige credencial administrativa.
 */
@RestController
@RequestMapping("/api/atualizacoes")
public class AtualizacaoController {

    private final ServicoAtualizacoes servico;

    public AtualizacaoController(ServicoAtualizacoes servico) {
        this.servico = servico;
    }

    @GetMapping
    public List<Map<String, Object>> listar(@RequestParam(required = false) Long jogadorId,
                                            @RequestParam(defaultValue = "30") int limite) {
        return servico.feed(jogadorId, limite);
    }
}
