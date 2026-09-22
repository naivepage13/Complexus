package com.complexus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.complexus.cadeia.ContratoFornecimento;
import com.complexus.cadeia.ServicoCadeia;
import com.complexus.cadeia.TipoInsumo;
import com.complexus.comum.RegraDeNegocioException;
import com.complexus.core.ServicoTurno;
import com.complexus.economia.Empresa;
import com.complexus.economia.ServicoEmpresa;
import com.complexus.economia.Setor;
import com.complexus.jogador.Jogador;
import com.complexus.jogador.ServicoJogador;
import com.complexus.politica.Municipio;
import com.complexus.politica.ServicoPolitica;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Testes da cadeia produtiva: proposta, resposta, limites, faturamento no turno
 * e rompimento de contrato.
 */
@SpringBootTest
class CadeiaProdutivaTest {

    @Autowired
    private ServicoCadeia servicoCadeia;
    @Autowired
    private ServicoEmpresa servicoEmpresa;
    @Autowired
    private ServicoJogador servicoJogador;
    @Autowired
    private ServicoPolitica servicoPolitica;
    @Autowired
    private ServicoTurno servicoTurno;

    @Test
    @DisplayName("Empresa do sistema aceita na hora um preco dentro da tolerancia")
    void sistemaAceitaPrecoJusto() {
        Empresa construtora = fundar(Setor.CONSTRUCAO, 4_000_000, 60);
        Empresa incorporadoraDoSistema = doSistema(Setor.IMOBILIARIO);

        double volume = volumeViavel(construtora, incorporadoraDoSistema);
        ContratoFornecimento contrato = servicoCadeia.propor(construtora.getId(),
                construtora.getDono().getId(), incorporadoraDoSistema.getId(),
                TipoInsumo.MATERIAL_CONSTRUCAO, true, volume, 1.02, 6);

        assertEquals(ContratoFornecimento.Status.ATIVO, contrato.getStatus());
        assertEquals(6, contrato.getTurnosRestantes());
    }

    @Test
    @DisplayName("Empresa do sistema recusa preco fora da tolerancia")
    void sistemaRecusaPrecoAbusivo() {
        Empresa construtora = fundar(Setor.CONSTRUCAO, 4_000_000, 60);
        Empresa incorporadoraDoSistema = doSistema(Setor.IMOBILIARIO);
        double volume = volumeViavel(construtora, incorporadoraDoSistema);

        ContratoFornecimento contrato = servicoCadeia.propor(construtora.getId(),
                construtora.getDono().getId(), incorporadoraDoSistema.getId(),
                TipoInsumo.MATERIAL_CONSTRUCAO, true, volume, 1.25, 6);

        assertEquals(ContratoFornecimento.Status.RECUSADO, contrato.getStatus());
    }

    @Test
    @DisplayName("A matriz de setores barra contrato incompativel")
    void setoresIncompativeisSaoBarrados() {
        Empresa padaria = fundar(Setor.ALIMENTICIO, 1_000_000, 30);
        Empresa incorporadoraDoSistema = doSistema(Setor.IMOBILIARIO);

        // Alimenticio nao fornece material de construcao a ninguem.
        assertThrows(RegraDeNegocioException.class, () -> servicoCadeia.propor(
                padaria.getId(), padaria.getDono().getId(), incorporadoraDoSistema.getId(),
                TipoInsumo.MATERIAL_CONSTRUCAO, true, 10_000, 1.0, 6));
    }

    @Test
    @DisplayName("Ninguem se compromete a fornecer mais do que consegue produzir")
    void volumeLimitadoPelaCapacidadeDoFornecedor() {
        Empresa construtora = fundar(Setor.CONSTRUCAO, 4_000_000, 60);
        Empresa incorporadoraDoSistema = doSistema(Setor.IMOBILIARIO);
        double livre = servicoCadeia.capacidadeLivreParaContrato(construtora);

        assertTrue(livre > 0, "uma construtora nova tem capacidade livre para contrato");
        assertThrows(RegraDeNegocioException.class, () -> servicoCadeia.propor(
                construtora.getId(), construtora.getDono().getId(), incorporadoraDoSistema.getId(),
                TipoInsumo.MATERIAL_CONSTRUCAO, true, livre * 2, 1.0, 6));
    }

    @Test
    @DisplayName("Preco fora da faixa e prazo fora do limite sao recusados")
    void precoEPrazoSaoValidados() {
        Empresa construtora = fundar(Setor.CONSTRUCAO, 4_000_000, 60);
        Long jogadorId = construtora.getDono().getId();
        Long compradorId = doSistema(Setor.IMOBILIARIO).getId();

        assertThrows(RegraDeNegocioException.class, () -> servicoCadeia.propor(construtora.getId(),
                jogadorId, compradorId, TipoInsumo.MATERIAL_CONSTRUCAO, true, 20_000, 1.60, 6));
        assertThrows(RegraDeNegocioException.class, () -> servicoCadeia.propor(construtora.getId(),
                jogadorId, compradorId, TipoInsumo.MATERIAL_CONSTRUCAO, true, 20_000, 1.0, 60));
    }

