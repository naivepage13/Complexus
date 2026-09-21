package com.complexus.atualizacoes;

import com.complexus.auditoria.EventoAuditoria;
import com.complexus.auditoria.ServicoAuditoria;
import com.complexus.jogador.Jogador;
import com.complexus.jogador.RepositorioJogador;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Canal de atualizacoes: o que o jogador ve sobre o mundo.
 *
 * A linha de auditoria continua sendo a fonte da verdade, mas ela e material
 * administrativo: guarda hash, ator e detalhes internos. Este servico filtra
 * essa base e devolve apenas fatos publicos, em linguagem de jogo.
 *
 * Regra: um fato so entra no canal se ja seria visivel de qualquer forma
 * (lei sancionada, empresa fundada, abertura de capital, falencia, obra
 * entregue, fechamento de turno). Movimentos privados de gestao e de carteira
 * ficam de fora, exceto para o proprio dono, que pode pedir o seu historico.
 */
@Service
public class ServicoAtualizacoes {

    /** Fatos que qualquer jogador pode ver. */
    private static final Set<String> ACOES_PUBLICAS = Set.of(
            "TURNO_PROCESSADO",
            "LEI_SANCIONADA",
            "PROJETO_APROVADO",
            "PROJETO_REJEITADO",
            "PROJETO_VETADO",
            "VETO_DERRUBADO",
            "MANDATO_ASSUMIDO",
            "MANDATO_ENCERRADO",
            "EMPRESA_FUNDADA",
            "EMPRESA_IPO",
            "EMPRESA_FALENCIA",
            "OBRA_CONCLUIDA",
            "MUNDO_INICIALIZADO");

    /** Fatos privados que so aparecem para o proprio jogador. */
    private static final Set<String> ACOES_PRIVADAS = Set.of(
            "EMPRESA_CAPEX",
            "EMPRESA_CONTRATACAO",
            "EMPRESA_DEMISSAO",
            "EMPRESA_GESTAO_AJUSTADA",
            "INVESTIMENTO_COMPRA",
            "INVESTIMENTO_VENDA",
            "OBRA_INICIADA",
            "OBRA_ATRASADA",
            "PROJETO_PROPOSTO",
            "PROJETO_PAUTADO",
            "PROJETO_VOTO");

    private final ServicoAuditoria auditoria;
    private final RepositorioJogador repositorioJogador;

    public ServicoAtualizacoes(ServicoAuditoria auditoria, RepositorioJogador repositorioJogador) {
        this.auditoria = auditoria;
        this.repositorioJogador = repositorioJogador;
    }

    /**
     * Feed do canal de atualizacoes.
     *
     * @param jogadorId quando informado, acrescenta os fatos privados do proprio jogador
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> feed(Long jogadorId, int limite) {
        String usuario = jogadorId == null
                ? null
                : repositorioJogador.findById(jogadorId).map(Jogador::getUsuario).orElse(null);

        // Busca com folga porque boa parte dos eventos e administrativa e sera descartada.
        int janela = Math.min(Math.max(limite, 1) * 6, 500);

        return auditoria.ultimos(janela).stream()
                .filter(evento -> visivelPara(evento, usuario))
                .limit(Math.min(Math.max(limite, 1), 100))
                .map(evento -> traduzir(evento, usuario))
                .toList();
    }

    private boolean visivelPara(EventoAuditoria evento, String usuario) {
        if (ACOES_PUBLICAS.contains(evento.getAcao())) {
            return true;
        }
        return usuario != null
                && usuario.equals(evento.getAtor())
                && ACOES_PRIVADAS.contains(evento.getAcao());
    }

    /** Converte o evento tecnico em uma linha de noticia, sem hash nem dados internos. */
    private Map<String, Object> traduzir(EventoAuditoria evento, String usuario) {
        Map<String, Object> linha = new LinkedHashMap<>();
        linha.put("id", evento.getId());
        linha.put("turno", evento.getTurno());
        linha.put("momento", evento.getMomento().toString());
        linha.put("categoria", categoriaDe(evento.getAcao()));
        linha.put("titulo", tituloDe(evento.getAcao()));
        linha.put("mensagem", evento.getDescricao());
        linha.put("propria", usuario != null && usuario.equals(evento.getAtor()));
        return linha;
    }

    private String categoriaDe(String acao) {
        if (acao.startsWith("PROJETO_") || acao.startsWith("LEI_")
                || acao.startsWith("MANDATO_") || acao.equals("VETO_DERRUBADO")) {
            return "POLITICA";
        }
        if (acao.startsWith("EMPRESA_") || acao.startsWith("OBRA_") || acao.startsWith("INVESTIMENTO_")) {
            return "ECONOMIA";
        }
        return "SISTEMA";
    }

    private String tituloDe(String acao) {
        return switch (acao) {
            case "TURNO_PROCESSADO" -> "Fechamento de turno";
            case "LEI_SANCIONADA" -> "Nova lei em vigor";
            case "PROJETO_APROVADO" -> "Projeto aprovado";
            case "PROJETO_REJEITADO" -> "Projeto rejeitado";
            case "PROJETO_VETADO" -> "Projeto vetado";
            case "VETO_DERRUBADO" -> "Veto derrubado";
            case "MANDATO_ASSUMIDO" -> "Posse em cargo publico";
            case "MANDATO_ENCERRADO" -> "Fim de mandato";
            case "EMPRESA_FUNDADA" -> "Nova empresa no mercado";
            case "EMPRESA_IPO" -> "Abertura de capital";
            case "EMPRESA_FALENCIA" -> "Empresa encerrada";
            case "OBRA_CONCLUIDA" -> "Obra entregue";
            case "OBRA_INICIADA" -> "Obra iniciada";
            case "OBRA_ATRASADA" -> "Obra atrasada";
            case "EMPRESA_CAPEX" -> "Aporte de capital";
            case "EMPRESA_CONTRATACAO" -> "Contratacao";
            case "EMPRESA_DEMISSAO" -> "Desligamento";
            case "EMPRESA_GESTAO_AJUSTADA" -> "Gestao ajustada";
            case "INVESTIMENTO_COMPRA" -> "Compra de acoes";
            case "INVESTIMENTO_VENDA" -> "Venda de acoes";
            case "PROJETO_PROPOSTO" -> "Projeto protocolado";
            case "PROJETO_PAUTADO" -> "Projeto em pauta";
            case "PROJETO_VOTO" -> "Voto registrado";
            case "MUNDO_INICIALIZADO" -> "Inicio da partida";
            default -> "Atualizacao";
        };
    }
}
