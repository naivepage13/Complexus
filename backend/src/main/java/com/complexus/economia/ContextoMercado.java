package com.complexus.economia;

import java.util.Map;

/**
 * Condicoes macroeconomicas vigentes no turno, iguais para todas as empresas.
 *
 * @param rendaMedia     renda mensal media da populacao
 * @param rendaReferencia renda usada como base das elasticidades
 * @param taxaJurosAnual taxa basica de juros anual (fracao)
 * @param inflacaoAnual  inflacao anual (fracao)
 * @param estabilidade   estabilidade institucional 0-100
 * @param modificadores  choques por setor no turno
 */
public record ContextoMercado(double rendaMedia,
                              double rendaReferencia,
                              double taxaJurosAnual,
                              double inflacaoAnual,
                              double estabilidade,
                              Map<Setor, ModificadorSetorial> modificadores) {

    public ModificadorSetorial modificador(Setor setor) {
        return modificadores.getOrDefault(setor, ModificadorSetorial.neutro());
    }
}
