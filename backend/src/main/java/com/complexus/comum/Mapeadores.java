package com.complexus.comum;

import com.complexus.auditoria.EventoAuditoria;
import com.complexus.core.EstadoJogo;
import com.complexus.economia.Departamento;
import com.complexus.economia.Empreendimento;
import com.complexus.economia.Empresa;
import com.complexus.economia.HistoricoEmpresa;
import com.complexus.economia.LinhaProduto;
import com.complexus.economia.Unidade;
import com.complexus.estatistica.EstatisticaSetor;
import com.complexus.financas.Financiamento;
import com.complexus.estatistica.SnapshotTurno;
import com.complexus.investimento.LancamentoFinanceiro;
import com.complexus.jogador.Jogador;
import com.complexus.politica.Estado;
import com.complexus.politica.Mandato;
import com.complexus.politica.Municipio;
import com.complexus.politica.Pais;
import com.complexus.politica.ProjetoDeLei;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Conversao de entidades para os mapas devolvidos pela API.
 *
 * Manter a serializacao em um unico lugar evita expor a entidade diretamente e
 * facilita evoluir o banco sem quebrar o contrato do frontend.
 */
public final class Mapeadores {

    private Mapeadores() {
    }

    public static <T> List<Map<String, Object>> lista(List<T> itens, Function<T, Map<String, Object>> conversor) {
        return itens.stream().map(conversor).toList();
    }

    public static Map<String, Object> jogador(Jogador jogador) {
        Map<String, Object> mapa = new LinkedHashMap<>();
        mapa.put("id", jogador.getId());
        mapa.put("usuario", jogador.getUsuario());
        mapa.put("nome", jogador.getNome());
        mapa.put("saldo", jogador.getSaldo());
        return mapa;
    }

    public static Map<String, Object> empresa(Empresa empresa) {
        Map<String, Object> mapa = new LinkedHashMap<>();
        mapa.put("id", empresa.getId());
        mapa.put("nome", empresa.getNome());
        mapa.put("setor", empresa.getSetor().name());
        mapa.put("setorRotulo", empresa.getSetor().getRotulo());
        mapa.put("donoId", empresa.getDono() == null ? null : empresa.getDono().getId());
        mapa.put("dono", empresa.getDono() == null ? "Sistema" : empresa.getDono().getNome());
        mapa.put("municipioId", empresa.getMunicipio().getId());
        mapa.put("municipio", empresa.getMunicipio().getNome());
        mapa.put("estado", empresa.getMunicipio().getEstado().getSigla());
        mapa.put("ativa", empresa.isAtiva());
        mapa.put("caixa", empresa.getCaixa());
        mapa.put("patrimonio", empresa.getPatrimonio());
        mapa.put("divida", empresa.getDivida());
        mapa.put("patrimonioLiquido", empresa.patrimonioLiquido());
        mapa.put("receitaMensal", empresa.getReceitaMensal());
        mapa.put("custoMensal", empresa.getCustoMensal());
        mapa.put("impostosMensais", empresa.getImpostosMensais());
        mapa.put("lucroMensal", empresa.getLucroMensal());
        mapa.put("crescimentoLucro", empresa.getCrescimentoLucro());
        mapa.put("lucroAcumulado", empresa.getLucroAcumulado());
        mapa.put("funcionarios", empresa.getFuncionarios());
        mapa.put("salarioMedio", empresa.getSalarioMedio());
        mapa.put("produtividade", empresa.getProdutividade());
        mapa.put("marketingMensal", empresa.getMarketingMensal());
        mapa.put("reputacao", empresa.getReputacao());
        mapa.put("marketShare", empresa.getMarketShare());
        mapa.put("valuation", empresa.getValuation());
        mapa.put("precoAcao", empresa.getPrecoAcao());
        mapa.put("acoesTotais", empresa.getAcoesTotais());
        mapa.put("acoesEmCirculacao", empresa.getAcoesEmCirculacao());
        mapa.put("capitalAberto", empresa.isCapitalAberto());
        mapa.put("payout", empresa.getPayout());
        mapa.put("indiceLastro", empresa.indiceLastro());
        mapa.put("turnoFundacao", empresa.getTurnoFundacao());
        return mapa;
    }

