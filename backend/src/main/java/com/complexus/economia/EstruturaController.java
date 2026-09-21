package com.complexus.economia;

import com.complexus.comum.Mapeadores;
import jakarta.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Estrutura interna da empresa: onde ela opera, o que vende e como se organiza.
 *
 * As rotas ficam sob a empresa porque estrutura nao existe sozinha - toda
 * unidade, linha e departamento pertence a uma companhia e so o dono administra.
 */
@RestController
@RequestMapping("/api/empresas/{empresaId}/estrutura")
public class EstruturaController {

    public record UnidadeRequest(@NotNull Long jogadorId, @NotNull Long municipioId, String nome,
                                 double capital, int funcionarios) {
    }

    public record TransferenciaRequest(@NotNull Long jogadorId, @NotNull Long origemId,
                                       @NotNull Long destinoId, double valor, int quantidade) {
    }

    public record LinhaRequest(@NotNull Long jogadorId, String nome,
                               LinhaProduto.Posicionamento posicionamento, Double fatiaMix) {
    }

    public record DepartamentoRequest(@NotNull Long jogadorId, @NotNull Departamento.Area area,
                                      double orcamentoMensal) {
    }

    private final ServicoEstrutura servico;

    public EstruturaController(ServicoEstrutura servico) {
        this.servico = servico;
    }

    /** Catalogo de posicionamentos e areas, para a interface montar os formularios. */
    @GetMapping("/catalogo")
    public Map<String, Object> catalogo() {
        List<Map<String, Object>> posicionamentos = new ArrayList<>();
        for (LinhaProduto.Posicionamento posicionamento : LinhaProduto.Posicionamento.values()) {
            Map<String, Object> mapa = new LinkedHashMap<>();
            mapa.put("nome", posicionamento.name());
            mapa.put("rotulo", posicionamento.getRotulo());
            mapa.put("fatorPreco", posicionamento.getFatorPreco());
            mapa.put("fatorCusto", posicionamento.getFatorCusto());
            posicionamentos.add(mapa);
        }
        List<Map<String, Object>> areas = new ArrayList<>();
        for (Departamento.Area area : Departamento.Area.values()) {
            Map<String, Object> mapa = new LinkedHashMap<>();
            mapa.put("nome", area.name());
            mapa.put("rotulo", area.getRotulo());
            mapa.put("unidadeEfeito", area.getUnidadeEfeito());
            mapa.put("efeitoMaximo", area.getEfeitoMaximo());
            areas.add(mapa);
        }
        Map<String, Object> catalogo = new LinkedHashMap<>();
        catalogo.put("posicionamentos", posicionamentos);
        catalogo.put("areas", areas);
        return catalogo;
    }

    @GetMapping
    public Map<String, Object> estrutura(@PathVariable Long empresaId) {
        Map<String, Object> resposta = new LinkedHashMap<>();
        resposta.put("unidades", Mapeadores.lista(servico.unidades(empresaId), Mapeadores::unidade));
        resposta.put("linhas", Mapeadores.lista(servico.linhas(empresaId), Mapeadores::linhaProduto));
        resposta.put("departamentos",
                Mapeadores.lista(servico.departamentos(empresaId), Mapeadores::departamento));
        return resposta;
    }

    // ----- Unidades -----

    @GetMapping("/unidades")
    public List<Map<String, Object>> unidades(@PathVariable Long empresaId) {
        return Mapeadores.lista(servico.unidades(empresaId), Mapeadores::unidade);
    }

    @PostMapping("/unidades")
    public Map<String, Object> abrirUnidade(@PathVariable Long empresaId,
                                            @RequestBody UnidadeRequest requisicao) {
        return Mapeadores.unidade(servico.abrirUnidade(empresaId, requisicao.jogadorId(),
                requisicao.municipioId(), requisicao.nome(), requisicao.capital(),
                requisicao.funcionarios()));
    }

    @DeleteMapping("/unidades/{unidadeId}")
    public Map<String, Object> fecharUnidade(@PathVariable Long empresaId,
                                             @PathVariable Long unidadeId,
                                             @RequestParam @NotNull Long jogadorId) {
        return servico.fecharUnidade(unidadeId, jogadorId);
    }

    @PostMapping("/unidades/transferir-capital")
    public Map<String, Object> transferirCapital(@PathVariable Long empresaId,
                                                 @RequestBody TransferenciaRequest requisicao) {
        return servico.transferirCapital(requisicao.jogadorId(), requisicao.origemId(),
                requisicao.destinoId(), requisicao.valor());
    }

    @PostMapping("/unidades/transferir-equipe")
    public Map<String, Object> transferirEquipe(@PathVariable Long empresaId,
                                                @RequestBody TransferenciaRequest requisicao) {
        return servico.transferirEquipe(requisicao.jogadorId(), requisicao.origemId(),
                requisicao.destinoId(), requisicao.quantidade());
    }

    // ----- Linhas de produto -----

    @GetMapping("/linhas")
    public List<Map<String, Object>> linhas(@PathVariable Long empresaId) {
        return Mapeadores.lista(servico.linhas(empresaId), Mapeadores::linhaProduto);
    }

    @PostMapping("/linhas")
    public Map<String, Object> criarLinha(@PathVariable Long empresaId,
                                          @RequestBody LinhaRequest requisicao) {
        double fatia = requisicao.fatiaMix() == null ? 0 : requisicao.fatiaMix();
        return Mapeadores.linhaProduto(servico.criarLinha(empresaId, requisicao.jogadorId(),
                requisicao.nome(), requisicao.posicionamento(), fatia));
    }

    @PostMapping("/linhas/{linhaId}")
    public Map<String, Object> ajustarLinha(@PathVariable Long empresaId,
                                            @PathVariable Long linhaId,
                                            @RequestBody LinhaRequest requisicao) {
        return Mapeadores.linhaProduto(servico.ajustarLinha(linhaId, requisicao.jogadorId(),
                requisicao.posicionamento(), requisicao.fatiaMix()));
    }

    @DeleteMapping("/linhas/{linhaId}")
    public Map<String, Object> encerrarLinha(@PathVariable Long empresaId,
                                             @PathVariable Long linhaId,
                                             @RequestParam @NotNull Long jogadorId) {
        servico.encerrarLinha(linhaId, jogadorId);
        return Map.of("linhaId", linhaId, "encerrada", true);
    }

    // ----- Departamentos -----

    @GetMapping("/departamentos")
    public List<Map<String, Object>> departamentos(@PathVariable Long empresaId) {
        return Mapeadores.lista(servico.departamentos(empresaId), Mapeadores::departamento);
    }

    @PostMapping("/departamentos")
    public Map<String, Object> definirDepartamento(@PathVariable Long empresaId,
                                                   @RequestBody DepartamentoRequest requisicao) {
        return Mapeadores.departamento(servico.definirDepartamento(empresaId, requisicao.jogadorId(),
                requisicao.area(), requisicao.orcamentoMensal()));
    }
}
