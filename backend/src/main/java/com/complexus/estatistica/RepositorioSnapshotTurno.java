package com.complexus.estatistica;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RepositorioSnapshotTurno extends JpaRepository<SnapshotTurno, Long> {

    Optional<SnapshotTurno> findByTurno(int turno);

    List<SnapshotTurno> findAllByOrderByTurnoDesc(Pageable pageable);

    Optional<SnapshotTurno> findFirstByOrderByTurnoDesc();
}
