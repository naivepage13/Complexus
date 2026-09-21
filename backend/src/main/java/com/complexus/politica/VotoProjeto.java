package com.complexus.politica;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/** Voto nominal de um mandato em um projeto. Um voto por mandato por projeto. */
@Entity
@Table(name = "voto_projeto",
        uniqueConstraints = @UniqueConstraint(name = "uk_voto_mandato_projeto",
                columnNames = {"projeto_id", "mandato_id"}))
public class VotoProjeto {

    public enum Opcao {
        SIM, NAO, ABSTENCAO
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "projeto_id")
    private ProjetoDeLei projeto;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "mandato_id")
    private Mandato mandato;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 12)
    private Opcao opcao;

    @Column(nullable = false)
    private int turno;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public ProjetoDeLei getProjeto() { return projeto; }
    public void setProjeto(ProjetoDeLei projeto) { this.projeto = projeto; }
    public Mandato getMandato() { return mandato; }
    public void setMandato(Mandato mandato) { this.mandato = mandato; }
    public Opcao getOpcao() { return opcao; }
    public void setOpcao(Opcao opcao) { this.opcao = opcao; }
    public int getTurno() { return turno; }
    public void setTurno(int turno) { this.turno = turno; }
}
