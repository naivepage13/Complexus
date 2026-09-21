package com.complexus.config;

import com.complexus.auditoria.ServicoAuditoria;
import com.complexus.core.ServicoEstadoJogo;
import com.complexus.economia.Empresa;
import com.complexus.economia.RepositorioEmpresa;
import com.complexus.economia.RepositorioUnidade;
import com.complexus.economia.ServicoEstrutura;
import com.complexus.economia.Setor;
import com.complexus.jogador.RepositorioJogador;
import com.complexus.jogador.ServicoJogador;
import com.complexus.politica.CargoPolitico;
import com.complexus.politica.Esfera;
import com.complexus.politica.Estado;
import com.complexus.politica.Municipio;
import com.complexus.politica.Pais;
import com.complexus.politica.RepositorioEstado;
import com.complexus.politica.RepositorioMunicipio;
import com.complexus.politica.RepositorioPais;
import com.complexus.politica.ServicoPolitica;
import com.complexus.politica.VagasCargo;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.ApplicationArguments;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Carga inicial do mundo: um pais, tres estados, quatro municipios, as cadeiras
 * politicas ocupadas por NPCs e empresas concorrentes em cada setor.
 *
 * A carga e idempotente: so roda quando o banco esta vazio, o que permite
 * reiniciar a aplicacao sem duplicar o mundo.
 */
