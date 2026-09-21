package com.complexus.politica;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RepositorioProjetoDeLei extends JpaRepository<ProjetoDeLei, Long> {

    List<ProjetoDeLei> findByStatusOrderByIdDesc(StatusProjeto status);

    List<ProjetoDeLei> findByEsferaAndTerritorioIdOrderByIdDesc(Esfera esfera, Long territorioId);

    List<ProjetoDeLei> findByStatusAndTurnoVotacaoLessThanEqual(StatusProjeto status, int turno);

    List<ProjetoDeLei> findTop50ByOrderByIdDesc();
}