    /**
     * Visao publica de uma empresa, usada nas listagens.
     *
     * Nao traz lucro, custo, caixa nem patrimonio: resultado de empresa e
     * assunto da pagina da propria empresa, exibido ali em grafico.
     */
    public static Map<String, Object> empresaPublica(Empresa empresa) {
        Map<String, Object> mapa = new LinkedHashMap<>();
        mapa.put("id", empresa.getId());
        mapa.put("nome", empresa.getNome());
        mapa.put("setor", empresa.getSetor().name());
        mapa.put("setorRotulo", empresa.getSetor().getRotulo());
        mapa.put("donoId", empresa.getDono() == null ? null : empresa.getDono().getId());
        mapa.put("dono", empresa.getDono() == null ? "Sistema" : empresa.getDono().getNome());
        mapa.put("municipioId", empresa.getMunicipio().getId());
        mapa.put("municipio", empresa.getMunicipio().getNome());
        mapa.put("estado", empresa.getMunicipio().getEstado().getSigla());
        mapa.put("ativa", empresa.isAtiva());
        mapa.put("receitaMensal", empresa.getReceitaMensal());
        mapa.put("funcionarios", empresa.getFuncionarios());
        mapa.put("marketShare", empresa.getMarketShare());
        mapa.put("valuation", empresa.getValuation());
        mapa.put("precoAcao", empresa.getPrecoAcao());
        mapa.put("capitalAberto", empresa.isCapitalAberto());
        mapa.put("turnoFundacao", empresa.getTurnoFundacao());
        return mapa;
    }

    public static Map<String, Object> historico(HistoricoEmpresa historico) {
        Map<String, Object> mapa = new LinkedHashMap<>();
        mapa.put("turno", historico.getTurno());
        mapa.put("receita", historico.getReceita());
        mapa.put("custo", historico.getCusto());
        mapa.put("impostos", historico.getImpostos());
        mapa.put("lucro", historico.getLucro());
        mapa.put("crescimentoLucro", historico.getCrescimentoLucro());
        mapa.put("valuation", historico.getValuation());
        mapa.put("precoAcao", historico.getPrecoAcao());
        mapa.put("marketShare", historico.getMarketShare());
        mapa.put("caixa", historico.getCaixa());
        mapa.put("patrimonio", historico.getPatrimonio());
        mapa.put("funcionarios", historico.getFuncionarios());
        mapa.put("dividendosPagos", historico.getDividendosPagos());
        return mapa;
    }

    public static Map<String, Object> empreendimento(Empreendimento obra) {
        Map<String, Object> mapa = new LinkedHashMap<>();
        mapa.put("id", obra.getId());
        mapa.put("nome", obra.getNome());
        mapa.put("tipo", obra.getTipo().name());
        mapa.put("tipoRotulo", obra.getTipo().getRotulo());
        mapa.put("status", obra.getStatus().name());
        mapa.put("unidadeId", obra.getUnidade() == null ? null : obra.getUnidade().getId());
        mapa.put("unidade", obra.getUnidade() == null ? null : obra.getUnidade().getNome());
        mapa.put("custoTotal", obra.getCustoTotal());
        mapa.put("investido", obra.getInvestido());
        mapa.put("valorEstimado", obra.getValorEstimado());
        mapa.put("turnosTotais", obra.getTurnosTotais());
        mapa.put("turnosRestantes", obra.getTurnosRestantes());
        mapa.put("percentualConcluido", obra.percentualConcluido());
        return mapa;
    }

    public static Map<String, Object> unidade(Unidade unidade) {
        Map<String, Object> mapa = new LinkedHashMap<>();
        mapa.put("id", unidade.getId());
        mapa.put("empresaId", unidade.getEmpresa().getId());
        mapa.put("nome", unidade.getNome());
        mapa.put("municipioId", unidade.getMunicipio().getId());
        mapa.put("municipio", unidade.getMunicipio().getNome());
        mapa.put("estado", unidade.getMunicipio().getEstado().getSigla());
        mapa.put("sede", unidade.isSede());
        mapa.put("ativa", unidade.isAtiva());
        mapa.put("turnoAbertura", unidade.getTurnoAbertura());
        mapa.put("patrimonio", unidade.getPatrimonio());
        mapa.put("funcionarios", unidade.getFuncionarios());
        mapa.put("produtividade", unidade.getProdutividade());
        mapa.put("receitaMensal", unidade.getReceitaMensal());
        mapa.put("custoMensal", unidade.getCustoMensal());
        mapa.put("margemOperacional", unidade.getMargemOperacional());
        mapa.put("ocupacao", unidade.getOcupacao());
        mapa.put("marketShare", unidade.getMarketShare());
        return mapa;
    }

