package com.complexus.economia;

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
 * Linha de produto da empresa: o que ela vende e em que faixa.
 *
 * O mix das linhas define o preco que a empresa pratica em relacao ao preco de
 * referencia do setor e o custo do insumo que ela consome. E a decisao de
 * posicionamento: vender barato e girar volume, ou cobrar caro com custo maior
 * e correr o risco de perder demanda para o concorrente.
 */
@Entity
@Table(name = "linha_produto", indexes = @Index(name = "idx_linha_empresa", columnList = "empresa_id"))
public class LinhaProduto {

    /**
     * Faixa de mercado da linha.
     *
     * @param fatorPreco preco praticado sobre o preco de referencia do setor
     * @param fatorCusto custo de insumo sobre o padrao do setor
     */
    public enum Posicionamento {
        POPULAR("Popular", 0.85, 0.93),
        MEDIO("Medio", 1.00, 1.00),
        PREMIUM("Premium", 1.30, 1.12);

        private final String rotulo;
        private final double fatorPreco;
        private final double fatorCusto;

        Posicionamento(String rotulo, double fatorPreco, double fatorCusto) {
            this.rotulo = rotulo;
            this.fatorPreco = fatorPreco;
            this.fatorCusto = fatorCusto;
        }

        public String getRotulo() { return rotulo; }
        public double getFatorPreco() { return fatorPreco; }
        public double getFatorCusto() { return fatorCusto; }
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
    private Posicionamento posicionamento = Posicionamento.MEDIO;

    /** Participacao da linha no faturamento planejado, em fracao. */
    @Column(nullable = false)
    private double fatiaMix;

    @Column(nullable = false)
    private boolean ativa = true;

    @Column(nullable = false)
    private int turnoCriacao;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Empresa getEmpresa() { return empresa; }
    public void setEmpresa(Empresa empresa) { this.empresa = empresa; }
    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public Posicionamento getPosicionamento() { return posicionamento; }
    public void setPosicionamento(Posicionamento posicionamento) { this.posicionamento = posicionamento; }
    public double getFatiaMix() { return fatiaMix; }
    public void setFatiaMix(double fatiaMix) { this.fatiaMix = fatiaMix; }
    public boolean isAtiva() { return ativa; }
    public void setAtiva(boolean ativa) { this.ativa = ativa; }
    public int getTurnoCriacao() { return turnoCriacao; }
    public void setTurnoCriacao(int turnoCriacao) { this.turnoCriacao = turnoCriacao; }
}
