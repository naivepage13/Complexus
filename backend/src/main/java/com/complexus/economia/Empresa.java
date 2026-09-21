package com.complexus.economia;

import com.complexus.jogador.Jogador;
import com.complexus.politica.Municipio;
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
 * Empresa administrada por um jogador.
 *
 * O balanco e recalculado a cada turno (1 mes de jogo) por
 * {@code MotorSimulacao}. O valor de mercado (valuation) e lastreado por
 * patrimonio e lucro: valuation = patrimonio + lucro anualizado x multiplo do setor.
 */
@Entity
@Table(name = "empresa", indexes = {
        @Index(name = "idx_empresa_setor", columnList = "setor"),
        @Index(name = "idx_empresa_dono", columnList = "dono_id")
})
public class Empresa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String nome;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Setor setor;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "dono_id")
    private Jogador dono;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "municipio_id")
    private Municipio municipio;

    @Column(nullable = false)
    private int turnoFundacao;

    @Column(nullable = false)
    private boolean ativa = true;

    // ----- Posicao financeira -----

    /** Dinheiro disponivel em conta. */
    @Column(nullable = false)
    private double caixa;

    /** Ativos operacionais (instalacoes, estoque, terrenos, obras). */
    @Column(nullable = false)
    private double patrimonio;

    /** Divida onerosa acumulada. */
    @Column(nullable = false)
    private double divida;

    // ----- Resultado do ultimo turno -----

    @Column(nullable = false)
    private double receitaMensal;

    @Column(nullable = false)
    private double custoMensal;

    @Column(nullable = false)
    private double impostosMensais;

    @Column(nullable = false)
    private double lucroMensal;

    /** Variacao do lucro em relacao ao turno anterior (fracao). */
    @Column(nullable = false)
    private double crescimentoLucro;

    @Column(nullable = false)
    private double lucroAcumulado;

    // ----- Operacao -----

    @Column(nullable = false)
    private int funcionarios;

    @Column(nullable = false)
    private double salarioMedio = 2800.0;

    /** Produtividade 0.5 - 2.0. Sobe com capex e infraestrutura publica. */
    @Column(nullable = false)
    private double produtividade = 1.0;

    /** Investimento mensal em marketing, em R$ de jogo. */
    @Column(nullable = false)
    private double marketingMensal;

    /** Reputacao 0-100. Influencia participacao de mercado e preco da acao. */
    @Column(nullable = false)
    private double reputacao = 50.0;

    /** Participacao de mercado no setor, em fracao. */
    @Column(nullable = false)
    private double marketShare;

    // ----- Mercado de capitais -----

    @Column(nullable = false)
    private double valuation;

    @Column(nullable = false)
    private long acoesTotais = 1_000_000L;

    /** Acoes em poder de investidores (free float). */
    @Column(nullable = false)
    private long acoesEmCirculacao;

    @Column(nullable = false)
    private double precoAcao;

    @Column(nullable = false)
    private boolean capitalAberto = false;

    /** Fracao do lucro distribuida como dividendo aos investidores. */
    @Column(nullable = false)
    private double payout = 0.30;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public Setor getSetor() { return setor; }
    public void setSetor(Setor setor) { this.setor = setor; }
    public Jogador getDono() { return dono; }
    public void setDono(Jogador dono) { this.dono = dono; }
    public Municipio getMunicipio() { return municipio; }
    public void setMunicipio(Municipio municipio) { this.municipio = municipio; }
    public int getTurnoFundacao() { return turnoFundacao; }
    public void setTurnoFundacao(int turnoFundacao) { this.turnoFundacao = turnoFundacao; }
    public boolean isAtiva() { return ativa; }
    public void setAtiva(boolean ativa) { this.ativa = ativa; }
    public double getCaixa() { return caixa; }
    public void setCaixa(double caixa) { this.caixa = caixa; }
    public double getPatrimonio() { return patrimonio; }
    public void setPatrimonio(double patrimonio) { this.patrimonio = patrimonio; }
    public double getDivida() { return divida; }
    public void setDivida(double divida) { this.divida = divida; }
    public double getReceitaMensal() { return receitaMensal; }
    public void setReceitaMensal(double receitaMensal) { this.receitaMensal = receitaMensal; }
    public double getCustoMensal() { return custoMensal; }
    public void setCustoMensal(double custoMensal) { this.custoMensal = custoMensal; }
    public double getImpostosMensais() { return impostosMensais; }
    public void setImpostosMensais(double impostosMensais) { this.impostosMensais = impostosMensais; }
    public double getLucroMensal() { return lucroMensal; }
    public void setLucroMensal(double lucroMensal) { this.lucroMensal = lucroMensal; }
    public double getCrescimentoLucro() { return crescimentoLucro; }
    public void setCrescimentoLucro(double crescimentoLucro) { this.crescimentoLucro = crescimentoLucro; }
    public double getLucroAcumulado() { return lucroAcumulado; }
    public void setLucroAcumulado(double lucroAcumulado) { this.lucroAcumulado = lucroAcumulado; }
    public int getFuncionarios() { return funcionarios; }
    public void setFuncionarios(int funcionarios) { this.funcionarios = funcionarios; }
    public double getSalarioMedio() { return salarioMedio; }
    public void setSalarioMedio(double salarioMedio) { this.salarioMedio = salarioMedio; }
    public double getProdutividade() { return produtividade; }
    public void setProdutividade(double produtividade) { this.produtividade = produtividade; }
    public double getMarketingMensal() { return marketingMensal; }
    public void setMarketingMensal(double marketingMensal) { this.marketingMensal = marketingMensal; }
    public double getReputacao() { return reputacao; }
    public void setReputacao(double reputacao) { this.reputacao = reputacao; }
    public double getMarketShare() { return marketShare; }
    public void setMarketShare(double marketShare) { this.marketShare = marketShare; }
    public double getValuation() { return valuation; }
    public void setValuation(double valuation) { this.valuation = valuation; }
    public long getAcoesTotais() { return acoesTotais; }
    public void setAcoesTotais(long acoesTotais) { this.acoesTotais = acoesTotais; }
    public long getAcoesEmCirculacao() { return acoesEmCirculacao; }
    public void setAcoesEmCirculacao(long acoesEmCirculacao) { this.acoesEmCirculacao = acoesEmCirculacao; }
    public double getPrecoAcao() { return precoAcao; }
    public void setPrecoAcao(double precoAcao) { this.precoAcao = precoAcao; }
    public boolean isCapitalAberto() { return capitalAberto; }
    public void setCapitalAberto(boolean capitalAberto) { this.capitalAberto = capitalAberto; }
    public double getPayout() { return payout; }
    public void setPayout(double payout) { this.payout = payout; }

    /** Lastro: quanto do valor de mercado esta coberto por patrimonio liquido. */
    public double indiceLastro() {
        if (valuation <= 0) {
            return 0;
        }
        return (patrimonio + caixa - divida) / valuation;
    }

    public double patrimonioLiquido() {
        return patrimonio + caixa - divida;
    }
}
