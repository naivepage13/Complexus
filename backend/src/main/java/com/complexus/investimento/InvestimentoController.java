package com.complexus.investimento;

import com.complexus.comum.Mapeadores;
import com.complexus.economia.Empresa;
import com.complexus.economia.ServicoEmpresa;
import jakarta.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Mercado de acoes: vitrine, carteira e ordens de compra e venda. */
@RestController
@RequestMapping("/api/investimentos")
public class InvestimentoController {

    public record OrdemRequest(@NotNull Long jogadorId, @NotNull Long empresaId, long quantidade) {
    }

    private final ServicoInvestimento servico;
    private final ServicoEmpresa servicoEmpresa;
    private final ServicoRazao razao;

    public InvestimentoController(ServicoInvestimento servico, ServicoEmpresa servicoEmpresa,
                                  ServicoRazao razao) {
        this.servico = servico;
        this.servicoEmpresa = servicoEmpresa;
        this.razao = razao;
    }

    /**
     * Empresas com acoes negociaveis, com os indicadores que sustentam a
     * decisao de investir: lastro, crescimento do lucro e dividendo projetado.
     */
    @GetMapping("/mercado")
    public List<Map<String, Object>> mercado() {
        List<Map<String, Object>> mercado = new ArrayList<>();
        for (Empresa empresa : servicoEmpresa.listarNegociaveis()) {
            Map<String, Object> linha = new LinkedHashMap<>();
            linha.put("empresaId", empresa.getId());
            linha.put("nome", empresa.getNome());
            linha.put("setor", empresa.getSetor().name());
            linha.put("setorRotulo", empresa.getSetor().getRotulo());
            linha.put("precoAcao", empresa.getPrecoAcao());
            linha.put("acoesDisponiveis", servico.acoesDisponiveis(empresa));
            linha.put("valuation", empresa.getValuation());
            linha.put("lucroMensal", empresa.getLucroMensal());
            linha.put("crescimentoLucro", empresa.getCrescimentoLucro());
            linha.put("indiceLastro", empresa.indiceLastro());
            linha.put("payout", empresa.getPayout());
            linha.put("dividendoProjetadoPorAcao",
                    empresa.getAcoesTotais() <= 0 ? 0
                            : Math.max(empresa.getLucroMensal(), 0) * empresa.getPayout() / empresa.getAcoesTotais());
            linha.put("municipio", empresa.getMunicipio().getNome());
            mercado.add(linha);
        }
        return mercado;
    }

    @GetMapping("/carteira")
    public Map<String, Object> carteira(@RequestParam Long jogadorId) {
        List<Map<String, Object>> posicoes = servico.carteira(jogadorId);
        double valorAtual = posicoes.stream()
                .mapToDouble(p -> ((Number) p.get("valorAtual")).doubleValue()).sum();
        double retorno = posicoes.stream()
                .mapToDouble(p -> ((Number) p.get("retornoTotal")).doubleValue()).sum();

        Map<String, Object> resposta = new LinkedHashMap<>();
        resposta.put("posicoes", posicoes);
        resposta.put("valorAtual", valorAtual);
        resposta.put("retornoTotal", retorno);
        resposta.put("extrato", Mapeadores.lista(razao.porJogador(jogadorId, 30), Mapeadores::lancamento));
        return resposta;
    }

    @PostMapping("/comprar")
    public Map<String, Object> comprar(@RequestBody OrdemRequest requisicao) {
        Investimento investimento = servico.comprar(requisicao.jogadorId(), requisicao.empresaId(),
                requisicao.quantidade());
        return posicao(investimento);
    }

    @PostMapping("/vender")
    public Map<String, Object> vender(@RequestBody OrdemRequest requisicao) {
        Investimento investimento = servico.vender(requisicao.jogadorId(), requisicao.empresaId(),
                requisicao.quantidade());
        return posicao(investimento);
    }

    private Map<String, Object> posicao(Investimento investimento) {
        Map<String, Object> mapa = new LinkedHashMap<>();
        mapa.put("id", investimento.getId());
        mapa.put("empresaId", investimento.getEmpresa().getId());
        mapa.put("empresa", investimento.getEmpresa().getNome());
        mapa.put("acoes", investimento.getAcoes());
        mapa.put("precoMedio", investimento.getPrecoMedio());
        mapa.put("capitalAportado", investimento.getCapitalAportado());
        mapa.put("dividendosRecebidos", investimento.getDividendosRecebidos());
        mapa.put("lucroRealizado", investimento.getLucroRealizado());
        mapa.put("ativo", investimento.isAtivo());
        return mapa;
    }
}
