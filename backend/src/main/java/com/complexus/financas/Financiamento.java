package com.complexus.financas;

import com.complexus.economia.Empresa;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * Divida contratada por uma empresa.
 *
 * A taxa e travada na contratacao: quem pegou credito barato continua pagando
 * barato mesmo que a Selic do jogo suba depois - e o que faz o momento de tomar
 * o emprestimo virar decisao, e nao detalhe.
 *
 * A amortizacao e constante (SAC): a parcela comeca alta e cai a cada turno,
 * porque os juros incidem sobre o saldo que ainda resta.
 */
@Entity
@Table(name = "financiamento", indexes = {
        @Index(name = "idx_financiamento_empresa", columnList = "empresa_id"),
        @Index(name = "idx_financiamento_status", columnList = "status")
})
public class Financiamento {

    /**
     * Linha de credito. Cada uma tem prazo, custo e limite proprios.
     *
     * @param spreadAnual     acrescimo sobre a taxa basica do jogo
     * @param prazoMinimo     prazo minimo em turnos
     * @param prazoMaximo     prazo maximo em turnos
     * @param fracaoDoLimite  quanto do patrimonio liquido a linha admite
     * @param exigeGarantia   se compromete patrimonio como garantia
     */
    public enum Modalidade {
        CAPITAL_DE_GIRO("Capital de giro", "Cobre o descasamento entre pagar e receber",
                0.040, 3, 18, 0.30, false),
        INVESTIMENTO("Investimento", "Financia expansao com garantia em patrimonio",
                0.020, 12, 60, 0.70, true),
        ANTECIPACAO_RECEBIVEIS("Antecipacao de recebiveis", "Adianta faturamento ja contratado",
                0.070, 1, 6, 0.25, false),
        ROTATIVO("Credito rotativo", "Socorro automatico de caixa negativo, no custo mais caro do mercado",
                0.220, 6, 6, 0.40, false);

        private final String rotulo;
        private final String descricao;
        private final double spreadAnual;
        private final int prazoMinimo;
        private final int prazoMaximo;
        private final double fracaoDoLimite;
        private final boolean exigeGarantia;

        Modalidade(String rotulo, String descricao, double spreadAnual, int prazoMinimo,
                   int prazoMaximo, double fracaoDoLimite, boolean exigeGarantia) {
            this.rotulo = rotulo;
            this.descricao = descricao;
            this.spreadAnual = spreadAnual;
            this.prazoMinimo = prazoMinimo;
            this.prazoMaximo = prazoMaximo;
            this.fracaoDoLimite = fracaoDoLimite;
            this.exigeGarantia = exigeGarantia;
        }

        public String getRotulo() { return rotulo; }
        public String getDescricao() { return descricao; }
        public double getSpreadAnual() { return spreadAnual; }
        public int getPrazoMinimo() { return prazoMinimo; }
        public int getPrazoMaximo() { return prazoMaximo; }
        public double getFracaoDoLimite() { return fracaoDoLimite; }
        public boolean isExigeGarantia() { return exigeGarantia; }
    }

    public enum Status {
        ATIVO("Em dia"),
        INADIMPLENTE("Em atraso"),
        RENEGOCIADO("Renegociado"),
        QUITADO("Quitado"),
        EXECUTADO("Garantia executada");

        private final String rotulo;

        Status(String rotulo) {
            this.rotulo = rotulo;
        }

        public String getRotulo() { return rotulo; }
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "empresa_id")
    private Empresa empresa;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private Modalidade modalidade;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private Status status = Status.ATIVO;

    /** Valor originalmente liberado no caixa da empresa. */
    @Column(nullable = false)
    private double principal;

    @Column(nullable = false)
    private double saldoDevedor;

    /** Taxa mensal travada na contratacao. */
    @Column(nullable = false)
    private double taxaMensal;

    /** Nota de credito da empresa no dia da contratacao, para leitura do historico. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 2)
    private NotaCredito notaNaContratacao = NotaCredito.B;

    @Column(nullable = false)
    private int prazoTurnos;

    @Column(nullable = false)
    private int turnosRestantes;

    @Column(nullable = false)
    private int turnoContratacao;

    /** Patrimonio comprometido como garantia; so as linhas com garantia usam. */
    @Column(nullable = false)
    private double garantia;

    @Column(nullable = false)
    private double jurosPagos;

    @Column(nullable = false)
    private double amortizado;

    /** Parcelas seguidas nao pagas. Tres derrubam a garantia ou a empresa. */
    @Column(nullable = false)
    private int parcelasEmAtraso;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Empresa getEmpresa() { return empresa; }
    public void setEmpresa(Empresa empresa) { this.empresa = empresa; }
    public Modalidade getModalidade() { return modalidade; }
    public void setModalidade(Modalidade modalidade) { this.modalidade = modalidade; }
    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }
    public double getPrincipal() { return principal; }
    public void setPrincipal(double principal) { this.principal = principal; }
    public double getSaldoDevedor() { return saldoDevedor; }
    public void setSaldoDevedor(double saldoDevedor) { this.saldoDevedor = saldoDevedor; }
    public double getTaxaMensal() { return taxaMensal; }
    public void setTaxaMensal(double taxaMensal) { this.taxaMensal = taxaMensal; }
    public NotaCredito getNotaNaContratacao() { return notaNaContratacao; }
    public void setNotaNaContratacao(NotaCredito notaNaContratacao) { this.notaNaContratacao = notaNaContratacao; }
    public int getPrazoTurnos() { return prazoTurnos; }
    public void setPrazoTurnos(int prazoTurnos) { this.prazoTurnos = prazoTurnos; }
    public int getTurnosRestantes() { return turnosRestantes; }
    public void setTurnosRestantes(int turnosRestantes) { this.turnosRestantes = turnosRestantes; }
    public int getTurnoContratacao() { return turnoContratacao; }
    public void setTurnoContratacao(int turnoContratacao) { this.turnoContratacao = turnoContratacao; }
    public double getGarantia() { return garantia; }
    public void setGarantia(double garantia) { this.garantia = garantia; }
    public double getJurosPagos() { return jurosPagos; }
    public void setJurosPagos(double jurosPagos) { this.jurosPagos = jurosPagos; }
    public double getAmortizado() { return amortizado; }
    public void setAmortizado(double amortizado) { this.amortizado = amortizado; }
    public int getParcelasEmAtraso() { return parcelasEmAtraso; }
    public void setParcelasEmAtraso(int parcelasEmAtraso) { this.parcelasEmAtraso = parcelasEmAtraso; }

    public boolean emAberto() {
        return status == Status.ATIVO || status == Status.INADIMPLENTE || status == Status.RENEGOCIADO;
    }

    /** Juros do mes sobre o saldo que ainda resta. */
    public double jurosDoMes() {
        return saldoDevedor * taxaMensal;
    }

    /** Amortizacao constante do mes (SAC). */
    public double amortizacaoDoMes() {
        if (turnosRestantes <= 0) {
            return saldoDevedor;
        }
        return saldoDevedor / turnosRestantes;
    }

    /** O que sai do caixa neste turno: amortizacao mais juros. */
    public double parcelaDoMes() {
        return amortizacaoDoMes() + jurosDoMes();
    }

    public double taxaAnual() {
        return taxaMensal * 12;
    }
}
