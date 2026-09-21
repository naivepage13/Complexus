package com.complexus.estatistica;

import com.complexus.economia.Setor;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

/** Consolidacao por setor economico em um turno. */
@Entity
@Table(name = "estatistica_setor", indexes = {
        @Index(name = "idx_estatistica_setor_turno", columnList = "turno,setor")
})
public class EstatisticaSetor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private int turno;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Setor setor;

    @Column(nullable = false)
    private int empresas;

    @Column(nullable = false)
    private double receita;

    @Column(nullable = false)
    private double lucro;

    @Column(nullable = false)
    private double valuation;

    @Column(nullable = false)
    private double crescimentoMedio;

    @Column(nullable = false)
    private double margemMedia;

    @Column(length = 120)
    private String empresaLider;

    @Column(nullable = false)
    private int empregos;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public int getTurno() { return turno; }
    public void setTurno(int turno) { this.turno = turno; }
    public Setor getSetor() { return setor; }
    public void setSetor(Setor setor) { this.setor = setor; }
    public int getEmpresas() { return empresas; }
    public void setEmpresas(int empresas) { this.empresas = empresas; }
    public double getReceita() { return receita; }
    public void setReceita(double receita) { this.receita = receita; }
    public double getLucro() { return lucro; }
    public void setLucro(double lucro) { this.lucro = lucro; }
    public double getValuation() { return valuation; }
    public void setValuation(double valuation) { this.valuation = valuation; }
    public double getCrescimentoMedio() { return crescimentoMedio; }
    public void setCrescimentoMedio(double crescimentoMedio) { this.crescimentoMedio = crescimentoMedio; }
    public double getMargemMedia() { return margemMedia; }
    public void setMargemMedia(double margemMedia) { this.margemMedia = margemMedia; }
    public String getEmpresaLider() { return empresaLider; }
    public void setEmpresaLider(String empresaLider) { this.empresaLider = empresaLider; }
    public int getEmpregos() { return empregos; }
    public void setEmpregos(int empregos) { this.empregos = empregos; }
}
