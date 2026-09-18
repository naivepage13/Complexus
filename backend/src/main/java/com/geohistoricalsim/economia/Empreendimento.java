package com.geohistoricalsim.economia;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * Obra ou projeto de longo prazo de uma empresa dos setores imobiliario e de
 * construcao. Consome caixa durante os turnos de obra e, ao concluir, vira
 * patrimonio (imobiliario) ou receita de entrega (construcao).
 */
@Entity
@Table(name = "empreendimento")
public class Empreendimento {

    public enum TipoEmpreendimento {
        RESIDENCIAL("Residencial", 1.35),
        COMERCIAL("Comercial", 1.45),
        INDUSTRIAL("Industrial", 1.25),
        INFRAESTRUTURA("Infraestrutura", 1.18);

        private final String rotulo;
        /** Multiplicador do custo que estima o valor de mercado ao concluir. */
        private final double multiplicadorValor;

        TipoEmpreendimento(String rotulo, double multiplicadorValor) {
            this.rotulo = rotulo;
            this.multiplicadorValor = multiplicadorValor;
        }

        public String getRotulo() { return rotulo; }
        public double getMultiplicadorValor() { return multiplicadorValor; }
    }

    public enum StatusEmpreendimento {
        EM_OBRA, CONCLUIDO, VENDIDO, CANCELADO
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "empresa_id")
    private Empresa empresa;

    @Column(nullable = false, length = 120)
    private String nome;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoEmpreendimento tipo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatusEmpreendimento status = StatusEmpreendimento.EM_OBRA;

    @Column(nullable = false)
    private double custoTotal;

    @Column(nullable = false)
    private double investido;

    @Column(nullable = false)
    private double valorEstimado;

    @Column(nullable = false)
    private int turnosTotais;

    @Column(nullable = false)
    private int turnosRestantes;

    @Column(nullable = false)
    private int turnoInicio;

    private Integer turnoConclusao;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Empresa getEmpresa() { return empresa; }
    public void setEmpresa(Empresa empresa) { this.empresa = empresa; }
    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public TipoEmpreendimento getTipo() { return tipo; }
    public void setTipo(TipoEmpreendimento tipo) { this.tipo = tipo; }
    public StatusEmpreendimento getStatus() { return status; }
    public void setStatus(StatusEmpreendimento status) { this.status = status; }
    public double getCustoTotal() { return custoTotal; }
    public void setCustoTotal(double custoTotal) { this.custoTotal = custoTotal; }
    public double getInvestido() { return investido; }
    public void setInvestido(double investido) { this.investido = investido; }
    public double getValorEstimado() { return valorEstimado; }
    public void setValorEstimado(double valorEstimado) { this.valorEstimado = valorEstimado; }
    public int getTurnosTotais() { return turnosTotais; }
    public void setTurnosTotais(int turnosTotais) { this.turnosTotais = turnosTotais; }
    public int getTurnosRestantes() { return turnosRestantes; }
    public void setTurnosRestantes(int turnosRestantes) { this.turnosRestantes = turnosRestantes; }
    public int getTurnoInicio() { return turnoInicio; }
    public void setTurnoInicio(int turnoInicio) { this.turnoInicio = turnoInicio; }
    public Integer getTurnoConclusao() { return turnoConclusao; }
    public void setTurnoConclusao(Integer turnoConclusao) { this.turnoConclusao = turnoConclusao; }

    public double percentualConcluido() {
        if (turnosTotais <= 0) {
            return 1.0;
        }
        return (turnosTotais - turnosRestantes) / (double) turnosTotais;
    }
}
