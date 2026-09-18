package com.geohistoricalsim.politica;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Unidade federativa de topo. Concentra os agregados macroeconomicos que a
 * simulacao de turno recalcula e as aliquotas federais definidas por lei.
 */
@Entity
@Table(name = "pais")
public class Pais {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 80)
    private String nome;

    @Column(nullable = false, length = 3)
    private String sigla;

    @Column(nullable = false)
    private long populacao;

    /** Caixa do tesouro nacional, em R$ de jogo. */
    @Column(nullable = false)
    private double tesouro;

    /** PIB anualizado, em R$ de jogo. */
    @Column(nullable = false)
    private double pib;

    /** Aliquota federal sobre o lucro das empresas (fracao). */
    @Column(nullable = false)
    private double aliquotaImpostoEmpresarial = 0.15;

    /** Gasto social mensal do governo federal, em R$ de jogo. */
    @Column(nullable = false)
    private double gastoSocialMensal;

    /** Estabilidade institucional 0-100. Afeta risco e confianca do mercado. */
    @Column(nullable = false)
    private double estabilidade = 75.0;

    /** Aprovacao popular do governo 0-100. */
    @Column(nullable = false)
    private double aprovacaoGoverno = 50.0;

    /** Desemprego em fracao (0.08 = 8 por cento). */
    @Column(nullable = false)
    private double desemprego = 0.08;

    /** Renda media mensal por habitante, em R$ de jogo. */
    @Column(nullable = false)
    private double rendaMedia = 3200.0;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public String getSigla() { return sigla; }
    public void setSigla(String sigla) { this.sigla = sigla; }
    public long getPopulacao() { return populacao; }
    public void setPopulacao(long populacao) { this.populacao = populacao; }
    public double getTesouro() { return tesouro; }
    public void setTesouro(double tesouro) { this.tesouro = tesouro; }
    public double getPib() { return pib; }
    public void setPib(double pib) { this.pib = pib; }
    public double getAliquotaImpostoEmpresarial() { return aliquotaImpostoEmpresarial; }
    public void setAliquotaImpostoEmpresarial(double v) { this.aliquotaImpostoEmpresarial = v; }
    public double getGastoSocialMensal() { return gastoSocialMensal; }
    public void setGastoSocialMensal(double v) { this.gastoSocialMensal = v; }
    public double getEstabilidade() { return estabilidade; }
    public void setEstabilidade(double estabilidade) { this.estabilidade = estabilidade; }
    public double getAprovacaoGoverno() { return aprovacaoGoverno; }
    public void setAprovacaoGoverno(double v) { this.aprovacaoGoverno = v; }
    public double getDesemprego() { return desemprego; }
    public void setDesemprego(double desemprego) { this.desemprego = desemprego; }
    public double getRendaMedia() { return rendaMedia; }
    public void setRendaMedia(double rendaMedia) { this.rendaMedia = rendaMedia; }
}
