package com.geohistoricalsim.auditoria;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * Registro imutavel de um fato do sistema.
 *
 * Os eventos formam uma linha de auditoria encadeada por hash: cada evento
 * carrega o hash do evento anterior, de modo que qualquer alteracao retroativa
 * quebra a cadeia e e detectada por {@code ServicoAuditoria#verificarIntegridade}.
 */
@Entity
@Table(name = "evento_auditoria", indexes = {
        @Index(name = "idx_auditoria_turno", columnList = "turno"),
        @Index(name = "idx_auditoria_entidade", columnList = "entidade,entidadeId")
})
public class EventoAuditoria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Instant momento = Instant.now();

    @Column(nullable = false)
    private int turno;

    /** Quem provocou o fato: usuario do jogador ou "SISTEMA". */
    @Column(nullable = false, length = 60)
    private String ator;

    /** Verbo do fato, ex.: EMPRESA_CRIADA, TURNO_PROCESSADO, LEI_SANCIONADA. */
    @Column(nullable = false, length = 60)
    private String acao;

    @Column(nullable = false, length = 60)
    private String entidade;

    private Long entidadeId;

    @Column(length = 400)
    private String descricao;

    @Lob
    @Column(name = "detalhes")
    private String detalhes;

    @Column(length = 64)
    private String hashAnterior;

    @Column(nullable = false, length = 64)
    private String hash;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Instant getMomento() { return momento; }
    public void setMomento(Instant momento) { this.momento = momento; }
    public int getTurno() { return turno; }
    public void setTurno(int turno) { this.turno = turno; }
    public String getAtor() { return ator; }
    public void setAtor(String ator) { this.ator = ator; }
    public String getAcao() { return acao; }
    public void setAcao(String acao) { this.acao = acao; }
    public String getEntidade() { return entidade; }
    public void setEntidade(String entidade) { this.entidade = entidade; }
    public Long getEntidadeId() { return entidadeId; }
    public void setEntidadeId(Long entidadeId) { this.entidadeId = entidadeId; }
    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { this.descricao = descricao; }
    public String getDetalhes() { return detalhes; }
    public void setDetalhes(String detalhes) { this.detalhes = detalhes; }
    public String getHashAnterior() { return hashAnterior; }
    public void setHashAnterior(String hashAnterior) { this.hashAnterior = hashAnterior; }
    public String getHash() { return hash; }
    public void setHash(String hash) { this.hash = hash; }
}
