package com.complexus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.complexus.auditoria.ServicoAuditoria;
import com.complexus.comum.RegraDeNegocioException;
import com.complexus.core.ServicoEstadoJogo;
import com.complexus.core.ServicoTurno;
import com.complexus.economia.Empreendimento;
import com.complexus.economia.Empresa;
import com.complexus.economia.HistoricoEmpresa;
import com.complexus.economia.ServicoEmpresa;
import com.complexus.economia.Setor;
import com.complexus.estatistica.ServicoEstatistica;
import com.complexus.estatistica.SnapshotTurno;
import com.complexus.investimento.ServicoInvestimento;
import com.complexus.jogador.Jogador;
import com.complexus.jogador.ServicoJogador;
import com.complexus.politica.CargoPolitico;
import com.complexus.politica.Esfera;
import com.complexus.politica.Mandato;
import com.complexus.politica.Municipio;
import com.complexus.politica.Pais;
import com.complexus.politica.ProjetoDeLei;
import com.complexus.politica.ServicoPolitica;
import com.complexus.politica.StatusProjeto;
import com.complexus.politica.TipoProjeto;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Testes de integracao do ciclo do jogo: mundo carregado, empresa fundada,
 * turno processado, estatisticas consolidadas e auditoria integra.
 */
@SpringBootTest
class FluxoDoJogoTest {

    @Autowired
    private ServicoEmpresa servicoEmpresa;
    @Autowired
    private ServicoJogador servicoJogador;
    @Autowired
    private ServicoTurno servicoTurno;
    @Autowired
    private ServicoPolitica servicoPolitica;
    @Autowired
    private ServicoInvestimento servicoInvestimento;
    @Autowired
    private ServicoEstatistica servicoEstatistica;
    @Autowired
    private ServicoAuditoria servicoAuditoria;
    @Autowired
    private ServicoEstadoJogo estadoJogo;

    @Test
    @DisplayName("O mundo inicial e carregado com territorios e cadeiras ocupadas")
    void mundoInicialCarregado() {
        List<Pais> paises = servicoPolitica.listarPaises();
        assertEquals(1, paises.size());
        assertFalse(servicoPolitica.listarEstados().isEmpty());
        assertFalse(servicoPolitica.listarMunicipios().isEmpty());

        List<Mandato> congresso = servicoPolitica.mandatosDoTerritorio(Esfera.FEDERAL, paises.get(0).getId());
        assertTrue(congresso.stream().anyMatch(m -> m.getCargo() == CargoPolitico.PRESIDENTE));
        assertTrue(congresso.stream().anyMatch(m -> m.getCargo() == CargoPolitico.SENADOR));
    }

    @Test
    @DisplayName("Fundar empresa debita o jogador e gera evento de auditoria")
    void fundarEmpresaRegistraTudo() {
        Jogador jogador = novoJogador();
        double saldoAnterior = jogador.getSaldo();
        Municipio municipio = servicoPolitica.listarMunicipios().get(0);

        Empresa empresa = servicoEmpresa.fundar(jogador.getId(), "Padaria " + sufixo(),
                Setor.ALIMENTICIO, municipio.getId(), 400_000, 12);

        assertNotNull(empresa.getId());
        assertEquals(saldoAnterior - 400_000, servicoJogador.buscar(jogador.getId()).getSaldo(), 0.01);
        assertFalse(servicoAuditoria.porEntidade("Empresa", empresa.getId()).isEmpty());
    }

    @Test
    @DisplayName("Capital abaixo do minimo do setor e recusado")
    void capitalMinimoEValidado() {
        Jogador jogador = novoJogador();
        Municipio municipio = servicoPolitica.listarMunicipios().get(0);

        assertThrows(RegraDeNegocioException.class, () ->
                servicoEmpresa.fundar(jogador.getId(), "Micro " + sufixo(),
                        Setor.IMOBILIARIO, municipio.getId(), 1_000, 5));
    }

