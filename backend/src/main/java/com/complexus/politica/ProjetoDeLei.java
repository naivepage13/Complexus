package com.complexus.politica;

import com.complexus.economia.Setor;
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
 * Projeto de lei em tramitacao ou em vigor.
 *
 * O campo {@code parametro} e o numero que a lei fixa (aliquota, valor de
 * investimento, indice de zoneamento). Quando sancionada, a lei altera o
 * territorio correspondente e passa a influenciar a simulacao economica.
 */
@Entity
@Table(name = "projeto_lei", indexes = {
        @Index(name = "idx_projeto_status", columnList = "status"),
        @Index(name = "idx_projeto_territorio", columnList = "esfera,territorioId")
})
public class ProjetoDeLei {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 160)
    private String titulo;

    @Column(length = 600)
    private String ementa;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "autor_mandato_id")
    private Mandato autor;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 12)
    private Esfera esfera;

    @Column(nullable = false)
    private Long territorioId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private TipoProjeto tipo;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private Setor setorAlvo;

    @Column(nullable = false)
    private double parametro;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatusProjeto status = StatusProjeto.RASCUNHO;

    @Column(nullable = false)
    private int turnoCriacao;

    private Integer turnoVotacao;

    private Integer turnoVigencia;

    @Column(nullable = false)
    private int votosSim;

    @Column(nullable = false)
    private int votosNao;

    @Column(nullable = false)
    private int votosAbstencao;

    @Column(length = 400)
    private String justificativaVeto;

    /** Efeito registrado no momento da sancao, para rastreabilidade. */
    @Column(length = 300)
    private String efeitoAplicado;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) { this.titulo = titulo; }
    public String getEmenta() { return ementa; }
    public void setEmenta(String ementa) { this.ementa = ementa; }
    public Mandato getAutor() { return autor; }
    public void setAutor(Mandato autor) { this.autor = autor; }
    public Esfera getEsfera() { return esfera; }
    public void setEsfera(Esfera esfera) { this.esfera = esfera; }
    public Long getTerritorioId() { return territorioId; }
    public void setTerritorioId(Long territorioId) { this.territorioId = territorioId; }
    public TipoProjeto getTipo() { return tipo; }
    public void setTipo(TipoProjeto tipo) { this.tipo = tipo; }
    public Setor getSetorAlvo() { return setorAlvo; }
    public void setSetorAlvo(Setor setorAlvo) { this.setorAlvo = setorAlvo; }
    public double getParametro() { return parametro; }
    public void setParametro(double parametro) { this.parametro = parametro; }
    public StatusProjeto getStatus() { return status; }
    public void setStatus(StatusProjeto status) { this.status = status; }
    public int getTurnoCriacao() { return turnoCriacao; }
    public void setTurnoCriacao(int turnoCriacao) { this.turnoCriacao = turnoCriacao; }
    public Integer getTurnoVotacao() { return turnoVotacao; }
    public void setTurnoVotacao(Integer turnoVotacao) { this.turnoVotacao = turnoVotacao; }
    public Integer getTurnoVigencia() { return turnoVigencia; }
    public void setTurnoVigencia(Integer turnoVigencia) { this.turnoVigencia = turnoVigencia; }
    public int getVotosSim() { return votosSim; }
    public void setVotosSim(int votosSim) { this.votosSim = votosSim; }
    public int getVotosNao() { return votosNao; }
    public void setVotosNao(int votosNao) { this.votosNao = votosNao; }
    public int getVotosAbstencao() { return votosAbstencao; }
    public void setVotosAbstencao(int votosAbstencao) { this.votosAbstencao = votosAbstencao; }
    public String getJustificativaVeto() { return justificativaVeto; }
    public void setJustificativaVeto(String justificativaVeto) { this.justificativaVeto = justificativaVeto; }
    public String getEfeitoAplicado() { return efeitoAplicado; }
    public void setEfeitoAplicado(String efeitoAplicado) { this.efeitoAplicado = efeitoAplicado; }
}
