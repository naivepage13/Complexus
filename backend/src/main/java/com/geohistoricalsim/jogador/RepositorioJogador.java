package com.geohistoricalsim.jogador;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RepositorioJogador extends JpaRepository<Jogador, Long> {
    Optional<Jogador> findByUsuario(String usuario);
    boolean existsByUsuario(String usuario);
}
