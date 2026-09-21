package com.complexus.estatistica;

import com.complexus.economia.Setor;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RepositorioEstatisticaSetor extends JpaRepository<EstatisticaSetor, Long> {

    List<EstatisticaSetor> findByTurno(int turno);

    List<EstatisticaSetor> findBySetorOrderByTurnoDesc(Setor setor, Pageable pageable);
}
