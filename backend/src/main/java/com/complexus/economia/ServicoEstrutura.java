package com.complexus.economia;

import com.complexus.auditoria.ServicoAuditoria;
import com.complexus.comum.RecursoNaoEncontradoException;
import com.complexus.comum.RegraDeNegocioException;
import com.complexus.core.ServicoEstadoJogo;
import com.complexus.investimento.ServicoRazao;
import com.complexus.investimento.TipoLancamento;
import com.complexus.jogador.Jogador;
import com.complexus.jogador.ServicoJogador;
import com.complexus.politica.Municipio;
import com.complexus.politica.RepositorioMunicipio;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Estrutura interna da empresa: unidades, linhas de produto e departamentos.
 *
 * <h2>Unidades</h2>
 * A empresa deixa de ser uma caixa unica. Equipe e patrimonio ficam alocados em
 * unidades, cada uma instalada em um municipio e competindo no mercado de la. A
 * empresa continua publicando os totais - {@code patrimonio}, {@code funcionarios}
 * e {@code produtividade} sao sempre a soma (ou a media ponderada) das unidades,
 * mantida por {@link #sincronizarAgregados}.
 *
 * <h2>Linhas de produto</h2>
 * O mix define o preco praticado e o custo do insumo. A fatia nao declarada
 * segue o padrao do setor, entao declarar uma unica linha premium de 20 por
 * cento move o preco medio em 20 por cento do premio, nao no premio inteiro.
 *
 * <h2>Departamentos</h2>
 * Custo fixo mensal que compra vantagem com retorno decrescente. O orcamento sai
 * do resultado no fechamento do turno, junto com os juros.
 */
@Service
public class ServicoEstrutura {

    /** Custo de instalacao de uma filial, sobre o capital alocado. */
    private static final double CUSTO_INSTALACAO = 0.08;
    /** Desagio na venda dos ativos de uma unidade fechada. */
    private static final double DESAGIO_FECHAMENTO = 0.30;
    private static final double PRODUTIVIDADE_MAXIMA = 2.0;
    /**
     * Porte de referencia para o orcamento dos departamentos: 2 por cento da
     * capacidade mensal instalada. E o que faz o efeito saturar em escala
     * proporcional ao tamanho da empresa.
     */
    private static final double FRACAO_PORTE = 0.02;

    private final RepositorioUnidade repositorioUnidade;
    private final RepositorioLinhaProduto repositorioLinha;
    private final RepositorioDepartamento repositorioDepartamento;
    private final RepositorioEmpresa repositorioEmpresa;
    private final RepositorioMunicipio repositorioMunicipio;
    private final ServicoJogador servicoJogador;
    private final ServicoRazao razao;
    private final ServicoAuditoria auditoria;
    private final ServicoEstadoJogo estadoJogo;

    public ServicoEstrutura(RepositorioUnidade repositorioUnidade,
                            RepositorioLinhaProduto repositorioLinha,
                            RepositorioDepartamento repositorioDepartamento,
                            RepositorioEmpresa repositorioEmpresa,
                            RepositorioMunicipio repositorioMunicipio,
                            ServicoJogador servicoJogador,
                            ServicoRazao razao,
                            ServicoAuditoria auditoria,
                            ServicoEstadoJogo estadoJogo) {
        this.repositorioUnidade = repositorioUnidade;
        this.repositorioLinha = repositorioLinha;
        this.repositorioDepartamento = repositorioDepartamento;
        this.repositorioEmpresa = repositorioEmpresa;
        this.repositorioMunicipio = repositorioMunicipio;
        this.servicoJogador = servicoJogador;
        this.razao = razao;
        this.auditoria = auditoria;
        this.estadoJogo = estadoJogo;
    }

    // ------------------------------------------------------------------
    // Consultas
    // ------------------------------------------------------------------

    @Transactional(readOnly = true)
    public List<Unidade> unidades(Long empresaId) {
        return repositorioUnidade.findByEmpresaIdAndAtivaTrueOrderByIdAsc(empresaId);
    }

    @Transactional(readOnly = true)
    public List<LinhaProduto> linhas(Long empresaId) {
        return repositorioLinha.findByEmpresaIdAndAtivaTrueOrderByIdAsc(empresaId);
    }

    @Transactional(readOnly = true)
    public List<Departamento> departamentos(Long empresaId) {
        return repositorioDepartamento.findByEmpresaIdOrderByIdAsc(empresaId);
    }

    @Transactional(readOnly = true)
    public Unidade buscarUnidade(Long unidadeId) {
        return repositorioUnidade.findById(unidadeId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Unidade", unidadeId));
    }

    /** Unidade que recebe o que o jogador nao enderecou a nenhuma filial. */
    @Transactional(readOnly = true)
    public Unidade sede(Long empresaId) {
        return repositorioUnidade.findFirstByEmpresaIdAndAtivaTrueOrderBySedeDescIdAsc(empresaId)
                .orElseThrow(() -> new RegraDeNegocioException(
                        "A empresa nao tem nenhuma unidade ativa."));
    }

    // ------------------------------------------------------------------
    // Ciclo de vida das unidades
    // ------------------------------------------------------------------

    /** Cria a unidade sede de uma empresa recem-fundada. */
    @Transactional
    public Unidade criarSede(Empresa empresa, double patrimonio, int funcionarios, int turno) {
        Unidade sede = new Unidade();
        sede.setEmpresa(empresa);
        sede.setMunicipio(empresa.getMunicipio());
        sede.setNome("Sede " + empresa.getMunicipio().getNome());
        sede.setSede(true);
        sede.setTurnoAbertura(turno);
        sede.setPatrimonio(patrimonio);
        sede.setFuncionarios(funcionarios);
        sede.setProdutividade(empresa.getProdutividade());
        return repositorioUnidade.save(sede);
    }

    /**
     * Abre uma filial em outro municipio.
     *
     * O capital sai do caixa da empresa e vira patrimonio da unidade, menos o
     * custo de instalacao. A equipe vem de contratacao nova, com o mesmo custo
     * de admissao praticado na sede.
     */
    @Transactional
    public Unidade abrirUnidade(Long empresaId, Long jogadorId, Long municipioId, String nome,
                                double capital, int funcionarios) {
        Empresa empresa = buscarEmpresa(empresaId);
        Jogador jogador = exigirDono(empresa, jogadorId);
        Municipio municipio = repositorioMunicipio.findById(municipioId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Municipio", municipioId));

        double minimo = empresa.getSetor().capitalMinimoUnidade();
        if (capital < minimo) {
            throw new RegraDeNegocioException("Capital minimo para uma unidade do setor "
                    + empresa.getSetor().getRotulo() + " e R$ " + String.format("%.2f", minimo) + ".");
        }
        if (funcionarios < 1) {
            throw new RegraDeNegocioException("A unidade precisa de ao menos 1 funcionario.");
        }
        if (repositorioUnidade.existsByEmpresaIdAndMunicipioIdAndAtivaTrue(empresaId, municipioId)) {
            throw new RegraDeNegocioException("A empresa ja tem uma unidade em " + municipio.getNome() + ".");
        }
        double admissao = funcionarios * empresa.getSalarioMedio() * 0.5;
        double instalacao = capital * CUSTO_INSTALACAO;
        double desembolso = capital + admissao + instalacao;
        if (empresa.getCaixa() < desembolso) {
            throw new RegraDeNegocioException("Caixa insuficiente: a abertura custa R$ "
                    + String.format("%.2f", desembolso) + " (capital, instalacao e admissoes).");
        }

        int turno = estadoJogo.turnoAtual();
        empresa.setCaixa(empresa.getCaixa() - desembolso);

        Unidade unidade = new Unidade();
        unidade.setEmpresa(empresa);
        unidade.setMunicipio(municipio);
        unidade.setNome(nome == null || nome.isBlank() ? "Unidade " + municipio.getNome() : nome.trim());
        unidade.setSede(false);
        unidade.setTurnoAbertura(turno);
        unidade.setPatrimonio(capital);
        unidade.setFuncionarios(funcionarios);
        // A filial nasce com a produtividade media da empresa, nao do zero: o
        // metodo de trabalho ja existe, o que falta e escala.
        unidade.setProdutividade(empresa.getProdutividade());
        Unidade salva = repositorioUnidade.save(unidade);
        sincronizarAgregados(empresa);

        razao.registrar(turno, TipoLancamento.CAPEX, "empresa:" + empresa.getId(),
                "unidade:" + salva.getId(), desembolso, empresa.getId(), jogador.getId(),
                "Abertura da unidade " + salva.getNome());
        Map<String, Object> detalhes = new LinkedHashMap<>();
        detalhes.put("municipio", municipio.getNome());
        detalhes.put("capital", capital);
        detalhes.put("instalacao", instalacao);
        detalhes.put("funcionarios", funcionarios);
        auditoria.registrar(jogador.getUsuario(), "UNIDADE_ABERTA", "Unidade", salva.getId(),
                "Unidade " + salva.getNome() + " aberta pela empresa " + empresa.getNome(), detalhes);
        return salva;
    }

    /**
     * Fecha uma unidade: liquida os ativos com desagio, paga a rescisao da
     * equipe e devolve o que sobrou ao caixa da empresa.
     */
    @Transactional
    public Map<String, Object> fecharUnidade(Long unidadeId, Long jogadorId) {
        Unidade unidade = buscarUnidade(unidadeId);
        Empresa empresa = unidade.getEmpresa();
        Jogador jogador = exigirDono(empresa, jogadorId);
        if (!unidade.isAtiva()) {
            throw new RegraDeNegocioException("A unidade " + unidade.getNome() + " ja esta fechada.");
        }
        if (repositorioUnidade.countByEmpresaIdAndAtivaTrue(empresa.getId()) <= 1) {
            throw new RegraDeNegocioException(
                    "Esta e a ultima unidade ativa: para sair do mercado, encerre a empresa.");
        }

        double liquidacao = unidade.getPatrimonio() * (1 - DESAGIO_FECHAMENTO);
        double rescisao = unidade.getFuncionarios() * empresa.getSalarioMedio();
        double resultado = liquidacao - rescisao;

        unidade.setAtiva(false);
        unidade.setPatrimonio(0);
        unidade.setFuncionarios(0);
        unidade.setReceitaMensal(0);
        unidade.setCustoMensal(0);
        unidade.setMargemOperacional(0);
        unidade.setOcupacao(0);
        unidade.setMarketShare(0);
        repositorioUnidade.save(unidade);

        empresa.setCaixa(empresa.getCaixa() + resultado);
        // Fechar unidade custa marca: a cidade perde emprego e o mercado registra.
        empresa.setReputacao(Math.max(empresa.getReputacao() - 3.0, 0));
        sincronizarAgregados(empresa);

        int turno = estadoJogo.turnoAtual();
        razao.registrar(turno, TipoLancamento.CAPEX, "unidade:" + unidade.getId(),
                "empresa:" + empresa.getId(), liquidacao, empresa.getId(), jogador.getId(),
                "Liquidacao dos ativos da unidade " + unidade.getNome());
        razao.registrar(turno, TipoLancamento.CUSTO_OPERACIONAL, "empresa:" + empresa.getId(),
                "rescisoes", rescisao, empresa.getId(), jogador.getId(),
                "Rescisoes do fechamento da unidade " + unidade.getNome());

        Map<String, Object> resumo = new LinkedHashMap<>();
        resumo.put("unidadeId", unidade.getId());
        resumo.put("liquidacao", liquidacao);
        resumo.put("rescisao", rescisao);
        resumo.put("resultadoLiquido", resultado);
        auditoria.registrar(jogador.getUsuario(), "UNIDADE_FECHADA", "Unidade", unidade.getId(),
                "Unidade " + unidade.getNome() + " fechada", resumo);
        return resumo;
    }

    /** Move patrimonio de uma unidade para outra, sem passar pelo caixa. */
    @Transactional
    public Map<String, Object> transferirCapital(Long jogadorId, Long origemId, Long destinoId, double valor) {
        Unidade origem = buscarUnidade(origemId);
        Unidade destino = buscarUnidade(destinoId);
        Empresa empresa = exigirMesmaEmpresa(origem, destino);
        Jogador jogador = exigirDono(empresa, jogadorId);
        if (valor <= 0) {
            throw new RegraDeNegocioException("O valor transferido deve ser positivo.");
        }
        if (origem.getPatrimonio() < valor) {
            throw new RegraDeNegocioException("A unidade " + origem.getNome() + " tem apenas R$ "
                    + String.format("%.2f", origem.getPatrimonio()) + " em ativos.");
        }
        // Mudar ativo de lugar custa: parte do valor se perde no transporte e na
        // reinstalacao, entao transferir nao substitui planejar.
        double perda = valor * CUSTO_INSTALACAO;
        origem.setPatrimonio(origem.getPatrimonio() - valor);
        destino.setPatrimonio(destino.getPatrimonio() + valor - perda);
        repositorioUnidade.save(origem);
        repositorioUnidade.save(destino);
        sincronizarAgregados(empresa);

        Map<String, Object> resumo = new LinkedHashMap<>();
        resumo.put("valor", valor);
        resumo.put("perdaNaMudanca", perda);
        resumo.put("origem", origem.getNome());
        resumo.put("destino", destino.getNome());
        auditoria.registrar(jogador.getUsuario(), "UNIDADE_TRANSFERENCIA_CAPITAL", "Unidade", destino.getId(),
                "Transferencia de ativos entre unidades de " + empresa.getNome(), resumo);
        return resumo;
    }

    /** Move funcionarios de uma unidade para outra. */
    @Transactional
    public Map<String, Object> transferirEquipe(Long jogadorId, Long origemId, Long destinoId, int quantidade) {
        Unidade origem = buscarUnidade(origemId);
        Unidade destino = buscarUnidade(destinoId);
        Empresa empresa = exigirMesmaEmpresa(origem, destino);
        Jogador jogador = exigirDono(empresa, jogadorId);
        if (quantidade < 1 || quantidade > origem.getFuncionarios()) {
            throw new RegraDeNegocioException("Quantidade invalida: a unidade " + origem.getNome()
                    + " tem " + origem.getFuncionarios() + " funcionarios.");
        }
        double ajudaDeCusto = quantidade * empresa.getSalarioMedio() * 0.3;
        if (empresa.getCaixa() < ajudaDeCusto) {
            throw new RegraDeNegocioException("Caixa insuficiente para a ajuda de custo da transferencia (R$ "
                    + String.format("%.2f", ajudaDeCusto) + ").");
        }
        origem.setFuncionarios(origem.getFuncionarios() - quantidade);
        destino.setFuncionarios(destino.getFuncionarios() + quantidade);
        empresa.setCaixa(empresa.getCaixa() - ajudaDeCusto);
        repositorioUnidade.save(origem);
        repositorioUnidade.save(destino);
        sincronizarAgregados(empresa);

        int turno = estadoJogo.turnoAtual();
        razao.registrar(turno, TipoLancamento.CUSTO_OPERACIONAL, "empresa:" + empresa.getId(),
                "transferencia_de_equipe", ajudaDeCusto, empresa.getId(), jogador.getId(),
                "Ajuda de custo de " + quantidade + " transferencias");

        Map<String, Object> resumo = new LinkedHashMap<>();
        resumo.put("quantidade", quantidade);
        resumo.put("ajudaDeCusto", ajudaDeCusto);
        resumo.put("origem", origem.getNome());
        resumo.put("destino", destino.getNome());
        auditoria.registrar(jogador.getUsuario(), "UNIDADE_TRANSFERENCIA_EQUIPE", "Unidade", destino.getId(),
                "Transferencia de equipe entre unidades de " + empresa.getNome(), resumo);
        return resumo;
    }

    // ------------------------------------------------------------------
    // Linhas de produto
    // ------------------------------------------------------------------

    /** Cria uma linha de produto respeitando o limite de 100 por cento do mix. */
    @Transactional
    public LinhaProduto criarLinha(Long empresaId, Long jogadorId, String nome,
                                   LinhaProduto.Posicionamento posicionamento, double fatiaMix) {
        Empresa empresa = buscarEmpresa(empresaId);
        Jogador jogador = exigirDono(empresa, jogadorId);
        String nomeLimpo = nome == null ? "" : nome.trim();
        if (nomeLimpo.length() < 2) {
            throw new RegraDeNegocioException("A linha precisa de um nome com ao menos 2 caracteres.");
        }
        exigirMixValido(empresaId, null, fatiaMix);

        LinhaProduto linha = new LinhaProduto();
        linha.setEmpresa(empresa);
        linha.setNome(nomeLimpo);
        linha.setPosicionamento(posicionamento == null ? LinhaProduto.Posicionamento.MEDIO : posicionamento);
        linha.setFatiaMix(fatiaMix);
        linha.setTurnoCriacao(estadoJogo.turnoAtual());
        LinhaProduto salva = repositorioLinha.save(linha);

        auditoria.registrar(jogador.getUsuario(), "LINHA_CRIADA", "LinhaProduto", salva.getId(),
                "Linha " + salva.getNome() + " criada em " + empresa.getNome(),
                Map.of("posicionamento", salva.getPosicionamento().name(), "fatiaMix", fatiaMix));
        return salva;
    }

    @Transactional
    public LinhaProduto ajustarLinha(Long linhaId, Long jogadorId,
                                     LinhaProduto.Posicionamento posicionamento, Double fatiaMix) {
        LinhaProduto linha = repositorioLinha.findById(linhaId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("LinhaProduto", linhaId));
        Empresa empresa = linha.getEmpresa();
        Jogador jogador = exigirDono(empresa, jogadorId);
        Map<String, Object> mudancas = new LinkedHashMap<>();

        if (fatiaMix != null) {
            exigirMixValido(empresa.getId(), linhaId, fatiaMix);
            linha.setFatiaMix(fatiaMix);
            mudancas.put("fatiaMix", fatiaMix);
        }
        if (posicionamento != null) {
            linha.setPosicionamento(posicionamento);
            mudancas.put("posicionamento", posicionamento.name());
        }
        repositorioLinha.save(linha);
        auditoria.registrar(jogador.getUsuario(), "LINHA_AJUSTADA", "LinhaProduto", linha.getId(),
                "Linha " + linha.getNome() + " ajustada", mudancas);
        return linha;
    }

    @Transactional
    public void encerrarLinha(Long linhaId, Long jogadorId) {
        LinhaProduto linha = repositorioLinha.findById(linhaId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("LinhaProduto", linhaId));
        Jogador jogador = exigirDono(linha.getEmpresa(), jogadorId);
        linha.setAtiva(false);
        linha.setFatiaMix(0);
        repositorioLinha.save(linha);
        auditoria.registrar(jogador.getUsuario(), "LINHA_ENCERRADA", "LinhaProduto", linha.getId(),
                "Linha " + linha.getNome() + " encerrada", Map.of());
    }

    // ------------------------------------------------------------------
    // Departamentos
    // ------------------------------------------------------------------

    /** Define o orcamento mensal de uma area. Orcamento zero desmonta a area. */
    @Transactional
    public Departamento definirDepartamento(Long empresaId, Long jogadorId, Departamento.Area area,
                                            double orcamentoMensal) {
        Empresa empresa = buscarEmpresa(empresaId);
        Jogador jogador = exigirDono(empresa, jogadorId);
        if (orcamentoMensal < 0) {
            throw new RegraDeNegocioException("Orcamento de departamento nao pode ser negativo.");
        }
        Departamento departamento = repositorioDepartamento.findByEmpresaIdAndArea(empresaId, area)
                .orElseGet(() -> {
                    Departamento novo = new Departamento();
                    novo.setEmpresa(empresa);
                    novo.setArea(area);
                    return novo;
                });
        departamento.setOrcamentoMensal(orcamentoMensal);
        Departamento salvo = repositorioDepartamento.save(departamento);

        auditoria.registrar(jogador.getUsuario(), "DEPARTAMENTO_DEFINIDO", "Departamento", salvo.getId(),
                "Orcamento de " + area.getRotulo() + " em " + empresa.getNome(),
                Map.of("area", area.name(), "orcamentoMensal", orcamentoMensal,
                        "efeitoEstimado", area.efeito(orcamentoMensal, referenciaDePorte(empresa))));
        return salvo;
    }

    // ------------------------------------------------------------------
    // Leitura usada pelo motor de turnos
    // ------------------------------------------------------------------

    /**
     * Monta o perfil operacional de uma unidade no turno.
     *
     * O marketing e o esforco comercial da empresa sao rateados entre as
     * unidades pelo patrimonio: quem tem mais ativo instalado carrega mais da
     * verba e colhe mais do efeito.
     */
    public PerfilOperacional perfil(Unidade unidade, Empresa empresa, double fatorPreco,
                                    double fatorCustoVariavel, double bonusComercial) {
        double totalPatrimonio = Math.max(empresa.getPatrimonio(), 1.0);
        double rateio = Math.min(unidade.getPatrimonio() / totalPatrimonio, 1.0);
        return new PerfilOperacional(empresa.getSetor(), unidade.getPatrimonio(), unidade.getFuncionarios(),
                unidade.getProdutividade(), empresa.getSalarioMedio(),
                empresa.getMarketingMensal() * rateio, empresa.getReputacao(),
                fatorPreco, fatorCustoVariavel, bonusComercial);
    }

    /**
     * Preco medio praticado sobre o preco de referencia do setor.
     * A fatia nao declarada do mix fica no posicionamento medio.
     */
    public double fatorPrecoDoMix(List<LinhaProduto> linhas) {
        return ponderarMix(linhas, true);
    }

    /** Custo de insumo medio sobre o padrao do setor, segundo o mix declarado. */
    public double fatorCustoDoMix(List<LinhaProduto> linhas) {
        return ponderarMix(linhas, false);
    }

    private double ponderarMix(List<LinhaProduto> linhas, boolean preco) {
        if (linhas == null || linhas.isEmpty()) {
            return 1.0;
        }
        double declarado = 0;
        double soma = 0;
        for (LinhaProduto linha : linhas) {
            double fatia = Math.max(linha.getFatiaMix(), 0);
            declarado += fatia;
            soma += fatia * (preco ? linha.getPosicionamento().getFatorPreco()
                    : linha.getPosicionamento().getFatorCusto());
        }
        double restante = Math.max(1.0 - declarado, 0);
        return soma + restante; // o restante entra com fator 1.0 (posicionamento medio)
    }

    /** Orcamento mensal somado de todos os departamentos da empresa. */
    public double custoEstrutura(List<Departamento> departamentos) {
        return departamentos.stream().mapToDouble(Departamento::getOrcamentoMensal).sum();
    }

    /** Efeito de uma area, ja calibrado pelo porte da empresa. */
    public double efeitoDepartamento(List<Departamento> departamentos, Departamento.Area area, Empresa empresa) {
        double referencia = referenciaDePorte(empresa);
        return departamentos.stream()
                .filter(departamento -> departamento.getArea() == area)
                .mapToDouble(departamento -> area.efeito(departamento.getOrcamentoMensal(), referencia))
                .sum();
    }

    /** Escala de orcamento em que o efeito de um departamento chega a metade do teto. */
    public double referenciaDePorte(Empresa empresa) {
        return Math.max(empresa.getPatrimonio() * empresa.getSetor().getGiroAtivoMensal() * FRACAO_PORTE,
                5_000.0);
    }

    /**
     * Recalcula os totais da empresa a partir das unidades ativas.
     *
     * Chamado sempre que capital ou equipe mudam de lugar. A produtividade da
     * empresa e a media ponderada pelo patrimonio das unidades.
     */
    @Transactional
    public Empresa sincronizarAgregados(Empresa empresa) {
        List<Unidade> unidades = repositorioUnidade.findByEmpresaIdAndAtivaTrueOrderByIdAsc(empresa.getId());
        if (unidades.isEmpty()) {
            return repositorioEmpresa.save(empresa);
        }
        double patrimonio = 0;
        int funcionarios = 0;
        double produtividadePonderada = 0;
        for (Unidade unidade : unidades) {
            patrimonio += unidade.getPatrimonio();
            funcionarios += unidade.getFuncionarios();
            produtividadePonderada += unidade.getProdutividade() * Math.max(unidade.getPatrimonio(), 0);
        }
        empresa.setPatrimonio(patrimonio);
        empresa.setFuncionarios(funcionarios);
        empresa.setProdutividade(patrimonio > 0
                ? Math.min(produtividadePonderada / patrimonio, PRODUTIVIDADE_MAXIMA)
                : empresa.getProdutividade());
        return repositorioEmpresa.save(empresa);
    }

    // ------------------------------------------------------------------

    private void exigirMixValido(Long empresaId, Long linhaIgnorada, double fatiaMix) {
        if (fatiaMix < 0 || fatiaMix > 1) {
            throw new RegraDeNegocioException("A fatia do mix deve ficar entre 0 e 1.");
        }
        double outras = repositorioLinha.findByEmpresaIdAndAtivaTrueOrderByIdAsc(empresaId).stream()
                .filter(linha -> linhaIgnorada == null || !linha.getId().equals(linhaIgnorada))
                .mapToDouble(LinhaProduto::getFatiaMix)
                .sum();
        if (outras + fatiaMix > 1.0001) {
            throw new RegraDeNegocioException("O mix passaria de 100 por cento: restam "
                    + String.format("%.0f", (1 - outras) * 100) + " por cento a distribuir.");
        }
    }

    private Empresa exigirMesmaEmpresa(Unidade origem, Unidade destino) {
        if (!origem.getEmpresa().getId().equals(destino.getEmpresa().getId())) {
            throw new RegraDeNegocioException("As unidades pertencem a empresas diferentes.");
        }
        if (origem.getId().equals(destino.getId())) {
            throw new RegraDeNegocioException("Origem e destino sao a mesma unidade.");
        }
        if (!origem.isAtiva() || !destino.isAtiva()) {
            throw new RegraDeNegocioException("As duas unidades precisam estar ativas.");
        }
        return origem.getEmpresa();
    }

    private Empresa buscarEmpresa(Long empresaId) {
        return repositorioEmpresa.findById(empresaId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Empresa", empresaId));
    }

    private Jogador exigirDono(Empresa empresa, Long jogadorId) {
        Jogador jogador = servicoJogador.buscar(jogadorId);
        empresa.exigirControleDe(jogador);
        return jogador;
    }
}
