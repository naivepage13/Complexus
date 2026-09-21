package com.complexus.financas;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Como o mercado de credito enxerga a empresa em um dado turno.
 *
 * O jogador ve os quatro componentes do score, e nao so a nota final: sem isso
 * a recusa de credito vira ruido, e com eles vira diagnostico do que arrumar.
 *
 * @param score            0 a 100, media ponderada dos componentes
 * @param nota             faixa de risco correspondente ao score
 * @param alavancagem      divida sobre patrimonio liquido
 * @param coberturaDeJuros lucro anual sobre juros anuais
 * @param taxaBasica       taxa basica do jogo no turno
 * @param componentes      pontuacao de cada criterio, para a interface explicar
 */
public record AvaliacaoCredito(double score,
                               NotaCredito nota,
                               double alavancagem,
                               double coberturaDeJuros,
                               double dividaTotal,
                               double patrimonioLiquido,
                               double taxaBasica,
                               Map<String, Double> componentes,
                               List<String> observacoes) {

    /** Taxa anual que a empresa pagaria hoje em uma modalidade. */
    public double taxaAnual(Financiamento.Modalidade modalidade) {
        return taxaBasica + modalidade.getSpreadAnual() + nota.getSpreadAnual();
    }

    public Map<String, Object> comoMapa() {
        Map<String, Object> mapa = new LinkedHashMap<>();
        mapa.put("score", score);
        mapa.put("nota", nota.name());
        mapa.put("notaDescricao", nota.getDescricao());
        mapa.put("alavancagem", alavancagem);
        mapa.put("coberturaDeJuros", coberturaDeJuros);
        mapa.put("dividaTotal", dividaTotal);
        mapa.put("patrimonioLiquido", patrimonioLiquido);
        mapa.put("taxaBasica", taxaBasica);
        mapa.put("componentes", componentes);
        mapa.put("observacoes", observacoes);
        return mapa;
    }
}
