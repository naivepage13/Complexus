package com.complexus.economia;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * Foto do resultado de uma empresa ao fim de um turno.
 * E a base das series historicas de lucro, valuation e preco da acao.
 */
@Entity
@Table(name = "historico_empresa", indexes = {
        @Index(name = "idx_historico_empresa_turno", columnList = "empresa_id,turno")
})
public class HistoricoEmpresa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "empresa_id")
    private Empresa empresa;

    @Column(nullable = false)
    private int turno;

    @Column(nullable = false)
    private double receita;

    @Column(nullable = false)
    private double custo;

    @Column(nullable = false)
    private double impostos;

    @Column(nullable = false)
    private double lucro;

    @Column(nullable = false)
    private double crescimentoLucro;

    @Column(nullable = false)
    private double valuation;

    @Column(nullable = false)
    private double precoAcao;

    @Column(nullable = false)
    private double marketShare;

    @Column(nullable = false)
    private double caixa;

    @Column(nullable = false)
    private double patrimonio;

    @Column(nullable = false)
    private int funcionarios;

    @Column(nullable = false)
    private double dividendosPagos;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Empresa getEmpresa() { return empresa; }
    public void setEmpresa(Empresa empresa) { this.empresa = empresa; }
    public int getTurno() { return turno; }
    public void setTurno(int turno) { this.turno = turno; }
    public double getReceita() { return receita; }
    public void setReceita(double receita) { this.receita = receita; }
    public double getCusto() { return custo; }
    public void setCusto(double custo) { this.custo = custo; }
    public double getImpostos() { return impostos; }
    public void setImpostos(double impostos) { this.impostos = impostos; }
    public double getLucro() { return lucro; }
    public void setLucro(double lucro) { this.lucro = lucro; }
    public double getCrescimentoLucro() { return crescimentoLucro; }
    public void setCrescimentoLucro(double crescimentoLucro) { this.crescimentoLucro = crescimentoLucro; }
    public double getValuation() { return valuation; }
    public void setValuation(double valuation) { this.valuation = valuation; }
    public double getPrecoAcao() { return precoAcao; }
    public void setPrecoAcao(double precoAcao) { this.precoAcao = precoAcao; }
    public double getMarketShare() { return marketShare; }
    public void setMarketShare(double marketShare) { this.marketShare = marketShare; }
    public double getCaixa() { return caixa; }
    public void setCaixa(double caixa) { this.caixa = caixa; }
    public double getPatrimonio() { return patrimonio; }
    public void setPatrimonio(double patrimonio) { this.patrimonio = patrimonio; }
    public int getFuncionarios() { return funcionarios; }
    public void setFuncionarios(int funcionarios) { this.funcionarios = funcionarios; }
    public double getDividendosPagos() { return dividendosPagos; }
    public void setDividendosPagos(double dividendosPagos) { this.dividendosPagos = dividendosPagos; }
}
