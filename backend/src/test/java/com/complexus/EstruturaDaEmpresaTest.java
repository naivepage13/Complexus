package com.complexus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.complexus.comum.RegraDeNegocioException;
import com.complexus.core.ServicoTurno;
import com.complexus.economia.Departamento;
import com.complexus.economia.Empresa;
import com.complexus.economia.LinhaProduto;
import com.complexus.economia.ServicoEmpresa;
import com.complexus.economia.ServicoEstrutura;
import com.complexus.economia.Setor;
import com.complexus.economia.Unidade;
import com.complexus.jogador.Jogador;
import com.complexus.jogador.ServicoJogador;
import com.complexus.politica.Municipio;
import com.complexus.politica.ServicoPolitica;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Testes da estrutura interna: unidades, linhas de produto e departamentos.
 *
 * O que estes testes travam e a promessa central da estrutura - os totais da
 * empresa sao sempre a soma das unidades, e nenhuma decisao de estrutura muda o
 * balanco pelas costas do jogador.
 */
@SpringBootTest
class EstruturaDaEmpresaTest {

    @Autowired
    private ServicoEmpresa servicoEmpresa;
    @Autowired
    private ServicoEstrutura servicoEstrutura;
    @Autowired
    private ServicoJogador servicoJogador;
    @Autowired
    private ServicoPolitica servicoPolitica;
    @Autowired
    private ServicoTurno servicoTurno;

    @Test
    @DisplayName("A empresa fundada nasce com uma sede que carrega todo o patrimonio")
    void fundacaoCriaSede() {
        Empresa empresa = fundar(Setor.ALIMENTICIO, 600_000, 20, 0);

        List<Unidade> unidades = servicoEstrutura.unidades(empresa.getId());
        assertEquals(1, unidades.size());
        Unidade sede = unidades.get(0);
        assertTrue(sede.isSede());
        assertEquals(empresa.getPatrimonio(), sede.getPatrimonio(), 0.01);
        assertEquals(empresa.getFuncionarios(), sede.getFuncionarios());
    }

    @Test
    @DisplayName("Abrir filial debita o caixa e soma no total da empresa")
    void abrirFilialSomaNoTotal() {
        Empresa empresa = fundar(Setor.ALIMENTICIO, 2_000_000, 40, 0);
        Municipio outra = municipio(1);
        double caixaAntes = empresa.getCaixa();
        double patrimonioAntes = empresa.getPatrimonio();

        servicoEstrutura.abrirUnidade(empresa.getId(), empresa.getDono().getId(), outra.getId(),
                "Filial " + outra.getNome(), 200_000, 8);

        Empresa atual = servicoEmpresa.buscar(empresa.getId());
        assertEquals(2, servicoEstrutura.unidades(empresa.getId()).size());
        assertEquals(patrimonioAntes + 200_000, atual.getPatrimonio(), 0.01);
        assertEquals(48, atual.getFuncionarios());
        // Capital, instalacao e admissoes saem do caixa de uma vez.
        assertTrue(atual.getCaixa() < caixaAntes - 200_000);
    }

    @Test
    @DisplayName("A ultima unidade nao pode ser fechada")
    void ultimaUnidadeNaoFecha() {
        Empresa empresa = fundar(Setor.ALIMENTICIO, 500_000, 10, 0);
        Unidade sede = servicoEstrutura.sede(empresa.getId());

        assertThrows(RegraDeNegocioException.class,
                () -> servicoEstrutura.fecharUnidade(sede.getId(), empresa.getDono().getId()));
    }

    @Test
    @DisplayName("Fechar filial liquida os ativos com desagio e devolve o caixa")
    void fecharFilialLiquidaComDesagio() {
        Empresa empresa = fundar(Setor.ALIMENTICIO, 2_000_000, 40, 0);
        Long jogadorId = empresa.getDono().getId();
        Unidade filial = servicoEstrutura.abrirUnidade(empresa.getId(), jogadorId,
                municipio(1).getId(), "Filial", 200_000, 5);
        double caixaAntes = servicoEmpresa.buscar(empresa.getId()).getCaixa();

        servicoEstrutura.fecharUnidade(filial.getId(), jogadorId);

        Empresa atual = servicoEmpresa.buscar(empresa.getId());
        assertEquals(1, servicoEstrutura.unidades(empresa.getId()).size());
        // 70 por cento dos ativos voltam, menos a rescisao: nunca o valor cheio.
        assertTrue(atual.getCaixa() > caixaAntes, "a liquidacao devolve caixa");
        assertTrue(atual.getCaixa() < caixaAntes + 200_000, "a liquidacao tem desagio");
        assertEquals(40, atual.getFuncionarios());
    }

    @Test
    @DisplayName("O mix de linhas nao passa de 100 por cento")
    void mixLimitadoACem() {
        Empresa empresa = fundar(Setor.ALIMENTICIO, 500_000, 10, 0);
        Long jogadorId = empresa.getDono().getId();
        servicoEstrutura.criarLinha(empresa.getId(), jogadorId, "Basica",
                LinhaProduto.Posicionamento.POPULAR, 0.7);

        assertThrows(RegraDeNegocioException.class, () -> servicoEstrutura.criarLinha(
                empresa.getId(), jogadorId, "Especial", LinhaProduto.Posicionamento.PREMIUM, 0.5));
    }

