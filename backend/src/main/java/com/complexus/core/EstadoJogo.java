package com.complexus.core;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;

/**
 * Relogio e indicadores globais da partida. Linha unica (id = 1).
 *
 * Regra temporal: 1 turno = 1 hora de tempo real = 1 mes de tempo de jogo.
 */
@Entity
@Table(name = "estado_jogo")
public class EstadoJogo {

    public static final long ID_UNICO = 1L;

    @Id
    private Long id = ID_UNICO;

    @Column(nullable = false)
    private int turnoAtual = 0;

    /** Data corrente no calendario do jogo (avanca 1 mes por turno). */
    @Column(nullable = false)
    private LocalDate dataJogo = LocalDate.of(2025, 1, 1);

    @Column(nullable = false)
    private Instant iniciadoEm = Instant.now();

    private Instant ultimoProcessamento;

    private Instant proximoProcessamento;

    /** Indice sintetico do mercado de acoes do jogo (base 1000). */
    @Column(nullable = false)
    private double indiceMercado = 1000.0;

    /** Inflacao anualizada corrente, em fracao (0.045 = 4,5 por cento ao ano). */
    @Column(nullable = false)
    private double inflacaoAnual = 0.045;

    /** Taxa basica de juros anual, em fracao. */
    @Column(nullable = false)
    private double taxaJuros = 0.1075;

    @Column(nullable = false)
    private boolean processando = false;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public int getTurnoAtual() { return turnoAtual; }
    public void setTurnoAtual(int turnoAtual) { this.turnoAtual = turnoAtual; }
    public LocalDate getDataJogo() { return dataJogo; }
    public void setDataJogo(LocalDate dataJogo) { this.dataJogo = dataJogo; }
    public Instant getIniciadoEm() { return iniciadoEm; }
    public void setIniciadoEm(Instant iniciadoEm) { this.iniciadoEm = iniciadoEm; }
    public Instant getUltimoProcessamento() { return ultimoProcessamento; }
    public void setUltimoProcessamento(Instant ultimoProcessamento) { this.ultimoProcessamento = ultimoProcessamento; }
    public Instant getProximoProcessamento() { return proximoProcessamento; }
    public void setProximoProcessamento(Instant proximoProcessamento) { this.proximoProcessamento = proximoProcessamento; }
    public double getIndiceMercado() { return indiceMercado; }
    public void setIndiceMercado(double indiceMercado) { this.indiceMercado = indiceMercado; }
    public double getInflacaoAnual() { return inflacaoAnual; }
    public void setInflacaoAnual(double inflacaoAnual) { this.inflacaoAnual = inflacaoAnual; }
    public double getTaxaJuros() { return taxaJuros; }
    public void setTaxaJuros(double taxaJuros) { this.taxaJuros = taxaJuros; }
    public boolean isProcessando() { return processando; }
    public void setProcessando(boolean processando) { this.processando = processando; }
}
