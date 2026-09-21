package com.complexus.core;

import com.complexus.auditoria.ServicoAuditoria;
import com.complexus.config.PropriedadesJogo;
import com.complexus.economia.ContextoMercado;
import com.complexus.economia.Empreendimento;
import com.complexus.economia.Empresa;
import com.complexus.economia.HistoricoEmpresa;
import com.complexus.economia.MotorSimulacao;
import com.complexus.economia.RepositorioEmpreendimento;
import com.complexus.economia.RepositorioEmpresa;
import com.complexus.economia.RepositorioHistoricoEmpresa;
import com.complexus.economia.ResultadoMensal;
import com.complexus.economia.Setor;
import com.complexus.estatistica.ServicoEstatistica;
import com.complexus.estatistica.SnapshotTurno;
import com.complexus.integracao.ClienteAnalitico;
import com.complexus.investimento.ServicoInvestimento;
import com.complexus.investimento.ServicoRazao;
import com.complexus.investimento.TipoLancamento;
import com.complexus.politica.Estado;
import com.complexus.politica.Municipio;
import com.complexus.politica.Pais;
import com.complexus.politica.RepositorioEstado;
import com.complexus.politica.RepositorioMunicipio;
import com.complexus.politica.RepositorioPais;
import com.complexus.politica.ServicoPolitica;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Motor de turnos: a rotina que faz o mundo andar.
 *
 * Um turno equivale a uma hora de tempo real e a um mes no calendario do jogo.
 * A ordem de processamento importa e e sempre a mesma:
 *
 * <ol>
 *   <li>avanca o relogio;</li>
 *   <li>busca os choques setoriais no servico analitico (Python);</li>
 *   <li>executa a rotina politica (apuracao, sancao, mandatos);</li>
 *   <li>executa os gastos publicos recorrentes;</li>
 *   <li>simula todas as empresas e distribui dividendos;</li>
 *   <li>recalcula os agregados macroeconomicos;</li>
 *   <li>consolida as estatisticas e fecha a linha de auditoria do turno.</li>
 * </ol>
 */
@Service
public class ServicoTurno {

    private static final Logger log = LoggerFactory.getLogger(ServicoTurno.class);

    private static final double RENDA_REFERENCIA = 3200.0;
    private static final double DEPRECIACAO_PRODUTIVIDADE = 0.995;
    private static final double LIMITE_ALAVANCAGEM_FALENCIA = 2.5;

    private final ServicoEstadoJogo estadoJogo;
    private final RepositorioEmpresa repositorioEmpresa;
    private final RepositorioHistoricoEmpresa repositorioHistorico;
    private final RepositorioEmpreendimento repositorioEmpreendimento;
    private final RepositorioPais repositorioPais;
    private final RepositorioEstado repositorioEstado;
    private final RepositorioMunicipio repositorioMunicipio;
    private final MotorSimulacao motor;
    private final ServicoPolitica servicoPolitica;
    private final ServicoInvestimento servicoInvestimento;
    private final ServicoEstatistica servicoEstatistica;
    private final ServicoRazao razao;
    private final ServicoAuditoria auditoria;
    private final ClienteAnalitico clienteAnalitico;
    private final PropriedadesJogo propriedades;

    public ServicoTurno(ServicoEstadoJogo estadoJogo,
                        RepositorioEmpresa repositorioEmpresa,
                        RepositorioHistoricoEmpresa repositorioHistorico,
                        RepositorioEmpreendimento repositorioEmpreendimento,
                        RepositorioPais repositorioPais,
                        RepositorioEstado repositorioEstado,
                        RepositorioMunicipio repositorioMunicipio,
                        MotorSimulacao motor,
                        ServicoPolitica servicoPolitica,
                        ServicoInvestimento servicoInvestimento,
                        ServicoEstatistica servicoEstatistica,
                        ServicoRazao razao,
                        ServicoAuditoria auditoria,
                        ClienteAnalitico clienteAnalitico,
                        PropriedadesJogo propriedades) {
        this.estadoJogo = estadoJogo;
        this.repositorioEmpresa = repositorioEmpresa;
        this.repositorioHistorico = repositorioHistorico;
        this.repositorioEmpreendimento = repositorioEmpreendimento;
        this.repositorioPais = repositorioPais;
        this.repositorioEstado = repositorioEstado;
        this.repositorioMunicipio = repositorioMunicipio;
        this.motor = motor;
        this.servicoPolitica = servicoPolitica;
        this.servicoInvestimento = servicoInvestimento;
        this.servicoEstatistica = servicoEstatistica;
        this.razao = razao;
        this.auditoria = auditoria;
        this.clienteAnalitico = clienteAnalitico;
        this.propriedades = propriedades;
    }