@Component
public class SeedDados implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(SeedDados.class);

    private final RepositorioPais repositorioPais;
    private final RepositorioEstado repositorioEstado;
    private final RepositorioMunicipio repositorioMunicipio;
    private final RepositorioEmpresa repositorioEmpresa;
    private final RepositorioJogador repositorioJogador;
    private final RepositorioUnidade repositorioUnidade;
    private final ServicoEstrutura servicoEstrutura;
    private final ServicoPolitica servicoPolitica;
    private final ServicoJogador servicoJogador;
    private final ServicoEstadoJogo estadoJogo;
    private final ServicoAuditoria auditoria;

    public SeedDados(RepositorioPais repositorioPais,
                     RepositorioEstado repositorioEstado,
                     RepositorioMunicipio repositorioMunicipio,
                     RepositorioEmpresa repositorioEmpresa,
                     RepositorioJogador repositorioJogador,
                     RepositorioUnidade repositorioUnidade,
                     ServicoEstrutura servicoEstrutura,
                     ServicoPolitica servicoPolitica,
                     ServicoJogador servicoJogador,
                     ServicoEstadoJogo estadoJogo,
                     ServicoAuditoria auditoria) {
        this.repositorioPais = repositorioPais;
        this.repositorioEstado = repositorioEstado;
        this.repositorioMunicipio = repositorioMunicipio;
        this.repositorioEmpresa = repositorioEmpresa;
        this.repositorioJogador = repositorioJogador;
        this.repositorioUnidade = repositorioUnidade;
        this.servicoEstrutura = servicoEstrutura;
        this.servicoPolitica = servicoPolitica;
        this.servicoJogador = servicoJogador;
        this.estadoJogo = estadoJogo;
        this.auditoria = auditoria;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        estadoJogo.obter();
        abrirSedesFaltantes();
        if (repositorioPais.count() > 0) {
            log.info("Mundo ja inicializado; carga inicial ignorada.");
            return;
        }
        log.info("Inicializando o mundo do Complexus...");

        Pais brasil = criarPais();
        Estado sp = criarEstado(brasil, "Sao Paulo", "SP", 44_000_000L, 4_000_000_000.0, 72);
        Estado rj = criarEstado(brasil, "Rio de Janeiro", "RJ", 17_000_000L, 1_600_000_000.0, 64);
        Estado mg = criarEstado(brasil, "Minas Gerais", "MG", 21_000_000L, 1_800_000_000.0, 60);

        Municipio saoPaulo = criarMunicipio(sp, "Sao Paulo", 11_400_000L, 900_000_000.0, 82, 130, 9_500);
        Municipio campinas = criarMunicipio(sp, "Campinas", 1_200_000L, 180_000_000.0, 68, 110, 4_200);
        Municipio rioDeJaneiro = criarMunicipio(rj, "Rio de Janeiro", 6_700_000L, 520_000_000.0, 76, 118, 7_800);
        Municipio beloHorizonte = criarMunicipio(mg, "Belo Horizonte", 2_500_000L, 260_000_000.0, 70, 105, 5_100);

        criarCadeirasPoliticas(brasil, List.of(sp, rj, mg),
                List.of(saoPaulo, campinas, rioDeJaneiro, beloHorizonte));

        criarEmpresaNpc("Mesa Farta Alimentos", Setor.ALIMENTICIO, saoPaulo, 4_500_000.0);
        criarEmpresaNpc("Sabor do Vale", Setor.ALIMENTICIO, campinas, 1_800_000.0);
        criarEmpresaNpc("Cozinha Carioca", Setor.ALIMENTICIO, rioDeJaneiro, 2_400_000.0);
        criarEmpresaNpc("Horizonte Incorporadora", Setor.IMOBILIARIO, beloHorizonte, 9_000_000.0);
        criarEmpresaNpc("Paulista Empreendimentos", Setor.IMOBILIARIO, saoPaulo, 15_000_000.0);
        criarEmpresaNpc("Construtora Bandeirante", Setor.CONSTRUCAO, saoPaulo, 12_000_000.0);
        criarEmpresaNpc("Guanabara Obras", Setor.CONSTRUCAO, rioDeJaneiro, 7_500_000.0);

        if (!repositorioJogador.existsByUsuario("demo")) {
            servicoJogador.registrar("demo", "Jogador Demonstracao", "demo1234");
        }

        auditoria.registrarSistema("MUNDO_INICIALIZADO", "Pais", brasil.getId(),
                "Carga inicial do mundo concluida",
                Map.of("estados", 3, "municipios", 4, "empresasNpc", 7));
        log.info("Mundo inicializado com sucesso.");
    }

    /**
     * Da uma sede as empresas criadas antes das unidades existirem.
     *
     * A partida em andamento continua de onde parou: o patrimonio e a equipe que
     * estavam soltos na empresa passam a morar na unidade sede, sem mudar nenhum
     * numero do balanco.
     */
    private void abrirSedesFaltantes() {
        int migradas = 0;
        for (Empresa empresa : repositorioEmpresa.findAll()) {
            if (!repositorioUnidade.findByEmpresaIdOrderByIdAsc(empresa.getId()).isEmpty()) {
                continue;
            }
            servicoEstrutura.criarSede(empresa, empresa.getPatrimonio(), empresa.getFuncionarios(),
                    empresa.getTurnoFundacao());
            migradas++;
        }
        if (migradas > 0) {
            log.info("Sede criada para {} empresas anteriores as unidades.", migradas);
            auditoria.registrarSistema("ESTRUTURA_MIGRADA", "Empresa", null,
                    "Empresas existentes receberam unidade sede", Map.of("empresas", migradas));
        }
    }

    private Pais criarPais() {
        Pais pais = new Pais();
        pais.setNome("Brasil");
        pais.setSigla("BRA");
        pais.setPopulacao(203_000_000L);
        pais.setTesouro(120_000_000_000.0);
        pais.setPib(10_900_000_000_000.0);
        pais.setAliquotaImpostoEmpresarial(0.15);
        pais.setGastoSocialMensal(0.0);
        pais.setEstabilidade(75);
        pais.setAprovacaoGoverno(50);
        pais.setDesemprego(0.08);
        pais.setRendaMedia(3_200.0);
        return repositorioPais.save(pais);
    }

    private Estado criarEstado(Pais pais, String nome, String sigla, long populacao,
                               double tesouro, double desenvolvimento) {
        Estado estado = new Estado();
        estado.setPais(pais);
        estado.setNome(nome);
        estado.setSigla(sigla);
        estado.setPopulacao(populacao);
        estado.setTesouro(tesouro);
        estado.setAliquotaEstadual(0.12);
        estado.setInvestimentoInfraestrutura(0.0);
        estado.setIndiceDesenvolvimento(desenvolvimento);
        return repositorioEstado.save(estado);
    }

    private Municipio criarMunicipio(Estado estado, String nome, long populacao, double tesouro,
                                     double urbanizacao, double demanda, double custoTerreno) {
        Municipio municipio = new Municipio();
        municipio.setEstado(estado);
        municipio.setNome(nome);
        municipio.setPopulacao(populacao);
        municipio.setTesouro(tesouro);
        municipio.setAliquotaMunicipal(0.05);
        municipio.setIndiceUrbanizacao(urbanizacao);
        municipio.setDemandaImobiliaria(demanda);
        municipio.setCustoTerrenoM2(custoTerreno);
        municipio.setZoneamento(50);
        return repositorioMunicipio.save(municipio);
    }

    /** Ocupa todas as cadeiras com NPCs para que o jogo politico funcione desde o turno zero. */
    private void criarCadeirasPoliticas(Pais pais, List<Estado> estados, List<Municipio> municipios) {
        for (CargoPolitico cargo : CargoPolitico.values()) {
            if (cargo.getEsfera() != Esfera.FEDERAL) {
                continue;
            }
            preencher(cargo, pais.getId());
        }
        for (Estado estado : estados) {
            for (CargoPolitico cargo : CargoPolitico.values()) {
                if (cargo.getEsfera() == Esfera.ESTADUAL) {
                    preencher(cargo, estado.getId());
                }
            }
        }
        for (Municipio municipio : municipios) {
            for (CargoPolitico cargo : CargoPolitico.values()) {
                if (cargo.getEsfera() == Esfera.MUNICIPAL) {
                    preencher(cargo, municipio.getId());
                }
            }
        }
    }

    /**
     * Ocupa as cadeiras com NPCs, deixando parte do legislativo vaga.
     *
     * Sao essas vagas que permitem a um jogador entrar na politica sem precisar
     * derrubar um mandatario. O indice tambem escalona o fim dos mandatos e
     * serve de semente para o nome e o partido do NPC.
     */
    private void preencher(CargoPolitico cargo, Long territorioId) {
        int vagas = VagasCargo.vagas(cargo);
        int reservadasParaJogadores = cargo.isLegislativo() ? Math.max(1, vagas / 3) : 0;
        for (int i = 0; i < vagas - reservadasParaJogadores; i++) {
            servicoPolitica.criarMandatoNpc(cargo, territorioId, i);
        }
    }

    /**
     * Empresas concorrentes controladas pelo sistema. Elas dao mercado ao
     * jogador desde o primeiro turno e servem de referencia de preco.
     */
    private void criarEmpresaNpc(String nome, Setor setor, Municipio municipio, double capital) {
        double patrimonio = capital * 0.7;
        Empresa empresa = new Empresa();
        empresa.setNome(nome);
        empresa.setSetor(setor);
        empresa.setMunicipio(municipio);
        empresa.setDono(null);
        empresa.setTurnoFundacao(0);
        empresa.setPatrimonio(patrimonio);
        empresa.setCaixa(capital * 0.3);
        // A equipe acompanha o patrimonio: assim a concorrente nasce equilibrada
        // mesmo quando os parametros do setor mudam.
        empresa.setFuncionarios(setor.equipeSugerida(patrimonio));
        empresa.setValuation(capital);
        empresa.setPrecoAcao(capital / empresa.getAcoesTotais());
        empresa.setReputacao(55);
        // Marketing calibrado como 1,5% da receita potencial, nao do capital:
        // e o que uma concorrente saudavel gastaria para sustentar a marca.
        empresa.setMarketingMensal(patrimonio * setor.getGiroAtivoMensal() * 0.015);
        Empresa salva = repositorioEmpresa.save(empresa);
        servicoEstrutura.criarSede(salva, salva.getPatrimonio(), salva.getFuncionarios(), 0);
    }
}
