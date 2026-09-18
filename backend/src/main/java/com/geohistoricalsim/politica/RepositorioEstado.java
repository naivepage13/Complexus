package com.geohistoricalsim.politica;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RepositorioEstado extends JpaRepository<Estado, Long> {
    List<Estado> findByPaisId(Long paisId);
    Optional<Estado> findBySigla(String sigla);
}
