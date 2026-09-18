package com.geohistoricalsim.economia;

import com.geohistoricalsim.auditoria.ServicoAuditoria;
import com.geohistoricalsim.comum.RecursoNaoEncontradoException;
import com.geohistoricalsim.comum.RegraDeNegocioException;
import com.geohistoricalsim.core.ServicoEstadoJogo;
import com.geohistoricalsim.investimento.ServicoRazao;
import com.geohistoricalsim.investimento.TipoLancamento;
import com.geohistoricalsim.jogador.Jogador;
import com.geohistoricalsim.jogador.ServicoJogador;
import com.geohistoricalsim.politica.Municipio;
import com.geohistoricalsim.politica.RepositorioMunicipio;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Regras de administracao de empresas: fundacao, capital, equipe, marketing,
 * abertura de capital e empreendimentos.
 *
 * Toda operacao que move dinheiro gera um lancamento no razao e um evento na
 * linha de auditoria.
 */
@Service
public class ServicoEmpresa {

    private static final double CAPEX_MINIMO = 10_000.0;
    private static final double GANHO_PRODUTIVIDADE_POR_CAPEX = 0.12;
    private static final double PRODUTIVIDADE_MAXIMA = 2.0;

    private final RepositorioEmpresa repositorio;
    private final RepositorioHistoricoEmpresa repositorioHistorico;
    private final RepositorioEmpreendimento repositorioEmpreendimento;
    private final RepositorioMunicipio repositorioMunicipio;
    private final ServicoJogador servicoJogador;
    private final ServicoRazao razao;
    private final ServicoAuditoria auditoria;
    private final ServicoEstadoJogo estadoJogo;
    private final MotorSimulacao motor;

    public ServicoEmpresa(RepositorioEmpresa repositorio,
                          RepositorioHistoricoEmpresa repositorioHistorico,
                          RepositorioEmpreendimento repositorioEmpreendimento,
                          RepositorioMunicipio repositorioMunicipio,
                          ServicoJogador servicoJogador,
                          ServicoRazao razao,
                          ServicoAuditoria auditoria,
                          ServicoEstadoJogo estadoJogo,
                          MotorSimulacao motor) {
        this.repositorio = repositorio;
        this.repositorioHistorico = repositorioHistorico;
        this.repositorioEmpreendimento = repositorioEmpreendimento;
        this.repositorioMunicipio = repositorioMunicipio;
        this.servicoJogador = servicoJogador;
        this.razao = razao;
        this.auditoria = auditoria;
        this.estadoJogo = estadoJogo;
        this.motor = motor;
    }

    // ------------------------------------------------------------------
    // Consultas
    // ------------------------------------------------------------------

    @Transactional(readOnly = true)
    public List<Empresa> listar() {
        return repositorio.findByAtivaTrue();
    }

    @Transactional(readOnly = true)
    public List<Empresa> listarPorSetor(Setor setor) {
        return repositorio.findBySetorAndAtivaTrue(setor);
    }

    @Transactional(readOnly = true)
    public List<Empresa> listarDoJogador(Long jogadorId) {
        return repositorio.findByDonoIdAndAtivaTrue(jogadorId);
    }

    @Transactional(readOnly = true)
    public List<Empresa> listarNegociaveis() {
        return repositorio.findByCapitalAbertoTrueAndAtivaTrueOrderByValuationDesc();
    }

    @Transactional(readOnly = true)
    public Empresa buscar(Long id) {
        return repositorio.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Empresa", id));
    }

    @Transactional(readOnly = true)
    public List<HistoricoEmpresa> historico(Long empresaId, int limite) {
        List<HistoricoEmpresa> historico = repositorioHistorico.findByEmpresaIdOrderByTurnoDesc(
                empresaId, PageRequest.of(0, Math.min(Math.max(limite, 1), 240)));
        return historico.reversed();
    }

    @Transactional(readOnly = true)
    public List<Empreendimento> empreendimentos(Long empresaId) {
        return repositorioEmpreendimento.findByEmpresaId(empresaId);
    }

    // ------------------------------------------------------------------
    // Comandos
    // ------------------------------------------------------------------

