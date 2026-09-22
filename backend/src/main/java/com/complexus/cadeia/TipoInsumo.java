package com.complexus.cadeia;

import com.complexus.economia.Setor;
import java.util.List;
import java.util.Set;

/**
 * O que uma empresa pode fornecer a outra.
 *
 * Cada tipo liga um setor fornecedor a um ou mais setores compradores. E essa
 * matriz que fecha a cadeia dos tres setores: a construcao entrega material e
 * obra ao imobiliario, o imobiliario aluga ponto ao alimenticio e o alimenticio
 * abastece o proprio setor. Um quarto setor entra aqui com uma linha nova.
 */
public enum TipoInsumo {

    MATERIAL_CONSTRUCAO("Material de construcao",
            "Cimento, estrutura e acabamento entregues na obra",
            Setor.CONSTRUCAO, Set.of(Setor.IMOBILIARIO, Setor.CONSTRUCAO)),

    SERVICO_DE_OBRA("Servico de obra",
            "Execucao de obra contratada por uma incorporadora",
            Setor.CONSTRUCAO, Set.of(Setor.IMOBILIARIO)),

    ESPACO_COMERCIAL("Espaco comercial",
            "Ponto alugado para operar loja ou cozinha",
            Setor.IMOBILIARIO, Set.of(Setor.ALIMENTICIO, Setor.CONSTRUCAO)),

    INSUMO_ALIMENTAR("Insumo alimentar",
            "Materia-prima e produto acabado no atacado",
            Setor.ALIMENTICIO, Set.of(Setor.ALIMENTICIO));

    private final String rotulo;
    private final String descricao;
    private final Setor setorFornecedor;
    private final Set<Setor> setoresCompradores;

    TipoInsumo(String rotulo, String descricao, Setor setorFornecedor, Set<Setor> setoresCompradores) {
        this.rotulo = rotulo;
        this.descricao = descricao;
        this.setorFornecedor = setorFornecedor;
        this.setoresCompradores = setoresCompradores;
    }

    public String getRotulo() { return rotulo; }
    public String getDescricao() { return descricao; }
    public Setor getSetorFornecedor() { return setorFornecedor; }
    public Set<Setor> getSetoresCompradores() { return setoresCompradores; }

    public boolean aceita(Setor fornecedor, Setor comprador) {
        return setorFornecedor == fornecedor && setoresCompradores.contains(comprador);
    }

    /** Tipos que um setor pode fornecer. */
    public static List<TipoInsumo> fornecidosPor(Setor setor) {
        return List.of(values()).stream().filter(tipo -> tipo.setorFornecedor == setor).toList();
    }

    /** Tipos que um setor pode comprar. */
    public static List<TipoInsumo> compradosPor(Setor setor) {
        return List.of(values()).stream().filter(tipo -> tipo.setoresCompradores.contains(setor)).toList();
    }
}
