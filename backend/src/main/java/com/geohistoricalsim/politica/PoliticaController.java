package com.geohistoricalsim.politica;

import com.geohistoricalsim.comum.Mapeadores;
import com.geohistoricalsim.economia.Setor;
import jakarta.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Cargos, mandatos e tramitacao de projetos de lei. */
@RestController
@RequestMapping("/api/politica")
public class PoliticaController {

    public record PosseRequest(@NotNull Long jogadorId, @NotNull CargoPolitico cargo,
                               @NotNull Long territorioId, String partido) {
    }

    public record ProjetoRequest(@NotNull Long jogadorId, @NotNull Long mandatoId, String titulo,
                                 String ementa, @NotNull TipoProjeto tipo, Setor setorAlvo,
                                 double parametro) {
    }

    public record JogadorRequest(@NotNull Long jogadorId) {
    }

    public record VotoRequest(@NotNull Long jogadorId, @NotNull Long mandatoId,
                              @NotNull VotoProjeto.Opcao opcao) {
    }

    public record SancaoRequest(@NotNull Long jogadorId, boolean sancionar, String justificativa) {
    }

    public record DerrubadaRequest(@NotNull Long jogadorId, @NotNull Long mandatoId) {
    }

    private final ServicoPolitica servico;

    public PoliticaController(ServicoPolitica servico) {
        this.servico = servico;
    }

    /** Catalogo de cargos com esfera, duracao e poderes. */
    @GetMapping("/cargos")
    public List<Map<String, Object>> cargos() {
        List<Map<String, Object>> cargos = new ArrayList<>();
        for (CargoPolitico cargo : CargoPolitico.values()) {
            Map<String, Object> mapa = new LinkedHashMap<>();
            mapa.put("nome", cargo.name());
            mapa.put("rotulo", cargo.getRotulo());
            mapa.put("esfera", cargo.getEsfera().name());
            mapa.put("duracaoMandatoTurnos", cargo.getDuracaoMandatoTurnos());
            mapa.put("executivo", cargo.isExecutivo());
            mapa.put("legislativo", cargo.isLegislativo());
            mapa.put("eletivo", cargo.isEletivo());
            mapa.put("chefiaExecutivo", cargo.chefiaExecutivo());
            mapa.put("vagas", VagasCargo.vagas(cargo));
            cargos.add(mapa);
        }
        return cargos;
    }

    /** Catalogo de instrumentos legais disponiveis. */
    @GetMapping("/tipos-projeto")
    public List<Map<String, Object>> tiposProjeto() {
        List<Map<String, Object>> tipos = new ArrayList<>();
        for (TipoProjeto tipo : TipoProjeto.values()) {
            Map<String, Object> mapa = new LinkedHashMap<>();
            mapa.put("nome", tipo.name());
            mapa.put("descricao", tipo.getDescricao());
            mapa.put("esferas", tipo.getEsferasPermitidas().stream().map(Enum::name).sorted().toList());
            mapa.put("parametroMinimo", tipo.getParametroMinimo());
            mapa.put("parametroMaximo", tipo.getParametroMaximo());
            mapa.put("exigeSetor", tipo.isExigeSetor());
            mapa.put("unidadeParametro", tipo.getUnidadeParametro());
            tipos.add(mapa);
        }
        return tipos;
    }

    @GetMapping("/territorios")
    public Map<String, Object> territorios() {
        Map<String, Object> mapa = new LinkedHashMap<>();
        mapa.put("paises", Mapeadores.lista(servico.listarPaises(), Mapeadores::pais));
        mapa.put("estados", Mapeadores.lista(servico.listarEstados(), Mapeadores::estado));
        mapa.put("municipios", Mapeadores.lista(servico.listarMunicipios(), Mapeadores::municipio));
        return mapa;
    }

    @GetMapping("/mandatos")
    public List<Map<String, Object>> mandatos(@RequestParam Esfera esfera, @RequestParam Long territorioId) {
        return Mapeadores.lista(servico.mandatosDoTerritorio(esfera, territorioId), Mapeadores::mandato);
    }

    @GetMapping("/mandatos/jogador/{jogadorId}")
    public List<Map<String, Object>> mandatosDoJogador(@PathVariable Long jogadorId) {
        return Mapeadores.lista(servico.mandatosDoJogador(jogadorId), Mapeadores::mandato);
    }

    @PostMapping("/mandatos")
    public Map<String, Object> assumirCargo(@RequestBody PosseRequest requisicao) {
        return Mapeadores.mandato(servico.assumirCargo(requisicao.jogadorId(), requisicao.cargo(),
                requisicao.territorioId(), requisicao.partido()));
    }

    @GetMapping("/projetos")
    public List<Map<String, Object>> projetos(@RequestParam(required = false) Esfera esfera,
                                              @RequestParam(required = false) Long territorioId) {
        List<ProjetoDeLei> projetos = (esfera != null && territorioId != null)
                ? servico.projetosDoTerritorio(esfera, territorioId)
                : servico.projetosRecentes();
        return Mapeadores.lista(projetos, Mapeadores::projeto);
    }

    @GetMapping("/projetos/{id}")
    public Map<String, Object> projeto(@PathVariable Long id) {
        return Mapeadores.projeto(servico.buscarProjeto(id));
    }

    @GetMapping("/leis")
    public List<Map<String, Object>> leis() {
        return Mapeadores.lista(servico.leisEmVigor(), Mapeadores::projeto);
    }

    @PostMapping("/projetos")
    public Map<String, Object> propor(@RequestBody ProjetoRequest requisicao) {
        return Mapeadores.projeto(servico.propor(requisicao.jogadorId(), requisicao.mandatoId(),
                requisicao.titulo(), requisicao.ementa(), requisicao.tipo(), requisicao.setorAlvo(),
                requisicao.parametro()));
    }

    @PostMapping("/projetos/{id}/pautar")
    public Map<String, Object> pautar(@PathVariable Long id, @RequestBody JogadorRequest requisicao) {
        return Mapeadores.projeto(servico.pautar(requisicao.jogadorId(), id));
    }

    @PostMapping("/projetos/{id}/votar")
    public Map<String, Object> votar(@PathVariable Long id, @RequestBody VotoRequest requisicao) {
        servico.votar(requisicao.jogadorId(), requisicao.mandatoId(), id, requisicao.opcao());
        return Mapeadores.projeto(servico.buscarProjeto(id));
    }

    @PostMapping("/projetos/{id}/sancionar")
    public Map<String, Object> sancionar(@PathVariable Long id, @RequestBody SancaoRequest requisicao) {
        return Mapeadores.projeto(servico.sancionar(requisicao.jogadorId(), id,
                requisicao.sancionar(), requisicao.justificativa()));
    }

    @PostMapping("/projetos/{id}/derrubar-veto")
    public Map<String, Object> derrubarVeto(@PathVariable Long id, @RequestBody DerrubadaRequest requisicao) {
        return Mapeadores.projeto(servico.derrubarVeto(requisicao.jogadorId(), requisicao.mandatoId(), id));
    }
}