    /**
     * Processa um turno completo.
     *
     * @param origem quem disparou o turno (AGENDADOR ou usuario que chamou a API)
     * @return relatorio do turno
     */
    @Transactional
    public synchronized Map<String, Object> processarTurno(String origem) {
        long inicio = System.currentTimeMillis();
        EstadoJogo estado = estadoJogo.obter();

        estado.setTurnoAtual(estado.getTurnoAtual() + 1);
        estado.setDataJogo(estado.getDataJogo().plusMonths(1));
        // Publica o novo turno no relogio antes de qualquer efeito, para que os
        // eventos de auditoria deste processamento ja nascam com o turno certo.
        estadoJogo.salvar(estado);
        int turno = estado.getTurnoAtual();
        log.info("Processando turno {} ({})", turno, estado.getDataJogo());

        ClienteAnalitico.Resultado analitico = clienteAnalitico.modificadores(
                turno, estado.getInflacaoAnual(), estado.getTaxaJuros());

        Pais pais = repositorioPais.findAll().stream().findFirst().orElse(null);
        if (pais == null) {
            throw new IllegalStateException("Mundo nao inicializado: nenhum pais cadastrado.");
        }

        Map<String, Object> resumoPolitico = servicoPolitica.processarTurno(turno);
        executarGastosPublicos(turno, pais);

        ContextoMercado contexto = new ContextoMercado(
                pais.getRendaMedia(), RENDA_REFERENCIA, estado.getTaxaJuros(),
                estado.getInflacaoAnual(), pais.getEstabilidade(), analitico.modificadores());

        ResumoEconomico economia = simularEmpresas(turno, contexto, pais);
        processarEmpreendimentos(turno);
        atualizarMacroeconomia(estado, pais, economia);

        SnapshotTurno snapshot = servicoEstatistica.consolidar(estado, economia.dividendos);

        Instant agora = Instant.now();
        estado.setUltimoProcessamento(agora);
        estado.setProximoProcessamento(agora.plus(Duration.ofMinutes(propriedades.getTurno().getDuracaoMinutos())));
        estadoJogo.salvar(estado);

        long duracao = System.currentTimeMillis() - inicio;
        Map<String, Object> relatorio = new LinkedHashMap<>();
        relatorio.put("turno", turno);
        relatorio.put("dataJogo", estado.getDataJogo().toString());
        relatorio.put("origem", origem);
        relatorio.put("empresasProcessadas", economia.empresas);
        relatorio.put("falencias", economia.falencias);
        relatorio.put("receitaAgregada", economia.receita);
        relatorio.put("lucroAgregado", economia.lucro);
        relatorio.put("impostosArrecadados", economia.impostos);
        relatorio.put("dividendosPagos", economia.dividendos);
        relatorio.put("indiceMercado", snapshot.getIndiceMercado());
        relatorio.put("variacaoIndice", snapshot.getVariacaoIndice());
        relatorio.put("fonteModificadores", analitico.origem().name());
        relatorio.put("politica", resumoPolitico);
        relatorio.put("duracaoMs", duracao);

        auditoria.registrarSistema("TURNO_PROCESSADO", "EstadoJogo", EstadoJogo.ID_UNICO,
                "Turno " + turno + " processado por " + origem, relatorio);
        return relatorio;
    }

