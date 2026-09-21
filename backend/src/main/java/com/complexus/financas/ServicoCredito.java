package com.complexus.financas;

import com.complexus.auditoria.ServicoAuditoria;
import com.complexus.comum.RecursoNaoEncontradoException;
import com.complexus.comum.RegraDeNegocioException;
import com.complexus.core.ServicoEstadoJogo;
import com.complexus.economia.Empresa;
import com.complexus.economia.RepositorioEmpresa;
import com.complexus.economia.RepositorioUnidade;
import com.complexus.economia.ServicoEstrutura;
import com.complexus.economia.Unidade;
import com.complexus.investimento.ServicoRazao;
import com.complexus.investimento.TipoLancamento;
import com.complexus.jogador.Jogador;
import com.complexus.jogador.ServicoJogador;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Mercado de credito das empresas.
 *
 * <h2>Como a divida funciona</h2>
 * Toda divida da empresa e um {@link Financiamento} com prazo, taxa travada na
 * contratacao e amortizacao constante. O campo {@code Empresa.divida} continua
 * existindo como agregado, recalculado a partir dos contratos em aberto.
 *
 * <h2>Preco do risco</h2>
 * A taxa e {@code taxaBasica + spread da modalidade + spread da nota}. A nota vem
 * de um score com quatro componentes - alavancagem, cobertura de juros, lastro e
 * historico de pagamento -, entao endividar demais nao bloqueia o credito: encarece.
 *
 * <h2>Quando a conta nao fecha</h2>
 * Parcela nao paga vira atraso: multa de 2 por cento, juros de mora incorporados
 * ao saldo e reputacao em queda. Na terceira parcela seguida em atraso, a linha
 * com garantia executa o patrimonio dado em garantia; a sem garantia empurra a
 * empresa para a alavancagem que dispara a falencia.
 */
@Service
public class ServicoCredito {

    private static final List<Financiamento.Status> EM_ABERTO = List.of(
            Financiamento.Status.ATIVO, Financiamento.Status.INADIMPLENTE,
            Financiamento.Status.RENEGOCIADO);

    /** Multa sobre a parcela nao paga. */
    private static final double MULTA_ATRASO = 0.02;
    /** Juros de mora mensais sobre a parcela em atraso. */
    private static final double MORA_MENSAL = 0.01;
    private static final int PARCELAS_ATE_EXECUCAO = 3;
    /** Custo de reestruturar uma divida, incorporado ao saldo. */
    private static final double CUSTO_RENEGOCIACAO = 0.01;
    /** Acrescimo de taxa cobrado de quem renegocia. */
    private static final double SPREAD_RENEGOCIACAO = 0.03;
    /** Garantia exigida sobre o valor liberado nas linhas com garantia real. */
    private static final double COBERTURA_GARANTIA = 1.30;
    private static final double VALOR_MINIMO = 10_000.0;

    private final RepositorioFinanciamento repositorio;
    private final RepositorioEmpresa repositorioEmpresa;
    private final RepositorioUnidade repositorioUnidade;
    private final ServicoEstrutura servicoEstrutura;
    private final ServicoJogador servicoJogador;
    private final ServicoRazao razao;
    private final ServicoAuditoria auditoria;
    private final ServicoEstadoJogo estadoJogo;

    public ServicoCredito(RepositorioFinanciamento repositorio,
                          RepositorioEmpresa repositorioEmpresa,
                          RepositorioUnidade repositorioUnidade,
                          ServicoEstrutura servicoEstrutura,
                          ServicoJogador servicoJogador,
                          ServicoRazao razao,
                          ServicoAuditoria auditoria,
                          ServicoEstadoJogo estadoJogo) {
        this.repositorio = repositorio;
        this.repositorioEmpresa = repositorioEmpresa;
        this.repositorioUnidade = repositorioUnidade;
        this.servicoEstrutura = servicoEstrutura;
        this.servicoJogador = servicoJogador;
        this.razao = razao;
        this.auditoria = auditoria;
        this.estadoJogo = estadoJogo;
    }

