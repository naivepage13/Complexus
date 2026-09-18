package com.geohistoricalsim.politica;

import com.geohistoricalsim.auditoria.ServicoAuditoria;
import com.geohistoricalsim.comum.RecursoNaoEncontradoException;
import com.geohistoricalsim.comum.RegraDeNegocioException;
import com.geohistoricalsim.core.ServicoEstadoJogo;
import com.geohistoricalsim.economia.Setor;
import com.geohistoricalsim.jogador.Jogador;
import com.geohistoricalsim.jogador.ServicoJogador;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Regras do jogo politico: mandatos, tramitacao de projetos de lei, votacao,
 * sancao e efeitos das leis sobre a economia.
 *
 * <h2>Divisao de poderes (simplificada)</h2>
 * <ul>
 *   <li>Legislativo (senadores, deputados, vereadores) propoe e vota;</li>
 *   <li>Executivo (presidente, governador, prefeito) sanciona ou veta;</li>
 *   <li>Veto cai com dois tercos dos votos do legislativo da mesma esfera;</li>
 *   <li>Cadeiras vagas sao ocupadas por NPCs, que votam por heuristica.</li>
 * </ul>
 *
 * <h2>Prazos</h2>
 * Um projeto colocado em votacao e apurado no turno seguinte. Um projeto
 * aprovado e sancionado tacitamente se o executivo nao se manifestar em dois
 * turnos, espelhando o prazo constitucional de sancao.
 */
@Service
public class ServicoPolitica {

    private static final int TURNOS_ATE_APURACAO = 1;
    private static final int TURNOS_PARA_SANCAO = 2;
    private static final String[] PARTIDOS = {"PRD", "MDU", "PSL", "PTN", "PVC", "PLB"};
    private static final String[] NOMES_NPC = {
            "Ana Prado", "Carlos Bastos", "Helena Viana", "Joao Ribeiro", "Marta Lins",
            "Otavio Nunes", "Paula Serra", "Rui Camargo", "Sonia Teles", "Vitor Braga",
            "Bruna Aguiar", "Diego Matos", "Elisa Fontes", "Fabio Rangel", "Gisele Moura"
    };

    private final RepositorioMandato repositorioMandato;
    private final RepositorioProjetoDeLei repositorioProjeto;
    private final RepositorioVotoProjeto repositorioVoto;
    private final RepositorioPais repositorioPais;
    private final RepositorioEstado repositorioEstado;
    private final RepositorioMunicipio repositorioMunicipio;
    private final ServicoJogador servicoJogador;
    private final ServicoAuditoria auditoria;
    private final ServicoEstadoJogo estadoJogo;

    public ServicoPolitica(RepositorioMandato repositorioMandato,
                           RepositorioProjetoDeLei repositorioProjeto,
                           RepositorioVotoProjeto repositorioVoto,
                           RepositorioPais repositorioPais,
                           RepositorioEstado repositorioEstado,
                           RepositorioMunicipio repositorioMunicipio,
                           ServicoJogador servicoJogador,
                           ServicoAuditoria auditoria,
                           ServicoEstadoJogo estadoJogo) {
        this.repositorioMandato = repositorioMandato;
        this.repositorioProjeto = repositorioProjeto;
        this.repositorioVoto = repositorioVoto;
        this.repositorioPais = repositorioPais;
        this.repositorioEstado = repositorioEstado;
        this.repositorioMunicipio = repositorioMunicipio;
        this.servicoJogador = servicoJogador;
        this.auditoria = auditoria;
        this.estadoJogo = estadoJogo;
    }

    // ------------------------------------------------------------------
    // Mandatos
    // ------------------------------------------------------------------

    // ------------------------------------------------------------------
    // Territorios
    // ------------------------------------------------------------------

    @Transactional(readOnly = true)
    public List<Pais> listarPaises() {
        return repositorioPais.findAll();
    }

    @Transactional(readOnly = true)
    public List<Estado> listarEstados() {
        return repositorioEstado.findAll();
    }

    @Transactional(readOnly = true)
    public List<Municipio> listarMunicipios() {
        return repositorioMunicipio.findAll();
    }

    @Transactional(readOnly = true)
    public List<Mandato> mandatosDoTerritorio(Esfera esfera, Long territorioId) {
        return repositorioMandato.findByEsferaAndTerritorioIdAndAtivoTrue(esfera, territorioId);
    }

