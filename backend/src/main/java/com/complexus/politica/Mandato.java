package com.complexus.politica;

import com.complexus.jogador.Jogador;
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
 * Ocupacao de um cargo politico por um jogador (ou por um NPC) em um territorio
 * e por uma janela de turnos.
 *
 * O territorio e identificado por {@code esfera} + {@code territorioId}:
 * FEDERAL aponta para um Pais, ESTADUAL para um Estado e MUNICIPAL para um Municipio.
 */
@Entity
@Table(name = "mandato", indexes = {
        @Index(name = "idx_mandato_territorio", columnList = "esfera,territorioId"),
        @Index(name = "idx_mandato_cargo", columnList = "cargo")
})
public class Mandato {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Nulo quando a cadeira e ocupada por um NPC. */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "jogador_id")
    private Jogador jogador;

    @Column(nullable = false, length = 120)
    private String titular;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private CargoPolitico cargo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 12)
    private Esfera esfera;

    @Column(nullable = false)
    private Long territorioId;

    @Column(length = 40)
    private String partido;

    @Column(nullable = false)
    private int turnoInicio;

    @Column(nullable = false)
    private int turnoFim;

    @Column(nullable = false)
    private boolean ativo = true;

    @Column(nullable = false)
    private boolean npc = false;

    /** Aprovacao do mandatario 0-100: base para reeleicao e para peso politico. */
    @Column(nullable = false)
    private double aprovacao = 50.0;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Jogador getJogador() { return jogador; }
    public void setJogador(Jogador jogador) { this.jogador = jogador; }
    public String getTitular() { return titular; }
    public void setTitular(String titular) { this.titular = titular; }
    public CargoPolitico getCargo() { return cargo; }
    public void setCargo(CargoPolitico cargo) { this.cargo = cargo; }
    public Esfera getEsfera() { return esfera; }
    public void setEsfera(Esfera esfera) { this.esfera = esfera; }
    public Long getTerritorioId() { return territorioId; }
    public void setTerritorioId(Long territorioId) { this.territorioId = territorioId; }
    public String getPartido() { return partido; }
    public void setPartido(String partido) { this.partido = partido; }
    public int getTurnoInicio() { return turnoInicio; }
    public void setTurnoInicio(int turnoInicio) { this.turnoInicio = turnoInicio; }
    public int getTurnoFim() { return turnoFim; }
    public void setTurnoFim(int turnoFim) { this.turnoFim = turnoFim; }
    public boolean isAtivo() { return ativo; }
    public void setAtivo(boolean ativo) { this.ativo = ativo; }
    public boolean isNpc() { return npc; }
    public void setNpc(boolean npc) { this.npc = npc; }
    public double getAprovacao() { return aprovacao; }
    public void setAprovacao(double aprovacao) { this.aprovacao = aprovacao; }
}
