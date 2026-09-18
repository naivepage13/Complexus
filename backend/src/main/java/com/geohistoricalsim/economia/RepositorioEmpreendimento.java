package com.geohistoricalsim.economia;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RepositorioEmpreendimento extends JpaRepository<Empreendimento, Long> {

    List<Empreendimento> findByEmpresaId(Long empresaId);

    List<Empreendimento> findByStatus(Empreendimento.StatusEmpreendimento status);
}
