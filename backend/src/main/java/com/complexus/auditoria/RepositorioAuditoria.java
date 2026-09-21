package com.complexus.auditoria;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RepositorioAuditoria extends JpaRepository<EventoAuditoria, Long> {

    Optional<EventoAuditoria> findFirstByOrderByIdDesc();

    List<EventoAuditoria> findAllByOrderByIdDesc(Pageable pageable);

    List<EventoAuditoria> findByEntidadeAndEntidadeIdOrderByIdDesc(String entidade, Long entidadeId);

    List<EventoAuditoria> findByTurnoOrderByIdAsc(int turno);

    List<EventoAuditoria> findByAtorOrderByIdDesc(String ator, Pageable pageable);
}
