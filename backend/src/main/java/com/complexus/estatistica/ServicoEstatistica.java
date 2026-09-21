package com.complexus.estatistica;

import com.complexus.core.EstadoJogo;
import com.complexus.economia.Empresa;
import com.complexus.economia.RepositorioEmpresa;
import com.complexus.economia.Setor;
import com.complexus.investimento.ServicoInvestimento;
import com.complexus.politica.Pais;
import com.complexus.politica.RepositorioPais;
import com.complexus.politica.ServicoPolitica;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Consolidacao das estatisticas gerais ao fim de cada turno e as consultas que
 * alimentam os paineis de estatisticas do frontend.
 */
@Service
public class ServicoEstatistica {

    private final RepositorioSnapshotTurno repositorioSnapshot;
    private final RepositorioEstatisticaSetor repositorioSetor;
    private final RepositorioEmpresa repositorioEmpresa;
    private final RepositorioPais repositorioPais;
    private final ServicoInvestimento servicoInvestimento;
    private final ServicoPolitica servicoPolitica;

    public ServicoEstatistica(RepositorioSnapshotTurno repositorioSnapshot,
                              RepositorioEstatisticaSetor repositorioSetor,
                              RepositorioEmpresa repositorioEmpresa,
                              RepositorioPais repositorioPais,
                              ServicoInvestimento servicoInvestimento,
                              ServicoPolitica servicoPolitica) {
        this.repositorioSnapshot = repositorioSnapshot;
        this.repositorioSetor = repositorioSetor;
        this.repositorioEmpresa = repositorioEmpresa;
        this.repositorioPais = repositorioPais;
        this.servicoInvestimento = servicoInvestimento;
        this.servicoPolitica = servicoPolitica;
    }

    /**
     * Fecha o turno em numeros: agrega empresas e setores, calcula o indice de
     * mercado e grava a linha historica do turno.
     *
     * @param dividendosPagos total distribuido no turno, informado pelo motor de turnos
     */
    @Transactional
    public SnapshotTurno consolidar(EstadoJogo estado, double dividendosPagos) {
        int turno = estado.getTurnoAtual();
        List<Empresa> empresas = repositorioEmpresa.findByAtivaTrue();

        double receita = 0;
        double lucro = 0;
        double impostos = 0;
        double valuation = 0;
        int empregos = 0;
        for (Empresa empresa : empresas) {
            receita += empresa.getReceitaMensal();
            lucro += empresa.getLucroMensal();
            impostos += empresa.getImpostosMensais();
            valuation += empresa.getValuation();
            empregos += empresa.getFuncionarios();
        }

        consolidarSetores(turno, empresas);

        SnapshotTurno anterior = repositorioSnapshot.findFirstByOrderByTurnoDesc().orElse(null);
        double indiceAnterior = anterior == null ? 1000.0 : anterior.getIndiceMercado();
        double valuationAnterior = anterior == null ? valuation : anterior.getValuationAgregado();
        double variacao = valuationAnterior <= 0 ? 0 : (valuation - valuationAnterior) / valuationAnterior;
        double indice = Math.max(indiceAnterior * (1 + variacao), 1.0);

        Pais pais = repositorioPais.findAll().stream().findFirst().orElse(null);

        SnapshotTurno snapshot = repositorioSnapshot.findByTurno(turno).orElseGet(SnapshotTurno::new);
        snapshot.setTurno(turno);
        snapshot.setDataJogo(estado.getDataJogo());
        snapshot.setEmpresasAtivas(empresas.size());
        snapshot.setReceitaAgregada(receita);
        snapshot.setLucroAgregado(lucro);
        snapshot.setImpostosArrecadados(impostos);
        snapshot.setDividendosPagos(dividendosPagos);
        snapshot.setValuationAgregado(valuation);
        snapshot.setIndiceMercado(indice);
        snapshot.setVariacaoIndice(variacao);
        snapshot.setInflacaoAnual(estado.getInflacaoAnual());
        snapshot.setTaxaJuros(estado.getTaxaJuros());
        snapshot.setPib(pais == null ? 0 : pais.getPib());
        snapshot.setDesemprego(pais == null ? 0 : pais.getDesemprego());
        snapshot.setEstabilidade(pais == null ? 0 : pais.getEstabilidade());
        snapshot.setAprovacaoGoverno(pais == null ? 0 : pais.getAprovacaoGoverno());
        snapshot.setLeisEmVigor(servicoPolitica.leisEmVigor().size());
        snapshot.setInvestidoresAtivos(servicoInvestimento.investidoresAtivos());
        snapshot.setCapitalInvestido(servicoInvestimento.capitalTotalInvestido());
        snapshot.setEmpregosTotais(empregos);

        estado.setIndiceMercado(indice);
        return repositorioSnapshot.save(snapshot);
    }

