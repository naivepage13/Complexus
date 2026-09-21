package com.complexus.politica;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/** Unidade federativa intermediaria (governador, deputados estaduais, ICMS). */
@Entity
@Table(name = "estado_federativo")
public class Estado {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 80)
    private String nome;

    @Column(nullable = false, length = 3)
    private String sigla;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "pais_id")
    private Pais pais;

    @Column(nullable = false)
    private long populacao;

    @Column(nullable = false)
    private double tesouro;

    /** Aliquota estadual sobre circulacao de bens (fracao). */
    @Column(nullable = false)
    private double aliquotaEstadual = 0.12;

    /** Investimento mensal em infraestrutura, em R$ de jogo. */
    @Column(nullable = false)
    private double investimentoInfraestrutura;

    /** Indice de desenvolvimento 0-100: produtividade e atratividade da regiao. */
    @Column(nullable = false)
    private double indiceDesenvolvimento = 60.0;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public String getSigla() { return sigla; }
    public void setSigla(String sigla) { this.sigla = sigla; }
    public Pais getPais() { return pais; }
    public void setPais(Pais pais) { this.pais = pais; }
    public long getPopulacao() { return populacao; }
    public void setPopulacao(long populacao) { this.populacao = populacao; }
    public double getTesouro() { return tesouro; }
    public void setTesouro(double tesouro) { this.tesouro = tesouro; }
    public double getAliquotaEstadual() { return aliquotaEstadual; }
    public void setAliquotaEstadual(double v) { this.aliquotaEstadual = v; }
    public double getInvestimentoInfraestrutura() { return investimentoInfraestrutura; }
    public void setInvestimentoInfraestrutura(double v) { this.investimentoInfraestrutura = v; }
    public double getIndiceDesenvolvimento() { return indiceDesenvolvimento; }
    public void setIndiceDesenvolvimento(double v) { this.indiceDesenvolvimento = v; }
}
