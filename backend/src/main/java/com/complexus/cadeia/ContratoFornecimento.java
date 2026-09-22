package com.complexus.cadeia;

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
 * Contrato de fornecimento entre duas empresas.
 *
 * O volume e medido em R$ por turno a preco de referencia do setor, e o
 * {@code precoRelativo} diz quanto se paga sobre essa referencia. Para o
 * comprador, o contrato troca insumo de mercado - sujeito ao choque de custo do
 * turno - por insumo de preco travado. Para o fornecedor, e receita garantida
 * que ocupa capacidade: o que vai para o contrato nao vai para a prateleira.
 */
@Entity
@Table(name = "contrato_fornecimento", indexes = {
        @Index(name = "idx_contrato_fornecedor", columnList = "fornecedor_id"),
        @Index(name = "idx_contrato_comprador", columnList = "comprador_id"),
        @Index(name = "idx_contrato_status", columnList = "status")
})
public class ContratoFornecimento {

    public enum Status {
        PROPOSTO("Aguardando resposta"),
        ATIVO("Em vigor"),
        RECUSADO("Recusado"),
        CONCLUIDO("Concluido"),
        ROMPIDO("Rompido");

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
    @JoinColumn(name = "fornecedor_id")
    private Empresa fornecedor;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "comprador_id")
    private Empresa comprador;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TipoInsumo tipo;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private Status status = Status.PROPOSTO;

    /** Quem enviou a proposta; a outra parte aceita, recusa ou deixa vencer. */
    @Column(nullable = false, length = 12)
    private String propostoPor;

    /** Volume mensal em R$ a preco de referencia do setor. */
    @Column(nullable = false)
    private double volumeMensal;

    /** Preco pago sobre a referencia: 0,85 e desconto de 15 por cento. */
    @Column(nullable = false)
    private double precoRelativo = 1.0;

    @Column(nullable = false)
    private int prazoTurnos;

    @Column(nullable = false)
    private int turnosRestantes;

    @Column(nullable = false)
    private int turnoProposta;

    private Integer turnoInicio;

    private Integer turnoEncerramento;

    /** Total ja faturado pelo fornecedor neste contrato. */
    @Column(nullable = false)
    private double totalFaturado;

    /** Turnos em que o fornecedor nao tinha capacidade para entregar tudo. */
    @Column(nullable = false)
    private int falhasDeEntrega;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Empresa getFornecedor() { return fornecedor; }
    public void setFornecedor(Empresa fornecedor) { this.fornecedor = fornecedor; }
    public Empresa getComprador() { return comprador; }
    public void setComprador(Empresa comprador) { this.comprador = comprador; }
    public TipoInsumo getTipo() { return tipo; }
    public void setTipo(TipoInsumo tipo) { this.tipo = tipo; }
    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }
    public String getPropostoPor() { return propostoPor; }
    public void setPropostoPor(String propostoPor) { this.propostoPor = propostoPor; }
    public double getVolumeMensal() { return volumeMensal; }
    public void setVolumeMensal(double volumeMensal) { this.volumeMensal = volumeMensal; }
    public double getPrecoRelativo() { return precoRelativo; }
    public void setPrecoRelativo(double precoRelativo) { this.precoRelativo = precoRelativo; }
    public int getPrazoTurnos() { return prazoTurnos; }
    public void setPrazoTurnos(int prazoTurnos) { this.prazoTurnos = prazoTurnos; }
    public int getTurnosRestantes() { return turnosRestantes; }
    public void setTurnosRestantes(int turnosRestantes) { this.turnosRestantes = turnosRestantes; }
    public int getTurnoProposta() { return turnoProposta; }
    public void setTurnoProposta(int turnoProposta) { this.turnoProposta = turnoProposta; }
    public Integer getTurnoInicio() { return turnoInicio; }
    public void setTurnoInicio(Integer turnoInicio) { this.turnoInicio = turnoInicio; }
    public Integer getTurnoEncerramento() { return turnoEncerramento; }
    public void setTurnoEncerramento(Integer turnoEncerramento) { this.turnoEncerramento = turnoEncerramento; }
    public double getTotalFaturado() { return totalFaturado; }
    public void setTotalFaturado(double totalFaturado) { this.totalFaturado = totalFaturado; }
    public int getFalhasDeEntrega() { return falhasDeEntrega; }
    public void setFalhasDeEntrega(int falhasDeEntrega) { this.falhasDeEntrega = falhasDeEntrega; }

    /** Faturamento do turno para o fornecedor. */
    public double faturamentoMensal() {
        return volumeMensal * precoRelativo;
    }

    /** O que o comprador economiza por turno em relacao ao insumo de mercado. */
    public double economiaMensalDoComprador() {
        return volumeMensal * (1 - precoRelativo);
    }

    /** Valor que ainda seria movimentado ate o fim do prazo. */
    public double valorRemanescente() {
        return faturamentoMensal() * Math.max(turnosRestantes, 0);
    }
}
