package com.complexus.economia;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RepositorioLinhaProduto extends JpaRepository<LinhaProduto, Long> {

    List<LinhaProduto> findByEmpresaIdAndAtivaTrueOrderByIdAsc(Long empresaId);

    List<LinhaProduto> findByEmpresaIdOrderByIdAsc(Long empresaId);
}
