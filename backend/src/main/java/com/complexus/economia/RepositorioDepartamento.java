package com.complexus.economia;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RepositorioDepartamento extends JpaRepository<Departamento, Long> {

    List<Departamento> findByEmpresaIdOrderByIdAsc(Long empresaId);

    Optional<Departamento> findByEmpresaIdAndArea(Long empresaId, Departamento.Area area);
}