    // ------------------------------------------------------------------
    // Consultas
    // ------------------------------------------------------------------

    @Transactional(readOnly = true)
    public List<Financiamento> historico(Long empresaId) {
        return repositorio.findByEmpresaIdOrderByIdDesc(empresaId);
    }

    @Transactional(readOnly = true)
    public List<Financiamento> emAberto(Long empresaId) {
        return repositorio.findByEmpresaIdAndStatusInOrderByIdAsc(empresaId, EM_ABERTO);
    }

    @Transactional(readOnly = true)
    public Financiamento buscar(Long id) {
        return repositorio.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Financiamento", id));
    }

    /**
     * Avaliacao de credito da empresa no turno corrente.
     *
     * Os quatro componentes tem peso diferente porque medem coisas diferentes:
     * alavancagem e cobertura dizem se a divida cabe no resultado, o lastro diz
     * se o valor de mercado tem patrimonio por tras e o historico diz se a
     * empresa costuma pagar.
     */
    @Transactional(readOnly = true)
    public AvaliacaoCredito avaliar(Empresa empresa) {
        List<Financiamento> contratos = emAberto(empresa.getId());
        double dividaTotal = contratos.stream().mapToDouble(Financiamento::getSaldoDevedor).sum();
        double jurosAnuais = contratos.stream().mapToDouble(Financiamento::jurosDoMes).sum() * 12;
        double patrimonioLiquido = empresa.patrimonioLiquido();
        double lucroAnual = empresa.getLucroMensal() * 12;

        double alavancagem = patrimonioLiquido > 0 ? dividaTotal / patrimonioLiquido : (dividaTotal > 0 ? 99 : 0);
        // Sem juros nao ha o que cobrir: quem nao deve tem cobertura maxima.
        double cobertura = jurosAnuais > 0 ? lucroAnual / jurosAnuais : 99;
        int atrasos = contratos.stream().mapToInt(Financiamento::getParcelasEmAtraso).sum();

        double pontosAlavancagem = 100 * Math.clamp(1 - alavancagem / 2.5, 0, 1);
        double pontosCobertura = 100 * Math.clamp(cobertura / 6.0, 0, 1);
        double pontosLastro = 100 * Math.clamp(empresa.indiceLastro(), 0, 1);
        double pontosHistorico = Math.max(100 - atrasos * 25.0, 0);

        double score = pontosAlavancagem * 0.35 + pontosCobertura * 0.30
                + pontosLastro * 0.15 + pontosHistorico * 0.20;
        NotaCredito nota = NotaCredito.porScore(score);

        Map<String, Double> componentes = new LinkedHashMap<>();
        componentes.put("alavancagem", pontosAlavancagem);
        componentes.put("coberturaDeJuros", pontosCobertura);
        componentes.put("lastro", pontosLastro);
        componentes.put("historicoDePagamento", pontosHistorico);

        List<String> observacoes = new ArrayList<>();
        if (alavancagem > 1.5) {
            observacoes.add("Divida acima de 1,5 vez o patrimonio liquido encarece qualquer linha nova.");
        }
        if (cobertura < 2 && jurosAnuais > 0) {
            observacoes.add("O lucro cobre menos de duas vezes os juros: sobra pouco para imprevisto.");
        }
        if (atrasos > 0) {
            observacoes.add(atrasos + " parcela(s) em atraso pesam no score ate serem regularizadas.");
        }
        if (empresa.getLucroMensal() <= 0) {
            observacoes.add("Sem lucro no ultimo turno, o limite fica preso ao patrimonio.");
        }

        return new AvaliacaoCredito(score, nota, alavancagem, cobertura, dividaTotal,
                patrimonioLiquido, estadoJogo.obter().getTaxaJuros(), componentes, observacoes);
    }

