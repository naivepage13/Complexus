package com.geohistoricalsim.politica;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RepositorioVotoProjeto extends JpaRepository<VotoProjeto, Long> {
    List<VotoProjeto> findByProjetoId(Long projetoId);
    boolean existsByProjetoIdAndMandatoId(Long projetoId, Long mandatoId);
}
