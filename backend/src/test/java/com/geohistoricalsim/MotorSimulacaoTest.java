package com.geohistoricalsim;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.geohistoricalsim.economia.ContextoMercado;
import com.geohistoricalsim.economia.Empresa;
import com.geohistoricalsim.economia.ModificadorSetorial;
import com.geohistoricalsim.economia.MotorSimulacao;
import com.geohistoricalsim.economia.ResultadoMensal;
import com.geohistoricalsim.economia.Setor;
import com.geohistoricalsim.politica.Estado;
import com.geohistoricalsim.politica.Municipio;
import com.geohistoricalsim.politica.Pais;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Testes das regras economicas puras, sem contexto Spring. */
class MotorSimulacaoTest {

    private final MotorSimulacao motor = new MotorSimulacao();

    @Test
    @DisplayName("A receita nunca ultrapassa a capacidade instalada")
    void receitaLimitadaPelaCapacidade() {
        Empresa empresa = empresa(Setor.ALIMENTICIO, 2, 1_000_000);
        ResultadoMensal resultado = simular(empresa, 500_000_000, 0.5, 0, 0);

        assertTrue(resultado.receita() <= resultado.capacidade() + 0.001,
                "receita deveria respeitar a capacidade produtiva");
        assertEquals(resultado.capacidade(), resultado.receita(), 0.001,
                "com demanda abundante a empresa vende toda a capacidade");
    }

    @Test
    @DisplayName("Subsidio aumenta o lucro e regulacao reduz")
    void efeitoDeSubsidioEregulacao() {
        Empresa empresa = empresa(Setor.CONSTRUCAO, 200, 20_000_000);
        double lucroNeutro = simular(empresa, 100_000_000, 0.4, 0, 0).lucro();
        double lucroComSubsidio = simular(empresa, 100_000_000, 0.4, 0.10, 0).lucro();
        double lucroComRegulacao = simular(empresa, 100_000_000, 0.4, 0, 0.10).lucro();

        assertTrue(lucroComSubsidio > lucroNeutro, "subsidio deveria elevar o lucro");
        assertTrue(lucroComRegulacao < lucroNeutro, "regulacao deveria reduzir o lucro");
    }

    @Test
    @DisplayName("Valuation nunca fica abaixo do piso de lastro")
    void valuationRespeitaOLastro() {
        Empresa empresa = empresa(Setor.IMOBILIARIO, 50, 10_000_000);
        double valuation = motor.calcularValuation(empresa, -500_000, contexto());

        assertTrue(valuation >= empresa.patrimonioLiquido() * 0.6,
                "mesmo com prejuizo o valor de mercado tem piso patrimonial");
    }

    @Test
    @DisplayName("Lucro maior eleva o valuation")
    void lucroElevaValuation() {
        Empresa empresa = empresa(Setor.ALIMENTICIO, 100, 5_000_000);
        double semLucro = motor.calcularValuation(empresa, 0, contexto());
        double comLucro = motor.calcularValuation(empresa, 400_000, contexto());

        assertTrue(comLucro > semLucro);
    }

    @Test
    @DisplayName("Preco da acao converge gradualmente para o valor justo")
    void precoConvergeSuavemente() {
        Empresa empresa = empresa(Setor.ALIMENTICIO, 100, 5_000_000);
        empresa.setPrecoAcao(10.0);
        double valuation = 20.0 * empresa.getAcoesTotais();

        double preco = motor.calcularPrecoAcao(empresa, valuation);

        assertTrue(preco > 10.0 && preco < 20.0, "o preco deve caminhar sem saltar para o valor justo");
        assertEquals(14.0, preco, 0.001);
    }

    @Test
    @DisplayName("Crescimento lida com base zero e base negativa")
    void crescimentoComBaseDegenerada() {
        assertEquals(1.0, motor.calcularCrescimento(100, 0), 0.0001);
        assertEquals(0.0, motor.calcularCrescimento(0, 0), 0.0001);
        assertEquals(2.0, motor.calcularCrescimento(100, -100), 0.0001);
    }

    @Test
    @DisplayName("Mercado potencial cresce com populacao e renda")
    void mercadoPotencialAcompanhaRenda() {
        Pais pais = pais();
        Estado estado = estado(pais);
        Municipio municipio = municipio(estado);

        double base = motor.mercadoPotencial(Setor.ALIMENTICIO, municipio, estado, pais);
        pais.setRendaMedia(pais.getRendaMedia() * 2);
        double dobro = motor.mercadoPotencial(Setor.ALIMENTICIO, municipio, estado, pais);

        assertTrue(dobro > base * 1.9, "dobrar a renda deveria aproximadamente dobrar o mercado");
    }

    // ------------------------------------------------------------------

    private ResultadoMensal simular(Empresa empresa, double mercado, double participacao,
                                    double subsidio, double regulacao) {
        Pais pais = pais();
        Estado estado = estado(pais);
        Municipio municipio = municipio(estado);
        return motor.simularMes(empresa, mercado, participacao, contexto(), estado, municipio,
                pais, subsidio, regulacao);
    }

    private ContextoMercado contexto() {
        return new ContextoMercado(3200, 3200, 0.1075, 0.045, 75,
                Map.of(Setor.ALIMENTICIO, ModificadorSetorial.neutro(),
                        Setor.IMOBILIARIO, ModificadorSetorial.neutro(),
                        Setor.CONSTRUCAO, ModificadorSetorial.neutro()));
    }

    private Empresa empresa(Setor setor, int funcionarios, double patrimonio) {
        Empresa empresa = new Empresa();
        empresa.setNome("Teste " + setor.name());
        empresa.setSetor(setor);
        empresa.setFuncionarios(funcionarios);
        empresa.setPatrimonio(patrimonio);
        empresa.setCaixa(patrimonio * 0.2);
        empresa.setProdutividade(1.0);
        empresa.setSalarioMedio(2800);
        empresa.setReputacao(50);
        return empresa;
    }

    private Pais pais() {
        Pais pais = new Pais();
        pais.setNome("Brasil");
        pais.setSigla("BRA");
        pais.setPopulacao(203_000_000L);
        pais.setRendaMedia(3200);
        pais.setDesemprego(0.08);
        pais.setEstabilidade(75);
        pais.setAliquotaImpostoEmpresarial(0.15);
        return pais;
    }

    private Estado estado(Pais pais) {
        Estado estado = new Estado();
        estado.setPais(pais);
        estado.setNome("Sao Paulo");
        estado.setSigla("SP");
        estado.setPopulacao(44_000_000L);
        estado.setAliquotaEstadual(0.12);
        estado.setIndiceDesenvolvimento(72);
        return estado;
    }

    private Municipio municipio(Estado estado) {
        Municipio municipio = new Municipio();
        municipio.setEstado(estado);
        municipio.setNome("Sao Paulo");
        municipio.setPopulacao(11_400_000L);
        municipio.setAliquotaMunicipal(0.05);
        municipio.setIndiceUrbanizacao(82);
        municipio.setDemandaImobiliaria(130);
        municipio.setCustoTerrenoM2(9500);
        return municipio;
    }
}
