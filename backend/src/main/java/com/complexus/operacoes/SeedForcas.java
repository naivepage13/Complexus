package com.complexus.operacoes;

import com.complexus.auditoria.ServicoAuditoria;
import java.util.Map;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Posiciona as forcas iniciais. Separado de SeedDados para que partidas ja em
 * andamento tambem recebam as unidades na primeira subida desta versao.
 */
@Component
public class SeedForcas implements ApplicationRunner {

    private final RepositorioUnidadeMilitar repositorio;
    private final CartografiaMundo cartografia;
    private final ServicoAuditoria auditoria;

    public SeedForcas(RepositorioUnidadeMilitar repositorio, CartografiaMundo cartografia,
                      ServicoAuditoria auditoria) {
        this.repositorio = repositorio;
        this.cartografia = cartografia;
        this.auditoria = auditoria;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        semear();
    }

    /** Idempotente: so age quando nao ha unidade nenhuma. */
    @Transactional
    public void semear() {
        if (repositorio.count() > 0) {
            return;
        }
        for (CartografiaMundo.UnidadeInicial inicial : cartografia.unidadesIniciais()) {
            UnidadeMilitar unidade = new UnidadeMilitar();
            unidade.setNome(inicial.nome());
            unidade.setTipo(inicial.tipo());
            unidade.setPonto(inicial.posicao());
            repositorio.save(unidade);
        }
        auditoria.registrarSistema("FORCAS_INICIALIZADAS", "UnidadeMilitar", null,
                "Forcas militares posicionadas nas bases",
                Map.of("unidades", cartografia.unidadesIniciais().size()));
    }
}
