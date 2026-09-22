package com.complexus.cadeia;

import com.complexus.auditoria.ServicoAuditoria;
import com.complexus.comum.RecursoNaoEncontradoException;
import com.complexus.comum.RegraDeNegocioException;
import com.complexus.core.ServicoEstadoJogo;
import com.complexus.economia.EfeitoCadeia;
import com.complexus.economia.Empresa;
import com.complexus.economia.MotorSimulacao;
import com.complexus.economia.PerfilOperacional;
import com.complexus.economia.RepositorioEmpresa;
import com.complexus.economia.ServicoEstrutura;
import com.complexus.economia.Setor;
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
 * Cadeia produtiva: contratos de fornecimento entre empresas.
 *
 * <h2>O que o contrato muda</h2>
 * Para o <b>comprador</b>, a parte do insumo coberta por contrato passa a ter
 * preco travado e deixa de sofrer o choque de custo do turno. Para o
 * <b>fornecedor</b>, e receita garantida que ocupa capacidade: o que sai pelo
 * contrato nao disputa o mercado aberto da cidade.
 *
 * <h2>Limites</h2>
 * Ninguem vende mais do que produz nem compra mais insumo do que consome, entao
 * o volume tem teto dos dois lados. O preco fica entre 70 e 130 por cento da
 * referencia do setor: fora disso o contrato viraria transferencia disfarcada
 * de patrimonio entre empresas do mesmo dono.
 *
 * <h2>Empresas do sistema</h2>
 * Empresas sem dono respondem por regra: aceitam fornecer com desconto de ate 5
 * por cento e aceitam comprar pagando ate 5 por cento acima da referencia. E o
 * que permite ao jogador negociar desde o turno zero, sem depender de outro
 * jogador estar online.
 */
@Service
public class ServicoCadeia {

    private static final List<ContratoFornecimento.Status> VIGENTES =
            List.of(ContratoFornecimento.Status.ATIVO);

    /** Teto do volume sobre a capacidade produtiva do fornecedor. */
    private static final double TETO_CAPACIDADE_FORNECEDOR = 0.30;
    /** Teto do volume sobre o insumo que o comprador consome por turno. */
    private static final double TETO_INSUMO_COMPRADOR = 0.40;
    private static final double PRECO_MINIMO = 0.70;
    private static final double PRECO_MAXIMO = 1.30;
    private static final int PRAZO_MINIMO = 3;
    private static final int PRAZO_MAXIMO = 36;
    /** Multa de quem rompe o contrato antes do prazo, sobre o valor remanescente. */
    private static final double MULTA_ROMPIMENTO = 0.10;
    /** Desconto maximo que uma empresa do sistema aceita dar ao fornecer. */
    private static final double TOLERANCIA_NPC = 0.05;

    private final RepositorioContratoFornecimento repositorio;
    private final RepositorioEmpresa repositorioEmpresa;
    private final ServicoEstrutura servicoEstrutura;
    private final ServicoJogador servicoJogador;
    private final MotorSimulacao motor;
    private final ServicoRazao razao;
    private final ServicoAuditoria auditoria;
    private final ServicoEstadoJogo estadoJogo;

    public ServicoCadeia(RepositorioContratoFornecimento repositorio,
                         RepositorioEmpresa repositorioEmpresa,
                         ServicoEstrutura servicoEstrutura,
                         ServicoJogador servicoJogador,
                         MotorSimulacao motor,
                         ServicoRazao razao,
                         ServicoAuditoria auditoria,
                         ServicoEstadoJogo estadoJogo) {
        this.repositorio = repositorio;
        this.repositorioEmpresa = repositorioEmpresa;
        this.servicoEstrutura = servicoEstrutura;
        this.servicoJogador = servicoJogador;
        this.motor = motor;
        this.razao = razao;
        this.auditoria = auditoria;
        this.estadoJogo = estadoJogo;
    }

    // ------------------------------------------------------------------
    // Consultas
    // ------------------------------------------------------------------

    @Transactional(readOnly = true)
    public List<ContratoFornecimento> daEmpresa(Long empresaId) {
        return repositorio.daEmpresa(empresaId);
    }

    @Transactional(readOnly = true)
    public List<ContratoFornecimento> vigentesComoFornecedor(Long empresaId) {
        return repositorio.findByFornecedorIdAndStatus(empresaId, ContratoFornecimento.Status.ATIVO);
    }