    // ------------------------------------------------------------------
    // Etapas
    // ------------------------------------------------------------------

    /** Transferencias publicas recorrentes fixadas por leis em vigor. */
    private void executarGastosPublicos(int turno, Pais pais) {
        if (pais.getGastoSocialMensal() > 0) {
            double gasto = Math.min(pais.getGastoSocialMensal(), Math.max(pais.getTesouro(), 0));
            pais.setTesouro(pais.getTesouro() - gasto);
            // Transferencia de renda eleva a renda media das familias.
            double impacto = gasto / Math.max(pais.getPopulacao(), 1);
            pais.setRendaMedia(pais.getRendaMedia() + impacto * 0.6);
            razao.registrar(turno, TipoLancamento.TRANSFERENCIA_TESOURO, "tesouro_nacional",
                    "populacao", gasto, null, null, "Programa social do turno " + turno);
        }
        repositorioPais.save(pais);

        for (Estado estado : repositorioEstado.findAll()) {
            double investimento = estado.getInvestimentoInfraestrutura();
            if (investimento <= 0) {
                continue;
            }
            double efetivo = Math.min(investimento, Math.max(estado.getTesouro(), 0));
            estado.setTesouro(estado.getTesouro() - efetivo);
            // Infraestrutura eleva o indice de desenvolvimento com retorno decrescente.
            double ganho = Math.sqrt(efetivo) / 5_000.0;
            estado.setIndiceDesenvolvimento(Math.min(estado.getIndiceDesenvolvimento() + ganho, 100));
            repositorioEstado.save(estado);
            razao.registrar(turno, TipoLancamento.TRANSFERENCIA_TESOURO, "tesouro_estadual:" + estado.getSigla(),
                    "infraestrutura", efetivo, null, null, "Investimento em infraestrutura no turno " + turno);
        }
    }

    /**
     * Simula todas as empresas ativas. As empresas do mesmo setor e municipio
     * disputam o mesmo mercado: a participacao de cada uma sai da competitividade
     * relativa dentro do grupo.
     */
    private ResumoEconomico simularEmpresas(int turno, ContextoMercado contexto, Pais pais) {
        ResumoEconomico resumo = new ResumoEconomico();
        List<Empresa> empresas = repositorioEmpresa.findByAtivaTrue();

        Map<String, List<Empresa>> grupos = new HashMap<>();
        for (Empresa empresa : empresas) {
            String chave = empresa.getMunicipio().getId() + ":" + empresa.getSetor().name();
            grupos.computeIfAbsent(chave, k -> new ArrayList<>()).add(empresa);
        }

        for (List<Empresa> grupo : grupos.values()) {
            Municipio municipio = grupo.get(0).getMunicipio();
            Estado estado = municipio.getEstado();
            Setor setor = grupo.get(0).getSetor();
            double mercadoPotencial = motor.mercadoPotencial(setor, municipio, estado, pais);
            double somaCapacidades = grupo.stream().mapToDouble(motor::capacidadeProdutiva).sum();
            double mercado = motor.mercadoDisputavel(mercadoPotencial, somaCapacidades);

            double somaCompetitividade = grupo.stream().mapToDouble(motor::competitividade).sum();
            for (Empresa empresa : grupo) {
                double participacao = somaCompetitividade <= 0
                        ? 1.0 / grupo.size()
                        : motor.competitividade(empresa) / somaCompetitividade;
                processarEmpresa(turno, empresa, mercado, participacao, contexto, estado, municipio, pais, resumo);
            }
        }
        repositorioPais.save(pais);
        return resumo;
    }