    @Test
    @DisplayName("A fatia nao declarada do mix fica no posicionamento medio")
    void fatiaNaoDeclaradaEhNeutra() {
        Empresa empresa = fundar(Setor.ALIMENTICIO, 500_000, 10, 0);
        servicoEstrutura.criarLinha(empresa.getId(), empresa.getDono().getId(), "Selecao",
                LinhaProduto.Posicionamento.PREMIUM, 0.5);

        double fatorPreco = servicoEstrutura.fatorPrecoDoMix(servicoEstrutura.linhas(empresa.getId()));

        // Metade premium (1,30) e metade no padrao (1,00) dao 1,15.
        assertEquals(1.15, fatorPreco, 0.0001);
    }

    @Test
    @DisplayName("O efeito de um departamento satura conforme o porte da empresa")
    void efeitoDeDepartamentoSatura() {
        Empresa empresa = fundar(Setor.ALIMENTICIO, 1_000_000, 20, 0);
        Long jogadorId = empresa.getDono().getId();
        double referencia = servicoEstrutura.referenciaDePorte(empresa);

        servicoEstrutura.definirDepartamento(empresa.getId(), jogadorId,
                Departamento.Area.PESQUISA, referencia);
        double comOrcamentoIgualAoPorte = servicoEstrutura.efeitoDepartamento(
                servicoEstrutura.departamentos(empresa.getId()), Departamento.Area.PESQUISA, empresa);

        servicoEstrutura.definirDepartamento(empresa.getId(), jogadorId,
                Departamento.Area.PESQUISA, referencia * 10);
        double comOrcamentoDezVezesMaior = servicoEstrutura.efeitoDepartamento(
                servicoEstrutura.departamentos(empresa.getId()), Departamento.Area.PESQUISA, empresa);

        assertEquals(Departamento.Area.PESQUISA.getEfeitoMaximo() / 2, comOrcamentoIgualAoPorte, 0.0001);
        assertTrue(comOrcamentoDezVezesMaior < Departamento.Area.PESQUISA.getEfeitoMaximo(),
                "o efeito nunca alcanca o teto");
        assertTrue(comOrcamentoDezVezesMaior < comOrcamentoIgualAoPorte * 2,
                "dobrar o orcamento nao dobra o efeito");
    }

    @Test
    @DisplayName("No turno cada unidade opera na sua cidade e a empresa fecha uma vez so")
    void turnoSomaAsUnidadesUmaVez() {
        Empresa empresa = fundar(Setor.ALIMENTICIO, 3_000_000, 60, 0);
        servicoEstrutura.abrirUnidade(empresa.getId(), empresa.getDono().getId(),
                municipio(1).getId(), "Filial", 400_000, 15);

        servicoTurno.processarTurno("TESTE");

        Empresa atual = servicoEmpresa.buscar(empresa.getId());
        List<Unidade> unidades = servicoEstrutura.unidades(empresa.getId());
        double receitaDasUnidades = unidades.stream().mapToDouble(Unidade::getReceitaMensal).sum();

        assertEquals(2, unidades.size());
        assertTrue(atual.getReceitaMensal() > 0, "a empresa deveria faturar no primeiro turno");
        assertEquals(receitaDasUnidades, atual.getReceitaMensal(), 0.01,
                "a receita da empresa e exatamente a soma das unidades");
        assertFalse(unidades.stream().anyMatch(unidade -> unidade.getMarketShare() > 1.0),
                "nenhuma unidade pode ter mais de 100 por cento do mercado");
    }

    @Test
    @DisplayName("Departamento entra como custo mensal no fechamento do turno")
    void departamentoPesaNoResultado() {
        Empresa empresa = fundar(Setor.ALIMENTICIO, 1_500_000, 30, 0);
        Long jogadorId = empresa.getDono().getId();
        servicoTurno.processarTurno("TESTE");
        double custoSemEstrutura = servicoEmpresa.buscar(empresa.getId()).getCustoMensal();

        double orcamento = servicoEstrutura.referenciaDePorte(servicoEmpresa.buscar(empresa.getId()));
        servicoEstrutura.definirDepartamento(empresa.getId(), jogadorId,
                Departamento.Area.QUALIDADE, orcamento);
        servicoTurno.processarTurno("TESTE");

        double custoComEstrutura = servicoEmpresa.buscar(empresa.getId()).getCustoMensal();
        assertTrue(custoComEstrutura > custoSemEstrutura,
                "o orcamento do departamento precisa aparecer no custo do turno");
    }

    // ------------------------------------------------------------------

    private Empresa fundar(Setor setor, double capital, int funcionarios, int indiceMunicipio) {
        Jogador jogador = servicoJogador.registrar("estrutura" + sufixo(), "Jogador estrutura", "senha123");
        return servicoEmpresa.fundar(jogador.getId(), "Estrutura " + sufixo(), setor,
                municipio(indiceMunicipio).getId(), capital, funcionarios);
    }

    private Municipio municipio(int indice) {
        return servicoPolitica.listarMunicipios().get(indice);
    }

    private String sufixo() {
        return UUID.randomUUID().toString().substring(0, 8);
    }
}
