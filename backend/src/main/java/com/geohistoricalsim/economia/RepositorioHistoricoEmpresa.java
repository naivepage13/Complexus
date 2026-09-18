package com.geohistoricalsim.economia;

import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RepositorioHistoricoEmpresa extends JpaRepository<HistoricoEmpresa, Long> {

    List<HistoricoEmpresa> findByEmpresaIdOrderByTurnoAsc(Long empresaId);

    List<HistoricoEmpresa> findByEmpresaIdOrderByTurnoDesc(Long empresaId, Pageable pageable);

    List<HistoricoEmpresa> findByTurno(int turno);
}
