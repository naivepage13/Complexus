package com.complexus.financas;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface RepositorioFinanciamento extends JpaRepository<Financiamento, Long> {

    List<Financiamento> findByEmpresaIdOrderByIdDesc(Long empresaId);

    List<Financiamento> findByEmpresaIdAndStatusInOrderByIdAsc(Long empresaId,
                                                               List<Financiamento.Status> status);

    List<Financiamento> findByStatusInAndEmpresaAtivaTrue(List<Financiamento.Status> status);

    @Query("select coalesce(sum(f.saldoDevedor), 0) from Financiamento f "
            + "where f.empresa.id = ?1 and f.status in ?2")
    double saldoTotal(Long empresaId, List<Financiamento.Status> status);

    @Query("select coalesce(sum(f.garantia), 0) from Financiamento f "
            + "where f.empresa.id = ?1 and f.status in ?2")
    double garantiaComprometida(Long empresaId, List<Financiamento.Status> status);
}
