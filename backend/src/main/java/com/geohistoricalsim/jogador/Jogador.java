package com.geohistoricalsim.jogador;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * Conta de jogador. Um jogador pode acumular papeis: empresario (dono de
 * empresas), investidor (posicoes acionarias) e agente politico (mandatos).
 */
@Entity
@Table(name = "jogador")
public class Jogador {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 40)
    private String usuario;

    @Column(nullable = false, length = 120)
    private String nome;

    @Column(nullable = false, length = 128)
    private String senhaHash;

    /** Caixa pessoal do jogador, em R$ de jogo. */
    @Column(nullable = false)
    private double saldo;

    @Column(nullable = false)
    private Instant criadoEm = Instant.now();

    @Column(nullable = false)
    private boolean ativo = true;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getUsuario() { return usuario; }
    public void setUsuario(String usuario) { this.usuario = usuario; }
    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public String getSenhaHash() { return senhaHash; }
    public void setSenhaHash(String senhaHash) { this.senhaHash = senhaHash; }
    public double getSaldo() { return saldo; }
    public void setSaldo(double saldo) { this.saldo = saldo; }
    public Instant getCriadoEm() { return criadoEm; }
    public void setCriadoEm(Instant criadoEm) { this.criadoEm = criadoEm; }
    public boolean isAtivo() { return ativo; }
    public void setAtivo(boolean ativo) { this.ativo = ativo; }
}
