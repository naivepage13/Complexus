package com.complexus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.complexus.comum.RegraDeNegocioException;
import com.complexus.core.ServicoTurno;
import com.complexus.economia.Empresa;
import com.complexus.economia.ServicoEmpresa;
import com.complexus.economia.Setor;
import com.complexus.financas.AvaliacaoCredito;
import com.complexus.financas.Financiamento;
import com.complexus.financas.NotaCredito;
import com.complexus.financas.ServicoCredito;
import com.complexus.jogador.Jogador;
import com.complexus.jogador.ServicoJogador;
import com.complexus.politica.Municipio;
import com.complexus.politica.ServicoPolitica;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Testes do credito da empresa: avaliacao, contratacao, servico da divida,
 * amortizacao antecipada, renegociacao e inadimplencia.
 */
@SpringBootTest
class FinancasDaEmpresaTest {

    @Autowired
    private ServicoCredito servicoCredito;
    @Autowired
    private ServicoEmpresa servicoEmpresa;
    @Autowired
    private ServicoJogador servicoJogador;
    @Autowired
    private ServicoPolitica servicoPolitica;
    @Autowired
    private ServicoTurno servicoTurno;

    @Test
    @DisplayName("Empresa sem divida nasce com a melhor nota e limite disponivel")
    void empresaSemDividaTemNotaAlta() {
        Empresa empresa = fundar(3_000_000);

        AvaliacaoCredito avaliacao = servicoCredito.avaliar(empresa);

        assertEquals(NotaCredito.A, avaliacao.nota());
        assertEquals(0, avaliacao.dividaTotal(), 0.01);
        assertTrue(servicoCredito.limiteDisponivel(empresa,
                Financiamento.Modalidade.CAPITAL_DE_GIRO, avaliacao) > 0);
    }

    @Test
    @DisplayName("Credito contratado cai no caixa e vira divida da empresa")
    void contratacaoCreditaOCaixa() {
        Empresa empresa = fundar(3_000_000);
        double caixaAntes = empresa.getCaixa();

        Financiamento contrato = servicoCredito.contratar(empresa.getId(), empresa.getDono().getId(),
                Financiamento.Modalidade.CAPITAL_DE_GIRO, 200_000, 12);

        Empresa atual = servicoEmpresa.buscar(empresa.getId());
        assertEquals(caixaAntes + 200_000, atual.getCaixa(), 0.01);
        assertEquals(200_000, atual.getDivida(), 0.01);
        assertEquals(200_000, contrato.getSaldoDevedor(), 0.01);
        assertTrue(contrato.taxaAnual() > 0, "a taxa sai da Selic mais os spreads de linha e nota");
    }

    @Test
    @DisplayName("O limite da nota barra o valor pedido acima do teto")
    void limiteDeCreditoEhRespeitado() {
        Empresa empresa = fundar(3_000_000);
        AvaliacaoCredito avaliacao = servicoCredito.avaliar(empresa);
        double disponivel = servicoCredito.limiteDisponivel(empresa,
                Financiamento.Modalidade.CAPITAL_DE_GIRO, avaliacao);

        assertThrows(RegraDeNegocioException.class, () -> servicoCredito.contratar(
                empresa.getId(), empresa.getDono().getId(),
                Financiamento.Modalidade.CAPITAL_DE_GIRO, disponivel * 2, 12));
    }

    @Test
    @DisplayName("Prazo fora da faixa da linha e recusado, e o rotativo nao se contrata")
    void prazoEModalidadeSaoValidados() {
        Empresa empresa = fundar(3_000_000);
        Long jogadorId = empresa.getDono().getId();

        assertThrows(RegraDeNegocioException.class, () -> servicoCredito.contratar(
                empresa.getId(), jogadorId, Financiamento.Modalidade.CAPITAL_DE_GIRO, 100_000, 48));
        assertThrows(RegraDeNegocioException.class, () -> servicoCredito.contratar(
                empresa.getId(), jogadorId, Financiamento.Modalidade.ROTATIVO, 100_000, 6));
    }

    @Test
    @DisplayName("Linha com garantia compromete patrimonio e reduz o patrimonio livre")
    void garantiaCompromentePatrimonio() {
        Empresa empresa = fundar(5_000_000);
        double livreAntes = servicoCredito.patrimonioLivre(empresa);

        Financiamento contrato = servicoCredito.contratar(empresa.getId(), empresa.getDono().getId(),
                Financiamento.Modalidade.INVESTIMENTO, 300_000, 24);

        assertEquals(390_000, contrato.getGarantia(), 0.01, "a cobertura exigida e 1,3x o valor");
        assertEquals(livreAntes - 390_000, servicoCredito.patrimonioLivre(
                servicoEmpresa.buscar(empresa.getId())), 0.01);
    }