    @Transactional(readOnly = true)
    public List<ContratoFornecimento> vigentesComoComprador(Long empresaId) {
        return repositorio.findByCompradorIdAndStatus(empresaId, ContratoFornecimento.Status.ATIVO);
    }

    @Transactional(readOnly = true)
    public ContratoFornecimento buscar(Long id) {
        return repositorio.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("ContratoFornecimento", id));
    }

    /** Parceiros possiveis para um tipo de insumo, com o que cada um consegue entregar. */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> parceiros(Long empresaId, TipoInsumo tipo, boolean comoFornecedor) {
        Empresa empresa = buscarEmpresa(empresaId);
        Setor setorProcurado = comoFornecedor
                ? null // qualquer setor comprador do tipo serve
                : tipo.getSetorFornecedor();

        List<Map<String, Object>> parceiros = new ArrayList<>();
        for (Empresa outra : repositorioEmpresa.findByAtivaTrue()) {
            if (outra.getId().equals(empresa.getId())) {
                continue;
            }
            boolean compativel = comoFornecedor
                    ? tipo.aceita(empresa.getSetor(), outra.getSetor())
                    : tipo.aceita(outra.getSetor(), empresa.getSetor());
            if (!compativel || (setorProcurado != null && outra.getSetor() != setorProcurado)) {
                continue;
            }
            Map<String, Object> linha = new LinkedHashMap<>();
            linha.put("empresaId", outra.getId());
            linha.put("nome", outra.getNome());
            linha.put("setor", outra.getSetor().getRotulo());
            linha.put("municipio", outra.getMunicipio().getNome());
            linha.put("doSistema", outra.getDono() == null);
            linha.put("capacidadeLivreParaContrato", capacidadeLivreParaContrato(outra));
            linha.put("insumoDisponivelParaContrato", insumoLivreParaContrato(outra));
            linha.put("reputacao", outra.getReputacao());
            parceiros.add(linha);
        }
        return parceiros;
    }

    /** Quanto a empresa ainda pode se comprometer a fornecer por turno. */
    @Transactional(readOnly = true)
    public double capacidadeLivreParaContrato(Empresa empresa) {
        double teto = capacidadeTotal(empresa) * TETO_CAPACIDADE_FORNECEDOR;
        double comprometido = repositorio.volumeFornecido(empresa.getId(), VIGENTES);
        return Math.max(teto - comprometido, 0);
    }

    /** Quanto de insumo a empresa ainda pode contratar por turno. */
    @Transactional(readOnly = true)
    public double insumoLivreParaContrato(Empresa empresa) {
        double insumoMensal = capacidadeTotal(empresa) * (1 - empresa.getSetor().getMargemBase());
        double teto = insumoMensal * TETO_INSUMO_COMPRADOR;
        double contratado = repositorio.volumeComprado(empresa.getId(), VIGENTES);
        return Math.max(teto - contratado, 0);
    }

    // ------------------------------------------------------------------
    // Negociacao
    // ------------------------------------------------------------------

    /**
     * Propoe um contrato. Quem propoe pode ser o fornecedor ou o comprador; a
     * outra parte aceita ou recusa. Se a contraparte for uma empresa do sistema,
     * a resposta sai na hora, pela regra de preco.
     */
    @Transactional
    public ContratoFornecimento propor(Long empresaId, Long jogadorId, Long contraparteId,
                                       TipoInsumo tipo, boolean comoFornecedor,
                                       double volumeMensal, double precoRelativo, int prazoTurnos) {
        Empresa empresa = buscarEmpresa(empresaId);
        Jogador jogador = exigirDono(empresa, jogadorId);
        Empresa contraparte = buscarEmpresa(contraparteId);
        if (contraparte.getId().equals(empresa.getId())) {
            throw new RegraDeNegocioException("Uma empresa nao contrata consigo mesma.");
        }
        if (!contraparte.isAtiva()) {
            throw new RegraDeNegocioException("A empresa " + contraparte.getNome() + " esta inativa.");
        }

        Empresa fornecedor = comoFornecedor ? empresa : contraparte;
        Empresa comprador = comoFornecedor ? contraparte : empresa;
        validar(tipo, fornecedor, comprador, volumeMensal, precoRelativo, prazoTurnos);

        int turno = estadoJogo.turnoAtual();
        ContratoFornecimento contrato = new ContratoFornecimento();
        contrato.setFornecedor(fornecedor);
        contrato.setComprador(comprador);
        contrato.setTipo(tipo);
        contrato.setPropostoPor(comoFornecedor ? "FORNECEDOR" : "COMPRADOR");
        contrato.setVolumeMensal(volumeMensal);
        contrato.setPrecoRelativo(precoRelativo);
        contrato.setPrazoTurnos(prazoTurnos);
        contrato.setTurnosRestantes(prazoTurnos);
        contrato.setTurnoProposta(turno);
        ContratoFornecimento salvo = repositorio.save(contrato);

        Map<String, Object> detalhes = new LinkedHashMap<>();
        detalhes.put("tipo", tipo.name());
        detalhes.put("fornecedor", fornecedor.getNome());
        detalhes.put("comprador", comprador.getNome());
        detalhes.put("volumeMensal", volumeMensal);
        detalhes.put("precoRelativo", precoRelativo);
        detalhes.put("prazoTurnos", prazoTurnos);
        auditoria.registrar(jogador.getUsuario(), "FORNECIMENTO_PROPOSTO", "ContratoFornecimento",
                salvo.getId(), "Proposta de fornecimento de " + tipo.getRotulo(), detalhes);

        Empresa respondente = comoFornecedor ? comprador : fornecedor;
        if (respondente.getDono() == null) {
            responderPeloSistema(salvo, comoFornecedor, turno);
        }
        return salvo;
    }

    /** Aceita uma proposta recebida. Quem aceita e o dono da contraparte. */
    @Transactional
    public ContratoFornecimento aceitar(Long contratoId, Long jogadorId) {
        ContratoFornecimento contrato = buscar(contratoId);
        Empresa respondente = respondente(contrato);
        Jogador jogador = exigirDono(respondente, jogadorId);
        if (contrato.getStatus() != ContratoFornecimento.Status.PROPOSTO) {
            throw new RegraDeNegocioException("Esta proposta ja foi respondida.");
        }
        // Os limites sao conferidos de novo: o mundo andou desde a proposta.
        validar(contrato.getTipo(), contrato.getFornecedor(), contrato.getComprador(),
                contrato.getVolumeMensal(), contrato.getPrecoRelativo(), contrato.getPrazoTurnos());

        int turno = estadoJogo.turnoAtual();
        contrato.setStatus(ContratoFornecimento.Status.ATIVO);
        contrato.setTurnoInicio(turno);
        repositorio.save(contrato);

        auditoria.registrar(jogador.getUsuario(), "FORNECIMENTO_ACEITO", "ContratoFornecimento",
                contrato.getId(), "Contrato de fornecimento em vigor entre "
                        + contrato.getFornecedor().getNome() + " e " + contrato.getComprador().getNome(),
                Map.of("volumeMensal", contrato.getVolumeMensal(),
                        "precoRelativo", contrato.getPrecoRelativo(),
                        "prazoTurnos", contrato.getPrazoTurnos()));
        return contrato;
    }

    @Transactional
    public ContratoFornecimento recusar(Long contratoId, Long jogadorId) {
        ContratoFornecimento contrato = buscar(contratoId);
        Jogador jogador = exigirDono(respondente(contrato), jogadorId);
        if (contrato.getStatus() != ContratoFornecimento.Status.PROPOSTO) {
            throw new RegraDeNegocioException("Esta proposta ja foi respondida.");
        }
        contrato.setStatus(ContratoFornecimento.Status.RECUSADO);
        contrato.setTurnoEncerramento(estadoJogo.turnoAtual());
        repositorio.save(contrato);
        auditoria.registrar(jogador.getUsuario(), "FORNECIMENTO_RECUSADO", "ContratoFornecimento",
                contrato.getId(), "Proposta de fornecimento recusada", Map.of());
        return contrato;
    }

    /**
     * Rompe um contrato em vigor. Quem rompe paga multa sobre o que ainda seria
     * movimentado ate o fim do prazo - e o que impede fechar contrato bom hoje
     * e abandonar no primeiro turno ruim.
     */
    @Transactional
    public Map<String, Object> romper(Long contratoId, Long jogadorId) {
        ContratoFornecimento contrato = buscar(contratoId);
        if (contrato.getStatus() != ContratoFornecimento.Status.ATIVO) {
            throw new RegraDeNegocioException("So um contrato em vigor pode ser rompido.");
        }
        Jogador jogador = servicoJogador.buscar(jogadorId);
        boolean ehFornecedor = pertence(contrato.getFornecedor(), jogador);
        boolean ehComprador = pertence(contrato.getComprador(), jogador);
        if (!ehFornecedor && !ehComprador) {
            throw new RegraDeNegocioException("Apenas as partes do contrato podem rompe-lo.");
        }

        Empresa quemRompe = ehFornecedor ? contrato.getFornecedor() : contrato.getComprador();
        Empresa prejudicada = ehFornecedor ? contrato.getComprador() : contrato.getFornecedor();
        double multa = contrato.valorRemanescente() * MULTA_ROMPIMENTO;
        multa = Math.min(multa, Math.max(quemRompe.getCaixa(), 0));

        quemRompe.setCaixa(quemRompe.getCaixa() - multa);
        prejudicada.setCaixa(prejudicada.getCaixa() + multa);
        // Quebrar contrato queima a marca no mercado.
        quemRompe.setReputacao(Math.max(quemRompe.getReputacao() - 4.0, 0));
        repositorioEmpresa.save(quemRompe);
        repositorioEmpresa.save(prejudicada);

        int turno = estadoJogo.turnoAtual();
        contrato.setStatus(ContratoFornecimento.Status.ROMPIDO);
        contrato.setTurnoEncerramento(turno);
        repositorio.save(contrato);

        if (multa > 0) {
            razao.registrar(turno, TipoLancamento.MULTA_CONTRATUAL, "empresa:" + quemRompe.getId(),
                    "empresa:" + prejudicada.getId(), multa, quemRompe.getId(), jogador.getId(),
                    "Multa por rompimento do contrato " + contrato.getId());
        }
        Map<String, Object> resumo = new LinkedHashMap<>();
        resumo.put("contratoId", contrato.getId());
        resumo.put("multa", multa);
        resumo.put("quemRompeu", quemRompe.getNome());
        resumo.put("indenizada", prejudicada.getNome());
        auditoria.registrar(jogador.getUsuario(), "FORNECIMENTO_ROMPIDO", "ContratoFornecimento",
                contrato.getId(), "Contrato rompido por " + quemRompe.getNome(), resumo);
        return resumo;
    }

    // ------------------------------------------------------------------
    // Rotina do turno
    // ------------------------------------------------------------------

    /**
     * Apura, antes de qualquer empresa produzir, o que sera entregue no turno.
     *
     * A entrega e limitada pela capacidade real do fornecedor: quem encolheu
     * entrega menos, registra falha e perde reputacao - e o comprador recebe
     * menos insumo contratado no mesmo turno, nao no seguinte. Fazer a apuracao
     * antes da producao e o que mantem as duas pontas coerentes.
     *
     * Nenhum dinheiro se move aqui: a receita do fornecedor e o custo do
     * comprador entram no resultado de cada um, e e o resultado que mexe no
     * caixa no fechamento do turno.
     *
     * @return apuracao por empresa, para fornecedores e compradores
     */
    @Transactional
    public Map<Long, ApuracaoCadeia> apurarEntregas(int turno) {
        Map<Long, Acumulador> acumuladores = new LinkedHashMap<>();
        List<ContratoFornecimento> ativos =
                repositorio.findByStatus(ContratoFornecimento.Status.ATIVO);

        // Agrupa por fornecedor para respeitar a capacidade de cada um.
        Map<Long, List<ContratoFornecimento>> porFornecedor = new LinkedHashMap<>();
        for (ContratoFornecimento contrato : ativos) {
            porFornecedor.computeIfAbsent(contrato.getFornecedor().getId(), id -> new ArrayList<>())
                    .add(contrato);
        }

        for (List<ContratoFornecimento> contratos : porFornecedor.values()) {
            Empresa fornecedor = contratos.get(0).getFornecedor();
            if (!fornecedor.isAtiva()) {
                for (ContratoFornecimento contrato : contratos) {
                    encerrar(contrato, turno, ContratoFornecimento.Status.ROMPIDO,
                            "Fornecedor fora de operacao: contrato encerrado");
                }
                continue;
            }
            double disponivel = capacidadeTotal(fornecedor);

            for (ContratoFornecimento contrato : contratos) {
                Empresa comprador = contrato.getComprador();
                if (!comprador.isAtiva()) {
                    encerrar(contrato, turno, ContratoFornecimento.Status.ROMPIDO,
                            "Comprador fora de operacao: contrato encerrado");
                    continue;
                }
                double volume = Math.min(contrato.getVolumeMensal(), Math.max(disponivel, 0));
                if (volume < contrato.getVolumeMensal() - 0.01) {
                    contrato.setFalhasDeEntrega(contrato.getFalhasDeEntrega() + 1);
                    // Entregar menos do que prometeu custa reputacao.
                    fornecedor.setReputacao(Math.max(fornecedor.getReputacao() - 2.0, 0));
                    repositorioEmpresa.save(fornecedor);
                    auditoria.registrarSistema("FORNECIMENTO_FALHA", "ContratoFornecimento",
                            contrato.getId(), "Entrega parcial de " + fornecedor.getNome(),
                            Map.of("contratado", contrato.getVolumeMensal(), "entregue", volume));
                }
                disponivel -= volume;

                double valor = volume * contrato.getPrecoRelativo();
                double insumoDoFornecedor = volume * (1 - fornecedor.getSetor().getMargemBase());
                acumuladores.computeIfAbsent(fornecedor.getId(), id -> new Acumulador())
                        .fornecer(volume, valor, insumoDoFornecedor);
                acumuladores.computeIfAbsent(comprador.getId(), id -> new Acumulador())
                        .comprar(volume, valor);

                contrato.setTotalFaturado(contrato.getTotalFaturado() + valor);
                contrato.setTurnosRestantes(contrato.getTurnosRestantes() - 1);
                razao.registrar(turno, TipoLancamento.FORNECIMENTO, "empresa:" + comprador.getId(),
                        "empresa:" + fornecedor.getId(), valor, fornecedor.getId(), null,
                        "Fornecimento de " + contrato.getTipo().getRotulo() + " no turno " + turno);

                if (contrato.getTurnosRestantes() <= 0) {
                    encerrar(contrato, turno, ContratoFornecimento.Status.CONCLUIDO,
                            "Contrato cumprido ate o fim do prazo");
                } else {
                    repositorio.save(contrato);
                }
            }
        }

        Map<Long, ApuracaoCadeia> apuracao = new LinkedHashMap<>();
        acumuladores.forEach((empresaId, acumulador) -> apuracao.put(empresaId, acumulador.fechar()));
        return apuracao;
    }

    /** Acumula as duas pontas de uma empresa durante a apuracao do turno. */
    private static final class Acumulador {
        private double capacidadeReservada;
        private double receitaContratos;
        private double custoContratos;
        private double insumoContratado;
        private double pagoPeloInsumo;

        private void fornecer(double volume, double valor, double insumo) {
            capacidadeReservada += volume;
            receitaContratos += valor;
            custoContratos += insumo;
        }

        private void comprar(double volume, double valor) {
            insumoContratado += volume;
            pagoPeloInsumo += valor;
        }

        private ApuracaoCadeia fechar() {
            double preco = insumoContratado > 0 ? pagoPeloInsumo / insumoContratado : 1.0;
            return new ApuracaoCadeia(capacidadeReservada, insumoContratado, preco,
                    receitaContratos, custoContratos);
        }
    }

    /** Cancela os contratos de uma empresa que saiu do jogo, sem multa. */
    @Transactional
    public void encerrarContratosDe(Empresa empresa, int turno, String motivo) {
        for (ContratoFornecimento contrato : repositorio.daEmpresa(empresa.getId())) {
            if (contrato.getStatus() == ContratoFornecimento.Status.ATIVO
                    || contrato.getStatus() == ContratoFornecimento.Status.PROPOSTO) {
                encerrar(contrato, turno, ContratoFornecimento.Status.ROMPIDO, motivo);
            }
        }
    }

    // ------------------------------------------------------------------

    /** Resposta automatica das empresas do sistema, guiada so pelo preco. */
    private void responderPeloSistema(ContratoFornecimento contrato, boolean propostoPeloFornecedor,
                                      int turno) {
        boolean aceita = propostoPeloFornecedor
                // O sistema compra se o preco nao passar de 5 por cento acima da referencia.
                ? contrato.getPrecoRelativo() <= 1 + TOLERANCIA_NPC
                // E fornece se o desconto pedido nao passar de 5 por cento.
                : contrato.getPrecoRelativo() >= 1 - TOLERANCIA_NPC;

        contrato.setStatus(aceita ? ContratoFornecimento.Status.ATIVO
                : ContratoFornecimento.Status.RECUSADO);
        if (aceita) {
            contrato.setTurnoInicio(turno);
        } else {
            contrato.setTurnoEncerramento(turno);
        }
        repositorio.save(contrato);
        auditoria.registrarSistema(aceita ? "FORNECIMENTO_ACEITO" : "FORNECIMENTO_RECUSADO",
                "ContratoFornecimento", contrato.getId(),
                "Empresa do sistema respondeu a proposta de fornecimento",
                Map.of("precoRelativo", contrato.getPrecoRelativo(), "aceito", aceita));
    }

    private void encerrar(ContratoFornecimento contrato, int turno,
                          ContratoFornecimento.Status status, String motivo) {
        contrato.setStatus(status);
        contrato.setTurnoEncerramento(turno);
        repositorio.save(contrato);
        auditoria.registrarSistema("FORNECIMENTO_ENCERRADO", "ContratoFornecimento", contrato.getId(),
                motivo, Map.of("status", status.name(), "totalFaturado", contrato.getTotalFaturado()));
    }

    private void validar(TipoInsumo tipo, Empresa fornecedor, Empresa comprador,
                         double volumeMensal, double precoRelativo, int prazoTurnos) {
        if (!tipo.aceita(fornecedor.getSetor(), comprador.getSetor())) {
            throw new RegraDeNegocioException(tipo.getRotulo() + " vai do setor "
                    + tipo.getSetorFornecedor().getRotulo() + " para "
                    + tipo.getSetoresCompradores().stream().map(Setor::getRotulo).toList() + ".");
        }
        if (precoRelativo < PRECO_MINIMO || precoRelativo > PRECO_MAXIMO) {
            throw new RegraDeNegocioException("O preco do contrato fica entre "
                    + PRECO_MINIMO + " e " + PRECO_MAXIMO + " da referencia do setor.");
        }
        if (prazoTurnos < PRAZO_MINIMO || prazoTurnos > PRAZO_MAXIMO) {
            throw new RegraDeNegocioException("O prazo do contrato vai de " + PRAZO_MINIMO
                    + " a " + PRAZO_MAXIMO + " turnos.");
        }
        if (volumeMensal <= 0) {
            throw new RegraDeNegocioException("O volume mensal deve ser positivo.");
        }
        double capacidadeLivre = capacidadeLivreParaContrato(fornecedor);
        if (volumeMensal > capacidadeLivre + 0.01) {
            throw new RegraDeNegocioException(fornecedor.getNome() + " so pode comprometer mais R$ "
                    + String.format("%.2f", capacidadeLivre) + " por turno em contratos.");
        }
        double insumoLivre = insumoLivreParaContrato(comprador);
        if (volumeMensal > insumoLivre + 0.01) {
            throw new RegraDeNegocioException(comprador.getNome() + " so consome mais R$ "
                    + String.format("%.2f", insumoLivre) + " de insumo contratado por turno.");
        }
    }

    /** Capacidade produtiva somada das unidades, sem descontar o ja comprometido. */
    private double capacidadeTotal(Empresa empresa) {
        double total = 0;
        for (Unidade unidade : servicoEstrutura.unidades(empresa.getId())) {
            PerfilOperacional perfil = servicoEstrutura.perfil(unidade, empresa, 1.0, 1.0, 0.0);
            total += motor.capacidadeProdutiva(perfil);
        }
        return total;
    }

    private Empresa respondente(ContratoFornecimento contrato) {
        return "FORNECEDOR".equals(contrato.getPropostoPor())
                ? contrato.getComprador()
                : contrato.getFornecedor();
    }

    private boolean pertence(Empresa empresa, Jogador jogador) {
        return empresa.getDono() != null && empresa.getDono().getId().equals(jogador.getId());
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