    public static Map<String, Object> linhaProduto(LinhaProduto linha) {
        Map<String, Object> mapa = new LinkedHashMap<>();
        mapa.put("id", linha.getId());
        mapa.put("empresaId", linha.getEmpresa().getId());
        mapa.put("nome", linha.getNome());
        mapa.put("posicionamento", linha.getPosicionamento().name());
        mapa.put("posicionamentoRotulo", linha.getPosicionamento().getRotulo());
        mapa.put("fatorPreco", linha.getPosicionamento().getFatorPreco());
        mapa.put("fatorCusto", linha.getPosicionamento().getFatorCusto());
        mapa.put("fatiaMix", linha.getFatiaMix());
        mapa.put("ativa", linha.isAtiva());
        mapa.put("turnoCriacao", linha.getTurnoCriacao());
        return mapa;
    }

    public static Map<String, Object> departamento(Departamento departamento) {
        Map<String, Object> mapa = new LinkedHashMap<>();
        mapa.put("id", departamento.getId());
        mapa.put("empresaId", departamento.getEmpresa().getId());
        mapa.put("area", departamento.getArea().name());
        mapa.put("areaRotulo", departamento.getArea().getRotulo());
        mapa.put("unidadeEfeito", departamento.getArea().getUnidadeEfeito());
        mapa.put("orcamentoMensal", departamento.getOrcamentoMensal());
        return mapa;
    }

    public static Map<String, Object> financiamento(Financiamento contrato) {
        Map<String, Object> mapa = new LinkedHashMap<>();
        mapa.put("id", contrato.getId());
        mapa.put("empresaId", contrato.getEmpresa().getId());
        mapa.put("modalidade", contrato.getModalidade().name());
        mapa.put("modalidadeRotulo", contrato.getModalidade().getRotulo());
        mapa.put("status", contrato.getStatus().name());
        mapa.put("statusRotulo", contrato.getStatus().getRotulo());
        mapa.put("principal", contrato.getPrincipal());
        mapa.put("saldoDevedor", contrato.getSaldoDevedor());
        mapa.put("taxaMensal", contrato.getTaxaMensal());
        mapa.put("taxaAnual", contrato.taxaAnual());
        mapa.put("notaNaContratacao", contrato.getNotaNaContratacao().name());
        mapa.put("prazoTurnos", contrato.getPrazoTurnos());
        mapa.put("turnosRestantes", contrato.getTurnosRestantes());
        mapa.put("turnoContratacao", contrato.getTurnoContratacao());
        mapa.put("garantia", contrato.getGarantia());
        mapa.put("jurosPagos", contrato.getJurosPagos());
        mapa.put("amortizado", contrato.getAmortizado());
        mapa.put("parcelasEmAtraso", contrato.getParcelasEmAtraso());
        mapa.put("jurosDoMes", contrato.jurosDoMes());
        mapa.put("amortizacaoDoMes", contrato.amortizacaoDoMes());
        mapa.put("parcelaDoMes", contrato.parcelaDoMes());
        return mapa;
    }

    public static Map<String, Object> mandato(Mandato mandato) {
        Map<String, Object> mapa = new LinkedHashMap<>();
        mapa.put("id", mandato.getId());
        mapa.put("titular", mandato.getTitular());
        mapa.put("jogadorId", mandato.getJogador() == null ? null : mandato.getJogador().getId());
        mapa.put("cargo", mandato.getCargo().name());
        mapa.put("cargoRotulo", mandato.getCargo().getRotulo());
        mapa.put("esfera", mandato.getEsfera().name());
        mapa.put("territorioId", mandato.getTerritorioId());
        mapa.put("partido", mandato.getPartido());
        mapa.put("turnoInicio", mandato.getTurnoInicio());
        mapa.put("turnoFim", mandato.getTurnoFim());
        mapa.put("npc", mandato.isNpc());
        mapa.put("aprovacao", mandato.getAprovacao());
        mapa.put("legislativo", mandato.getCargo().isLegislativo());
        mapa.put("executivo", mandato.getCargo().isExecutivo());
        return mapa;
    }