    /**
     * Quanto a empresa ainda consegue tomar em uma modalidade.
     *
     * O limite olha o risco agregado: toda a divida ja contratada consome o
     * espaco, nao importa em qual linha ela esteja.
     */
    @Transactional(readOnly = true)
    public double limiteDisponivel(Empresa empresa, Financiamento.Modalidade modalidade,
                                   AvaliacaoCredito avaliacao) {
        double limite = Math.max(avaliacao.patrimonioLiquido(), 0)
                * modalidade.getFracaoDoLimite() * avaliacao.nota().getFatorLimite();
        if (modalidade == Financiamento.Modalidade.ANTECIPACAO_RECEBIVEIS) {
            // Antecipacao tem lastro no faturamento, nao no patrimonio.
            limite = Math.min(limite, empresa.getReceitaMensal() * 3);
        }
        return Math.max(limite - avaliacao.dividaTotal(), 0);
    }

    /** Vitrine de credito: o que cada linha oferece a esta empresa hoje. */
    @Transactional(readOnly = true)
    public Map<String, Object> vitrine(Long empresaId) {
        Empresa empresa = buscarEmpresa(empresaId);
        AvaliacaoCredito avaliacao = avaliar(empresa);

        List<Map<String, Object>> linhas = new ArrayList<>();
        for (Financiamento.Modalidade modalidade : Financiamento.Modalidade.values()) {
            Map<String, Object> linha = new LinkedHashMap<>();
            linha.put("modalidade", modalidade.name());
            linha.put("rotulo", modalidade.getRotulo());
            linha.put("descricao", modalidade.getDescricao());
            linha.put("taxaAnual", avaliacao.taxaAnual(modalidade));
            linha.put("prazoMinimo", modalidade.getPrazoMinimo());
            linha.put("prazoMaximo", modalidade.getPrazoMaximo());
            linha.put("exigeGarantia", modalidade.isExigeGarantia());
            linha.put("limiteDisponivel", limiteDisponivel(empresa, modalidade, avaliacao));
            linha.put("contratavel", modalidade != Financiamento.Modalidade.ROTATIVO);
            linhas.add(linha);
        }

        Map<String, Object> vitrine = new LinkedHashMap<>();
        vitrine.put("avaliacao", avaliacao.comoMapa());
        vitrine.put("linhas", linhas);
        vitrine.put("patrimonioLivre", patrimonioLivre(empresa));
        vitrine.put("servicoDaDividaMensal", jurosDoMes(empresa.getId())
                + emAberto(empresaId).stream().mapToDouble(Financiamento::amortizacaoDoMes).sum());
        return vitrine;
    }

    // ------------------------------------------------------------------
    // Comandos do jogador
    // ------------------------------------------------------------------

