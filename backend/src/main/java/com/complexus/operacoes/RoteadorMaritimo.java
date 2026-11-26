package com.complexus.operacoes;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Tracado de navios: linha reta quando nao ha terra no meio; do contrario,
 * pelo corredor maritimo (uma sequencia de pontos ao largo da costa).
 */
public final class RoteadorMaritimo {

    private static final int ORIGEM = 0;
    private static final int DESTINO = 1;

    private final TerritorioGeografico terra;
    private final List<Ponto> corredor;

    public RoteadorMaritimo(TerritorioGeografico terra, List<Ponto> corredor) {
        this.terra = terra;
        this.corredor = List.copyOf(corredor);
    }

    /** Rota com origem e destino inclusos, ou vazio quando nao da para chegar por mar. */
    public Optional<List<Ponto>> rota(Ponto origem, Ponto destino) {
        if (terra.contem(origem) || terra.contem(destino)) {
            return Optional.empty();
        }
        if (!terra.cruza(origem, destino)) {
            return Optional.of(List.of(origem, destino));
        }
        // Dijkstra sobre origem, destino e os pontos do corredor (do indice 2 em diante).
        List<Ponto> nos = new ArrayList<>();
        nos.add(origem);
        nos.add(destino);
        nos.addAll(corredor);
        int total = nos.size();
        double[] custo = new double[total];
        int[] anterior = new int[total];
        boolean[] fechado = new boolean[total];
        Arrays.fill(custo, Double.MAX_VALUE);
        Arrays.fill(anterior, -1);
        custo[ORIGEM] = 0;
        while (true) {
            int atual = -1;
            for (int i = 0; i < total; i++) {
                if (!fechado[i] && custo[i] < Double.MAX_VALUE && (atual < 0 || custo[i] < custo[atual])) {
                    atual = i;
                }
            }
            if (atual < 0) {
                return Optional.empty();
            }
            if (atual == DESTINO) {
                break;
            }
            fechado[atual] = true;
            for (int vizinho = 1; vizinho < total; vizinho++) {
                if (fechado[vizinho] || !podemLigar(atual, vizinho)) {
                    continue;
                }
                double novo = custo[atual] + nos.get(atual).distanciaKm(nos.get(vizinho));
                if (novo < custo[vizinho] && !terra.cruza(nos.get(atual), nos.get(vizinho))) {
                    custo[vizinho] = novo;
                    anterior[vizinho] = atual;
                }
            }
        }
        List<Ponto> rota = new ArrayList<>();
        for (int no = DESTINO; no >= 0; no = anterior[no]) {
            rota.add(nos.get(no));
        }
        Collections.reverse(rota);
        return Optional.of(rota);
    }

    /** Origem e destino enxergam todo o corredor; dentro dele, so vizinhos se ligam. */
    private boolean podemLigar(int a, int b) {
        return a < 2 || b < 2 || Math.abs(a - b) == 1;
    }
}