    @Transactional(readOnly = true)
    public List<Mandato> mandatosDoJogador(Long jogadorId) {
        return repositorioMandato.findByJogadorIdAndAtivoTrue(jogadorId);
    }

    @Transactional(readOnly = true)
    public Mandato buscarMandato(Long id) {
        return repositorioMandato.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Mandato", id));
    }

    /** Cadeiras livres de um cargo em um territorio. */
    @Transactional(readOnly = true)
    public int vagasDisponiveis(CargoPolitico cargo, Long territorioId) {
        int ocupadas = repositorioMandato.findByCargoAndTerritorioIdAndAtivoTrue(cargo, territorioId).size();
        return Math.max(VagasCargo.vagas(cargo) - ocupadas, 0);
    }

    /**
     * Um jogador assume um cargo.
     *
     * Eleicao simplificada da versao 0.2: se ha cadeira vaga, a posse e direta;
     * se todas estao ocupadas, o jogador desafia o NPC de menor aprovacao e
     * vence quando essa aprovacao esta abaixo de 45.
     */
    @Transactional
    public Mandato assumirCargo(Long jogadorId, CargoPolitico cargo, Long territorioId, String partido) {
        Jogador jogador = servicoJogador.buscar(jogadorId);
        validarTerritorio(cargo.getEsfera(), territorioId);

        boolean jaOcupa = repositorioMandato.findByJogadorIdAndAtivoTrue(jogadorId).stream()
                .anyMatch(m -> m.getCargo() == cargo && m.getTerritorioId().equals(territorioId));
        if (jaOcupa) {
            throw new RegraDeNegocioException("Voce ja ocupa esse cargo neste territorio.");
        }
        if (!cargo.isEletivo()) {
            throw new RegraDeNegocioException("O cargo " + cargo.getRotulo()
                    + " e de nomeacao e nao pode ser assumido diretamente.");
        }

        int turno = estadoJogo.turnoAtual();
        if (vagasDisponiveis(cargo, territorioId) == 0) {
            List<Mandato> ocupantes = repositorioMandato.findByCargoAndTerritorioIdAndAtivoTrue(cargo, territorioId);
            Mandato maisFraco = ocupantes.stream()
                    .filter(Mandato::isNpc)
                    .min(Comparator.comparingDouble(Mandato::getAprovacao))
                    .orElseThrow(() -> new RegraDeNegocioException(
                            "Nao ha cadeira disponivel para " + cargo.getRotulo() + " neste territorio."));
            if (maisFraco.getAprovacao() >= 45.0) {
                throw new RegraDeNegocioException("O ocupante atual tem aprovacao de "
                        + String.format("%.1f", maisFraco.getAprovacao())
                        + " e nao pode ser desafiado agora.");
            }
            maisFraco.setAtivo(false);
            maisFraco.setTurnoFim(turno);
            repositorioMandato.save(maisFraco);
            auditoria.registrarSistema("MANDATO_ENCERRADO", "Mandato", maisFraco.getId(),
                    "Cadeira perdida para candidato com maior apoio",
                    Map.of("cargo", cargo.name(), "aprovacao", maisFraco.getAprovacao()));
        }

        Mandato mandato = new Mandato();
        mandato.setJogador(jogador);
        mandato.setTitular(jogador.getNome());
        mandato.setCargo(cargo);
        mandato.setEsfera(cargo.getEsfera());
        mandato.setTerritorioId(territorioId);
        mandato.setPartido(partido == null || partido.isBlank() ? "IND" : partido.trim().toUpperCase());
        mandato.setTurnoInicio(turno);
        mandato.setTurnoFim(turno + cargo.getDuracaoMandatoTurnos());
        mandato.setNpc(false);
        mandato.setAprovacao(50.0);
        Mandato salvo = repositorioMandato.save(mandato);

        Map<String, Object> detalhes = new LinkedHashMap<>();
        detalhes.put("cargo", cargo.name());
        detalhes.put("esfera", cargo.getEsfera().name());
        detalhes.put("territorioId", territorioId);
        detalhes.put("turnoFim", mandato.getTurnoFim());
        auditoria.registrar(jogador.getUsuario(), "MANDATO_ASSUMIDO", "Mandato", salvo.getId(),
                jogador.getNome() + " assumiu o cargo de " + cargo.getRotulo(), detalhes);
        return salvo;
    }

