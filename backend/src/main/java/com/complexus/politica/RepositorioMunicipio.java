package com.complexus.politica;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RepositorioMunicipio extends JpaRepository<Municipio, Long> {
    List<Municipio> findByEstadoId(Long estadoId);
}
