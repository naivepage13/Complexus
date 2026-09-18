package com.geohistoricalsim.investimento;

import com.geohistoricalsim.economia.Empresa;
import com.geohistoricalsim.jogador.Jogador;
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
import jakarta.persistence.UniqueConstraint;

/**
 * Posicao acionaria de um jogador em uma empresa.
 *
 * O investimento e lastreado: o preco pago tem contrapartida em patrimonio e
 * lucro da empresa, e o retorno vem de dividendos mensais somados a variacao
 * do preco da acao.
 */
@Entity
@Table(name = "investimento",
        uniqueConstraints = @UniqueConstraint(name = "uk_investimento_jogador_empresa",
                columnNames = {"jogador_id", "empresa_id"}),
        indexes = @Index(name = "idx_investimento_empresa", columnList = "empresa_id"))
public class Investimento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "jogador_id")
    private Jogador jogador;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "empresa_id")
    private Empresa empresa;

    @Column(nullable = false)
    private long acoes;

    /** Capital total aportado liquido de vendas, em R$ de jogo. */
    @Column(nullable = false)
    private double capitalAportado;

    @Column(nullable = false)
    private double precoMedio;

    @Column(nullable = false)
    private double dividendosRecebidos;

    /** Resultado realizado em vendas de acoes. */
    @Column(nullable = false)
    private double lucroRealizado;

    @Column(nullable = false)
    private int turnoEntrada;

    @Column(nullable = false)
    private boolean ativo = true;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Jogador getJogador() { return jogador; }
    public void setJogador(Jogador jogador) { this.jogador = jogador; }
    public Empresa getEmpresa() { return empresa; }
    public void setEmpresa(Empresa empresa) { this.empresa = empresa; }
    public long getAcoes() { return acoes; }
    public void setAcoes(long acoes) { this.acoes = acoes; }
    public double getCapitalAportado() { return capitalAportado; }
    public void setCapitalAportado(double capitalAportado) { this.capitalAportado = capitalAportado; }
    public double getPrecoMedio() { return precoMedio; }
    public void setPrecoMedio(double precoMedio) { this.precoMedio = precoMedio; }
    public double getDividendosRecebidos() { return dividendosRecebidos; }
    public void setDividendosRecebidos(double dividendosRecebidos) { this.dividendosRecebidos = dividendosRecebidos; }
    public double getLucroRealizado() { return lucroRealizado; }
    public void setLucroRealizado(double lucroRealizado) { this.lucroRealizado = lucroRealizado; }
    public int getTurnoEntrada() { return turnoEntrada; }
    public void setTurnoEntrada(int turnoEntrada) { this.turnoEntrada = turnoEntrada; }
    public boolean isAtivo() { return ativo; }
    public void setAtivo(boolean ativo) { this.ativo = ativo; }
}