    private void processarEmpresa(int turno, Empresa empresa, double mercado, double participacao,
                                  ContextoMercado contexto, Estado estado, Municipio municipio,
                                  Pais pais, ResumoEconomico resumo) {

        double subsidio = servicoPolitica.subsidioVigente(empresa.getSetor(), estado.getId());
        double regulacao = servicoPolitica.regulacaoVigente(empresa.getSetor(), estado.getId());

        ResultadoMensal resultado = motor.simularMes(empresa, mercado, participacao, contexto,
                estado, municipio, pais, subsidio, regulacao);

        double lucroAnterior = empresa.getLucroMensal();
        double impostosTotais = resultado.impostoIndireto() + resultado.impostoRenda();

        empresa.setReceitaMensal(resultado.receita());
        empresa.setCustoMensal(resultado.custoOperacional());
        empresa.setImpostosMensais(impostosTotais);
        empresa.setLucroMensal(resultado.lucro());
        empresa.setCrescimentoLucro(motor.calcularCrescimento(resultado.lucro(), lucroAnterior));
        empresa.setLucroAcumulado(empresa.getLucroAcumulado() + resultado.lucro());
        empresa.setMarketShare(participacao);
        empresa.setCaixa(empresa.getCaixa() + resultado.lucro());
        empresa.setProdutividade(Math.max(empresa.getProdutividade() * DEPRECIACAO_PRODUTIVIDADE, 0.4));
        empresa.setReputacao(novaReputacao(empresa, resultado));

        distribuirTributos(turno, empresa, resultado, pais, estado, municipio);
        if (resultado.subsidioRecebido() > 0) {
            pais.setTesouro(pais.getTesouro() - resultado.subsidioRecebido());
            razao.registrar(turno, TipoLancamento.SUBSIDIO, "tesouro_nacional",
                    "empresa:" + empresa.getId(), resultado.subsidioRecebido(), empresa.getId(),
                    null, "Subsidio setorial no turno " + turno);
        }

        double dividendos = servicoInvestimento.distribuirDividendos(empresa, resultado.lucro(), turno);
        aplicarEndividamento(turno, empresa);

        double valuation = motor.calcularValuation(empresa, resultado.lucro(), contexto);
        empresa.setValuation(valuation);
        empresa.setPrecoAcao(motor.calcularPrecoAcao(empresa, valuation));

        boolean faliu = verificarFalencia(empresa);
        repositorioEmpresa.save(empresa);
        registrarHistorico(turno, empresa, resultado, dividendos);

        razao.registrar(turno, TipoLancamento.RECEITA_OPERACIONAL, "mercado",
                "empresa:" + empresa.getId(), resultado.receita(), empresa.getId(), null,
                "Receita do turno " + turno);
        razao.registrar(turno, TipoLancamento.CUSTO_OPERACIONAL, "empresa:" + empresa.getId(),
                "fornecedores_e_folha", resultado.custoOperacional(), empresa.getId(), null,
                "Custo do turno " + turno);

        resumo.empresas++;
        resumo.receita += resultado.receita();
        resumo.lucro += resultado.lucro();
        resumo.impostos += impostosTotais;
        resumo.dividendos += dividendos;
        resumo.empregos += empresa.getFuncionarios();
        if (faliu) {
            resumo.falencias++;
        }
    }

    /** Reputacao sobe com operacao saudavel e marketing, e cai com ociosidade. */
    private double novaReputacao(Empresa empresa, ResultadoMensal resultado) {
        double efeitoOcupacao = (resultado.ocupacao() - 0.75) * 3.0;
        double efeitoMarketing = resultado.receita() <= 0
                ? 0
                : Math.min(empresa.getMarketingMensal() / resultado.receita(), 0.15) * 20.0;
        double efeitoPrejuizo = resultado.lucro() < 0 ? -1.5 : 0.5;
        double nova = empresa.getReputacao() + efeitoOcupacao + efeitoMarketing + efeitoPrejuizo;
        return Math.clamp(nova, 0, 100);
    }

