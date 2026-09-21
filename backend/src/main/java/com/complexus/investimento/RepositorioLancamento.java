package com.complexus.investimento;

import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface RepositorioLancamento extends JpaRepository<LancamentoFinanceiro, Long> {

    List<LancamentoFinanceiro> findByTurnoOrderByIdAsc(int turno);

    List<LancamentoFinanceiro> findByEmpresaIdOrderByIdDesc(Long empresaId, Pageable pageable);

    List<LancamentoFinanceiro> findByJogadorIdOrderByIdDesc(Long jogadorId, Pageable pageable);

    List<LancamentoFinanceiro> findAllByOrderByIdDesc(Pageable pageable);

    @Query("select coalesce(sum(l.valor), 0) from LancamentoFinanceiro l where l.tipo = ?1 and l.turno = ?2")
    double totalPorTipoNoTurno(TipoLancamento tipo, int turno);
}
