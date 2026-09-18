package com.geohistoricalsim.investimento;

import com.geohistoricalsim.auditoria.ServicoAuditoria;
import com.geohistoricalsim.comum.RegraDeNegocioException;
import com.geohistoricalsim.core.ServicoEstadoJogo;
import com.geohistoricalsim.economia.Empresa;
import com.geohistoricalsim.economia.RepositorioEmpresa;
import com.geohistoricalsim.economia.ServicoEmpresa;
import com.geohistoricalsim.jogador.Jogador;
import com.geohistoricalsim.jogador.ServicoJogador;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Mercado de acoes do jogo.
 *
 * <h2>Regras de lastro</h2>
 * <ul>
 *   <li>Compra e feita no mercado primario: o dinheiro entra no caixa da
 *       empresa e vira capacidade produtiva, de modo que todo aporte tem
 *       contrapartida real no balanco.</li>
 *   <li>Venda e liquidada pelo mercado (formadores de liquidez), devolvendo as
 *       acoes ao lote negociavel sem drenar o caixa da empresa.</li>
 *   <li>O retorno do investidor vem de dividendos mensais (fracao do lucro) e
 *       da variacao do preco da acao, que acompanha lucro e patrimonio.</li>
 * </ul>
 */
@Service
public class ServicoInvestimento {

    /** Custo de transacao cobrado do investidor na compra e na venda. */
    private static final double SPREAD = 0.005;
    private static final long LOTE_MINIMO = 100L;

    private final RepositorioInvestimento repositorio;
    private final RepositorioEmpresa repositorioEmpresa;
    private final ServicoEmpresa servicoEmpresa;
    private final ServicoJogador servicoJogador;
    private final ServicoRazao razao;
    private final ServicoAuditoria auditoria;
    private final ServicoEstadoJogo estadoJogo;

    public ServicoInvestimento(RepositorioInvestimento repositorio,
                               RepositorioEmpresa repositorioEmpresa,
                               ServicoEmpresa servicoEmpresa,
                               ServicoJogador servicoJogador,
                               ServicoRazao razao,
                               ServicoAuditoria auditoria,
                               ServicoEstadoJogo estadoJogo) {
        this.repositorio = repositorio;
        this.repositorioEmpresa = repositorioEmpresa;
        this.servicoEmpresa = servicoEmpresa;
        this.servicoJogador = servicoJogador;
        this.razao = razao;
        this.auditoria = auditoria;
        this.estadoJogo = estadoJogo;
    }

    /** Acoes ainda disponiveis para compra no lote negociavel da empresa. */
    @Transactional(readOnly = true)
    public long acoesDisponiveis(Empresa empresa) {
        return Math.max(empresa.getAcoesEmCirculacao() - repositorio.acoesEmPoderDeInvestidores(empresa.getId()), 0);
    }