    public static Map<String, Object> projeto(ProjetoDeLei projeto) {
        Map<String, Object> mapa = new LinkedHashMap<>();
        mapa.put("id", projeto.getId());
        mapa.put("titulo", projeto.getTitulo());
        mapa.put("ementa", projeto.getEmenta());
        mapa.put("autor", projeto.getAutor().getTitular());
        mapa.put("autorMandatoId", projeto.getAutor().getId());
        mapa.put("autorCargo", projeto.getAutor().getCargo().getRotulo());
        mapa.put("esfera", projeto.getEsfera().name());
        mapa.put("territorioId", projeto.getTerritorioId());
        mapa.put("tipo", projeto.getTipo().name());
        mapa.put("tipoDescricao", projeto.getTipo().getDescricao());
        mapa.put("unidadeParametro", projeto.getTipo().getUnidadeParametro());
        mapa.put("setorAlvo", projeto.getSetorAlvo() == null ? null : projeto.getSetorAlvo().name());
        mapa.put("parametro", projeto.getParametro());
        mapa.put("status", projeto.getStatus().name());
        mapa.put("statusRotulo", projeto.getStatus().getRotulo());
        mapa.put("turnoCriacao", projeto.getTurnoCriacao());
        mapa.put("turnoVotacao", projeto.getTurnoVotacao());
        mapa.put("turnoVigencia", projeto.getTurnoVigencia());
        mapa.put("votosSim", projeto.getVotosSim());
        mapa.put("votosNao", projeto.getVotosNao());
        mapa.put("votosAbstencao", projeto.getVotosAbstencao());
        mapa.put("justificativaVeto", projeto.getJustificativaVeto());
        mapa.put("efeitoAplicado", projeto.getEfeitoAplicado());
        return mapa;
    }

    public static Map<String, Object> evento(EventoAuditoria evento) {
        Map<String, Object> mapa = new LinkedHashMap<>();
        mapa.put("id", evento.getId());
        mapa.put("momento", evento.getMomento().toString());
        mapa.put("turno", evento.getTurno());
        mapa.put("ator", evento.getAtor());
        mapa.put("acao", evento.getAcao());
        mapa.put("entidade", evento.getEntidade());
        mapa.put("entidadeId", evento.getEntidadeId());
        mapa.put("descricao", evento.getDescricao());
        mapa.put("detalhes", evento.getDetalhes());
        mapa.put("hashAnterior", evento.getHashAnterior());
        mapa.put("hash", evento.getHash());
        return mapa;
    }

    public static Map<String, Object> lancamento(LancamentoFinanceiro lancamento) {
        Map<String, Object> mapa = new LinkedHashMap<>();
        mapa.put("id", lancamento.getId());
        mapa.put("momento", lancamento.getMomento().toString());
        mapa.put("turno", lancamento.getTurno());
        mapa.put("tipo", lancamento.getTipo().name());
        mapa.put("tipoRotulo", lancamento.getTipo().getRotulo());
        mapa.put("origem", lancamento.getOrigem());
        mapa.put("destino", lancamento.getDestino());
        mapa.put("valor", lancamento.getValor());
        mapa.put("empresaId", lancamento.getEmpresaId());
        mapa.put("jogadorId", lancamento.getJogadorId());
        mapa.put("descricao", lancamento.getDescricao());
        return mapa;
    }

    public static Map<String, Object> snapshot(SnapshotTurno s) {
        Map<String, Object> mapa = new LinkedHashMap<>();
        mapa.put("turno", s.getTurno());
        mapa.put("dataJogo", s.getDataJogo().toString());
        mapa.put("empresasAtivas", s.getEmpresasAtivas());
        mapa.put("receitaAgregada", s.getReceitaAgregada());
        mapa.put("lucroAgregado", s.getLucroAgregado());
        mapa.put("impostosArrecadados", s.getImpostosArrecadados());
        mapa.put("dividendosPagos", s.getDividendosPagos());
        mapa.put("valuationAgregado", s.getValuationAgregado());
        mapa.put("indiceMercado", s.getIndiceMercado());
        mapa.put("variacaoIndice", s.getVariacaoIndice());
        mapa.put("inflacaoAnual", s.getInflacaoAnual());
        mapa.put("taxaJuros", s.getTaxaJuros());
        mapa.put("pib", s.getPib());
        mapa.put("desemprego", s.getDesemprego());
        mapa.put("estabilidade", s.getEstabilidade());
        mapa.put("aprovacaoGoverno", s.getAprovacaoGoverno());
        mapa.put("leisEmVigor", s.getLeisEmVigor());
        mapa.put("investidoresAtivos", s.getInvestidoresAtivos());
        mapa.put("capitalInvestido", s.getCapitalInvestido());
        mapa.put("empregosTotais", s.getEmpregosTotais());
        return mapa;
    }

