package com.complexus.operacoes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * Unidade militar da partida. Parada, vale a coordenada gravada; em movimento,
 * a posicao sai do trajeto e do instante de partida, sem atualizar a linha a
 * cada passo.
 */
@Entity
@Table(name = "unidade_militar")
public class UnidadeMilitar {

    /** Folga para o trajeto rodoviario mais longo, em JSON. */
    private static final int TAMANHO_MAXIMO_DO_TRAJETO = 2_000_000;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 80)
    private String nome;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TipoUnidade tipo;

    @Column(nullable = false)
    private double longitude;

    @Column(nullable = false)
    private double latitude;

    /** Marcos [longitude, latitude, hora] do deslocamento; nulo quando parada. */
    @Column(length = TAMANHO_MAXIMO_DO_TRAJETO)
    private String trajeto;

    private Instant partidaEm;

    public Long getId() { return id; }
    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public TipoUnidade getTipo() { return tipo; }
    public void setTipo(TipoUnidade tipo) { this.tipo = tipo; }
    public double getLongitude() { return longitude; }
    public double getLatitude() { return latitude; }
    public String getTrajeto() { return trajeto; }
    public void setTrajeto(String trajeto) { this.trajeto = trajeto; }
    public Instant getPartidaEm() { return partidaEm; }
    public void setPartidaEm(Instant partidaEm) { this.partidaEm = partidaEm; }

    public Ponto getPonto() { return new Ponto(longitude, latitude); }

    public void setPonto(Ponto ponto) {
        this.longitude = ponto.lon();
        this.latitude = ponto.lat();
    }
}
