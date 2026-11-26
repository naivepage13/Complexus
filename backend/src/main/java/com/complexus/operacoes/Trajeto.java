package com.complexus.operacoes;

import java.util.ArrayList;
import java.util.List;

/**
 * Caminho com horario: cada marco diz em que hora de jogo, contada da partida,
 * a unidade passa por aquele ponto. A posicao em qualquer instante sai daqui,
 * sem que nada precise ser atualizado a cada segundo.
 */
public final class Trajeto {

    public record Marco(Ponto ponto, double horas) {
    }

    private final List<Marco> marcos;

    private Trajeto(List<Marco> marcos) {
        this.marcos = List.copyOf(marcos);
    }

    public static Construtor aPartirDe(Ponto inicio) {
        return new Construtor(inicio);
    }

    /** Reconstroi um trajeto gravado. */
    public static Trajeto deMarcos(List<Marco> marcos) {
        if (marcos.isEmpty()) {
            throw new IllegalArgumentException("Trajeto sem marcos.");
        }
        return new Trajeto(marcos);
    }

    public List<Marco> marcos() {
        return marcos;
    }

    public double duracaoHoras() {
        return marcos.get(marcos.size() - 1).horas();
    }

    public Ponto destino() {
        return marcos.get(marcos.size() - 1).ponto();
    }

    public Ponto posicaoEm(double horas) {
        if (horas <= 0) {
            return marcos.get(0).ponto();
        }
        for (int i = 1; i < marcos.size(); i++) {
            Marco fim = marcos.get(i);
            if (horas < fim.horas()) {
                Marco inicio = marcos.get(i - 1);
                double fracao = (horas - inicio.horas()) / (fim.horas() - inicio.horas());
                return inicio.ponto().interpolar(fim.ponto(), fracao);
            }
        }
        return destino();
    }

    public static final class Construtor {
        private final List<Marco> marcos = new ArrayList<>();

        private Construtor(Ponto inicio) {
            marcos.add(new Marco(inicio, 0));
        }

        /** Acrescenta uma perna ate {@code ponto}, percorrida a {@code kmPorHora}. */
        public Construtor ate(Ponto ponto, double kmPorHora) {
            Marco anterior = marcos.get(marcos.size() - 1);
            double distancia = anterior.ponto().distanciaKm(ponto);
            if (distancia > 0) {
                marcos.add(new Marco(ponto, anterior.horas() + distancia / kmPorHora));
            }
            return this;
        }

        public Trajeto construir() {
            return new Trajeto(marcos);
        }
    }
}