    /** Contrata uma linha de credito. O valor cai no caixa da empresa no ato. */
    @Transactional
    public Financiamento contratar(Long empresaId, Long jogadorId,
                                   Financiamento.Modalidade modalidade, double valor, int prazoTurnos) {
        Empresa empresa = buscarEmpresa(empresaId);
        Jogador jogador = exigirDono(empresa, jogadorId);
        if (modalidade == Financiamento.Modalidade.ROTATIVO) {
            throw new RegraDeNegocioException(
                    "O credito rotativo e automatico: ele so aparece quando o caixa fecha negativo.");
        }
        if (valor < VALOR_MINIMO) {
            throw new RegraDeNegocioException("O valor minimo de um contrato e R$ "
                    + String.format("%.2f", VALOR_MINIMO) + ".");
        }
        if (prazoTurnos < modalidade.getPrazoMinimo() || prazoTurnos > modalidade.getPrazoMaximo()) {
            throw new RegraDeNegocioException("O prazo de " + modalidade.getRotulo() + " vai de "
                    + modalidade.getPrazoMinimo() + " a " + modalidade.getPrazoMaximo() + " turnos.");
        }
        AvaliacaoCredito avaliacao = avaliar(empresa);
        double disponivel = limiteDisponivel(empresa, modalidade, avaliacao);
        if (valor > disponivel) {
            throw new RegraDeNegocioException("Limite disponivel em " + modalidade.getRotulo()
                    + " e R$ " + String.format("%.2f", disponivel) + " com a nota "
                    + avaliacao.nota().name() + ".");
        }
        double garantia = 0;
        if (modalidade.isExigeGarantia()) {
            garantia = valor * COBERTURA_GARANTIA;
            double livre = patrimonioLivre(empresa);
            if (garantia > livre) {
                throw new RegraDeNegocioException("Garantia exigida de R$ "
                        + String.format("%.2f", garantia) + " e o patrimonio livre e R$ "
                        + String.format("%.2f", livre) + ".");
            }
        }

        int turno = estadoJogo.turnoAtual();
        Financiamento contrato = new Financiamento();
        contrato.setEmpresa(empresa);
        contrato.setModalidade(modalidade);
        contrato.setPrincipal(valor);
        contrato.setSaldoDevedor(valor);
        contrato.setTaxaMensal(avaliacao.taxaAnual(modalidade) / 12.0);
        contrato.setNotaNaContratacao(avaliacao.nota());
        contrato.setPrazoTurnos(prazoTurnos);
        contrato.setTurnosRestantes(prazoTurnos);
        contrato.setTurnoContratacao(turno);
        contrato.setGarantia(garantia);
        Financiamento salvo = repositorio.save(contrato);

        empresa.setCaixa(empresa.getCaixa() + valor);
        sincronizarDivida(empresa);

        razao.registrar(turno, TipoLancamento.EMPRESTIMO, "mercado_de_credito",
                "empresa:" + empresa.getId(), valor, empresa.getId(), jogador.getId(),
                modalidade.getRotulo() + " em " + prazoTurnos + " turnos");
        Map<String, Object> detalhes = new LinkedHashMap<>();
        detalhes.put("modalidade", modalidade.name());
        detalhes.put("valor", valor);
        detalhes.put("prazoTurnos", prazoTurnos);
        detalhes.put("taxaAnual", contrato.taxaAnual());
        detalhes.put("nota", avaliacao.nota().name());
        detalhes.put("garantia", garantia);
        auditoria.registrar(jogador.getUsuario(), "CREDITO_CONTRATADO", "Financiamento", salvo.getId(),
                "Credito contratado por " + empresa.getNome(), detalhes);
        return salvo;
    }

    /**
     * Amortizacao antecipada. Como os juros incidem sobre o saldo, pagar antes
     * ja economiza os juros futuros - nao existe desconto adicional.
     */
    @Transactional
    public Financiamento amortizar(Long financiamentoId, Long jogadorId, double valor) {
        Financiamento contrato = buscar(financiamentoId);
        Empresa empresa = contrato.getEmpresa();
        Jogador jogador = exigirDono(empresa, jogadorId);
        if (!contrato.emAberto()) {
            throw new RegraDeNegocioException("Este contrato ja foi encerrado.");
        }
        if (valor <= 0) {
            throw new RegraDeNegocioException("O valor amortizado deve ser positivo.");
        }
        double efetivo = Math.min(valor, contrato.getSaldoDevedor());
        if (empresa.getCaixa() < efetivo) {
            throw new RegraDeNegocioException("Caixa insuficiente: disponivel R$ "
                    + String.format("%.2f", empresa.getCaixa()) + ".");
        }
        empresa.setCaixa(empresa.getCaixa() - efetivo);
        contrato.setSaldoDevedor(contrato.getSaldoDevedor() - efetivo);
        contrato.setAmortizado(contrato.getAmortizado() + efetivo);
        if (contrato.getSaldoDevedor() <= 0.01) {
            quitar(contrato);
        }
        repositorio.save(contrato);
        sincronizarDivida(empresa);

        int turno = estadoJogo.turnoAtual();
        razao.registrar(turno, TipoLancamento.AMORTIZACAO, "empresa:" + empresa.getId(),
                "mercado_de_credito", efetivo, empresa.getId(), jogador.getId(),
                "Amortizacao antecipada do contrato " + contrato.getId());
        auditoria.registrar(jogador.getUsuario(), "CREDITO_AMORTIZADO", "Financiamento", contrato.getId(),
                "Amortizacao antecipada de " + empresa.getNome(),
                Map.of("valor", efetivo, "saldoRestante", contrato.getSaldoDevedor(),
                        "status", contrato.getStatus().name()));
        return contrato;
    }

