package com.complexus.politica;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RepositorioPais extends JpaRepository<Pais, Long> {
    Optional<Pais> findBySigla(String sigla);
}
