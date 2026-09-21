package com.complexus.politica;

/**
 * Cargos politicos simulados, espelhando a estrutura brasileira de forma
 * simplificada, porem preservando a divisao de poderes:
 *
 * <ul>
 *   <li>Executivo propoe orcamento, sanciona ou veta leis da sua esfera;</li>
 *   <li>Legislativo propoe projetos e vota; tambem derruba vetos;</li>
 *   <li>Cargos nomeados (ministro, secretario) executam politicas, nao votam.</li>
 * </ul>
 *
 * Duracao de mandato em turnos: 1 turno = 1 mes de jogo, logo 48 turnos = 4 anos.
 */
public enum CargoPolitico {

    PRESIDENTE(Esfera.FEDERAL, "Presidente da Republica", 48, true, false, true),
    VICE_PRESIDENTE(Esfera.FEDERAL, "Vice-Presidente", 48, false, false, true),
    MINISTRO(Esfera.FEDERAL, "Ministro de Estado", 48, true, false, false),
    SENADOR(Esfera.FEDERAL, "Senador", 96, false, true, true),
    DEPUTADO_FEDERAL(Esfera.FEDERAL, "Deputado Federal", 48, false, true, true),

    GOVERNADOR(Esfera.ESTADUAL, "Governador", 48, true, false, true),
    VICE_GOVERNADOR(Esfera.ESTADUAL, "Vice-Governador", 48, false, false, true),
    SECRETARIO_ESTADUAL(Esfera.ESTADUAL, "Secretario Estadual", 48, true, false, false),
    DEPUTADO_ESTADUAL(Esfera.ESTADUAL, "Deputado Estadual", 48, false, true, true),

    PREFEITO(Esfera.MUNICIPAL, "Prefeito", 48, true, false, true),
    VICE_PREFEITO(Esfera.MUNICIPAL, "Vice-Prefeito", 48, false, false, true),
    SECRETARIO_MUNICIPAL(Esfera.MUNICIPAL, "Secretario Municipal", 48, true, false, false),
    VEREADOR(Esfera.MUNICIPAL, "Vereador", 48, false, true, true);

    private final Esfera esfera;
    private final String rotulo;
    private final int duracaoMandatoTurnos;
    private final boolean executivo;
    private final boolean legislativo;
    private final boolean eletivo;

    CargoPolitico(Esfera esfera, String rotulo, int duracaoMandatoTurnos,
                  boolean executivo, boolean legislativo, boolean eletivo) {
        this.esfera = esfera;
        this.rotulo = rotulo;
        this.duracaoMandatoTurnos = duracaoMandatoTurnos;
        this.executivo = executivo;
        this.legislativo = legislativo;
        this.eletivo = eletivo;
    }

    public Esfera getEsfera() { return esfera; }
    public String getRotulo() { return rotulo; }
    public int getDuracaoMandatoTurnos() { return duracaoMandatoTurnos; }
    public boolean isExecutivo() { return executivo; }
    public boolean isLegislativo() { return legislativo; }
    public boolean isEletivo() { return eletivo; }

    /** Somente o chefe do executivo da esfera sanciona ou veta. */
    public boolean chefiaExecutivo() {
        return this == PRESIDENTE || this == GOVERNADOR || this == PREFEITO;
    }
}