    /**
     * Renegocia o contrato: alonga o prazo para aliviar a parcela, ao custo de
     * uma taxa maior e de uma comissao de reestruturacao somada ao saldo. Zera o
     * atraso, o que interrompe a contagem para a execucao da garantia.
     */
    @Transactional
    public Financiamento renegociar(Long financiamentoId, Long jogadorId, int novoPrazo) {
        Financiamento contrato = buscar(financiamentoId);
        Empresa empresa = contrato.getEmpresa();
        Jogador jogador = exigirDono(empresa, jogadorId);
        if (!contrato.emAberto()) {
            throw new RegraDeNegocioException("Este contrato ja foi encerrado.");
        }
        if (novoPrazo <= contrato.getTurnosRestantes()) {
            throw new RegraDeNegocioException("Renegociar serve para alongar: informe um prazo maior que "
                    + contrato.getTurnosRestantes() + " turnos.");
        }
        if (novoPrazo > contrato.getModalidade().getPrazoMaximo() * 2) {
            throw new RegraDeNegocioException("O prazo renegociado nao passa de "
                    + contrato.getModalidade().getPrazoMaximo() * 2 + " turnos.");
        }
        double comissao = contrato.getSaldoDevedor() * CUSTO_RENEGOCIACAO;
        contrato.setSaldoDevedor(contrato.getSaldoDevedor() + comissao);
        contrato.setTaxaMensal(contrato.getTaxaMensal() + SPREAD_RENEGOCIACAO / 12.0);
        contrato.setTurnosRestantes(novoPrazo);
        contrato.setPrazoTurnos(contrato.getPrazoTurnos() + novoPrazo);
        contrato.setParcelasEmAtraso(0);
        contrato.setStatus(Financiamento.Status.RENEGOCIADO);
        repositorio.save(contrato);
        sincronizarDivida(empresa);

        Map<String, Object> detalhes = new LinkedHashMap<>();
        detalhes.put("novoPrazo", novoPrazo);
        detalhes.put("comissao", comissao);
        detalhes.put("novaTaxaAnual", contrato.taxaAnual());
        detalhes.put("saldoDevedor", contrato.getSaldoDevedor());
        auditoria.registrar(jogador.getUsuario(), "CREDITO_RENEGOCIADO", "Financiamento", contrato.getId(),
                "Renegociacao de divida de " + empresa.getNome(), detalhes);
        return contrato;
    }

    // ------------------------------------------------------------------
    // Rotina do turno
    // ------------------------------------------------------------------

    /** Juros do mes de toda a divida em aberto, que entram como despesa no resultado. */
    @Transactional(readOnly = true)
    public double jurosDoMes(Long empresaId) {
        return emAberto(empresaId).stream().mapToDouble(Financiamento::jurosDoMes).sum();
    }

