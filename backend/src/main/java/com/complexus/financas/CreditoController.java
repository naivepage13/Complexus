package com.complexus.financas;

import com.complexus.comum.Mapeadores;
import jakarta.validation.constraints.NotNull;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Financas da empresa: avaliacao de credito, contratacao e gestao da divida.
 */
@RestController
@RequestMapping("/api/empresas/{empresaId}/financas")
public class CreditoController {

    public record ContratacaoRequest(@NotNull Long jogadorId,
                                     @NotNull Financiamento.Modalidade modalidade,
                                     double valor, int prazoTurnos) {
    }

    public record AmortizacaoRequest(@NotNull Long jogadorId, double valor) {
    }

    public record RenegociacaoRequest(@NotNull Long jogadorId, int novoPrazo) {
    }

    private final ServicoCredito servico;

    public CreditoController(ServicoCredito servico) {
        this.servico = servico;
    }

    /** Vitrine de credito: nota, limites e taxa de cada linha para esta empresa. */
    @GetMapping
    public Map<String, Object> financas(@PathVariable Long empresaId) {
        Map<String, Object> resposta = new LinkedHashMap<>(servico.vitrine(empresaId));
        resposta.put("contratos",
                Mapeadores.lista(servico.emAberto(empresaId), Mapeadores::financiamento));
        resposta.put("historico",
                Mapeadores.lista(servico.historico(empresaId), Mapeadores::financiamento));
        return resposta;
    }

    @GetMapping("/contratos")
    public List<Map<String, Object>> contratos(@PathVariable Long empresaId) {
        return Mapeadores.lista(servico.historico(empresaId), Mapeadores::financiamento);
    }

    @PostMapping("/contratos")
    public Map<String, Object> contratar(@PathVariable Long empresaId,
                                         @RequestBody ContratacaoRequest requisicao) {
        return Mapeadores.financiamento(servico.contratar(empresaId, requisicao.jogadorId(),
                requisicao.modalidade(), requisicao.valor(), requisicao.prazoTurnos()));
    }

    @PostMapping("/contratos/{contratoId}/amortizar")
    public Map<String, Object> amortizar(@PathVariable Long empresaId,
                                         @PathVariable Long contratoId,
                                         @RequestBody AmortizacaoRequest requisicao) {
        return Mapeadores.financiamento(servico.amortizar(contratoId, requisicao.jogadorId(),
                requisicao.valor()));
    }

    @PostMapping("/contratos/{contratoId}/renegociar")
    public Map<String, Object> renegociar(@PathVariable Long empresaId,
                                          @PathVariable Long contratoId,
                                          @RequestBody RenegociacaoRequest requisicao) {
        return Mapeadores.financiamento(servico.renegociar(contratoId, requisicao.jogadorId(),
                requisicao.novoPrazo()));
    }
}