    /** Divide os tributos entre os tres niveis de governo. */
    private void distribuirTributos(int turno, Empresa empresa, ResultadoMensal resultado,
                                    Pais pais, Estado estado, Municipio municipio) {
        double indireto = resultado.impostoIndireto();
        double renda = resultado.impostoRenda();
        if (indireto <= 0 && renda <= 0) {
            return;
        }
        // Tributo indireto fica com estado e municipio; imposto de renda e federal.
        double parteEstadual = indireto * 0.7;
        double parteMunicipal = indireto * 0.3;

        estado.setTesouro(estado.getTesouro() + parteEstadual);
        municipio.setTesouro(municipio.getTesouro() + parteMunicipal);
        pais.setTesouro(pais.getTesouro() + renda);
        repositorioEstado.save(estado);
        repositorioMunicipio.save(municipio);

        razao.registrar(turno, TipoLancamento.IMPOSTO, "empresa:" + empresa.getId(),
                "tesouros_publicos", indireto + renda, empresa.getId(), null,
                "Tributos do turno " + turno);
    }

    /** Caixa negativo vira divida automatica com juros de mercado. */
    private void aplicarEndividamento(int turno, Empresa empresa) {
        if (empresa.getCaixa() >= 0) {
            return;
        }
        double emprestimo = -empresa.getCaixa();
        empresa.setDivida(empresa.getDivida() + emprestimo);
        empresa.setCaixa(0);
        razao.registrar(turno, TipoLancamento.JUROS, "mercado_de_credito",
                "empresa:" + empresa.getId(), emprestimo, empresa.getId(), null,
                "Credito automatico para cobrir caixa negativo");
    }

    private boolean verificarFalencia(Empresa empresa) {
        double patrimonio = Math.max(empresa.getPatrimonio(), 1);
        if (empresa.getDivida() <= patrimonio * LIMITE_ALAVANCAGEM_FALENCIA) {
            return false;
        }
        empresa.setAtiva(false);
        auditoria.registrarSistema("EMPRESA_FALENCIA", "Empresa", empresa.getId(),
                "Empresa " + empresa.getNome() + " encerrou as atividades por insolvencia",
                Map.of("divida", empresa.getDivida(), "patrimonio", empresa.getPatrimonio()));
        return true;
    }

    private void registrarHistorico(int turno, Empresa empresa, ResultadoMensal resultado, double dividendos) {
        HistoricoEmpresa historico = new HistoricoEmpresa();
        historico.setEmpresa(empresa);
        historico.setTurno(turno);
        historico.setReceita(resultado.receita());
        historico.setCusto(resultado.custoOperacional());
        historico.setImpostos(resultado.impostoIndireto() + resultado.impostoRenda());
        historico.setLucro(resultado.lucro());
        historico.setCrescimentoLucro(empresa.getCrescimentoLucro());
        historico.setValuation(empresa.getValuation());
        historico.setPrecoAcao(empresa.getPrecoAcao());
        historico.setMarketShare(empresa.getMarketShare());
        historico.setCaixa(empresa.getCaixa());
        historico.setPatrimonio(empresa.getPatrimonio());
        historico.setFuncionarios(empresa.getFuncionarios());
        historico.setDividendosPagos(dividendos);
        repositorioHistorico.save(historico);
    }