    /**
     * Cobra as parcelas do turno.
     *
     * Os juros ja sairam no resultado, entao aqui so a amortizacao deixa o
     * caixa - cobrar a parcela inteira desceria os juros duas vezes.
     *
     * @return resumo do que foi pago, do que atrasou e do que foi executado
     */
    @Transactional
    public Map<String, Object> cobrarParcelas(Empresa empresa, int turno) {
        double amortizado = 0;
        double jurosCobrados = 0;
        double emAtraso = 0;
        int contratosEmAtraso = 0;
        int quitados = 0;
        int executados = 0;

        for (Financiamento contrato : emAberto(empresa.getId())) {
            double juros = contrato.jurosDoMes();
            double amortizacao = contrato.amortizacaoDoMes();
            contrato.setJurosPagos(contrato.getJurosPagos() + juros);
            jurosCobrados += juros;

            if (empresa.getCaixa() >= amortizacao) {
                empresa.setCaixa(empresa.getCaixa() - amortizacao);
                contrato.setSaldoDevedor(contrato.getSaldoDevedor() - amortizacao);
                contrato.setAmortizado(contrato.getAmortizado() + amortizacao);
                contrato.setTurnosRestantes(Math.max(contrato.getTurnosRestantes() - 1, 0));
                contrato.setParcelasEmAtraso(0);
                if (contrato.getStatus() == Financiamento.Status.INADIMPLENTE) {
                    contrato.setStatus(Financiamento.Status.ATIVO);
                }
                amortizado += amortizacao;
                razao.registrar(turno, TipoLancamento.AMORTIZACAO, "empresa:" + empresa.getId(),
                        "mercado_de_credito", amortizacao, empresa.getId(), null,
                        "Parcela do contrato " + contrato.getId());
                if (contrato.getSaldoDevedor() <= 0.01) {
                    quitar(contrato);
                    quitados++;
                }
            } else {
                // Sem caixa: a parcela nao paga volta para o saldo com multa e mora.
                double encargo = amortizacao * MULTA_ATRASO + contrato.getSaldoDevedor() * MORA_MENSAL;
                contrato.setSaldoDevedor(contrato.getSaldoDevedor() + encargo);
                contrato.setParcelasEmAtraso(contrato.getParcelasEmAtraso() + 1);
                contrato.setStatus(Financiamento.Status.INADIMPLENTE);
                emAtraso += amortizacao;
                contratosEmAtraso++;
                auditoria.registrarSistema("CREDITO_ATRASO", "Financiamento", contrato.getId(),
                        "Parcela em atraso de " + empresa.getNome(),
                        Map.of("parcela", amortizacao, "encargo", encargo,
                                "parcelasEmAtraso", contrato.getParcelasEmAtraso()));
                if (contrato.getParcelasEmAtraso() >= PARCELAS_ATE_EXECUCAO && contrato.getGarantia() > 0) {
                    executarGarantia(contrato, empresa, turno);
                    executados++;
                }
            }
            repositorio.save(contrato);
        }

        if (contratosEmAtraso > 0) {
            // Atraso queima a marca: fornecedor e cliente ficam sabendo.
            empresa.setReputacao(Math.max(empresa.getReputacao() - contratosEmAtraso * 2.0, 0));
        }
        sincronizarDivida(empresa);

        Map<String, Object> resumo = new LinkedHashMap<>();
        resumo.put("amortizado", amortizado);
        resumo.put("jurosCobrados", jurosCobrados);
        resumo.put("emAtraso", emAtraso);
        resumo.put("contratosEmAtraso", contratosEmAtraso);
        resumo.put("quitados", quitados);
        resumo.put("garantiasExecutadas", executados);
        return resumo;
    }

    /**
     * Abre o credito rotativo que cobre um caixa negativo.
     *
     * Continua automatico como antes, mas agora vira contrato: tem prazo, taxa
     * e aparece na tela de financas como a divida mais cara da empresa.
     */
    @Transactional
    public Financiamento abrirRotativo(Empresa empresa, double valor, int turno) {
        Financiamento.Modalidade modalidade = Financiamento.Modalidade.ROTATIVO;
        double taxaAnual = estadoJogo.obter().getTaxaJuros() + modalidade.getSpreadAnual()
                + NotaCredito.porScore(avaliar(empresa).score()).getSpreadAnual();

        Financiamento contrato = new Financiamento();
        contrato.setEmpresa(empresa);
        contrato.setModalidade(modalidade);
        contrato.setPrincipal(valor);
        contrato.setSaldoDevedor(valor);
        contrato.setTaxaMensal(taxaAnual / 12.0);
        contrato.setPrazoTurnos(modalidade.getPrazoMaximo());
        contrato.setTurnosRestantes(modalidade.getPrazoMaximo());
        contrato.setTurnoContratacao(turno);
        Financiamento salvo = repositorio.save(contrato);
        sincronizarDivida(empresa);

        razao.registrar(turno, TipoLancamento.EMPRESTIMO, "mercado_de_credito",
                "empresa:" + empresa.getId(), valor, empresa.getId(), null,
                "Credito rotativo automatico para cobrir caixa negativo");
        auditoria.registrarSistema("CREDITO_ROTATIVO", "Financiamento", salvo.getId(),
                "Credito rotativo aberto para " + empresa.getNome(),
                Map.of("valor", valor, "taxaAnual", taxaAnual));
        return salvo;
    }

