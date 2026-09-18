package com.geohistoricalsim.politica;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RepositorioMandato extends JpaRepository<Mandato, Long> {

    List<Mandato> findByAtivoTrue();

    List<Mandato> findByEsferaAndTerritorioIdAndAtivoTrue(Esfera esfera, Long territorioId);

    List<Mandato> findByCargoAndTerritorioIdAndAtivoTrue(CargoPolitico cargo, Long territorioId);

    List<Mandato> findByJogadorIdAndAtivoTrue(Long jogadorId);

    Optional<Mandato> findFirstByCargoAndTerritorioIdAndAtivoTrue(CargoPolitico cargo, Long territorioId);

    List<Mandato> findByAtivoTrueAndTurnoFimLessThanEqual(int turno);
}
