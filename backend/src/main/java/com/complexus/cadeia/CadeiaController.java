package com.complexus.cadeia;

import com.complexus.comum.Mapeadores;
import com.complexus.economia.Empresa;
import com.complexus.economia.RepositorioEmpresa;
import com.complexus.comum.RecursoNaoEncontradoException;
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

/** Contratos de fornecimento entre empresas. */
@RestController
@RequestMapping("/api/empresas/{empresaId}/fornecimento")
public class CadeiaController {

    public record PropostaRequest(@NotNull Long jogadorId, @NotNull Long contraparteId,
                                  @NotNull TipoInsumo tipo, boolean comoFornecedor,
                                  double volumeMensal, double precoRelativo, int prazoTurnos) {
    }

    public record RespostaRequest(@NotNull Long jogadorId) {
    }

    private final ServicoCadeia servico;
    private final RepositorioEmpresa repositorioEmpresa;

    public CadeiaController(ServicoCadeia servico, RepositorioEmpresa repositorioEmpresa) {
        this.servico = servico;
        this.repositorioEmpresa = repositorioEmpresa;
    }

    /**
     * Situacao da empresa na cadeia: o que ela pode fornecer e comprar, quanto
     * ainda cabe em contrato e todos os contratos em que ela aparece.
     */
    @GetMapping
    public Map<String, Object> panorama(@PathVariable Long empresaId) {
        Empresa empresa = buscar(empresaId);
        List<ContratoFornecimento> contratos = servico.daEmpresa(empresaId);

        Map<String, Object> resposta = new LinkedHashMap<>();
        resposta.put("empresaId", empresaId);
        resposta.put("setor", empresa.getSetor().name());
        resposta.put("podeFornecer", catalogo(TipoInsumo.fornecidosPor(empresa.getSetor())));
        resposta.put("podeComprar", catalogo(TipoInsumo.compradosPor(empresa.getSetor())));
        resposta.put("capacidadeLivreParaContrato", servico.capacidadeLivreParaContrato(empresa));
        resposta.put("insumoLivreParaContrato", servico.insumoLivreParaContrato(empresa));
        resposta.put("contratos", Mapeadores.lista(contratos, Mapeadores::contratoFornecimento));
        return resposta;
    }

    @GetMapping("/parceiros")
    public List<Map<String, Object>> parceiros(@PathVariable Long empresaId,
                                               @RequestParam TipoInsumo tipo,
                                               @RequestParam(defaultValue = "false") boolean comoFornecedor) {
        return servico.parceiros(empresaId, tipo, comoFornecedor);
    }

    @PostMapping("/propostas")
    public Map<String, Object> propor(@PathVariable Long empresaId,
                                      @RequestBody PropostaRequest requisicao) {
        return Mapeadores.contratoFornecimento(servico.propor(empresaId, requisicao.jogadorId(),
                requisicao.contraparteId(), requisicao.tipo(), requisicao.comoFornecedor(),
                requisicao.volumeMensal(), requisicao.precoRelativo(), requisicao.prazoTurnos()));
    }

    @PostMapping("/contratos/{contratoId}/aceitar")
    public Map<String, Object> aceitar(@PathVariable Long empresaId, @PathVariable Long contratoId,
                                       @RequestBody RespostaRequest requisicao) {
        return Mapeadores.contratoFornecimento(servico.aceitar(contratoId, requisicao.jogadorId()));
    }

    @PostMapping("/contratos/{contratoId}/recusar")
    public Map<String, Object> recusar(@PathVariable Long empresaId, @PathVariable Long contratoId,
                                       @RequestBody RespostaRequest requisicao) {
        return Mapeadores.contratoFornecimento(servico.recusar(contratoId, requisicao.jogadorId()));
    }

    @PostMapping("/contratos/{contratoId}/romper")
    public Map<String, Object> romper(@PathVariable Long empresaId, @PathVariable Long contratoId,
                                      @RequestBody RespostaRequest requisicao) {
        return servico.romper(contratoId, requisicao.jogadorId());
    }

    private List<Map<String, Object>> catalogo(List<TipoInsumo> tipos) {
        List<Map<String, Object>> lista = new ArrayList<>();
        for (TipoInsumo tipo : tipos) {
            Map<String, Object> mapa = new LinkedHashMap<>();
            mapa.put("nome", tipo.name());
            mapa.put("rotulo", tipo.getRotulo());
            mapa.put("descricao", tipo.getDescricao());
            mapa.put("setorFornecedor", tipo.getSetorFornecedor().getRotulo());
            mapa.put("setoresCompradores", tipo.getSetoresCompradores().stream()
                    .map(setor -> setor.getRotulo()).sorted().toList());
            lista.add(mapa);
        }
        return lista;
    }

    private Empresa buscar(Long empresaId) {
        return repositorioEmpresa.findById(empresaId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Empresa", empresaId));
    }
}