    /**
     * Funda uma empresa. O capital sai do caixa do jogador e vira patrimonio e
     * caixa da empresa, mantendo o lastro do investimento inicial.
     */
    @Transactional
    public Empresa fundar(Long jogadorId, String nome, Setor setor, Long municipioId,
                          double capitalInicial, int funcionarios) {
        Jogador jogador = servicoJogador.buscar(jogadorId);
        Municipio municipio = repositorioMunicipio.findById(municipioId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Municipio", municipioId));

        String nomeLimpo = nome == null ? "" : nome.trim();
        if (nomeLimpo.length() < 3) {
            throw new RegraDeNegocioException("Nome da empresa precisa de ao menos 3 caracteres.");
        }
        if (repositorio.existsByNomeIgnoreCase(nomeLimpo)) {
            throw new RegraDeNegocioException("Ja existe uma empresa com o nome " + nomeLimpo + ".");
        }
        if (capitalInicial < setor.getCapitalMinimo()) {
            throw new RegraDeNegocioException("Capital minimo para o setor " + setor.getRotulo()
                    + " e R$ " + String.format("%.2f", setor.getCapitalMinimo()) + ".");
        }
        if (funcionarios < 1) {
            throw new RegraDeNegocioException("A empresa precisa de ao menos 1 funcionario.");
        }
        servicoJogador.debitar(jogador, capitalInicial, "fundar a empresa " + nomeLimpo);

        int turno = estadoJogo.turnoAtual();
        Empresa empresa = new Empresa();
        empresa.setNome(nomeLimpo);
        empresa.setSetor(setor);
        empresa.setDono(jogador);
        empresa.setMunicipio(municipio);
        empresa.setTurnoFundacao(turno);
        // 70 por cento vira estrutura produtiva, 30 por cento fica em caixa.
        empresa.setPatrimonio(capitalInicial * 0.7);
        empresa.setCaixa(capitalInicial * 0.3);
        empresa.setFuncionarios(funcionarios);
        empresa.setValuation(capitalInicial);
        empresa.setPrecoAcao(capitalInicial / empresa.getAcoesTotais());
        Empresa salva = repositorio.save(empresa);

        razao.registrar(turno, TipoLancamento.APORTE_FUNDACAO, "jogador:" + jogador.getUsuario(),
                "empresa:" + salva.getId(), capitalInicial, salva.getId(), jogador.getId(),
                "Aporte de fundacao da empresa " + nomeLimpo);
        Map<String, Object> detalhes = new LinkedHashMap<>();
        detalhes.put("setor", setor.name());
        detalhes.put("municipio", municipio.getNome());
        detalhes.put("capitalInicial", capitalInicial);
        detalhes.put("funcionarios", funcionarios);
        auditoria.registrar(jogador.getUsuario(), "EMPRESA_FUNDADA", "Empresa", salva.getId(),
                "Empresa " + nomeLimpo + " fundada", detalhes);
        return salva;
    }

    /** Aporte de capital do dono na empresa (vira patrimonio e produtividade). */
    @Transactional
    public Empresa investirCapital(Long empresaId, Long jogadorId, double valor) {
        Empresa empresa = buscar(empresaId);
        Jogador jogador = exigirDono(empresa, jogadorId);
        if (valor < CAPEX_MINIMO) {
            throw new RegraDeNegocioException("O aporte minimo e R$ " + String.format("%.2f", CAPEX_MINIMO) + ".");
        }
        servicoJogador.debitar(jogador, valor, "investir na empresa " + empresa.getNome());

        double patrimonioAnterior = empresa.getPatrimonio();
        empresa.setPatrimonio(patrimonioAnterior + valor);
        // Ganho de produtividade decrescente: aportes grandes rendem menos por real.
        double ganho = GANHO_PRODUTIVIDADE_POR_CAPEX * (valor / Math.max(patrimonioAnterior, valor));
        empresa.setProdutividade(Math.min(empresa.getProdutividade() + ganho, PRODUTIVIDADE_MAXIMA));
        repositorio.save(empresa);

        int turno = estadoJogo.turnoAtual();
        razao.registrar(turno, TipoLancamento.CAPEX, "jogador:" + jogador.getUsuario(),
                "empresa:" + empresa.getId(), valor, empresa.getId(), jogador.getId(),
                "Aporte de capital");
        auditoria.registrar(jogador.getUsuario(), "EMPRESA_CAPEX", "Empresa", empresa.getId(),
                "Aporte de capital na empresa " + empresa.getNome(),
                Map.of("valor", valor, "produtividade", empresa.getProdutividade()));
        return empresa;
    }

    /** Contrata funcionarios, cobrando custo de admissao equivalente a meio salario. */
    @Transactional
    public Empresa contratar(Long empresaId, Long jogadorId, int quantidade) {
        Empresa empresa = buscar(empresaId);
        Jogador jogador = exigirDono(empresa, jogadorId);
        if (quantidade < 1) {
            throw new RegraDeNegocioException("Quantidade de contratacoes deve ser positiva.");
        }
        double custo = quantidade * empresa.getSalarioMedio() * 0.5;
        if (empresa.getCaixa() < custo) {
            throw new RegraDeNegocioException("Caixa insuficiente para admissao. Necessario R$ "
                    + String.format("%.2f", custo));
        }
        empresa.setCaixa(empresa.getCaixa() - custo);
        empresa.setFuncionarios(empresa.getFuncionarios() + quantidade);
        repositorio.save(empresa);

        int turno = estadoJogo.turnoAtual();
        razao.registrar(turno, TipoLancamento.CUSTO_OPERACIONAL, "empresa:" + empresa.getId(),
                "mercado_de_trabalho", custo, empresa.getId(), jogador.getId(),
                "Admissao de " + quantidade + " funcionarios");
        auditoria.registrar(jogador.getUsuario(), "EMPRESA_CONTRATACAO", "Empresa", empresa.getId(),
                "Contratacao de " + quantidade + " funcionarios",
                Map.of("quantidade", quantidade, "custo", custo));
        return empresa;
    }

    /** Demite funcionarios pagando rescisao de um salario por pessoa. */
    @Transactional
    public Empresa demitir(Long empresaId, Long jogadorId, int quantidade) {
        Empresa empresa = buscar(empresaId);
        Jogador jogador = exigirDono(empresa, jogadorId);
        if (quantidade < 1 || quantidade > empresa.getFuncionarios()) {
            throw new RegraDeNegocioException("Quantidade invalida de desligamentos.");
        }
        double custo = quantidade * empresa.getSalarioMedio();
        empresa.setCaixa(empresa.getCaixa() - custo);
        empresa.setFuncionarios(empresa.getFuncionarios() - quantidade);
        // Desligamentos em massa desgastam a reputacao da empresa.
        empresa.setReputacao(Math.max(empresa.getReputacao() - quantidade * 0.05, 0));
        repositorio.save(empresa);

        int turno = estadoJogo.turnoAtual();
        razao.registrar(turno, TipoLancamento.CUSTO_OPERACIONAL, "empresa:" + empresa.getId(),
                "rescisoes", custo, empresa.getId(), jogador.getId(),
                "Desligamento de " + quantidade + " funcionarios");
        auditoria.registrar(jogador.getUsuario(), "EMPRESA_DEMISSAO", "Empresa", empresa.getId(),
                "Desligamento de " + quantidade + " funcionarios",
                Map.of("quantidade", quantidade, "custo", custo));
        return empresa;
    }

    /** Ajusta parametros de gestao: marketing, salario medio e payout. */
    @Transactional
    public Empresa ajustarGestao(Long empresaId, Long jogadorId, Double marketingMensal,
                                 Double salarioMedio, Double payout) {
        Empresa empresa = buscar(empresaId);
        Jogador jogador = exigirDono(empresa, jogadorId);
        Map<String, Object> mudancas = new LinkedHashMap<>();

        if (marketingMensal != null) {
            if (marketingMensal < 0) {
                throw new RegraDeNegocioException("Marketing nao pode ser negativo.");
            }
            empresa.setMarketingMensal(marketingMensal);
            mudancas.put("marketingMensal", marketingMensal);
        }
        if (salarioMedio != null) {
            if (salarioMedio < 1200) {
                throw new RegraDeNegocioException("Salario medio minimo do jogo e R$ 1.200,00.");
            }
            empresa.setSalarioMedio(salarioMedio);
            mudancas.put("salarioMedio", salarioMedio);
        }
        if (payout != null) {
            if (payout < 0 || payout > 0.9) {
                throw new RegraDeNegocioException("Payout deve estar entre 0 e 0,9.");
            }
            empresa.setPayout(payout);
            mudancas.put("payout", payout);
        }
        repositorio.save(empresa);
        auditoria.registrar(jogador.getUsuario(), "EMPRESA_GESTAO_AJUSTADA", "Empresa", empresa.getId(),
                "Parametros de gestao atualizados", mudancas);
        return empresa;
    }

    /**
     * Abre o capital da empresa: parte das acoes passa a ser negociavel pelos
     * demais jogadores. O dono mantem o restante.
     */
    @Transactional
    public Empresa abrirCapital(Long empresaId, Long jogadorId, double fracaoOfertada) {
        Empresa empresa = buscar(empresaId);
        Jogador jogador = exigirDono(empresa, jogadorId);
        if (empresa.isCapitalAberto()) {
            throw new RegraDeNegocioException("A empresa ja tem capital aberto.");
        }
        if (fracaoOfertada < 0.05 || fracaoOfertada > 0.49) {
            throw new RegraDeNegocioException("A oferta publica deve ficar entre 5 e 49 por cento das acoes.");
        }
        if (empresa.getLucroAcumulado() <= 0) {
            throw new RegraDeNegocioException("So empresas com lucro acumulado positivo podem abrir capital.");
        }
        empresa.setCapitalAberto(true);
        empresa.setAcoesEmCirculacao(Math.round(empresa.getAcoesTotais() * fracaoOfertada));
        repositorio.save(empresa);

        auditoria.registrar(jogador.getUsuario(), "EMPRESA_IPO", "Empresa", empresa.getId(),
                "Abertura de capital da empresa " + empresa.getNome(),
                Map.of("fracaoOfertada", fracaoOfertada,
                        "acoesOfertadas", empresa.getAcoesEmCirculacao(),
                        "precoAcao", empresa.getPrecoAcao()));
        return empresa;
    }

    /**
     * Inicia um empreendimento (obra). Disponivel para os setores imobiliario e
     * de construcao: consome caixa ao longo dos turnos e vira valor ao concluir.
     */
    @Transactional
    public Empreendimento iniciarEmpreendimento(Long empresaId, Long jogadorId, String nome,
                                                Empreendimento.TipoEmpreendimento tipo,
                                                double custoTotal, int turnosTotais) {
        Empresa empresa = buscar(empresaId);
        Jogador jogador = exigirDono(empresa, jogadorId);
        if (empresa.getSetor() == Setor.ALIMENTICIO) {
            throw new RegraDeNegocioException("Empreendimentos sao exclusivos dos setores imobiliario e de construcao.");
        }
        if (custoTotal <= 0 || turnosTotais < 1 || turnosTotais > 36) {
            throw new RegraDeNegocioException("Obra invalida: custo positivo e prazo entre 1 e 36 turnos.");
        }
        double parcela = custoTotal / turnosTotais;
        if (empresa.getCaixa() < parcela) {
            throw new RegraDeNegocioException("Caixa insuficiente para a primeira parcela da obra (R$ "
                    + String.format("%.2f", parcela) + ").");
        }
        int turno = estadoJogo.turnoAtual();
        Empreendimento obra = new Empreendimento();
        obra.setEmpresa(empresa);
        obra.setNome(nome == null || nome.isBlank() ? "Empreendimento " + turno : nome.trim());
        obra.setTipo(tipo);
        obra.setCustoTotal(custoTotal);
        obra.setTurnosTotais(turnosTotais);
        obra.setTurnosRestantes(turnosTotais);
        obra.setTurnoInicio(turno);
        // Valor estimado acompanha o custo do terreno local e o tipo da obra.
        double fatorLocal = 0.7 + empresa.getMunicipio().getIndiceUrbanizacao() / 100.0 * 0.6;
        obra.setValorEstimado(custoTotal * tipo.getMultiplicadorValor() * fatorLocal);
        Empreendimento salva = repositorioEmpreendimento.save(obra);

        auditoria.registrar(jogador.getUsuario(), "OBRA_INICIADA", "Empreendimento", salva.getId(),
                "Obra " + salva.getNome() + " iniciada pela empresa " + empresa.getNome(),
                Map.of("custoTotal", custoTotal, "turnos", turnosTotais,
                        "valorEstimado", salva.getValorEstimado()));
        return salva;
    }

    /**
     * Diagnostico de capacidade: mostra qual dos dois tetos esta limitando a
     * producao e qual seria a equipe coerente com o patrimonio atual.
     */
    @Transactional(readOnly = true)
    public Map<String, Object> diagnosticoCapacidade(Long empresaId) {
        Empresa empresa = buscar(empresaId);
        double porEquipe = motor.capacidadePorEquipe(empresa);
        double porCapital = motor.capacidadePorCapital(empresa);

        Map<String, Object> diagnostico = new LinkedHashMap<>();
        diagnostico.put("capacidadePorEquipe", porEquipe);
        diagnostico.put("capacidadePorCapital", porCapital);
        diagnostico.put("capacidadeEfetiva", Math.min(porEquipe, porCapital));
        diagnostico.put("gargalo", porEquipe <= porCapital ? "EQUIPE" : "CAPITAL");
        diagnostico.put("equipeSugerida", empresa.getSetor().equipeSugerida(empresa.getPatrimonio()));
        diagnostico.put("ocupacao", porEquipe <= 0 || porCapital <= 0
                ? 0.0
                : empresa.getReceitaMensal() / Math.min(porEquipe, porCapital));
        return diagnostico;
    }

    /** Projecao simples de valor de mercado com o lucro corrente. */
    @Transactional(readOnly = true)
    public double valuationProjetado(Empresa empresa, ContextoMercado contexto) {
        return motor.calcularValuation(empresa, empresa.getLucroMensal(), contexto);
    }

    private Jogador exigirDono(Empresa empresa, Long jogadorId) {
        Jogador jogador = servicoJogador.buscar(jogadorId);
        if (empresa.getDono() == null || !empresa.getDono().getId().equals(jogador.getId())) {
            throw new RegraDeNegocioException("Apenas o dono pode administrar a empresa " + empresa.getNome() + ".");
        }
        if (!empresa.isAtiva()) {
            throw new RegraDeNegocioException("A empresa " + empresa.getNome() + " esta inativa.");
        }
        return jogador;
    }
}
