package com.complexus.investimento;

import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Livro-razao do jogo. Toda movimentacao financeira passa por aqui, o que
 * permite reconstruir o caixa de qualquer empresa ou jogador a partir dos
 * lancamentos e cruzar os numeros com a linha de auditoria.
 */
@Service
public class ServicoRazao {

    private final RepositorioLancamento repositorio;

    public ServicoRazao(RepositorioLancamento repositorio) {
        this.repositorio = repositorio;
    }

    @Transactional
    public LancamentoFinanceiro registrar(int turno, TipoLancamento tipo, String origem, String destino,
                                          double valor, Long empresaId, Long jogadorId, String descricao) {
        LancamentoFinanceiro lancamento = new LancamentoFinanceiro();
        lancamento.setTurno(turno);
        lancamento.setTipo(tipo);
        lancamento.setOrigem(origem);
        lancamento.setDestino(destino);
        lancamento.setValor(valor);
        lancamento.setEmpresaId(empresaId);
        lancamento.setJogadorId(jogadorId);
        lancamento.setDescricao(descricao);
        return repositorio.save(lancamento);
    }

    @Transactional(readOnly = true)
    public List<LancamentoFinanceiro> ultimos(int limite) {
        return repositorio.findAllByOrderByIdDesc(PageRequest.of(0, Math.min(Math.max(limite, 1), 500)));
    }

    @Transactional(readOnly = true)
    public List<LancamentoFinanceiro> porEmpresa(Long empresaId, int limite) {
        return repositorio.findByEmpresaIdOrderByIdDesc(empresaId,
                PageRequest.of(0, Math.min(Math.max(limite, 1), 500)));
    }

    @Transactional(readOnly = true)
    public List<LancamentoFinanceiro> porJogador(Long jogadorId, int limite) {
        return repositorio.findByJogadorIdOrderByIdDesc(jogadorId,
                PageRequest.of(0, Math.min(Math.max(limite, 1), 500)));
    }

    @Transactional(readOnly = true)
    public List<LancamentoFinanceiro> porTurno(int turno) {
        return repositorio.findByTurnoOrderByIdAsc(turno);
    }
}