    private void consolidarSetores(int turno, List<Empresa> empresas) {
        for (Setor setor : Setor.values()) {
            List<Empresa> doSetor = empresas.stream().filter(e -> e.getSetor() == setor).toList();
            EstatisticaSetor estatistica = new EstatisticaSetor();
            estatistica.setTurno(turno);
            estatistica.setSetor(setor);
            estatistica.setEmpresas(doSetor.size());

            double receita = doSetor.stream().mapToDouble(Empresa::getReceitaMensal).sum();
            double lucro = doSetor.stream().mapToDouble(Empresa::getLucroMensal).sum();
            double valuation = doSetor.stream().mapToDouble(Empresa::getValuation).sum();
            double crescimento = doSetor.stream().mapToDouble(Empresa::getCrescimentoLucro).average().orElse(0);

            estatistica.setReceita(receita);
            estatistica.setLucro(lucro);
            estatistica.setValuation(valuation);
            estatistica.setCrescimentoMedio(crescimento);
            estatistica.setMargemMedia(receita <= 0 ? 0 : lucro / receita);
            estatistica.setEmpregos(doSetor.stream().mapToInt(Empresa::getFuncionarios).sum());
            estatistica.setEmpresaLider(doSetor.stream()
                    .max(Comparator.comparingDouble(Empresa::getReceitaMensal))
                    .map(Empresa::getNome)
                    .orElse(null));
            repositorioSetor.save(estatistica);
        }
    }

    @Transactional(readOnly = true)
    public List<SnapshotTurno> serieHistorica(int limite) {
        List<SnapshotTurno> serie = repositorioSnapshot.findAllByOrderByTurnoDesc(
                PageRequest.of(0, Math.min(Math.max(limite, 1), 240)));
        return serie.reversed();
    }

    @Transactional(readOnly = true)
    public SnapshotTurno ultimoSnapshot() {
        return repositorioSnapshot.findFirstByOrderByTurnoDesc().orElse(null);
    }

    @Transactional(readOnly = true)
    public List<EstatisticaSetor> setoresNoTurno(int turno) {
        return repositorioSetor.findByTurno(turno);
    }

    @Transactional(readOnly = true)
    public List<EstatisticaSetor> serieDoSetor(Setor setor, int limite) {
        List<EstatisticaSetor> serie = repositorioSetor.findBySetorOrderByTurnoDesc(
                setor, PageRequest.of(0, Math.min(Math.max(limite, 1), 240)));
        return serie.reversed();
    }

    /** Ranking de empresas por valor de mercado, com indicadores de qualidade do lastro. */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> ranking(int limite) {
        List<Map<String, Object>> ranking = new ArrayList<>();
        repositorioEmpresa.findByAtivaTrue().stream()
                .sorted(Comparator.comparingDouble(Empresa::getValuation).reversed())
                .limit(Math.min(Math.max(limite, 1), 100))
                .forEach(empresa -> {
                    Map<String, Object> linha = new LinkedHashMap<>();
                    linha.put("empresaId", empresa.getId());
                    linha.put("nome", empresa.getNome());
                    linha.put("setor", empresa.getSetor().name());
                    linha.put("valuation", empresa.getValuation());
                    // Sem lucro absoluto: o numero pertence a pagina da empresa.
                    linha.put("crescimentoLucro", empresa.getCrescimentoLucro());
                    linha.put("marketShare", empresa.getMarketShare());
                    linha.put("indiceLastro", empresa.indiceLastro());
                    linha.put("funcionarios", empresa.getFuncionarios());
                    linha.put("capitalAberto", empresa.isCapitalAberto());
                    ranking.add(linha);
                });
        return ranking;
    }
}