    public static Map<String, Object> estatisticaSetor(EstatisticaSetor e) {
        Map<String, Object> mapa = new LinkedHashMap<>();
        mapa.put("turno", e.getTurno());
        mapa.put("setor", e.getSetor().name());
        mapa.put("setorRotulo", e.getSetor().getRotulo());
        mapa.put("empresas", e.getEmpresas());
        mapa.put("receita", e.getReceita());
        mapa.put("lucro", e.getLucro());
        mapa.put("valuation", e.getValuation());
        mapa.put("crescimentoMedio", e.getCrescimentoMedio());
        mapa.put("margemMedia", e.getMargemMedia());
        mapa.put("empresaLider", e.getEmpresaLider());
        mapa.put("empregos", e.getEmpregos());
        return mapa;
    }

    public static Map<String, Object> estadoJogo(EstadoJogo estado) {
        Map<String, Object> mapa = new LinkedHashMap<>();
        mapa.put("turnoAtual", estado.getTurnoAtual());
        mapa.put("dataJogo", estado.getDataJogo().toString());
        mapa.put("iniciadoEm", estado.getIniciadoEm().toString());
        mapa.put("ultimoProcessamento",
                estado.getUltimoProcessamento() == null ? null : estado.getUltimoProcessamento().toString());
        mapa.put("proximoProcessamento",
                estado.getProximoProcessamento() == null ? null : estado.getProximoProcessamento().toString());
        mapa.put("indiceMercado", estado.getIndiceMercado());
        mapa.put("inflacaoAnual", estado.getInflacaoAnual());
        mapa.put("taxaJuros", estado.getTaxaJuros());
        return mapa;
    }

    public static Map<String, Object> pais(Pais pais) {
        Map<String, Object> mapa = new LinkedHashMap<>();
        mapa.put("id", pais.getId());
        mapa.put("nome", pais.getNome());
        mapa.put("sigla", pais.getSigla());
        mapa.put("populacao", pais.getPopulacao());
        mapa.put("tesouro", pais.getTesouro());
        mapa.put("pib", pais.getPib());
        mapa.put("aliquotaImpostoEmpresarial", pais.getAliquotaImpostoEmpresarial());
        mapa.put("gastoSocialMensal", pais.getGastoSocialMensal());
        mapa.put("estabilidade", pais.getEstabilidade());
        mapa.put("aprovacaoGoverno", pais.getAprovacaoGoverno());
        mapa.put("desemprego", pais.getDesemprego());
        mapa.put("rendaMedia", pais.getRendaMedia());
        return mapa;
    }

    public static Map<String, Object> estado(Estado estado) {
        Map<String, Object> mapa = new LinkedHashMap<>();
        mapa.put("id", estado.getId());
        mapa.put("nome", estado.getNome());
        mapa.put("sigla", estado.getSigla());
        mapa.put("paisId", estado.getPais().getId());
        mapa.put("populacao", estado.getPopulacao());
        mapa.put("tesouro", estado.getTesouro());
        mapa.put("aliquotaEstadual", estado.getAliquotaEstadual());
        mapa.put("investimentoInfraestrutura", estado.getInvestimentoInfraestrutura());
        mapa.put("indiceDesenvolvimento", estado.getIndiceDesenvolvimento());
        return mapa;
    }

    public static Map<String, Object> municipio(Municipio municipio) {
        Map<String, Object> mapa = new LinkedHashMap<>();
        mapa.put("id", municipio.getId());
        mapa.put("nome", municipio.getNome());
        mapa.put("estadoId", municipio.getEstado().getId());
        mapa.put("estado", municipio.getEstado().getSigla());
        mapa.put("populacao", municipio.getPopulacao());
        mapa.put("tesouro", municipio.getTesouro());
        mapa.put("aliquotaMunicipal", municipio.getAliquotaMunicipal());
        mapa.put("indiceUrbanizacao", municipio.getIndiceUrbanizacao());
        mapa.put("demandaImobiliaria", municipio.getDemandaImobiliaria());
        mapa.put("custoTerrenoM2", municipio.getCustoTerrenoM2());
        mapa.put("zoneamento", municipio.getZoneamento());
        return mapa;
    }
}
