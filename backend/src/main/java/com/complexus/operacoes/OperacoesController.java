package com.complexus.operacoes;

import com.complexus.comum.Mapeadores;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Mapa tatico: trafego civil, forcas militares e ordens de movimento. */
@RestController
@RequestMapping("/api/operacoes")
public class OperacoesController {

    public record OrdemRequest(@NotNull Long jogadorId, @NotNull Long unidadeId,
                               @NotNull Double longitude, @NotNull Double latitude) {
    }

    public record ComandanteRequest(@NotNull Long jogadorId) {
    }

    private final ServicoOperacoes servico;
    private final CartografiaMundo cartografia;
    private final RelogioOperacoes relogio;

    public OperacoesController(ServicoOperacoes servico, CartografiaMundo cartografia, RelogioOperacoes relogio) {
        this.servico = servico;
        this.cartografia = cartografia;
        this.relogio = relogio;
    }

    /** Parte fixa do mapa: portos, aeroportos, rotas civis e seus veiculos. */
    @GetMapping("/malha")
    public Map<String, Object> malha() {
        Map<String, Object> mapa = new LinkedHashMap<>();
        mapa.put("portos", Mapeadores.lista(cartografia.portos(), Mapeadores::porto));
        mapa.put("aeroportos", Mapeadores.lista(cartografia.aeroportos(), Mapeadores::aeroporto));
        mapa.put("rotas", Mapeadores.lista(cartografia.rotasCivis(), Mapeadores::rotaCivil));
        mapa.put("veiculos", Mapeadores.lista(cartografia.veiculos(), Mapeadores::veiculo));
        return mapa;
    }

    /** Parte viva: relogio de movimento e onde cada unidade militar esta. */
    @GetMapping("/situacao")
    public Map<String, Object> situacao(@RequestParam Long jogadorId) {
        Map<String, Object> mapa = new LinkedHashMap<>();
        mapa.put("podeComandar", servico.podeComandar(jogadorId));
        mapa.put("unidades", Mapeadores.lista(servico.situacao(), Mapeadores::unidadeMilitar));
        // Lido depois das unidades, para o cliente nunca ver um "agora" anterior a elas.
        mapa.put("agora", Instant.now().toEpochMilli());
        mapa.put("horasJogoPorSegundo", relogio.horasJogoPorSegundo());
        return mapa;
    }

    @PostMapping("/ordens")
    public Map<String, Object> ordenar(@Valid @RequestBody OrdemRequest requisicao) {
        return Mapeadores.unidadeMilitar(servico.emitirOrdem(requisicao.jogadorId(), requisicao.unidadeId(),
                new Ponto(requisicao.longitude(), requisicao.latitude())));
    }

    @PostMapping("/unidades/{id}/parar")
    public Map<String, Object> parar(@PathVariable Long id, @Valid @RequestBody ComandanteRequest requisicao) {
        return Mapeadores.unidadeMilitar(servico.parar(requisicao.jogadorId(), id));
    }
}