    @Test
    @DisplayName("O turno fatura o contrato, anda o prazo e reserva capacidade")
    void turnoFaturaOContrato() {
        Empresa construtora = fundar(Setor.CONSTRUCAO, 5_000_000, 70);
        Empresa incorporadoraDoSistema = doSistema(Setor.IMOBILIARIO);
        double volume = volumeViavel(construtora, incorporadoraDoSistema);

        ContratoFornecimento contrato = servicoCadeia.propor(construtora.getId(),
                construtora.getDono().getId(), incorporadoraDoSistema.getId(),
                TipoInsumo.MATERIAL_CONSTRUCAO, true, volume, 1.0, 6);
        assertEquals(ContratoFornecimento.Status.ATIVO, contrato.getStatus());

        servicoTurno.processarTurno("TESTE");

        ContratoFornecimento apos = servicoCadeia.buscar(contrato.getId());
        assertEquals(5, apos.getTurnosRestantes());
        assertTrue(apos.getTotalFaturado() > 0, "o contrato fatura no turno em que esta em vigor");
        assertEquals(volume * 1.0, apos.getTotalFaturado(), volume * 0.01);
    }

    @Test
    @DisplayName("O contrato se encerra sozinho ao fim do prazo")
    void contratoConcluiNoPrazo() {
        Empresa construtora = fundar(Setor.CONSTRUCAO, 5_000_000, 70);
        Empresa compradorDoSistema = doSistema(Setor.IMOBILIARIO);
        double volume = volumeViavel(construtora, compradorDoSistema);

        ContratoFornecimento contrato = servicoCadeia.propor(construtora.getId(),
                construtora.getDono().getId(), compradorDoSistema.getId(),
                TipoInsumo.MATERIAL_CONSTRUCAO, true, volume, 1.0, 3);

        for (int i = 0; i < 3; i++) {
            servicoTurno.processarTurno("TESTE");
        }

        assertEquals(ContratoFornecimento.Status.CONCLUIDO,
                servicoCadeia.buscar(contrato.getId()).getStatus());
    }

    @Test
    @DisplayName("Romper contrato paga multa a outra parte e queima reputacao")
    void rompimentoPagaMulta() {
        Empresa construtora = fundar(Setor.CONSTRUCAO, 5_000_000, 70);
        Long jogadorId = construtora.getDono().getId();
        Empresa compradorDoSistema = doSistema(Setor.IMOBILIARIO);
        double volume = volumeViavel(construtora, compradorDoSistema);

        ContratoFornecimento contrato = servicoCadeia.propor(construtora.getId(), jogadorId,
                compradorDoSistema.getId(), TipoInsumo.MATERIAL_CONSTRUCAO, true, volume, 1.0, 12);
        double reputacaoAntes = servicoEmpresa.buscar(construtora.getId()).getReputacao();
        double caixaAntes = servicoEmpresa.buscar(construtora.getId()).getCaixa();

        Map<String, Object> resumo = servicoCadeia.romper(contrato.getId(), jogadorId);

        double multa = (double) resumo.get("multa");
        assertTrue(multa > 0, "quem rompe paga pelo que deixou de entregar");
        assertEquals(ContratoFornecimento.Status.ROMPIDO,
                servicoCadeia.buscar(contrato.getId()).getStatus());
        Empresa apos = servicoEmpresa.buscar(construtora.getId());
        assertEquals(caixaAntes - multa, apos.getCaixa(), 0.01);
        assertTrue(apos.getReputacao() < reputacaoAntes);
    }

    @Test
    @DisplayName("A empresa so enxerga os tipos de insumo do proprio setor")
    void catalogoRespeitaOSetor() {
        assertEquals(List.of(TipoInsumo.INSUMO_ALIMENTAR), TipoInsumo.fornecidosPor(Setor.ALIMENTICIO));
        assertEquals(List.of(TipoInsumo.MATERIAL_CONSTRUCAO, TipoInsumo.SERVICO_DE_OBRA),
                TipoInsumo.fornecidosPor(Setor.CONSTRUCAO));
        assertTrue(TipoInsumo.compradosPor(Setor.ALIMENTICIO)
                .containsAll(List.of(TipoInsumo.ESPACO_COMERCIAL, TipoInsumo.INSUMO_ALIMENTAR)));
    }

    // ------------------------------------------------------------------

    private Empresa fundar(Setor setor, double capital, int funcionarios) {
        Jogador jogador = servicoJogador.registrar("cadeia" + sufixo(), "Jogador cadeia", "senha123");
        Municipio municipio = servicoPolitica.listarMunicipios().get(0);
        return servicoEmpresa.fundar(jogador.getId(), "Cadeia " + sufixo(), setor,
                municipio.getId(), capital, funcionarios);
    }

    /**
     * Volume que cabe nas duas pontas: o fornecedor nao pode prometer mais do que
     * produz, nem o comprador contratar mais insumo do que consome. Os testes
     * dividem o mesmo mundo, entao o espaco livre muda conforme eles rodam.
     */
    private double volumeViavel(Empresa fornecedor, Empresa comprador) {
        double capacidade = servicoCadeia.capacidadeLivreParaContrato(fornecedor);
        double insumo = servicoCadeia.insumoLivreParaContrato(comprador);
        return Math.min(capacidade, insumo) * 0.4;
    }

    /** Uma empresa do mundo inicial, sem dono, que responde por regra. */
    private Empresa doSistema(Setor setor) {
        return servicoEmpresa.listarPorSetor(setor).stream()
                .filter(empresa -> empresa.getDono() == null)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("mundo inicial sem empresa do setor " + setor));
    }

    private String sufixo() {
        return UUID.randomUUID().toString().substring(0, 8);
    }
}
