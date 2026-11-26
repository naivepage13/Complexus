package com.complexus.operacoes;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

/**
 * Geografia fixa do mapa tatico, montada uma vez a partir dos arquivos do jogo:
 * fronteiras e rodovias (os mesmos GeoJSON que a tela desenha) e
 * mundo/operacoes.json (portos, aeroportos, rotas e forcas iniciais). Nada aqui
 * muda durante a partida.
 */
@Component
public class CartografiaMundo {

    private static final String PASTA_MAPA = "static/dados/mapa/";
    /** Falhas do dado rodoviario menores que isso sao consideradas continuidade. */
    private static final double EMENDA_MAXIMA_KM = 10;

    public record Porto(String id, String nome, Ponto posicao, Ponto ancoradouro) {
    }

    public record Aeroporto(String id, String nome, Ponto posicao) {
    }

    public record RotaCivil(String id, TipoVeiculo tipo, String nome, Trajeto trajeto) {
    }

    /** Veiculo civil: vai e volta na rota, defasado dos demais. */
    public record Veiculo(String id, String rotaId, double deslocamentoHoras) {
    }

    public record UnidadeInicial(String nome, TipoUnidade tipo, Ponto posicao) {
    }

    private record Lugar(String nome, Ponto ponto) {
    }

    private final TerritorioGeografico terra;
    private final TerritorioGeografico areaMaritima;
    private final PlanejadorMovimento planejador;
    private final List<Ponto> corredorMaritimo;
    private final List<Porto> portos = new ArrayList<>();
    private final List<Aeroporto> aeroportos = new ArrayList<>();
    private final List<RotaCivil> rotasCivis = new ArrayList<>();
    private final List<Veiculo> veiculos = new ArrayList<>();
    private final List<UnidadeInicial> unidadesIniciais = new ArrayList<>();

    public CartografiaMundo(ObjectMapper json) {
        JsonNode mundo = ler(json, "mundo/operacoes.json");
        terra = TerritorioGeografico.deGeoJson(ler(json, PASTA_MAPA + "estados.geojson"));
        areaMaritima = new TerritorioGeografico(List.of(List.of(pontos(mundo.get("areaMaritima")))));
        corredorMaritimo = pontos(mundo.get("corredorMaritimo"));

        MalhaViaria malha = new MalhaViaria(trechos(ler(json, PASTA_MAPA + "estradas.geojson")), EMENDA_MAXIMA_KM);
        RoteadorMaritimo roteador = new RoteadorMaritimo(terra, corredorMaritimo);
        planejador = new PlanejadorMovimento(terra, areaMaritima, malha, roteador);

        for (JsonNode porto : mundo.get("portos")) {
            portos.add(new Porto(porto.get("id").asText(), porto.get("nome").asText(),
                    ponto(porto.get("posicao")), ponto(porto.get("ancoradouro"))));
        }
        for (JsonNode aeroporto : mundo.get("aeroportos")) {
            aeroportos.add(new Aeroporto(aeroporto.get("id").asText(), aeroporto.get("nome").asText(),
                    ponto(aeroporto.get("posicao"))));
        }
        for (JsonNode unidade : mundo.get("unidades")) {
            unidadesIniciais.add(new UnidadeInicial(unidade.get("nome").asText(),
                    TipoUnidade.valueOf(unidade.get("tipo").asText()), ponto(unidade.get("posicao"))));
        }

        montarRotasRodoviarias(mundo.get("rotasRodoviarias"), cidades(ler(json, PASTA_MAPA + "cidades.geojson")), malha);
        montarRotasMaritimas(mundo.get("rotasMaritimas"), roteador);
        montarRotasAereas(mundo.get("rotasAereas"));
        distribuirVeiculos();
    }

    public TerritorioGeografico terra() { return terra; }
    public TerritorioGeografico areaMaritima() { return areaMaritima; }
    public PlanejadorMovimento planejador() { return planejador; }
    public List<Ponto> corredorMaritimo() { return corredorMaritimo; }
    public List<Porto> portos() { return List.copyOf(portos); }
    public List<Aeroporto> aeroportos() { return List.copyOf(aeroportos); }
    public List<RotaCivil> rotasCivis() { return List.copyOf(rotasCivis); }
    public List<Veiculo> veiculos() { return List.copyOf(veiculos); }
    public List<UnidadeInicial> unidadesIniciais() { return List.copyOf(unidadesIniciais); }

    private void montarRotasRodoviarias(JsonNode pares, Map<String, Lugar> cidades, MalhaViaria malha) {
        for (JsonNode par : pares) {
            Lugar origem = exigir(cidades, par.get(0).asText());
            Lugar destino = exigir(cidades, par.get(1).asText());
            Trajeto.Construtor construtor = Trajeto.aPartirDe(origem.ponto());
            malha.caminho(origem.ponto(), destino.ponto())
                    .forEach(p -> construtor.ate(p, TipoVeiculo.CAMINHAO.velocidade()));
            construtor.ate(destino.ponto(), TipoVeiculo.CAMINHAO.velocidade());
            registrar(par, TipoVeiculo.CAMINHAO, origem, destino, construtor.construir());
        }
    }

