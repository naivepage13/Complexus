package com.complexus.economia;

import com.complexus.politica.Municipio;
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
 * Unidade operacional da empresa: a fabrica, a loja, o canteiro.
 *
 * E a unidade, e nao a empresa, que disputa mercado: cada filial compete no
 * municipio onde esta instalada, com a equipe e o patrimonio alocados nela. A
 * empresa e a soma das suas unidades mais a camada financeira (caixa, divida e
 * tributo sobre o lucro).
 *
 * Toda empresa nasce com uma unidade sede e nunca fica sem nenhuma: fechar a
 * ultima unidade equivale a encerrar a empresa.
 */
@Entity
@Table(name = "unidade", indexes = {
        @Index(name = "idx_unidade_empresa", columnList = "empresa_id"),
        @Index(name = "idx_unidade_municipio", columnList = "municipio_id")
})
public class Unidade {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "empresa_id")
    private Empresa empresa;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "municipio_id")
    private Municipio municipio;

    @Column(nullable = false, length = 120)
    private String nome;

    /** A sede acompanha o municipio da empresa e recebe o que nao tem filial definida. */
    @Column(nullable = false)
    private boolean sede;

    @Column(nullable = false)
    private boolean ativa = true;

    @Column(nullable = false)
    private int turnoAbertura;

    // ----- Capacidade instalada -----

    /** Ativos operacionais alocados nesta unidade. */
    @Column(nullable = false)
    private double patrimonio;

    @Column(nullable = false)
    private int funcionarios;

    /** Produtividade 0.4 - 2.0 desta unidade. Sobe com capex e com P&D. */
    @Column(nullable = false)
    private double produtividade = 1.0;

    // ----- Resultado do ultimo turno -----

    @Column(nullable = false)
    private double receitaMensal;

    @Column(nullable = false)
    private double custoMensal;

    /** Sobra da operacao antes de juros, estrutura e imposto de renda. */
    @Column(nullable = false)
    private double margemOperacional;

    @Column(nullable = false)
    private double ocupacao;

    @Column(nullable = false)
    private double marketShare;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Empresa getEmpresa() { return empresa; }
    public void setEmpresa(Empresa empresa) { this.empresa = empresa; }
    public Municipio getMunicipio() { return municipio; }
    public void setMunicipio(Municipio municipio) { this.municipio = municipio; }
    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public boolean isSede() { return sede; }
    public void setSede(boolean sede) { this.sede = sede; }
    public boolean isAtiva() { return ativa; }
    public void setAtiva(boolean ativa) { this.ativa = ativa; }
    public int getTurnoAbertura() { return turnoAbertura; }
    public void setTurnoAbertura(int turnoAbertura) { this.turnoAbertura = turnoAbertura; }
    public double getPatrimonio() { return patrimonio; }
    public void setPatrimonio(double patrimonio) { this.patrimonio = patrimonio; }
    public int getFuncionarios() { return funcionarios; }
    public void setFuncionarios(int funcionarios) { this.funcionarios = funcionarios; }
    public double getProdutividade() { return produtividade; }
    public void setProdutividade(double produtividade) { this.produtividade = produtividade; }
    public double getReceitaMensal() { return receitaMensal; }
    public void setReceitaMensal(double receitaMensal) { this.receitaMensal = receitaMensal; }
    public double getCustoMensal() { return custoMensal; }
    public void setCustoMensal(double custoMensal) { this.custoMensal = custoMensal; }
    public double getMargemOperacional() { return margemOperacional; }
    public void setMargemOperacional(double margemOperacional) { this.margemOperacional = margemOperacional; }
    public double getOcupacao() { return ocupacao; }
    public void setOcupacao(double ocupacao) { this.ocupacao = ocupacao; }
    public double getMarketShare() { return marketShare; }
    public void setMarketShare(double marketShare) { this.marketShare = marketShare; }
}