    // ------------------------------------------------------------------
    // Projetos de lei
    // ------------------------------------------------------------------

    @Transactional(readOnly = true)
    public List<ProjetoDeLei> projetosRecentes() {
        return repositorioProjeto.findTop50ByOrderByIdDesc();
    }

    @Transactional(readOnly = true)
    public List<ProjetoDeLei> projetosDoTerritorio(Esfera esfera, Long territorioId) {
        return repositorioProjeto.findByEsferaAndTerritorioIdOrderByIdDesc(esfera, territorioId);
    }

    @Transactional(readOnly = true)
    public ProjetoDeLei buscarProjeto(Long id) {
        return repositorioProjeto.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("ProjetoDeLei", id));
    }

    @Transactional
    public ProjetoDeLei propor(Long jogadorId, Long mandatoId, String titulo, String ementa,
                               TipoProjeto tipo, Setor setorAlvo, double parametro) {
        Mandato mandato = buscarMandato(mandatoId);
        exigirTitular(mandato, jogadorId);
        if (!mandato.getCargo().isLegislativo() && !mandato.getCargo().chefiaExecutivo()) {
            throw new RegraDeNegocioException("Apenas parlamentares e chefes do executivo propoem projetos.");
        }
        if (!tipo.permite(mandato.getEsfera())) {
            throw new RegraDeNegocioException("O instrumento " + tipo.name()
                    + " nao tramita na esfera " + mandato.getEsfera().getRotulo() + ".");
        }
        if (tipo.isExigeSetor() && setorAlvo == null) {
            throw new RegraDeNegocioException("Esse tipo de projeto exige um setor alvo.");
        }
        if (parametro < tipo.getParametroMinimo() || parametro > tipo.getParametroMaximo()) {
            throw new RegraDeNegocioException("Parametro fora do intervalo permitido ("
                    + tipo.getParametroMinimo() + " a " + tipo.getParametroMaximo() + ").");
        }

        int turno = estadoJogo.turnoAtual();
        ProjetoDeLei projeto = new ProjetoDeLei();
        projeto.setTitulo(titulo == null || titulo.isBlank() ? tipo.getDescricao() : titulo.trim());
        projeto.setEmenta(ementa);
        projeto.setAutor(mandato);
        projeto.setEsfera(mandato.getEsfera());
        projeto.setTerritorioId(mandato.getTerritorioId());
        projeto.setTipo(tipo);
        projeto.setSetorAlvo(setorAlvo);
        projeto.setParametro(parametro);
        projeto.setStatus(StatusProjeto.RASCUNHO);
        projeto.setTurnoCriacao(turno);
        ProjetoDeLei salvo = repositorioProjeto.save(projeto);

        Map<String, Object> detalhes = new LinkedHashMap<>();
        detalhes.put("tipo", tipo.name());
        detalhes.put("parametro", parametro);
        detalhes.put("esfera", projeto.getEsfera().name());
        detalhes.put("setorAlvo", setorAlvo == null ? null : setorAlvo.name());
        auditoria.registrar(atorDe(mandato), "PROJETO_PROPOSTO", "ProjetoDeLei", salvo.getId(),
                "Projeto proposto: " + salvo.getTitulo(), detalhes);
        return salvo;
    }

    /** Coloca o projeto em pauta. A apuracao ocorre no processamento do proximo turno. */
    @Transactional
    public ProjetoDeLei pautar(Long jogadorId, Long projetoId) {
        ProjetoDeLei projeto = buscarProjeto(projetoId);
        exigirTitular(projeto.getAutor(), jogadorId);
        if (projeto.getStatus() != StatusProjeto.RASCUNHO) {
            throw new RegraDeNegocioException("O projeto ja saiu da fase de rascunho.");
        }
        int turno = estadoJogo.turnoAtual();
        projeto.setStatus(StatusProjeto.EM_VOTACAO);
        projeto.setTurnoVotacao(turno + TURNOS_ATE_APURACAO);
        repositorioProjeto.save(projeto);

        auditoria.registrar(atorDe(projeto.getAutor()), "PROJETO_PAUTADO", "ProjetoDeLei", projeto.getId(),
                "Projeto em votacao: " + projeto.getTitulo(),
                Map.of("turnoApuracao", projeto.getTurnoVotacao()));
        return projeto;
    }

    @Transactional
    public VotoProjeto votar(Long jogadorId, Long mandatoId, Long projetoId, VotoProjeto.Opcao opcao) {
        Mandato mandato = buscarMandato(mandatoId);
        exigirTitular(mandato, jogadorId);
        ProjetoDeLei projeto = buscarProjeto(projetoId);

        if (projeto.getStatus() != StatusProjeto.EM_VOTACAO) {
            throw new RegraDeNegocioException("Este projeto nao esta em votacao.");
        }
        if (!mandato.getCargo().isLegislativo()) {
            throw new RegraDeNegocioException("Somente parlamentares votam projetos de lei.");
        }
        if (mandato.getEsfera() != projeto.getEsfera()
                || !mandato.getTerritorioId().equals(projeto.getTerritorioId())) {
            throw new RegraDeNegocioException("O mandato nao pertence a casa legislativa deste projeto.");
        }
        if (repositorioVoto.existsByProjetoIdAndMandatoId(projetoId, mandatoId)) {
            throw new RegraDeNegocioException("Este mandato ja votou neste projeto.");
        }

        VotoProjeto voto = registrarVoto(projeto, mandato, opcao);
        auditoria.registrar(atorDe(mandato), "PROJETO_VOTO", "ProjetoDeLei", projeto.getId(),
                "Voto " + opcao.name() + " em " + projeto.getTitulo(),
                Map.of("mandatoId", mandatoId, "cargo", mandato.getCargo().name()));
        return voto;
    }

    /** Sancao ou veto pelo chefe do executivo da esfera do projeto. */
    @Transactional
    public ProjetoDeLei sancionar(Long jogadorId, Long projetoId, boolean sancionar, String justificativa) {
        ProjetoDeLei projeto = buscarProjeto(projetoId);
        if (projeto.getStatus() != StatusProjeto.APROVADO) {
            throw new RegraDeNegocioException("So projetos aprovados pelo legislativo vao a sancao.");
        }
        Mandato chefe = chefeDoExecutivo(projeto.getEsfera(), projeto.getTerritorioId())
                .orElseThrow(() -> new RegraDeNegocioException("Nao ha chefe do executivo em exercicio."));
        exigirTitular(chefe, jogadorId);

        if (sancionar) {
            aplicarSancao(projeto, atorDe(chefe));
        } else {
            projeto.setStatus(StatusProjeto.VETADO);
            projeto.setJustificativaVeto(justificativa);
            repositorioProjeto.save(projeto);
            auditoria.registrar(atorDe(chefe), "PROJETO_VETADO", "ProjetoDeLei", projeto.getId(),
                    "Veto ao projeto " + projeto.getTitulo(),
                    Map.of("justificativa", justificativa == null ? "" : justificativa));
        }
        return projeto;
    }

    /** Derrubada de veto: exige dois tercos dos votos SIM entre os parlamentares da casa. */
    @Transactional
    public ProjetoDeLei derrubarVeto(Long jogadorId, Long mandatoId, Long projetoId) {
        Mandato mandato = buscarMandato(mandatoId);
        exigirTitular(mandato, jogadorId);
        ProjetoDeLei projeto = buscarProjeto(projetoId);
        if (projeto.getStatus() != StatusProjeto.VETADO) {
            throw new RegraDeNegocioException("Este projeto nao esta vetado.");
        }
        if (!mandato.getCargo().isLegislativo()) {
            throw new RegraDeNegocioException("Somente parlamentares derrubam vetos.");
        }
        List<Mandato> casa = casaLegislativa(projeto.getEsfera(), projeto.getTerritorioId());
        int necessarios = (int) Math.ceil(casa.size() * 2.0 / 3.0);
        if (projeto.getVotosSim() < necessarios) {
            throw new RegraDeNegocioException("Derrubada de veto exige " + necessarios
                    + " votos favoraveis; o projeto teve " + projeto.getVotosSim() + ".");
        }
        aplicarSancao(projeto, atorDe(mandato));
        auditoria.registrar(atorDe(mandato), "VETO_DERRUBADO", "ProjetoDeLei", projeto.getId(),
                "Veto derrubado no projeto " + projeto.getTitulo(),
                Map.of("votosSim", projeto.getVotosSim(), "necessarios", necessarios));
        return projeto;
    }

    // ------------------------------------------------------------------
    // Processamento de turno
    // ------------------------------------------------------------------

    /**
     * Rotina politica do turno: apura votacoes, resolve sancoes pendentes,
     * encerra mandatos vencidos e recalcula a aprovacao dos mandatarios.
     *
     * @return resumo do que aconteceu, usado no relatorio do turno
     */
    @Transactional
    public Map<String, Object> processarTurno(int turno) {
        Map<String, Object> resumo = new LinkedHashMap<>();
        resumo.put("projetosApurados", apurarVotacoes(turno));
        resumo.put("sancoesAutomaticas", resolverSancoesPendentes(turno));
        resumo.put("mandatosRenovados", renovarMandatos(turno));
        return resumo;
    }

    private int apurarVotacoes(int turno) {
        List<ProjetoDeLei> emVotacao = repositorioProjeto
                .findByStatusAndTurnoVotacaoLessThanEqual(StatusProjeto.EM_VOTACAO, turno);
        for (ProjetoDeLei projeto : emVotacao) {
            List<Mandato> casa = casaLegislativa(projeto.getEsfera(), projeto.getTerritorioId());
            for (Mandato parlamentar : casa) {
                if (parlamentar.isNpc()
                        && !repositorioVoto.existsByProjetoIdAndMandatoId(projeto.getId(), parlamentar.getId())) {
                    registrarVoto(projeto, parlamentar, votoDoNpc(projeto, parlamentar));
                }
            }
            boolean aprovado = projeto.getVotosSim() > projeto.getVotosNao();
            projeto.setStatus(aprovado ? StatusProjeto.APROVADO : StatusProjeto.REJEITADO);
            repositorioProjeto.save(projeto);

            Map<String, Object> detalhes = new LinkedHashMap<>();
            detalhes.put("sim", projeto.getVotosSim());
            detalhes.put("nao", projeto.getVotosNao());
            detalhes.put("abstencao", projeto.getVotosAbstencao());
            auditoria.registrarSistema(aprovado ? "PROJETO_APROVADO" : "PROJETO_REJEITADO",
                    "ProjetoDeLei", projeto.getId(),
                    "Apuracao do projeto " + projeto.getTitulo(), detalhes);
        }
        return emVotacao.size();
    }

    private int resolverSancoesPendentes(int turno) {
        int resolvidos = 0;
        for (ProjetoDeLei projeto : repositorioProjeto.findByStatusOrderByIdDesc(StatusProjeto.APROVADO)) {
            Optional<Mandato> chefe = chefeDoExecutivo(projeto.getEsfera(), projeto.getTerritorioId());
            boolean chefeNpc = chefe.map(Mandato::isNpc).orElse(true);
            boolean prazoEsgotado = projeto.getTurnoVotacao() != null
                    && turno - projeto.getTurnoVotacao() >= TURNOS_PARA_SANCAO;

            if (chefeNpc) {
                if (sancaoDoNpc(projeto)) {
                    aplicarSancao(projeto, "NPC:EXECUTIVO");
                } else {
                    projeto.setStatus(StatusProjeto.VETADO);
                    projeto.setJustificativaVeto("Veto do executivo por impacto fiscal.");
                    repositorioProjeto.save(projeto);
                    auditoria.registrarSistema("PROJETO_VETADO", "ProjetoDeLei", projeto.getId(),
                            "Veto automatico do executivo", Map.of("tipo", projeto.getTipo().name()));
                }
                resolvidos++;
            } else if (prazoEsgotado) {
                // Sancao tacita: o executivo perdeu o prazo de manifestacao.
                aplicarSancao(projeto, "SANCAO_TACITA");
                resolvidos++;
            }
        }
        return resolvidos;
    }

    private int renovarMandatos(int turno) {
        List<Mandato> vencidos = repositorioMandato.findByAtivoTrueAndTurnoFimLessThanEqual(turno);
        for (Mandato mandato : vencidos) {
            mandato.setAtivo(false);
            repositorioMandato.save(mandato);
            auditoria.registrarSistema("MANDATO_ENCERRADO", "Mandato", mandato.getId(),
                    "Mandato encerrado ao fim do prazo",
                    Map.of("cargo", mandato.getCargo().name(), "titular", mandato.getTitular()));
            // A cadeira nao fica vazia: um NPC assume ate que um jogador dispute.
            criarMandatoNpc(mandato.getCargo(), mandato.getTerritorioId(), turno);
        }
        return vencidos.size();
    }

    /** Cria uma cadeira ocupada por NPC. Usado no seed e na renovacao de mandatos. */
    @Transactional
    public Mandato criarMandatoNpc(CargoPolitico cargo, Long territorioId, int turno) {
        Random random = new Random(cargo.name().hashCode() * 31L + territorioId * 17L + turno);
        Mandato mandato = new Mandato();
        mandato.setTitular(NOMES_NPC[random.nextInt(NOMES_NPC.length)]);
        mandato.setCargo(cargo);
        mandato.setEsfera(cargo.getEsfera());
        mandato.setTerritorioId(territorioId);
        mandato.setPartido(PARTIDOS[random.nextInt(PARTIDOS.length)]);
        mandato.setTurnoInicio(turno);
        mandato.setTurnoFim(turno + cargo.getDuracaoMandatoTurnos());
        mandato.setNpc(true);
        mandato.setAprovacao(40 + random.nextInt(35));
        return repositorioMandato.save(mandato);
    }

    // ------------------------------------------------------------------
    // Efeitos vigentes sobre a economia
    // ------------------------------------------------------------------

    /**
     * Subsidio vigente para o setor, como fracao da receita. Soma a lei federal
     * mais recente com a lei estadual mais recente do estado informado.
     */
    @Transactional(readOnly = true)
    public double subsidioVigente(Setor setor, Long estadoId) {
        return parametroVigente(TipoProjeto.SUBSIDIO_SETORIAL, setor, Esfera.FEDERAL, null)
                + parametroVigente(TipoProjeto.SUBSIDIO_SETORIAL, setor, Esfera.ESTADUAL, estadoId);
    }

    /** Custo regulatorio vigente para o setor, como acrescimo sobre o custo variavel. */
    @Transactional(readOnly = true)
    public double regulacaoVigente(Setor setor, Long estadoId) {
        return parametroVigente(TipoProjeto.REGULACAO_SETORIAL, setor, Esfera.FEDERAL, null)
                + parametroVigente(TipoProjeto.REGULACAO_SETORIAL, setor, Esfera.ESTADUAL, estadoId);
    }

    /** Leis sancionadas e ainda vigentes. */
    @Transactional(readOnly = true)
    public List<ProjetoDeLei> leisEmVigor() {
        return repositorioProjeto.findByStatusOrderByIdDesc(StatusProjeto.SANCIONADO);
    }

    private double parametroVigente(TipoProjeto tipo, Setor setor, Esfera esfera, Long territorioId) {
        return leisEmVigor().stream()
                .filter(lei -> lei.getTipo() == tipo)
                .filter(lei -> lei.getSetorAlvo() == setor)
                .filter(lei -> lei.getEsfera() == esfera)
                .filter(lei -> territorioId == null || territorioId.equals(lei.getTerritorioId()))
                .max(Comparator.comparing(ProjetoDeLei::getId))
                .map(ProjetoDeLei::getParametro)
                .orElse(0.0);
    }

    // ------------------------------------------------------------------
    // Internos
    // ------------------------------------------------------------------

    private void aplicarSancao(ProjetoDeLei projeto, String ator) {
        String efeito = aplicarEfeito(projeto);
        projeto.setStatus(StatusProjeto.SANCIONADO);
        projeto.setTurnoVigencia(estadoJogo.turnoAtual());
        projeto.setEfeitoAplicado(efeito);
        repositorioProjeto.save(projeto);

        Map<String, Object> detalhes = new LinkedHashMap<>();
        detalhes.put("tipo", projeto.getTipo().name());
        detalhes.put("parametro", projeto.getParametro());
        detalhes.put("efeito", efeito);
        auditoria.registrar(ator, "LEI_SANCIONADA", "ProjetoDeLei", projeto.getId(),
                "Lei sancionada: " + projeto.getTitulo(), detalhes);
    }

    /** Aplica o efeito da lei no territorio e devolve a descricao do que mudou. */
    private String aplicarEfeito(ProjetoDeLei projeto) {
        Long territorioId = projeto.getTerritorioId();
        switch (projeto.getTipo()) {
            case IMPOSTO_EMPRESARIAL -> {
                Pais pais = exigirPais(territorioId);
                double anterior = pais.getAliquotaImpostoEmpresarial();
                pais.setAliquotaImpostoEmpresarial(projeto.getParametro());
                repositorioPais.save(pais);
                return "Aliquota federal sobre lucro: " + formatar(anterior) + " para " + formatar(projeto.getParametro());
            }
            case IMPOSTO_ESTADUAL -> {
                Estado estado = exigirEstado(territorioId);
                double anterior = estado.getAliquotaEstadual();
                estado.setAliquotaEstadual(projeto.getParametro());
                repositorioEstado.save(estado);
                return "Aliquota estadual: " + formatar(anterior) + " para " + formatar(projeto.getParametro());
            }
            case IMPOSTO_MUNICIPAL -> {
                Municipio municipio = exigirMunicipio(territorioId);
                double anterior = municipio.getAliquotaMunicipal();
                municipio.setAliquotaMunicipal(projeto.getParametro());
                repositorioMunicipio.save(municipio);
                return "Aliquota municipal: " + formatar(anterior) + " para " + formatar(projeto.getParametro());
            }
            case INVESTIMENTO_INFRAESTRUTURA -> {
                Estado estado = exigirEstado(territorioId);
                estado.setInvestimentoInfraestrutura(projeto.getParametro());
                repositorioEstado.save(estado);
                return "Investimento mensal em infraestrutura fixado em R$ " + String.format("%.2f", projeto.getParametro());
            }
            case PROGRAMA_SOCIAL -> {
                Pais pais = exigirPais(projeto.getEsfera() == Esfera.FEDERAL ? territorioId : paisPadraoId());
                pais.setGastoSocialMensal(projeto.getParametro());
                repositorioPais.save(pais);
                return "Gasto social mensal fixado em R$ " + String.format("%.2f", projeto.getParametro());
            }
            case ZONEAMENTO_URBANO -> {
                Municipio municipio = exigirMunicipio(territorioId);
                double anterior = municipio.getZoneamento();
                municipio.setZoneamento(projeto.getParametro());
                // Zoneamento mais permissivo barateia o terreno e aquece a construcao.
                double variacao = (projeto.getParametro() - anterior) / 100.0;
                municipio.setCustoTerrenoM2(Math.max(municipio.getCustoTerrenoM2() * (1 - variacao * 0.3), 100));
                municipio.setDemandaImobiliaria(Math.max(municipio.getDemandaImobiliaria() * (1 + variacao * 0.2), 10));
                repositorioMunicipio.save(municipio);
                return "Zoneamento: " + formatar(anterior) + " para " + formatar(projeto.getParametro());
            }
            case SUBSIDIO_SETORIAL -> {
                return "Subsidio de " + formatar(projeto.getParametro()) + " da receita do setor "
                        + projeto.getSetorAlvo().getRotulo();
            }
            case REGULACAO_SETORIAL -> {
                return "Custo regulatorio de " + formatar(projeto.getParametro()) + " sobre o setor "
                        + projeto.getSetorAlvo().getRotulo();
            }
            default -> {
                return "Sem efeito direto";
            }
        }
    }

    private VotoProjeto registrarVoto(ProjetoDeLei projeto, Mandato mandato, VotoProjeto.Opcao opcao) {
        VotoProjeto voto = new VotoProjeto();
        voto.setProjeto(projeto);
        voto.setMandato(mandato);
        voto.setOpcao(opcao);
        voto.setTurno(estadoJogo.turnoAtual());
        repositorioVoto.save(voto);

        switch (opcao) {
            case SIM -> projeto.setVotosSim(projeto.getVotosSim() + 1);
            case NAO -> projeto.setVotosNao(projeto.getVotosNao() + 1);
            case ABSTENCAO -> projeto.setVotosAbstencao(projeto.getVotosAbstencao() + 1);
        }
        repositorioProjeto.save(projeto);
        return voto;
    }

    /**
     * Heuristica de voto do NPC: parlamentar tende a apoiar gasto e subsidio,
     * resistir a aumento de imposto e a regulacao pesada. A aprovacao do
     * mandatario desloca a chance para o lado governista.
     */
    private VotoProjeto.Opcao votoDoNpc(ProjetoDeLei projeto, Mandato mandato) {
        double inclinacao = switch (projeto.getTipo()) {
            case IMPOSTO_EMPRESARIAL, IMPOSTO_ESTADUAL, IMPOSTO_MUNICIPAL ->
                    0.65 - projeto.getParametro() * 2.2;
            case SUBSIDIO_SETORIAL -> 0.60 + projeto.getParametro();
            case REGULACAO_SETORIAL -> 0.45 - projeto.getParametro();
            case INVESTIMENTO_INFRAESTRUTURA, PROGRAMA_SOCIAL -> 0.70;
            case ZONEAMENTO_URBANO -> 0.50;
        };
        inclinacao += (mandato.getAprovacao() - 50.0) / 250.0;

        Random random = new Random(projeto.getId() * 1_000_003L + mandato.getId());
        double sorteio = random.nextDouble();
        if (sorteio < 0.08) {
            return VotoProjeto.Opcao.ABSTENCAO;
        }
        return sorteio < Math.clamp(inclinacao, 0.05, 0.95) ? VotoProjeto.Opcao.SIM : VotoProjeto.Opcao.NAO;
    }

    /** Executivo NPC veta projetos que derrubam receita publica de forma agressiva. */
    private boolean sancaoDoNpc(ProjetoDeLei projeto) {
        return switch (projeto.getTipo()) {
            case IMPOSTO_EMPRESARIAL -> projeto.getParametro() >= 0.08;
            case IMPOSTO_ESTADUAL -> projeto.getParametro() >= 0.06;
            case IMPOSTO_MUNICIPAL -> projeto.getParametro() >= 0.02;
            case SUBSIDIO_SETORIAL -> projeto.getParametro() <= 0.12;
            default -> true;
        };
    }

    private List<Mandato> casaLegislativa(Esfera esfera, Long territorioId) {
        List<Mandato> casa = new ArrayList<>();
        for (Mandato mandato : repositorioMandato.findByEsferaAndTerritorioIdAndAtivoTrue(esfera, territorioId)) {
            if (mandato.getCargo().isLegislativo()) {
                casa.add(mandato);
            }
        }
        return casa;
    }

    @Transactional(readOnly = true)
    public Optional<Mandato> chefeDoExecutivo(Esfera esfera, Long territorioId) {
        CargoPolitico cargo = switch (esfera) {
            case FEDERAL -> CargoPolitico.PRESIDENTE;
            case ESTADUAL -> CargoPolitico.GOVERNADOR;
            case MUNICIPAL -> CargoPolitico.PREFEITO;
        };
        return repositorioMandato.findFirstByCargoAndTerritorioIdAndAtivoTrue(cargo, territorioId);
    }

    private void exigirTitular(Mandato mandato, Long jogadorId) {
        if (mandato.getJogador() == null || !mandato.getJogador().getId().equals(jogadorId)) {
            throw new RegraDeNegocioException("Este mandato nao pertence ao jogador informado.");
        }
        if (!mandato.isAtivo()) {
            throw new RegraDeNegocioException("Mandato encerrado.");
        }
    }

    private void validarTerritorio(Esfera esfera, Long territorioId) {
        switch (esfera) {
            case FEDERAL -> exigirPais(territorioId);
            case ESTADUAL -> exigirEstado(territorioId);
            case MUNICIPAL -> exigirMunicipio(territorioId);
        }
    }

    private Pais exigirPais(Long id) {
        return repositorioPais.findById(id).orElseThrow(() -> new RecursoNaoEncontradoException("Pais", id));
    }

    private Estado exigirEstado(Long id) {
        return repositorioEstado.findById(id).orElseThrow(() -> new RecursoNaoEncontradoException("Estado", id));
    }

    private Municipio exigirMunicipio(Long id) {
        return repositorioMunicipio.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Municipio", id));
    }

    private Long paisPadraoId() {
        return repositorioPais.findAll().stream().findFirst()
                .map(Pais::getId)
                .orElseThrow(() -> new RegraDeNegocioException("Nenhum pais cadastrado."));
    }

    private String atorDe(Mandato mandato) {
        if (mandato.getJogador() != null) {
            return mandato.getJogador().getUsuario();
        }
        return "NPC:" + mandato.getTitular();
    }

    private String formatar(double valor) {
        return String.format("%.4f", valor);
    }
}
