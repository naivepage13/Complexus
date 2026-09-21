package com.complexus.config;

import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Ajustes de esquema que o {@code ddl-auto=update} nao faz sozinho.
 *
 * <h2>Colunas de enum</h2>
 * O H2 cria coluna de enum como tipo ENUM nativo, com a lista de valores
 * gravada no proprio tipo. O {@code update} do Hibernate acrescenta tabela e
 * coluna, mas nunca acrescenta um valor a esse tipo: qualquer constante nova -
 * um lancamento novo no razao, um setor novo, um status novo - passa a ser
 * recusada na hora de gravar, com erro 22030, em toda partida que ja existia.
 *
 * Esta migracao converte essas colunas para {@code varchar}, que aceita
 * qualquer valor que o Java escrever. Ela roda antes da carga inicial, e uma
 * vez convertida a coluna nao aparece mais na varredura, entao repetir a subida
 * nao repete o trabalho.
 *
 * Quando o projeto migrar para PostgreSQL com Flyway (RNF-02), esta classe sai
 * e o versionamento de esquema passa a ser responsabilidade das migrations.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class MigracaoEsquema implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(MigracaoEsquema.class);

    /** Cabe a maior constante de enum do jogo com folga. */
    private static final int TAMANHO_TEXTO = 60;

    private final JdbcTemplate jdbc;

    public MigracaoEsquema(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void run(ApplicationArguments args) {
        List<Map<String, Object>> colunas = jdbc.queryForList(
                "select table_name, column_name from information_schema.columns "
                        + "where table_schema = 'PUBLIC' and data_type = 'ENUM'");
        if (colunas.isEmpty()) {
            return;
        }
        for (Map<String, Object> coluna : colunas) {
            String tabela = String.valueOf(coluna.get("TABLE_NAME"));
            String nome = String.valueOf(coluna.get("COLUMN_NAME"));
            jdbc.execute("alter table \"" + tabela + "\" alter column \"" + nome
                    + "\" set data type varchar(" + TAMANHO_TEXTO + ")");
            log.info("Coluna {}.{} convertida de ENUM para varchar.", tabela, nome);
        }
        log.info("Migracao de esquema: {} colunas de enum convertidas.", colunas.size());
    }
}