    @Test
    @DisplayName("O turno cobra a parcela: saldo cai e o contrato anda")
    void turnoCobraAParcela() {
        Empresa empresa = fundar(4_000_000);
        Financiamento contrato = servicoCredito.contratar(empresa.getId(), empresa.getDono().getId(),
                Financiamento.Modalidade.CAPITAL_DE_GIRO, 240_000, 12);
        double saldoAntes = contrato.getSaldoDevedor();

        servicoTurno.processarTurno("TESTE");

        Financiamento apos = servicoCredito.buscar(contrato.getId());
        assertTrue(apos.getSaldoDevedor() < saldoAntes, "a amortizacao do turno reduz o saldo");
        assertEquals(11, apos.getTurnosRestantes());
        assertTrue(apos.getJurosPagos() > 0, "os juros do mes entram como despesa do turno");
        assertEquals(apos.getSaldoDevedor(), servicoEmpresa.buscar(empresa.getId()).getDivida(), 0.01,
                "a divida da empresa e a soma dos contratos em aberto");
    }

    @Test
    @DisplayName("Amortizacao antecipada quita o contrato e zera a divida")
    void amortizacaoAntecipadaQuita() {
        Empresa empresa = fundar(4_000_000);
        Long jogadorId = empresa.getDono().getId();
        Financiamento contrato = servicoCredito.contratar(empresa.getId(), jogadorId,
                Financiamento.Modalidade.CAPITAL_DE_GIRO, 150_000, 12);

        Financiamento quitado = servicoCredito.amortizar(contrato.getId(), jogadorId, 150_000);

        assertEquals(Financiamento.Status.QUITADO, quitado.getStatus());
        assertEquals(0, quitado.getSaldoDevedor(), 0.01);
        assertEquals(0, servicoEmpresa.buscar(empresa.getId()).getDivida(), 0.01);
    }

    @Test
    @DisplayName("Renegociar alonga o prazo, encarece a taxa e cobra comissao")
    void renegociacaoAlongaEEncarece() {
        Empresa empresa = fundar(4_000_000);
        Long jogadorId = empresa.getDono().getId();
        Financiamento contrato = servicoCredito.contratar(empresa.getId(), jogadorId,
                Financiamento.Modalidade.CAPITAL_DE_GIRO, 200_000, 12);
        double taxaAntes = contrato.getTaxaMensal();
        double saldoAntes = contrato.getSaldoDevedor();

        Financiamento renegociado = servicoCredito.renegociar(contrato.getId(), jogadorId, 24);

        assertEquals(24, renegociado.getTurnosRestantes());
        assertTrue(renegociado.getTaxaMensal() > taxaAntes, "alongar o prazo custa taxa maior");
        assertEquals(saldoAntes * 1.01, renegociado.getSaldoDevedor(), 0.01);
        assertEquals(Financiamento.Status.RENEGOCIADO, renegociado.getStatus());
        assertThrows(RegraDeNegocioException.class,
                () -> servicoCredito.renegociar(contrato.getId(), jogadorId, 12));
    }

    @Test
    @DisplayName("Sem caixa a parcela atrasa: saldo cresce com multa e mora, e a reputacao cai")
    void parcelaSemCaixaViraAtraso() {
        Empresa empresa = fundar(4_000_000);
        Long jogadorId = empresa.getDono().getId();
        Financiamento contrato = servicoCredito.contratar(empresa.getId(), jogadorId,
                Financiamento.Modalidade.CAPITAL_DE_GIRO, 300_000, 3);

        // Queima o caixa em admissoes: sobra menos do que a proxima parcela.
        Empresa comCredito = servicoEmpresa.buscar(empresa.getId());
        int admissoes = (int) (comCredito.getCaixa() / (comCredito.getSalarioMedio() * 0.5)) - 1;
        servicoEmpresa.contratar(empresa.getId(), jogadorId, admissoes, null);

        Empresa semCaixa = servicoEmpresa.buscar(empresa.getId());
        double saldoAntes = servicoCredito.buscar(contrato.getId()).getSaldoDevedor();
        double reputacaoAntes = semCaixa.getReputacao();

        Map<String, Object> resumo = servicoCredito.cobrarParcelas(semCaixa, 99);

        Financiamento apos = servicoCredito.buscar(contrato.getId());
        assertEquals(1, (int) resumo.get("contratosEmAtraso"));
        assertEquals(1, apos.getParcelasEmAtraso());
        assertEquals(Financiamento.Status.INADIMPLENTE, apos.getStatus());
        assertTrue(apos.getSaldoDevedor() > saldoAntes, "multa e mora entram no saldo devedor");
        assertTrue(servicoEmpresa.buscar(empresa.getId()).getReputacao() < reputacaoAntes,
                "atraso queima reputacao");
    }

    // ------------------------------------------------------------------

    private Empresa fundar(double capital) {
        Jogador jogador = servicoJogador.registrar("credito" + sufixo(), "Jogador credito", "senha123");
        Municipio municipio = servicoPolitica.listarMunicipios().get(0);
        return servicoEmpresa.fundar(jogador.getId(), "Credito " + sufixo(), Setor.ALIMENTICIO,
                municipio.getId(), capital, 30);
    }

    private String sufixo() {
        return UUID.randomUUID().toString().substring(0, 8);
    }
}