    @Transactional
    public Investimento comprar(Long jogadorId, Long empresaId, long quantidade) {
        Jogador jogador = servicoJogador.buscar(jogadorId);
        Empresa empresa = servicoEmpresa.buscar(empresaId);

        if (!empresa.isCapitalAberto() || !empresa.isAtiva()) {
            throw new RegraDeNegocioException("A empresa " + empresa.getNome() + " nao tem acoes negociaveis.");
        }
        if (quantidade < LOTE_MINIMO || quantidade % LOTE_MINIMO != 0) {
            throw new RegraDeNegocioException("Negociacao em lotes de " + LOTE_MINIMO + " acoes.");
        }
        long disponiveis = acoesDisponiveis(empresa);
        if (quantidade > disponiveis) {
            throw new RegraDeNegocioException("Apenas " + disponiveis + " acoes disponiveis para compra.");
        }
        if (empresa.getDono() != null && empresa.getDono().getId().equals(jogadorId)) {
            throw new RegraDeNegocioException("O dono ja detem o controle e nao compra do proprio lote.");
        }

        double precoUnitario = empresa.getPrecoAcao() * (1 + SPREAD);
        double total = precoUnitario * quantidade;
        servicoJogador.debitar(jogador, total, "comprar acoes de " + empresa.getNome());

        // Mercado primario: o capital entra na empresa e vira patrimonio.
        empresa.setCaixa(empresa.getCaixa() + total);
        repositorioEmpresa.save(empresa);

        Investimento investimento = repositorio.findByJogadorIdAndEmpresaId(jogadorId, empresaId)
                .orElseGet(() -> {
                    Investimento novo = new Investimento();
                    novo.setJogador(jogador);
                    novo.setEmpresa(empresa);
                    novo.setTurnoEntrada(estadoJogo.turnoAtual());
                    return novo;
                });
        long acoesAnteriores = investimento.getAcoes();
        double custoAnterior = acoesAnteriores * investimento.getPrecoMedio();
        long novasAcoes = acoesAnteriores + quantidade;

        investimento.setAcoes(novasAcoes);
        investimento.setPrecoMedio((custoAnterior + total) / novasAcoes);
        investimento.setCapitalAportado(investimento.getCapitalAportado() + total);
        investimento.setAtivo(true);
        Investimento salvo = repositorio.save(investimento);

        int turno = estadoJogo.turnoAtual();
        razao.registrar(turno, TipoLancamento.COMPRA_ACOES, "jogador:" + jogador.getUsuario(),
                "empresa:" + empresa.getId(), total, empresa.getId(), jogador.getId(),
                quantidade + " acoes a R$ " + String.format("%.4f", precoUnitario));
        Map<String, Object> detalhes = new LinkedHashMap<>();
        detalhes.put("empresa", empresa.getNome());
        detalhes.put("quantidade", quantidade);
        detalhes.put("precoUnitario", precoUnitario);
        detalhes.put("total", total);
        auditoria.registrar(jogador.getUsuario(), "INVESTIMENTO_COMPRA", "Investimento", salvo.getId(),
                "Compra de acoes de " + empresa.getNome(), detalhes);
        return salvo;
    }

    @Transactional
    public Investimento vender(Long jogadorId, Long empresaId, long quantidade) {
        Jogador jogador = servicoJogador.buscar(jogadorId);
        Empresa empresa = servicoEmpresa.buscar(empresaId);
        Investimento investimento = repositorio.findByJogadorIdAndEmpresaId(jogadorId, empresaId)
                .orElseThrow(() -> new RegraDeNegocioException("Voce nao possui acoes dessa empresa."));

        if (quantidade < LOTE_MINIMO || quantidade % LOTE_MINIMO != 0) {
            throw new RegraDeNegocioException("Negociacao em lotes de " + LOTE_MINIMO + " acoes.");
        }
        if (quantidade > investimento.getAcoes()) {
            throw new RegraDeNegocioException("Voce possui apenas " + investimento.getAcoes() + " acoes.");
        }

        double precoUnitario = empresa.getPrecoAcao() * (1 - SPREAD);
        double total = precoUnitario * quantidade;
        double custoBase = investimento.getPrecoMedio() * quantidade;

        investimento.setAcoes(investimento.getAcoes() - quantidade);
        investimento.setCapitalAportado(Math.max(investimento.getCapitalAportado() - custoBase, 0));
        investimento.setLucroRealizado(investimento.getLucroRealizado() + (total - custoBase));
        if (investimento.getAcoes() == 0) {
            investimento.setAtivo(false);
            investimento.setPrecoMedio(0);
        }
        repositorio.save(investimento);
        servicoJogador.creditar(jogador, total);

        int turno = estadoJogo.turnoAtual();
        razao.registrar(turno, TipoLancamento.VENDA_ACOES, "mercado",
                "jogador:" + jogador.getUsuario(), total, empresa.getId(), jogador.getId(),
                quantidade + " acoes a R$ " + String.format("%.4f", precoUnitario));
        Map<String, Object> detalhes = new LinkedHashMap<>();
        detalhes.put("empresa", empresa.getNome());
        detalhes.put("quantidade", quantidade);
        detalhes.put("precoUnitario", precoUnitario);
        detalhes.put("resultado", total - custoBase);
        auditoria.registrar(jogador.getUsuario(), "INVESTIMENTO_VENDA", "Investimento", investimento.getId(),
                "Venda de acoes de " + empresa.getNome(), detalhes);
        return investimento;
    }