    /**
     * Recalcula {@code Empresa.divida} a partir dos contratos em aberto.
     * A divida da empresa nunca e escrita direto: ela e sempre a soma daqui.
     */
    @Transactional
    public Empresa sincronizarDivida(Empresa empresa) {
        double saldo = repositorio.saldoTotal(empresa.getId(), EM_ABERTO);
        empresa.setDivida(saldo);
        return repositorioEmpresa.save(empresa);
    }

    /** Patrimonio que ainda nao foi dado em garantia. */
    @Transactional(readOnly = true)
    public double patrimonioLivre(Empresa empresa) {
        double comprometido = repositorio.garantiaComprometida(empresa.getId(), EM_ABERTO);
        return Math.max(empresa.getPatrimonio() - comprometido, 0);
    }

    // ------------------------------------------------------------------

    private void quitar(Financiamento contrato) {
        contrato.setSaldoDevedor(0);
        contrato.setTurnosRestantes(0);
        contrato.setGarantia(0);
        contrato.setParcelasEmAtraso(0);
        contrato.setStatus(Financiamento.Status.QUITADO);
    }

    /**
     * Executa a garantia: o credor toma o patrimonio dado em garantia e abate o
     * saldo. A perda sai das unidades na proporcao do que cada uma tem, porque
     * a garantia foi dada pela empresa, e nao por uma filial especifica.
     */
    private void executarGarantia(Financiamento contrato, Empresa empresa, int turno) {
        double tomado = Math.min(contrato.getGarantia(), contrato.getSaldoDevedor());
        double patrimonioTotal = empresa.getPatrimonio();
        if (patrimonioTotal > 0) {
            List<Unidade> unidades = servicoEstrutura.unidades(empresa.getId());
            for (Unidade unidade : unidades) {
                double fatia = unidade.getPatrimonio() / patrimonioTotal;
                unidade.setPatrimonio(Math.max(unidade.getPatrimonio() - tomado * fatia, 0));
                repositorioUnidade.save(unidade);
            }
            servicoEstrutura.sincronizarAgregados(empresa);
        }
        contrato.setSaldoDevedor(Math.max(contrato.getSaldoDevedor() - tomado, 0));
        contrato.setGarantia(0);
        contrato.setStatus(contrato.getSaldoDevedor() <= 0.01
                ? Financiamento.Status.QUITADO
                : Financiamento.Status.EXECUTADO);
        empresa.setReputacao(Math.max(empresa.getReputacao() - 8.0, 0));

        razao.registrar(turno, TipoLancamento.EXECUCAO_GARANTIA, "empresa:" + empresa.getId(),
                "mercado_de_credito", tomado, empresa.getId(), null,
                "Execucao de garantia do contrato " + contrato.getId());
        auditoria.registrarSistema("CREDITO_GARANTIA_EXECUTADA", "Financiamento", contrato.getId(),
                "Garantia executada em " + empresa.getNome(),
                Map.of("patrimonioTomado", tomado, "saldoRestante", contrato.getSaldoDevedor()));
    }

    private Empresa buscarEmpresa(Long empresaId) {
        return repositorioEmpresa.findById(empresaId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Empresa", empresaId));
    }

    private Jogador exigirDono(Empresa empresa, Long jogadorId) {
        Jogador jogador = servicoJogador.buscar(jogadorId);
        empresa.exigirControleDe(jogador);
        return jogador;
    }
}
