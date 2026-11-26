package com.complexus.operacoes;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

/**
 * Grafo das rodovias. O dado de origem vem em trechos soltos e com pequenas
 * falhas, entao nos de componentes diferentes e proximos entre si sao
 * emendados. Rotas usam apenas a maior parte conectada da malha.
 */
public final class MalhaViaria {

    /** Fracoes do raio maximo tentadas em ordem, para preferir a emenda mais curta. */
    private static final double[] PASSOS_DE_EMENDA = {0.05, 0.2, 1.0};
    private static final double ALCANCE_DA_PONTA_SOLTA = 0.5;
    private static final double KM_POR_GRAU = 111.0;

    private record Aresta(int destino, double km) {
    }

    private record Visita(int no, double km) {
    }

    private record Percurso(double[] custo, int[] anterior) {
    }

    private final List<Ponto> nos = new ArrayList<>();
    private final List<List<Aresta>> arestas = new ArrayList<>();
    private final List<Integer> pai = new ArrayList<>();
    private final boolean[] principal;

    public MalhaViaria(List<List<Ponto>> trechos, double pontesKm) {
        Map<Ponto, Integer> indices = new HashMap<>();
        for (List<Ponto> trecho : trechos) {
            int anterior = -1;
            for (Ponto ponto : trecho) {
                int atual = indices.computeIfAbsent(ponto, this::novoNo);
                if (anterior >= 0 && anterior != atual) {
                    ligar(anterior, atual);
                }
                anterior = atual;
            }
        }
        emendarPontasSoltas(pontesKm * ALCANCE_DA_PONTA_SOLTA);
        for (double passo : PASSOS_DE_EMENDA) {
            emendar(pontesKm * passo);
        }
        principal = marcarComponentePrincipal();
    }

    /**
     * Uma falha pequena precisa ser fechada mesmo quando os dois lados ja se
     * ligam por um desvio longo: toda ponta solta se liga ao que estiver perto.
     */
    private void emendarPontasSoltas(double raioKm) {
        List<Integer> pontas = new ArrayList<>();
        for (int no = 0; no < nos.size(); no++) {
            if (arestas.get(no).size() == 1) {
                pontas.add(no);
            }
        }
        Grade grade = new Grade(raioKm);
        for (int no = 0; no < nos.size(); no++) {
            grade.incluir(no);
        }
        for (int ponta : pontas) {
            for (int vizinho : grade.aoRedor(ponta)) {
                if (vizinho != ponta && nos.get(ponta).distanciaKm(nos.get(vizinho)) <= raioKm) {
                    ligar(ponta, vizinho);
                }
            }
        }
    }

    private int novoNo(Ponto ponto) {
        nos.add(ponto);
        arestas.add(new ArrayList<>());
        pai.add(nos.size() - 1);
        return nos.size() - 1;
    }

    private void ligar(int a, int b) {
        double km = nos.get(a).distanciaKm(nos.get(b));
        arestas.get(a).add(new Aresta(b, km));
        arestas.get(b).add(new Aresta(a, km));
        pai.set(raiz(a), raiz(b));
    }

    private int raiz(int no) {
        while (pai.get(no) != no) {
            pai.set(no, pai.get(pai.get(no)));
            no = pai.get(no);
        }
        return no;
    }

    /** Liga componentes distintos a ate {@code raioKm}. */
    private void emendar(double raioKm) {
        Grade grade = new Grade(raioKm);
        for (int no = 0; no < nos.size(); no++) {
            for (int vizinho : grade.aoRedor(no)) {
                if (raiz(no) != raiz(vizinho) && nos.get(no).distanciaKm(nos.get(vizinho)) <= raioKm) {
                    ligar(no, vizinho);
                }
            }
            grade.incluir(no);
        }
    }

    /** Grade espacial para achar nos proximos sem comparar todos com todos. */
    private final class Grade {
        private final double celula;
        private final Map<Long, List<Integer>> celulas = new HashMap<>();

        private Grade(double raioKm) {
            celula = raioKm / KM_POR_GRAU;
        }

        private void incluir(int no) {
            celulas.computeIfAbsent(chave(coluna(no), linha(no)), k -> new ArrayList<>()).add(no);
        }

