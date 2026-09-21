package com.complexus.economia;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RepositorioUnidade extends JpaRepository<Unidade, Long> {

    List<Unidade> findByEmpresaIdAndAtivaTrueOrderByIdAsc(Long empresaId);

    List<Unidade> findByEmpresaIdOrderByIdAsc(Long empresaId);

    List<Unidade> findByAtivaTrueAndEmpresaAtivaTrue();

    List<Unidade> findByMunicipioIdAndAtivaTrue(Long municipioId);

    Optional<Unidade> findFirstByEmpresaIdAndAtivaTrueOrderBySedeDescIdAsc(Long empresaId);

    long countByEmpresaIdAndAtivaTrue(Long empresaId);

    boolean existsByEmpresaIdAndMunicipioIdAndAtivaTrue(Long empresaId, Long municipioId);
}
