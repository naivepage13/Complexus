package com.geohistoricalsim.auditoria;

import com.geohistoricalsim.comum.Mapeadores;
import com.geohistoricalsim.investimento.ServicoRazao;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Consulta da linha de auditoria e do livro-razao.
 *
 * A verificacao de integridade recalcula a cadeia de hashes e aponta qualquer
 * evento alterado depois do registro.
 */
@RestController
@RequestMapping("/api/auditoria")
public class AuditoriaController {

    private final ServicoAuditoria servico;
    private final ServicoRazao razao;

    public AuditoriaController(ServicoAuditoria servico, ServicoRazao razao) {
        this.servico = servico;
        this.razao = razao;
    }

    @GetMapping("/eventos")
    public List<Map<String, Object>> eventos(@RequestParam(defaultValue = "100") int limite,
                                             @RequestParam(required = false) Integer turno,
                                             @RequestParam(required = false) String entidade,
                                             @RequestParam(required = false) Long entidadeId) {
        if (entidade != null && entidadeId != null) {
            return Mapeadores.lista(servico.porEntidade(entidade, entidadeId), Mapeadores::evento);
        }
        if (turno != null) {
            return Mapeadores.lista(servico.porTurno(turno), Mapeadores::evento);
        }
        return Mapeadores.lista(servico.ultimos(limite), Mapeadores::evento);
    }

    @GetMapping("/integridade")
    public Map<String, Object> integridade() {
        return servico.verificarIntegridade();
    }

    @GetMapping("/razao")
    public List<Map<String, Object>> razao(@RequestParam(defaultValue = "100") int limite,
                                           @RequestParam(required = false) Long empresaId,
                                           @RequestParam(required = false) Long jogadorId,
                                           @RequestParam(required = false) Integer turno) {
        if (empresaId != null) {
            return Mapeadores.lista(razao.porEmpresa(empresaId, limite), Mapeadores::lancamento);
        }
        if (jogadorId != null) {
            return Mapeadores.lista(razao.porJogador(jogadorId, limite), Mapeadores::lancamento);
        }
        if (turno != null) {
            return Mapeadores.lista(razao.porTurno(turno), Mapeadores::lancamento);
        }
        return Mapeadores.lista(razao.ultimos(limite), Mapeadores::lancamento);
    }
}