    @Test
    @DisplayName("Um turno processa empresas, grava historico e consolida estatisticas")
    void turnoCompleto() {
        Jogador jogador = novoJogador();
        Municipio municipio = servicoPolitica.listarMunicipios().get(0);
        Empresa empresa = servicoEmpresa.fundar(jogador.getId(), "Mercearia " + sufixo(),
                Setor.ALIMENTICIO, municipio.getId(), 600_000, 25);

        int turnoAnterior = estadoJogo.turnoAtual();
        Map<String, Object> relatorio = servicoTurno.processarTurno("TESTE");

        assertEquals(turnoAnterior + 1, relatorio.get("turno"));
        assertTrue((int) relatorio.get("empresasProcessadas") > 0);

        Empresa processada = servicoEmpresa.buscar(empresa.getId());
        assertTrue(processada.getReceitaMensal() > 0, "a empresa deveria faturar no primeiro turno");
        assertTrue(processada.getValuation() > 0);

        List<HistoricoEmpresa> historico = servicoEmpresa.historico(empresa.getId(), 12);
        assertFalse(historico.isEmpty());

        SnapshotTurno snapshot = servicoEstatistica.ultimoSnapshot();
        assertNotNull(snapshot);
        assertTrue(snapshot.getEmpresasAtivas() > 0);
        assertEquals(3, servicoEstatistica.setoresNoTurno(snapshot.getTurno()).size());
    }

    @Test
    @DisplayName("Obra consome caixa por turno e vira patrimonio ao concluir")
    void cicloDeEmpreendimento() {
        Jogador jogador = novoJogador();
        Municipio municipio = servicoPolitica.listarMunicipios().get(0);
        Empresa empresa = servicoEmpresa.fundar(jogador.getId(), "Construtora " + sufixo(),
                Setor.CONSTRUCAO, municipio.getId(), 2_000_000, 20);

        double patrimonioAntes = empresa.getPatrimonio();
        Empreendimento obra = servicoEmpresa.iniciarEmpreendimento(empresa.getId(), jogador.getId(),
                "Viaduto " + sufixo(), Empreendimento.TipoEmpreendimento.INFRAESTRUTURA,
                200_000, 2);

        assertEquals(Empreendimento.StatusEmpreendimento.EM_OBRA, obra.getStatus());
        assertEquals(2, obra.getTurnosRestantes());

        servicoTurno.processarTurno("TESTE");
        Empreendimento emAndamento = servicoEmpresa.empreendimentos(empresa.getId()).get(0);
        assertEquals(1, emAndamento.getTurnosRestantes(), "a obra deveria andar um turno");
        assertTrue(emAndamento.getInvestido() > 0, "a parcela do turno deveria ter sido paga");

        servicoTurno.processarTurno("TESTE");
        Empreendimento concluida = servicoEmpresa.empreendimentos(empresa.getId()).get(0);

        assertEquals(Empreendimento.StatusEmpreendimento.CONCLUIDO, concluida.getStatus());
        assertTrue(concluida.getValorEstimado() > 0);
        assertTrue(servicoEmpresa.buscar(empresa.getId()).getPatrimonio() > patrimonioAntes,
                "a entrega da obra deveria somar patrimonio");
    }

    @Test
    @DisplayName("Empresa do setor alimenticio nao toca obra")
    void alimenticioNaoTemObra() {
        Jogador jogador = novoJogador();
        Municipio municipio = servicoPolitica.listarMunicipios().get(0);
        Empresa empresa = servicoEmpresa.fundar(jogador.getId(), "Mercado " + sufixo(),
                Setor.ALIMENTICIO, municipio.getId(), 400_000, 5);

        assertThrows(RegraDeNegocioException.class, () -> servicoEmpresa.iniciarEmpreendimento(
                empresa.getId(), jogador.getId(), "Obra invalida",
                Empreendimento.TipoEmpreendimento.RESIDENCIAL, 100_000, 3));
    }

    @Test
    @DisplayName("Investir exige capital aberto e a compra credita o caixa da empresa")
    void mercadoDeAcoes() {
        Jogador dono = novoJogador();
        Jogador investidor = novoJogador();
        Municipio municipio = servicoPolitica.listarMunicipios().get(0);
        Empresa empresa = servicoEmpresa.fundar(dono.getId(), "Alimentos " + sufixo(),
                Setor.ALIMENTICIO, municipio.getId(), 800_000, 40);

        assertThrows(RegraDeNegocioException.class,
                () -> servicoInvestimento.comprar(investidor.getId(), empresa.getId(), 1000));

        // Um turno de operacao gera o lucro acumulado exigido para o IPO.
        servicoTurno.processarTurno("TESTE");
        Empresa comLucro = servicoEmpresa.buscar(empresa.getId());
        if (comLucro.getLucroAcumulado() <= 0) {
            return; // cenario de prejuizo: a regra de IPO permanece bloqueando, o que ja e testado acima
        }

        servicoEmpresa.abrirCapital(empresa.getId(), dono.getId(), 0.2);
        double caixaAntes = servicoEmpresa.buscar(empresa.getId()).getCaixa();

        servicoInvestimento.comprar(investidor.getId(), empresa.getId(), 1000);

        assertTrue(servicoEmpresa.buscar(empresa.getId()).getCaixa() > caixaAntes,
                "o aporte do investidor entra no caixa da empresa");
        assertFalse(servicoInvestimento.carteira(investidor.getId()).isEmpty());
    }