    /**
     * Distribui o dividendo de um turno. Os investidores recebem na proporcao
     * das acoes que detem; o restante vai para o caixa pessoal do dono.
     *
     * @return total efetivamente distribuido
     */
    @Transactional
    public double distribuirDividendos(Empresa empresa, double lucroDoTurno, int turno) {
        if (lucroDoTurno <= 0 || empresa.getPayout() <= 0) {
            return 0.0;
        }
        double total = lucroDoTurno * empresa.getPayout();
        if (total > empresa.getCaixa()) {
            total = Math.max(empresa.getCaixa(), 0);
        }
        if (total <= 0) {
            return 0.0;
        }
        empresa.setCaixa(empresa.getCaixa() - total);

        List<Investimento> posicoes = repositorio.findByEmpresaIdAndAtivoTrue(empresa.getId());
        long acoesTotais = Math.max(empresa.getAcoesTotais(), 1);
        double distribuidoInvestidores = 0.0;

        for (Investimento posicao : posicoes) {
            if (posicao.getAcoes() <= 0) {
                continue;
            }
            double parte = total * (posicao.getAcoes() / (double) acoesTotais);
            if (parte <= 0) {
                continue;
            }
            posicao.setDividendosRecebidos(posicao.getDividendosRecebidos() + parte);
            repositorio.save(posicao);
            servicoJogador.creditar(posicao.getJogador(), parte);
            distribuidoInvestidores += parte;
            razao.registrar(turno, TipoLancamento.DIVIDENDO, "empresa:" + empresa.getId(),
                    "jogador:" + posicao.getJogador().getUsuario(), parte, empresa.getId(),
                    posicao.getJogador().getId(), "Dividendo do turno " + turno);
        }

        double parteDono = total - distribuidoInvestidores;
        if (parteDono > 0 && empresa.getDono() != null) {
            servicoJogador.creditar(empresa.getDono(), parteDono);
            razao.registrar(turno, TipoLancamento.DIVIDENDO, "empresa:" + empresa.getId(),
                    "jogador:" + empresa.getDono().getUsuario(), parteDono, empresa.getId(),
                    empresa.getDono().getId(), "Distribuicao ao controlador no turno " + turno);
        }
        return total;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> carteira(Long jogadorId) {
        List<Map<String, Object>> carteira = new ArrayList<>();
        for (Investimento posicao : repositorio.findByJogadorIdAndAtivoTrue(jogadorId)) {
            Empresa empresa = posicao.getEmpresa();
            double valorAtual = posicao.getAcoes() * empresa.getPrecoAcao();
            double custo = posicao.getAcoes() * posicao.getPrecoMedio();
            Map<String, Object> linha = new LinkedHashMap<>();
            linha.put("investimentoId", posicao.getId());
            linha.put("empresaId", empresa.getId());
            linha.put("empresa", empresa.getNome());
            linha.put("setor", empresa.getSetor().name());
            linha.put("acoes", posicao.getAcoes());
            linha.put("precoMedio", posicao.getPrecoMedio());
            linha.put("precoAtual", empresa.getPrecoAcao());
            linha.put("valorAtual", valorAtual);
            linha.put("resultadoNaoRealizado", valorAtual - custo);
            linha.put("dividendosRecebidos", posicao.getDividendosRecebidos());
            linha.put("lucroRealizado", posicao.getLucroRealizado());
            linha.put("retornoTotal", valorAtual - custo + posicao.getDividendosRecebidos() + posicao.getLucroRealizado());
            linha.put("indiceLastro", empresa.indiceLastro());
            linha.put("crescimentoLucro", empresa.getCrescimentoLucro());
            carteira.add(linha);
        }
        return carteira;
    }

    @Transactional(readOnly = true)
    public List<Investimento> posicoesDaEmpresa(Long empresaId) {
        return repositorio.findByEmpresaIdAndAtivoTrue(empresaId);
    }

    @Transactional(readOnly = true)
    public int investidoresAtivos() {
        return repositorio.investidoresAtivos();
    }

    @Transactional(readOnly = true)
    public double capitalTotalInvestido() {
        return repositorio.capitalTotalInvestido();
    }
}
