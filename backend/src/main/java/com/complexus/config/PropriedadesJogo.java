package com.complexus.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Parametros globais de balanceamento e integracao do jogo.
 * Centralizados aqui para permitir ajuste de regras sem recompilar logica.
 */
@ConfigurationProperties(prefix = "jogo")
public class PropriedadesJogo {

    private Turno turno = new Turno();
    private Analitico analitico = new Analitico();
    private Admin admin = new Admin();

    public Turno getTurno() {
        return turno;
    }

    public void setTurno(Turno turno) {
        this.turno = turno;
    }

    public Analitico getAnalitico() {
        return analitico;
    }

    public void setAnalitico(Analitico analitico) {
        this.analitico = analitico;
    }

    public Admin getAdmin() {
        return admin;
    }

    public void setAdmin(Admin admin) {
        this.admin = admin;
    }

    public static class Turno {
        /** Duracao de um turno em minutos de tempo real. */
        private int duracaoMinutos = 60;
        /** Quando falso, o turno so avanca por chamada manual da API. */
        private boolean processamentoAutomatico = true;

        public int getDuracaoMinutos() {
            return duracaoMinutos;
        }

        public void setDuracaoMinutos(int duracaoMinutos) {
            this.duracaoMinutos = duracaoMinutos;
        }

        public boolean isProcessamentoAutomatico() {
            return processamentoAutomatico;
        }

        public void setProcessamentoAutomatico(boolean processamentoAutomatico) {
            this.processamentoAutomatico = processamentoAutomatico;
        }
    }

    /** Acesso administrativo, usado pelas rotas /api/admin. */
    public static class Admin {
        /**
         * Credencial exigida no cabecalho X-Admin-Token. Em producao deve vir
         * de variavel de ambiente, nunca do arquivo versionado.
         */
        private String token = "admin-local";

        public String getToken() {
            return token;
        }

        public void setToken(String token) {
            this.token = token;
        }
    }

    public static class Analitico {
        /** Habilita o uso do servico analitico em Python. */
        private boolean habilitado = true;
        private String url = "http://localhost:8100";
        private int timeoutMs = 1500;

        public boolean isHabilitado() {
            return habilitado;
        }

        public void setHabilitado(boolean habilitado) {
            this.habilitado = habilitado;
        }

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public int getTimeoutMs() {
            return timeoutMs;
        }

        public void setTimeoutMs(int timeoutMs) {
            this.timeoutMs = timeoutMs;
        }
    }
}
