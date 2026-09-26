package com.complexus.jogador;

import com.complexus.comum.Mapeadores;
import com.complexus.economia.ServicoEmpresa;
import com.complexus.investimento.ServicoInvestimento;
import com.complexus.politica.ServicoPolitica;
import jakarta.validation.constraints.NotBlank;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Cadastro, autenticacao simplificada e painel consolidado do jogador. */
@RestController
@RequestMapping("/api/jogadores")
public class JogadorController {

    public record CadastroRequest(@NotBlank String usuario, String nome, @NotBlank String senha) {
    }

    public record LoginRequest(@NotBlank String usuario, @NotBlank String senha) {
    }

    public record AlterarSenhaRequest(@NotBlank String senhaAtual, @NotBlank String novaSenha) {
    }

    public record AlterarEmailRequest(@NotBlank String email) {
    }

    public record ExcluirContaRequest(String senha, String confirmacao) {
    }

    private final ServicoJogador servicoJogador;
    private final ServicoEmpresa servicoEmpresa;
    private final ServicoInvestimento servicoInvestimento;
    private final ServicoPolitica servicoPolitica;

    public JogadorController(ServicoJogador servicoJogador,
                             ServicoEmpresa servicoEmpresa,
                             ServicoInvestimento servicoInvestimento,
                             ServicoPolitica servicoPolitica) {
        this.servicoJogador = servicoJogador;
        this.servicoEmpresa = servicoEmpresa;
        this.servicoInvestimento = servicoInvestimento;
        this.servicoPolitica = servicoPolitica;
    }

    @PostMapping("/registrar")
    public Map<String, Object> registrar(@RequestBody CadastroRequest requisicao) {
        return Mapeadores.jogador(
                servicoJogador.registrar(requisicao.usuario(), requisicao.nome(), requisicao.senha()));
    }

    @PostMapping("/login")
    public Map<String, Object> login(@RequestBody LoginRequest requisicao) {
        return Mapeadores.jogador(servicoJogador.autenticar(requisicao.usuario(), requisicao.senha()));
    }

    @GetMapping("/{id}")
    public Map<String, Object> buscar(@PathVariable Long id) {
        return Mapeadores.jogador(servicoJogador.buscar(id));
    }

    @PostMapping("/{id}/senha")
    public Map<String, Object> alterarSenha(@PathVariable Long id, @RequestBody AlterarSenhaRequest requisicao) {
        return Mapeadores.jogador(servicoJogador.alterarSenha(id, requisicao.senhaAtual(), requisicao.novaSenha()));
    }

    @PostMapping("/{id}/email")
    public Map<String, Object> alterarEmail(@PathVariable Long id, @RequestBody AlterarEmailRequest requisicao) {
        return Mapeadores.jogador(servicoJogador.alterarEmail(id, requisicao.email()));
    }

    @PostMapping("/{id}/excluir")
    public Map<String, Object> excluirConta(@PathVariable Long id, @RequestBody ExcluirContaRequest requisicao) {
        servicoJogador.excluirConta(id, requisicao.senha(), requisicao.confirmacao());
        return Map.of("excluida", true);
    }

    /** Visao consolidada usada pelo painel inicial do jogador. */
    @GetMapping("/{id}/painel")
    public Map<String, Object> painel(@PathVariable Long id) {
        Jogador jogador = servicoJogador.buscar(id);
        List<Map<String, Object>> empresas = Mapeadores.lista(
                servicoEmpresa.listarDoJogador(id), Mapeadores::empresa);
        List<Map<String, Object>> carteira = servicoInvestimento.carteira(id);
        List<Map<String, Object>> mandatos = Mapeadores.lista(
                servicoPolitica.mandatosDoJogador(id), Mapeadores::mandato);

        double valorEmpresas = empresas.stream()
                .mapToDouble(e -> ((Number) e.get("valuation")).doubleValue()).sum();
        double valorCarteira = carteira.stream()
                .mapToDouble(e -> ((Number) e.get("valorAtual")).doubleValue()).sum();
        double lucroMensal = empresas.stream()
                .mapToDouble(e -> ((Number) e.get("lucroMensal")).doubleValue()).sum();

        Map<String, Object> painel = new LinkedHashMap<>();
        painel.put("jogador", Mapeadores.jogador(jogador));
        painel.put("empresas", empresas);
        painel.put("carteira", carteira);
        painel.put("mandatos", mandatos);
        painel.put("patrimonioTotal", jogador.getSaldo() + valorEmpresas + valorCarteira);
        painel.put("valorEmpresas", valorEmpresas);
        painel.put("valorCarteira", valorCarteira);
        painel.put("lucroMensalEmpresas", lucroMensal);
        return painel;
    }
}
