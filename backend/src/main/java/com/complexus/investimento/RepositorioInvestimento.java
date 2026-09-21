package com.complexus.investimento;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface RepositorioInvestimento extends JpaRepository<Investimento, Long> {

    List<Investimento> findByJogadorIdAndAtivoTrue(Long jogadorId);

    List<Investimento> findByEmpresaIdAndAtivoTrue(Long empresaId);

    Optional<Investimento> findByJogadorIdAndEmpresaId(Long jogadorId, Long empresaId);

    List<Investimento> findByAtivoTrue();

    @Query("select coalesce(sum(i.acoes), 0) from Investimento i where i.empresa.id = ?1 and i.ativo = true")
    long acoesEmPoderDeInvestidores(Long empresaId);

    @Query("select count(distinct i.jogador.id) from Investimento i where i.ativo = true and i.acoes > 0")
    int investidoresAtivos();

    @Query("select coalesce(sum(i.capitalAportado), 0) from Investimento i where i.ativo = true")
    double capitalTotalInvestido();
}
