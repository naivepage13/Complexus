package com.geohistoricalsim.estatistica;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;

/**
 * Estatisticas gerais consolidadas ao fim de cada turno.
 * Uma linha por turno, imutavel: e a serie historica do jogo.
 */
@Entity
@Table(name = "snapshot_turno")
public class SnapshotTurno {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private int turno;

    @Column(nullable = false)
    private Instant momento = Instant.now();

    @Column(nullable = false)
    private LocalDate dataJogo;

    @Column(nullable = false)
    private int empresasAtivas;

    @Column(nullable = false)
    private double receitaAgregada;

    @Column(nullable = false)
    private double lucroAgregado;

    @Column(nullable = false)
    private double impostosArrecadados;

    @Column(nullable = false)
    private double dividendosPagos;

    @Column(nullable = false)
    private double valuationAgregado;

    @Column(nullable = false)
    private double indiceMercado;

    @Column(nullable = false)
    private double variacaoIndice;

    @Column(nullable = false)
    private double inflacaoAnual;

    @Column(nullable = false)
    private double taxaJuros;

    @Column(nullable = false)
    private double pib;

    @Column(nullable = false)
    private double desemprego;

    @Column(nullable = false)
    private double estabilidade;

    @Column(nullable = false)
    private double aprovacaoGoverno;

    @Column(nullable = false)
    private int leisEmVigor;

    @Column(nullable = false)
    private int investidoresAtivos;

    @Column(nullable = false)
    private double capitalInvestido;

    @Column(nullable = false)
    private int empregosTotais;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public int getTurno() { return turno; }
    public void setTurno(int turno) { this.turno = turno; }
    public Instant getMomento() { return momento; }
    public void setMomento(Instant momento) { this.momento = momento; }
    public LocalDate getDataJogo() { return dataJogo; }
    public void setDataJogo(LocalDate dataJogo) { this.dataJogo = dataJogo; }
    public int getEmpresasAtivas() { return empresasAtivas; }
    public void setEmpresasAtivas(int empresasAtivas) { this.empresasAtivas = empresasAtivas; }
    public double getReceitaAgregada() { return receitaAgregada; }
    public void setReceitaAgregada(double receitaAgregada) { this.receitaAgregada = receitaAgregada; }
    public double getLucroAgregado() { return lucroAgregado; }
    public void setLucroAgregado(double lucroAgregado) { this.lucroAgregado = lucroAgregado; }
    public double getImpostosArrecadados() { return impostosArrecadados; }
    public void setImpostosArrecadados(double impostosArrecadados) { this.impostosArrecadados = impostosArrecadados; }
    public double getDividendosPagos() { return dividendosPagos; }
    public void setDividendosPagos(double dividendosPagos) { this.dividendosPagos = dividendosPagos; }
    public double getValuationAgregado() { return valuationAgregado; }
    public void setValuationAgregado(double valuationAgregado) { this.valuationAgregado = valuationAgregado; }
    public double getIndiceMercado() { return indiceMercado; }
    public void setIndiceMercado(double indiceMercado) { this.indiceMercado = indiceMercado; }
    public double getVariacaoIndice() { return variacaoIndice; }
    public void setVariacaoIndice(double variacaoIndice) { this.variacaoIndice = variacaoIndice; }
    public double getInflacaoAnual() { return inflacaoAnual; }
    public void setInflacaoAnual(double inflacaoAnual) { this.inflacaoAnual = inflacaoAnual; }
    public double getTaxaJuros() { return taxaJuros; }
    public void setTaxaJuros(double taxaJuros) { this.taxaJuros = taxaJuros; }
    public double getPib() { return pib; }
    public void setPib(double pib) { this.pib = pib; }
    public double getDesemprego() { return desemprego; }
    public void setDesemprego(double desemprego) { this.desemprego = desemprego; }
    public double getEstabilidade() { return estabilidade; }
    public void setEstabilidade(double estabilidade) { this.estabilidade = estabilidade; }
    public double getAprovacaoGoverno() { return aprovacaoGoverno; }
    public void setAprovacaoGoverno(double aprovacaoGoverno) { this.aprovacaoGoverno = aprovacaoGoverno; }
    public int getLeisEmVigor() { return leisEmVigor; }
    public void setLeisEmVigor(int leisEmVigor) { this.leisEmVigor = leisEmVigor; }
    public int getInvestidoresAtivos() { return investidoresAtivos; }
    public void setInvestidoresAtivos(int investidoresAtivos) { this.investidoresAtivos = investidoresAtivos; }
    public double getCapitalInvestido() { return capitalInvestido; }
    public void setCapitalInvestido(double capitalInvestido) { this.capitalInvestido = capitalInvestido; }
    public int getEmpregosTotais() { return empregosTotais; }
    public void setEmpregosTotais(int empregosTotais) { this.empregosTotais = empregosTotais; }
}
