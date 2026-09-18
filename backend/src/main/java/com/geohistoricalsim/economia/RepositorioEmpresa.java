package com.geohistoricalsim.economia;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface RepositorioEmpresa extends JpaRepository<Empresa, Long> {

    List<Empresa> findByAtivaTrue();

    List<Empresa> findBySetorAndAtivaTrue(Setor setor);

    List<Empresa> findByDonoIdAndAtivaTrue(Long donoId);

    List<Empresa> findByCapitalAbertoTrueAndAtivaTrueOrderByValuationDesc();

    List<Empresa> findByMunicipioIdAndAtivaTrue(Long municipioId);

    boolean existsByNomeIgnoreCase(String nome);

    @Query("select coalesce(sum(e.receitaMensal), 0) from Empresa e where e.setor = ?1 and e.ativa = true")
    double receitaTotalDoSetor(Setor setor);
}