    private void montarRotasMaritimas(JsonNode pares, RoteadorMaritimo roteador) {
        Map<String, Porto> porId = new LinkedHashMap<>();
        portos.forEach(porto -> porId.put(porto.id(), porto));
        for (JsonNode par : pares) {
            Porto origem = exigir(porId, par.get(0).asText());
            Porto destino = exigir(porId, par.get(1).asText());
            List<Ponto> mar = roteador.rota(origem.ancoradouro(), destino.ancoradouro())
                    .orElseThrow(() -> new IllegalStateException(
                            "Sem rota maritima entre " + origem.id() + " e " + destino.id()));
            Trajeto.Construtor construtor = Trajeto.aPartirDe(origem.posicao());
            mar.forEach(p -> construtor.ate(p, TipoVeiculo.NAVIO_CARGA.velocidade()));
            construtor.ate(destino.posicao(), TipoVeiculo.NAVIO_CARGA.velocidade());
            registrar(par, TipoVeiculo.NAVIO_CARGA, new Lugar(origem.nome(), origem.posicao()),
                    new Lugar(destino.nome(), destino.posicao()), construtor.construir());
        }
    }

    private void montarRotasAereas(JsonNode pares) {
        Map<String, Lugar> porId = new LinkedHashMap<>();
        aeroportos.forEach(a -> porId.put(a.id(), new Lugar(a.nome(), a.posicao())));
        for (JsonNode par : pares) {
            Lugar origem = exigir(porId, par.get(0).asText());
            Lugar destino = exigir(porId, par.get(1).asText());
            Trajeto trajeto = Trajeto.aPartirDe(origem.ponto())
                    .ate(destino.ponto(), TipoVeiculo.AVIAO_COMERCIAL.velocidade()).construir();
            registrar(par, TipoVeiculo.AVIAO_COMERCIAL, origem, destino, trajeto);
        }
    }

    private void registrar(JsonNode par, TipoVeiculo tipo, Lugar origem, Lugar destino, Trajeto trajeto) {
        String id = tipo.name() + ":" + par.get(0).asText() + ":" + par.get(1).asText();
        rotasCivis.add(new RotaCivil(id, tipo, origem.nome() + " - " + destino.nome(), trajeto));
    }

    /** Espalha os veiculos pelo ciclo de ida e volta de cada rota. */
    private void distribuirVeiculos() {
        for (RotaCivil rota : rotasCivis) {
            double ciclo = 2 * rota.trajeto().duracaoHoras();
            int quantidade = rota.tipo().veiculosPorRota();
            double fase = Math.floorMod(rota.id().hashCode(), 1000) / 1000.0;
            for (int i = 0; i < quantidade; i++) {
                veiculos.add(new Veiculo(rota.id() + "#" + (i + 1), rota.id(),
                        ciclo * ((i + fase) / quantidade)));
            }
        }
    }

    private static <T> T exigir(Map<String, T> catalogo, String id) {
        T item = catalogo.get(id);
        if (item == null) {
            throw new IllegalStateException("mundo/operacoes.json cita um local desconhecido: " + id);
        }
        return item;
    }

    private static JsonNode ler(ObjectMapper json, String caminho) {
        try (InputStream entrada = new ClassPathResource(caminho).getInputStream()) {
            return json.readTree(entrada);
        } catch (IOException ex) {
            throw new UncheckedIOException("Nao foi possivel ler " + caminho, ex);
        }
    }

    private static Ponto ponto(JsonNode par) {
        return new Ponto(par.get(0).asDouble(), par.get(1).asDouble());
    }

    private static List<Ponto> pontos(JsonNode pares) {
        List<Ponto> pontos = new ArrayList<>();
        pares.forEach(par -> pontos.add(ponto(par)));
        return pontos;
    }

    /** Cada rodovia e um MultiLineString: devolve todos os trechos juntos. */
    private static List<List<Ponto>> trechos(JsonNode colecao) {
        List<List<Ponto>> trechos = new ArrayList<>();
        for (JsonNode feature : colecao.get("features")) {
            feature.get("geometry").get("coordinates").forEach(trecho -> trechos.add(pontos(trecho)));
        }
        return trechos;
    }

    private static Map<String, Lugar> cidades(JsonNode colecao) {
        Map<String, Lugar> cidades = new LinkedHashMap<>();
        for (JsonNode feature : colecao.get("features")) {
            JsonNode propriedades = feature.get("properties");
            cidades.put(propriedades.get("id").asText(), new Lugar(propriedades.get("nome").asText(),
                    ponto(feature.get("geometry").get("coordinates"))));
        }
        return cidades;
    }
}
