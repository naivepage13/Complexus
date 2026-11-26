package com.complexus.operacoes;

import com.complexus.comum.RegraDeNegocioException;
import java.util.List;

/**
 * Transforma uma ordem (quem, de onde, para onde) em um trajeto com horario,
 * recusando destinos que o ramo da unidade nao alcanca.
 */
public final class PlanejadorMovimento {

    private final TerritorioGeografico terra;
    private final TerritorioGeografico areaMaritima;
    private final MalhaViaria malha;
    private final RoteadorMaritimo roteadorMaritimo;

    public PlanejadorMovimento(TerritorioGeografico terra, TerritorioGeografico areaMaritima,
                               MalhaViaria malha, RoteadorMaritimo roteadorMaritimo) {
        this.terra = terra;
        this.areaMaritima = areaMaritima;
        this.malha = malha;
        this.roteadorMaritimo = roteadorMaritimo;
    }

    public Trajeto planejar(TipoUnidade tipo, Ponto origem, Ponto destino) {
        return switch (tipo.ramo()) {
            case EXERCITO -> porTerra(tipo, origem, destino);
            case MARINHA -> porMar(tipo, origem, destino);
            case AERONAUTICA -> peloAr(tipo, origem, destino);
        };
    }

    /** Compara a ida em linha reta com a ida pela rodovia e fica com a mais rapida. */
    private Trajeto porTerra(TipoUnidade tipo, Ponto origem, Ponto destino) {
        if (!terra.contem(destino)) {
            throw new RegraDeNegocioException("Tropas so podem ser enviadas para um ponto em terra, no Brasil.");
        }
        Trajeto direto = Trajeto.aPartirDe(origem).ate(destino, tipo.velocidadeForaDaEstrada()).construir();
        List<Ponto> estrada = malha.caminhoMaisRapido(origem, destino,
                tipo.velocidadeEstrada() / tipo.velocidadeForaDaEstrada());
        if (estrada.size() < 2) {
            return direto;
        }
        Trajeto.Construtor viaEstrada = Trajeto.aPartirDe(origem)
                .ate(estrada.get(0), tipo.velocidadeForaDaEstrada());
        for (Ponto ponto : estrada.subList(1, estrada.size())) {
            viaEstrada.ate(ponto, tipo.velocidadeEstrada());
        }
        Trajeto rodoviario = viaEstrada.ate(destino, tipo.velocidadeForaDaEstrada()).construir();
        return rodoviario.duracaoHoras() < direto.duracaoHoras() ? rodoviario : direto;
    }

    private Trajeto porMar(TipoUnidade tipo, Ponto origem, Ponto destino) {
        if (terra.contem(destino) || !areaMaritima.contem(destino)) {
            throw new RegraDeNegocioException(
                    "Navios so podem ser enviados para um ponto no mar, dentro da area de operacoes.");
        }
        List<Ponto> rota = roteadorMaritimo.rota(origem, destino)
                .orElseThrow(() -> new RegraDeNegocioException("Nao ha rota maritima ate esse destino."));
        Trajeto.Construtor construtor = Trajeto.aPartirDe(origem);
        rota.forEach(ponto -> construtor.ate(ponto, tipo.velocidadeEstrada()));
        return construtor.construir();
    }

    private Trajeto peloAr(TipoUnidade tipo, Ponto origem, Ponto destino) {
        if (!terra.contem(destino) && !areaMaritima.contem(destino)) {
            throw new RegraDeNegocioException("O destino esta fora do teatro de operacoes.");
        }
        return Trajeto.aPartirDe(origem).ate(destino, tipo.velocidadeEstrada()).construir();
    }
}
