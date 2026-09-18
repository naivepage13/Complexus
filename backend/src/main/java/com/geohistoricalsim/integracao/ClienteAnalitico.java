package com.geohistoricalsim.integracao;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.geohistoricalsim.config.PropriedadesJogo;
import com.geohistoricalsim.economia.ModificadorSetorial;
import com.geohistoricalsim.economia.Setor;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.EnumMap;
import java.util.Map;
import java.util.Random;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Ponte com o servico analitico escrito em Python.
 *
 * O Python calcula os choques setoriais do turno (demanda, custo e confianca)
 * a partir de sazonalidade e de um passeio aleatorio com semente derivada do
 * turno. Se o servico estiver fora do ar, o Java usa a mesma formula em modo
 * degradado, de modo que o turno nunca deixa de ser processado.
 */
@Component
public class ClienteAnalitico {

    private static final Logger log = LoggerFactory.getLogger(ClienteAnalitico.class);

    private final PropriedadesJogo propriedades;
    private final ObjectMapper mapper = new ObjectMapper();
    private final HttpClient http;

    public ClienteAnalitico(PropriedadesJogo propriedades) {
        this.propriedades = propriedades;
        this.http = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(propriedades.getAnalitico().getTimeoutMs()))
                .build();
    }

    /** Fonte usada no ultimo calculo: util para o relatorio do turno. */
    public enum Origem {
        PYTHON, FALLBACK_JAVA
    }

    public record Resultado(Map<Setor, ModificadorSetorial> modificadores, Origem origem) {
    }

    public Resultado modificadores(int turno, double inflacaoAnual, double taxaJuros) {
        if (propriedades.getAnalitico().isHabilitado()) {
            try {
                return new Resultado(consultar(turno, inflacaoAnual, taxaJuros), Origem.PYTHON);
            } catch (Exception ex) {
                log.warn("Servico analitico indisponivel ({}). Usando calculo local no turno {}.",
                        ex.getMessage(), turno);
            }
        }
        return new Resultado(calcularLocalmente(turno), Origem.FALLBACK_JAVA);
    }

    private Map<Setor, ModificadorSetorial> consultar(int turno, double inflacaoAnual, double taxaJuros)
            throws Exception {
        String corpo = mapper.createObjectNode()
                .put("turno", turno)
                .put("inflacaoAnual", inflacaoAnual)
                .put("taxaJuros", taxaJuros)
                .toString();

        HttpRequest requisicao = HttpRequest.newBuilder()
                .uri(URI.create(propriedades.getAnalitico().getUrl() + "/modificadores"))
                .timeout(Duration.ofMillis(propriedades.getAnalitico().getTimeoutMs()))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(corpo))
                .build();

        HttpResponse<String> resposta = http.send(requisicao, HttpResponse.BodyHandlers.ofString());
        if (resposta.statusCode() != 200) {
            throw new IllegalStateException("HTTP " + resposta.statusCode());
        }

        JsonNode raiz = mapper.readTree(resposta.body()).path("setores");
        Map<Setor, ModificadorSetorial> resultado = new EnumMap<>(Setor.class);
        for (Setor setor : Setor.values()) {
            JsonNode no = raiz.path(setor.name());
            if (no.isMissingNode()) {
                resultado.put(setor, ModificadorSetorial.neutro());
            } else {
                resultado.put(setor, new ModificadorSetorial(
                        no.path("choqueDemanda").asDouble(0.0),
                        no.path("choqueCusto").asDouble(0.0),
                        no.path("confianca").asDouble(1.0)));
            }
        }
        return resultado;
    }

    /**
     * Reproducao local da formula do servico Python: sazonalidade anual mais
     * ruido determinado pela semente do turno.
     */
    private Map<Setor, ModificadorSetorial> calcularLocalmente(int turno) {
        Map<Setor, ModificadorSetorial> resultado = new EnumMap<>(Setor.class);
        for (Setor setor : Setor.values()) {
            Random random = new Random(turno * 7919L + setor.name().hashCode());
            double sazonal = Math.sin((turno % 12) / 12.0 * 2 * Math.PI) * setor.getVolatilidade() * 0.5;
            double ruido = (random.nextDouble() - 0.5) * 2 * setor.getVolatilidade();
            double choqueDemanda = sazonal + ruido;
            double choqueCusto = (random.nextDouble() - 0.5) * setor.getVolatilidade();
            double confianca = Math.clamp(1.0 + choqueDemanda * 1.5, 0.5, 1.5);
            resultado.put(setor, new ModificadorSetorial(choqueDemanda, choqueCusto, confianca));
        }
        return resultado;
    }
}
