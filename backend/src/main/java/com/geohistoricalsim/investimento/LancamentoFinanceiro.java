package com.geohistoricalsim.investimento;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * Livro-razao do jogo: toda movimentacao de dinheiro vira um lancamento.
 *
 * E o lastro contabil das estatisticas e o contraponto financeiro da linha de
 * auditoria: a auditoria registra o fato, o lancamento registra o valor.
 */
@Entity
@Table(name = "lancamento_financeiro", indexes = {
        @Index(name = "idx_lancamento_turno", columnList = "turno"),
        @Index(name = "idx_lancamento_empresa", columnList = "empresaId"),
        @Index(name = "idx_lancamento_jogador", columnList = "jogadorId")
})
public class LancamentoFinanceiro {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Instant momento = Instant.now();

    @Column(nullable = false)
    private int turno;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TipoLancamento tipo;

    @Column(nullable = false, length = 60)
    private String origem;

    @Column(nullable = false, length = 60)
    private String destino;

    @Column(nullable = false)
    private double valor;

    private Long empresaId;

    private Long jogadorId;

    @Column(length = 300)
    private String descricao;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Instant getMomento() { return momento; }
    public void setMomento(Instant momento) { this.momento = momento; }
    public int getTurno() { return turno; }
    public void setTurno(int turno) { this.turno = turno; }
    public TipoLancamento getTipo() { return tipo; }
    public void setTipo(TipoLancamento tipo) { this.tipo = tipo; }
    public String getOrigem() { return origem; }
    public void setOrigem(String origem) { this.origem = origem; }
    public String getDestino() { return destino; }
    public void setDestino(String destino) { this.destino = destino; }
    public double getValor() { return valor; }
    public void setValor(double valor) { this.valor = valor; }
    public Long getEmpresaId() { return empresaId; }
    public void setEmpresaId(Long empresaId) { this.empresaId = empresaId; }
    public Long getJogadorId() { return jogadorId; }
    public void setJogadorId(Long jogadorId) { this.jogadorId = jogadorId; }
    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { this.descricao = descricao; }
}
