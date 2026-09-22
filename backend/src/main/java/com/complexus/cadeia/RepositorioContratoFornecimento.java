package com.complexus.cadeia;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface RepositorioContratoFornecimento extends JpaRepository<ContratoFornecimento, Long> {

    List<ContratoFornecimento> findByStatus(ContratoFornecimento.Status status);

    List<ContratoFornecimento> findByFornecedorIdAndStatus(Long fornecedorId,
                                                           ContratoFornecimento.Status status);

    List<ContratoFornecimento> findByCompradorIdAndStatus(Long compradorId,
                                                          ContratoFornecimento.Status status);

    @Query("select c from ContratoFornecimento c "
            + "where c.fornecedor.id = ?1 or c.comprador.id = ?1 order by c.id desc")
    List<ContratoFornecimento> daEmpresa(Long empresaId);

    @Query("select coalesce(sum(c.volumeMensal), 0) from ContratoFornecimento c "
            + "where c.fornecedor.id = ?1 and c.status in ?2")
    double volumeFornecido(Long fornecedorId, List<ContratoFornecimento.Status> status);

    @Query("select coalesce(sum(c.volumeMensal), 0) from ContratoFornecimento c "
            + "where c.comprador.id = ?1 and c.status in ?2")
    double volumeComprado(Long compradorId, List<ContratoFornecimento.Status> status);
}