        /** Candidatos a ate um raio de distancia (e alguns um pouco alem). */
        private List<Integer> aoRedor(int no) {
            List<Integer> candidatos = new ArrayList<>();
            // Duas colunas para cada lado cobrem o grau de longitude, mais curto no sul.
            for (int dx = -2; dx <= 2; dx++) {
                for (int dy = -1; dy <= 1; dy++) {
                    candidatos.addAll(celulas.getOrDefault(chave(coluna(no) + dx, linha(no) + dy), List.of()));
                }
            }
            return candidatos;
        }

        private int coluna(int no) {
            return (int) Math.floor(nos.get(no).lon() / celula);
        }

        private int linha(int no) {
            return (int) Math.floor(nos.get(no).lat() / celula);
        }

        private long chave(int coluna, int linha) {
            return ((long) coluna << 32) ^ (linha & 0xffffffffL);
        }
    }

    private boolean[] marcarComponentePrincipal() {
        Map<Integer, Integer> tamanhos = new HashMap<>();
        for (int no = 0; no < nos.size(); no++) {
            tamanhos.merge(raiz(no), 1, Integer::sum);
        }
        int maior = tamanhos.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(-1);
        boolean[] marcados = new boolean[nos.size()];
        for (int no = 0; no < nos.size(); no++) {
            marcados[no] = raiz(no) == maior;
        }
        return marcados;
    }

    private int maisProximo(Ponto ponto) {
        int melhor = -1;
        double menor = Double.MAX_VALUE;
        for (int no = 0; no < nos.size(); no++) {
            if (!principal[no]) {
                continue;
            }
            double distancia = ponto.distanciaKm(nos.get(no));
            if (distancia < menor) {
                menor = distancia;
                melhor = no;
            }
        }
        return melhor;
    }

    /**
     * Menor caminho pela malha entre os nos mais proximos da origem e do destino.
     * Lista vazia quando nao ha malha.
     */
    public List<Ponto> caminho(Ponto origem, Ponto destino) {
        int partida = maisProximo(origem);
        int chegada = maisProximo(destino);
        if (partida < 0 || chegada < 0) {
            return List.of();
        }
        return refazer(percorrer(partida, chegada).anterior(), chegada);
    }

    /**
     * Caminho que entra na malha perto da origem e sai no no que deixa a viagem
     * inteira mais curta, contando que cada quilometro fora da estrada ate o
     * destino custa {@code penalidadeForaDaEstrada} quilometros de estrada.
     */
    public List<Ponto> caminhoMaisRapido(Ponto origem, Ponto destino, double penalidadeForaDaEstrada) {
        int partida = maisProximo(origem);
        if (partida < 0) {
            return List.of();
        }
        Percurso percurso = percorrer(partida, -1);
        int saida = partida;
        double menor = Double.MAX_VALUE;
        for (int no = 0; no < nos.size(); no++) {
            if (percurso.custo()[no] >= menor) {
                continue;
            }
            double total = percurso.custo()[no] + nos.get(no).distanciaKm(destino) * penalidadeForaDaEstrada;
            if (total < menor) {
                menor = total;
                saida = no;
            }
        }
        return refazer(percurso.anterior(), saida);
    }

    /** Dijkstra a partir de {@code partida}; para ao fechar {@code chegada} (-1 percorre tudo). */
    private Percurso percorrer(int partida, int chegada) {
        double[] custo = new double[nos.size()];
        int[] anterior = new int[nos.size()];
        Arrays.fill(custo, Double.MAX_VALUE);
        Arrays.fill(anterior, -1);
        custo[partida] = 0;
        PriorityQueue<Visita> fila = new PriorityQueue<>((a, b) -> Double.compare(a.km(), b.km()));
        fila.add(new Visita(partida, 0));
        while (!fila.isEmpty()) {
            Visita visita = fila.poll();
            if (visita.no() == chegada) {
                break;
            }
            if (visita.km() > custo[visita.no()]) {
                continue;
            }
            for (Aresta aresta : arestas.get(visita.no())) {
                double novo = visita.km() + aresta.km();
                if (novo < custo[aresta.destino()]) {
                    custo[aresta.destino()] = novo;
                    anterior[aresta.destino()] = visita.no();
                    fila.add(new Visita(aresta.destino(), novo));
                }
            }
        }
        return new Percurso(custo, anterior);
    }

    private List<Ponto> refazer(int[] anterior, int chegada) {
        List<Ponto> caminho = new ArrayList<>();
        for (int no = chegada; no >= 0; no = anterior[no]) {
            caminho.add(nos.get(no));
        }
        Collections.reverse(caminho);
        return caminho;
    }
}