    /** Avanca as obras: consome a parcela do turno e entrega o que ficou pronto. */
    private void processarEmpreendimentos(int turno) {
        for (Empreendimento obra : repositorioEmpreendimento.findByStatus(Empreendimento.StatusEmpreendimento.EM_OBRA)) {
            Empresa empresa = obra.getEmpresa();
            if (!empresa.isAtiva()) {
                obra.setStatus(Empreendimento.StatusEmpreendimento.CANCELADO);
                repositorioEmpreendimento.save(obra);
                continue;
            }
            double parcela = obra.getCustoTotal() / obra.getTurnosTotais();
            if (empresa.getCaixa() < parcela) {
                // Sem caixa a obra atrasa: o prazo escorrega um turno.
                obra.setTurnosTotais(obra.getTurnosTotais() + 1);
                repositorioEmpreendimento.save(obra);
                auditoria.registrarSistema("OBRA_ATRASADA", "Empreendimento", obra.getId(),
                        "Obra " + obra.getNome() + " atrasada por falta de caixa",
                        Map.of("parcela", parcela, "caixa", empresa.getCaixa()));
                continue;
            }
            empresa.setCaixa(empresa.getCaixa() - parcela);
            obra.setInvestido(obra.getInvestido() + parcela);
            obra.setTurnosRestantes(obra.getTurnosRestantes() - 1);
            razao.registrar(turno, TipoLancamento.OBRA, "empresa:" + empresa.getId(),
                    "obra:" + obra.getId(), parcela, empresa.getId(), null,
                    "Parcela da obra " + obra.getNome());

            if (obra.getTurnosRestantes() <= 0) {
                obra.setStatus(Empreendimento.StatusEmpreendimento.CONCLUIDO);
                obra.setTurnoConclusao(turno);
                empresa.setPatrimonio(empresa.getPatrimonio() + obra.getValorEstimado());
                razao.registrar(turno, TipoLancamento.ENTREGA_OBRA, "obra:" + obra.getId(),
                        "empresa:" + empresa.getId(), obra.getValorEstimado(), empresa.getId(), null,
                        "Entrega da obra " + obra.getNome());
                auditoria.registrarSistema("OBRA_CONCLUIDA", "Empreendimento", obra.getId(),
                        "Obra " + obra.getNome() + " concluida",
                        Map.of("valorEstimado", obra.getValorEstimado(), "investido", obra.getInvestido()));
            }
            repositorioEmpreendimento.save(obra);
            repositorioEmpresa.save(empresa);
        }
    }

    /**
     * Recalcula PIB, desemprego, inflacao, juros, aprovacao e estabilidade.
     * Os indicadores convergem devagar para o alvo, evitando oscilacao brusca.
     */
    private void atualizarMacroeconomia(EstadoJogo estado, Pais pais, ResumoEconomico economia) {
        double pibAnualizado = economia.receita * 12 * 0.65 + pais.getGastoSocialMensal() * 12;
        pais.setPib(pibAnualizado);

        double baseOcupacional = Math.max(pais.getPopulacao() * 0.01, 1);
        double alvoDesemprego = Math.clamp(0.13 - (economia.empregos / baseOcupacional) * 0.05, 0.025, 0.25);
        pais.setDesemprego(pais.getDesemprego() + (alvoDesemprego - pais.getDesemprego()) * 0.25);

        double pressaoFiscal = pibAnualizado <= 0 ? 0 : (pais.getGastoSocialMensal() * 12) / pibAnualizado;
        double alvoInflacao = Math.clamp(0.03 + pressaoFiscal * 0.8 - (estado.getTaxaJuros() - 0.08) * 0.35,
                -0.02, 0.35);
        estado.setInflacaoAnual(estado.getInflacaoAnual() + (alvoInflacao - estado.getInflacaoAnual()) * 0.3);

        // Regra de juros no estilo Taylor: reage ao desvio da meta de inflacao.
        double juros = Math.clamp(0.02 + 1.5 * (estado.getInflacaoAnual() - 0.03) + 0.03, 0.02, 0.40);
        estado.setTaxaJuros(estado.getTaxaJuros() + (juros - estado.getTaxaJuros()) * 0.5);

        double satisfacao = 50
                - (pais.getDesemprego() - 0.08) * 220
                - (estado.getInflacaoAnual() - 0.04) * 180
                + (economia.lucro > 0 ? 4 : -4);
        pais.setAprovacaoGoverno(Math.clamp(
                pais.getAprovacaoGoverno() + (satisfacao - pais.getAprovacaoGoverno()) * 0.25, 0, 100));
        pais.setEstabilidade(Math.clamp(
                pais.getEstabilidade() + (pais.getAprovacaoGoverno() - pais.getEstabilidade()) * 0.15, 0, 100));
        repositorioPais.save(pais);
    }

    /** Acumulador interno do turno. */
    private static final class ResumoEconomico {
        private int empresas;
        private int falencias;
        private int empregos;
        private double receita;
        private double lucro;
        private double impostos;
        private double dividendos;
    }
}