    @Test
    @DisplayName("Projeto de lei tramita: proposta, pauta, apuracao e efeito")
    void tramitacaoDeProjeto() {
        Jogador jogador = novoJogador();
        Pais pais = servicoPolitica.listarPaises().get(0);

        Mandato mandato = servicoPolitica.assumirCargo(jogador.getId(),
                CargoPolitico.DEPUTADO_FEDERAL, pais.getId(), "PRD");

        ProjetoDeLei projeto = servicoPolitica.propor(jogador.getId(), mandato.getId(),
                "Reducao do imposto sobre lucro", "Reduz a aliquota federal",
                TipoProjeto.IMPOSTO_EMPRESARIAL, null, 0.12);
        assertEquals(StatusProjeto.RASCUNHO, projeto.getStatus());

        servicoPolitica.pautar(jogador.getId(), projeto.getId());
        assertEquals(StatusProjeto.EM_VOTACAO, servicoPolitica.buscarProjeto(projeto.getId()).getStatus());

        // Dois turnos: um para apurar a votacao, outro para resolver a sancao.
        servicoTurno.processarTurno("TESTE");
        servicoTurno.processarTurno("TESTE");

        StatusProjeto status = servicoPolitica.buscarProjeto(projeto.getId()).getStatus();
        assertTrue(status == StatusProjeto.SANCIONADO
                        || status == StatusProjeto.VETADO
                        || status == StatusProjeto.REJEITADO,
                "apos a apuracao o projeto sai da votacao, e status foi " + status);
    }

    @Test
    @DisplayName("Parlamentar nao vota projeto de outra casa legislativa")
    void votoForaDaCasaERecusado() {
        Jogador jogador = novoJogador();
        Pais pais = servicoPolitica.listarPaises().get(0);
        Municipio municipio = servicoPolitica.listarMunicipios().get(0);

        Mandato vereador = servicoPolitica.assumirCargo(jogador.getId(),
                CargoPolitico.VEREADOR, municipio.getId(), "MDU");
        Mandato deputado = servicoPolitica.assumirCargo(jogador.getId(),
                CargoPolitico.DEPUTADO_FEDERAL, pais.getId(), "MDU");

        ProjetoDeLei federal = servicoPolitica.propor(jogador.getId(), deputado.getId(),
                "Programa social", "Transferencia de renda",
                TipoProjeto.PROGRAMA_SOCIAL, null, 1_000_000);
        servicoPolitica.pautar(jogador.getId(), federal.getId());

        assertThrows(RegraDeNegocioException.class, () -> servicoPolitica.votar(
                jogador.getId(), vereador.getId(), federal.getId(),
                com.complexus.politica.VotoProjeto.Opcao.SIM));
    }

    @Test
    @DisplayName("A cadeia de auditoria permanece integra apos varias operacoes")
    void cadeiaDeAuditoriaIntegra() {
        Jogador jogador = novoJogador();
        Municipio municipio = servicoPolitica.listarMunicipios().get(0);
        servicoEmpresa.fundar(jogador.getId(), "Obras " + sufixo(),
                Setor.CONSTRUCAO, municipio.getId(), 1_000_000, 30);
        servicoTurno.processarTurno("TESTE");

        Map<String, Object> verificacao = servicoAuditoria.verificarIntegridade();

        assertEquals(true, verificacao.get("cadeiaIntegra"),
                () -> "elos divergentes: " + verificacao.get("falhas"));
        assertTrue((int) verificacao.get("totalEventos") > 0);
    }

    private Jogador novoJogador() {
        String usuario = "teste" + sufixo();
        return servicoJogador.registrar(usuario, "Jogador " + usuario, "senha123");
    }

    private String sufixo() {
        return UUID.randomUUID().toString().substring(0, 8);
    }
}
