package com.geohistoricalsim.economia;

import com.geohistoricalsim.comum.Mapeadores;
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

/** Administracao de empresas pelo jogador. */
@RestController
@RequestMapping("/api/empresas")
public class EmpresaController {

    public record FundacaoRequest(@NotNull Long jogadorId, String nome, @NotNull Setor setor,
                                  @NotNull Long municipioId, double capitalInicial, int funcionarios) {
    }

    public record ValorRequest(@NotNull Long jogadorId, double valor) {
    }

    public record QuantidadeRequest(@NotNull Long jogadorId, int quantidade) {
    }

    public record GestaoRequest(@NotNull Long jogadorId, Double marketingMensal,
                                Double salarioMedio, Double payout) {
    }

    public record IpoRequest(@NotNull Long jogadorId, double fracaoOfertada) {
    }

    public record ObraRequest(@NotNull Long jogadorId, String nome,
                              @NotNull Empreendimento.TipoEmpreendimento tipo,
                              double custoTotal, int turnosTotais) {
    }

    private final ServicoEmpresa servico;

    public EmpresaController(ServicoEmpresa servico) {
        this.servico = servico;
    }

    /** Catalogo de setores com os parametros que o jogador precisa conhecer. */
    @GetMapping("/setores")
    public List<Map<String, Object>> setores() {
        List<Map<String, Object>> setores = new ArrayList<>();
        for (Setor setor : Setor.values()) {
            Map<String, Object> mapa = new LinkedHashMap<>();
            mapa.put("nome", setor.name());
            mapa.put("rotulo", setor.getRotulo());
            mapa.put("margemBase", setor.getMargemBase());
            mapa.put("volatilidade", setor.getVolatilidade());
            mapa.put("giroAtivoMensal", setor.getGiroAtivoMensal());
            mapa.put("receitaPorFuncionario", setor.getReceitaPorFuncionario());
            mapa.put("multiploValuation", setor.getMultiploValuation());
            mapa.put("capitalMinimo", setor.getCapitalMinimo());
            mapa.put("elasticidadeRenda", setor.getElasticidadeRenda());
            mapa.put("elasticidadeJuros", setor.getElasticidadeJuros());
            setores.add(mapa);
        }
        return setores;
    }

    /**
     * Listagem do mercado. Devolve a visao publica da empresa; o resultado
     * financeiro aparece apenas no detalhe, na pagina da propria empresa.
     */
    @GetMapping
    public List<Map<String, Object>> listar(@RequestParam(required = false) Setor setor,
                                            @RequestParam(required = false) Long jogadorId) {
        if (jogadorId != null) {
            return Mapeadores.lista(servico.listarDoJogador(jogadorId), Mapeadores::empresa);
        }
        List<Empresa> empresas = setor != null ? servico.listarPorSetor(setor) : servico.listar();
        return Mapeadores.lista(empresas, Mapeadores::empresaPublica);
    }

    @GetMapping("/{id}")
    public Map<String, Object> detalhar(@PathVariable Long id) {
        Map<String, Object> resposta = new LinkedHashMap<>(Mapeadores.empresa(servico.buscar(id)));
        resposta.put("historico", Mapeadores.lista(servico.historico(id, 36), Mapeadores::historico));
        resposta.put("empreendimentos",
                Mapeadores.lista(servico.empreendimentos(id), Mapeadores::empreendimento));
        resposta.put("capacidade", servico.diagnosticoCapacidade(id));
        return resposta;
    }

    @GetMapping("/{id}/historico")
    public List<Map<String, Object>> historico(@PathVariable Long id,
                                               @RequestParam(defaultValue = "36") int limite) {
        return Mapeadores.lista(servico.historico(id, limite), Mapeadores::historico);
    }

    @PostMapping
    public Map<String, Object> fundar(@RequestBody FundacaoRequest requisicao) {
        return Mapeadores.empresa(servico.fundar(requisicao.jogadorId(), requisicao.nome(),
                requisicao.setor(), requisicao.municipioId(), requisicao.capitalInicial(),
                requisicao.funcionarios()));
    }

    @PostMapping("/{id}/capital")
    public Map<String, Object> investirCapital(@PathVariable Long id, @RequestBody ValorRequest requisicao) {
        return Mapeadores.empresa(servico.investirCapital(id, requisicao.jogadorId(), requisicao.valor()));
    }

    @PostMapping("/{id}/contratar")
    public Map<String, Object> contratar(@PathVariable Long id, @RequestBody QuantidadeRequest requisicao) {
        return Mapeadores.empresa(servico.contratar(id, requisicao.jogadorId(), requisicao.quantidade()));
    }

    @PostMapping("/{id}/demitir")
    public Map<String, Object> demitir(@PathVariable Long id, @RequestBody QuantidadeRequest requisicao) {
        return Mapeadores.empresa(servico.demitir(id, requisicao.jogadorId(), requisicao.quantidade()));
    }

    @PostMapping("/{id}/gestao")
    public Map<String, Object> ajustarGestao(@PathVariable Long id, @RequestBody GestaoRequest requisicao) {
        return Mapeadores.empresa(servico.ajustarGestao(id, requisicao.jogadorId(),
                requisicao.marketingMensal(), requisicao.salarioMedio(), requisicao.payout()));
    }

    @PostMapping("/{id}/ipo")
    public Map<String, Object> abrirCapital(@PathVariable Long id, @RequestBody IpoRequest requisicao) {
        return Mapeadores.empresa(servico.abrirCapital(id, requisicao.jogadorId(), requisicao.fracaoOfertada()));
    }

    @GetMapping("/{id}/empreendimentos")
    public List<Map<String, Object>> empreendimentos(@PathVariable Long id) {
        return Mapeadores.lista(servico.empreendimentos(id), Mapeadores::empreendimento);
    }

    @PostMapping("/{id}/empreendimentos")
    public Map<String, Object> iniciarEmpreendimento(@PathVariable Long id, @RequestBody ObraRequest requisicao) {
        return Mapeadores.empreendimento(servico.iniciarEmpreendimento(id, requisicao.jogadorId(),
                requisicao.nome(), requisicao.tipo(), requisicao.custoTotal(), requisicao.turnosTotais()));
    }
}
