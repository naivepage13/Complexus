package com.geohistoricalsim.politica;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * Municipio: sede das empresas e base do jogo politico local
 * (prefeito, vereadores, ISS, IPTU e zoneamento urbano).
 */
@Entity
@Table(name = "municipio")
public class Municipio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 80)
    private String nome;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "estado_id")
    private Estado estado;

    @Column(nullable = false)
    private long populacao;

    @Column(nullable = false)
    private double tesouro;

    /** Aliquota municipal sobre servicos (fracao). */
    @Column(nullable = false)
    private double aliquotaMunicipal = 0.05;

    /** Indice de urbanizacao 0-100: quanto maior, maior o valor do metro quadrado. */
    @Column(nullable = false)
    private double indiceUrbanizacao = 55.0;

    /** Demanda imobiliaria 0-200 (100 = equilibrio entre oferta e procura). */
    @Column(nullable = false)
    private double demandaImobiliaria = 100.0;

    /** Custo medio do terreno por metro quadrado, em R$ de jogo. */
    @Column(nullable = false)
    private double custoTerrenoM2 = 1200.0;

    /**
     * Permissividade do zoneamento 0-100. Zoneamento mais permissivo barateia
     * empreendimentos, porem pressiona o indice de urbanizacao.
     */
    @Column(nullable = false)
    private double zoneamento = 50.0;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public Estado getEstado() { return estado; }
    public void setEstado(Estado estado) { this.estado = estado; }
    public long getPopulacao() { return populacao; }
    public void setPopulacao(long populacao) { this.populacao = populacao; }
    public double getTesouro() { return tesouro; }
    public void setTesouro(double tesouro) { this.tesouro = tesouro; }
    public double getAliquotaMunicipal() { return aliquotaMunicipal; }
    public void setAliquotaMunicipal(double v) { this.aliquotaMunicipal = v; }
    public double getIndiceUrbanizacao() { return indiceUrbanizacao; }
    public void setIndiceUrbanizacao(double v) { this.indiceUrbanizacao = v; }
    public double getDemandaImobiliaria() { return demandaImobiliaria; }
    public void setDemandaImobiliaria(double v) { this.demandaImobiliaria = v; }
    public double getCustoTerrenoM2() { return custoTerrenoM2; }
    public void setCustoTerrenoM2(double v) { this.custoTerrenoM2 = v; }
    public double getZoneamento() { return zoneamento; }
    public void setZoneamento(double zoneamento) { this.zoneamento = zoneamento; }
}
