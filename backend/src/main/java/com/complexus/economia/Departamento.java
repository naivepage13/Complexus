package com.complexus.economia;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/**
 * Area administrativa mantida pela empresa, com orcamento mensal proprio.
 *
 * Departamento e custo fixo que compra vantagem: o dinheiro sai do resultado
 * todo turno, com ou sem venda, e volta como produtividade, reputacao, forca
 * comercial ou custo de insumo menor. O retorno satura - dobrar o orcamento nao
 * dobra o efeito -, entao existe um ponto a partir do qual manter a estrutura
 * passa a destruir lucro.
 */
@Entity
@Table(name = "departamento",
        uniqueConstraints = @UniqueConstraint(name = "uk_departamento_empresa_area",
                columnNames = {"empresa_id", "area"}))
public class Departamento {

    /**
     * O que cada area entrega no fechamento do turno.
     *
     * @param efeitoMaximo teto do efeito, alcancado apenas com orcamento muito
     *                     acima do porte da empresa
     */
    public enum Area {
        PESQUISA("Pesquisa e desenvolvimento", "ganho de produtividade por turno", 0.06),
        QUALIDADE("Qualidade", "pontos de reputacao por turno", 3.00),
        COMERCIAL("Comercial", "acrescimo de competitividade", 0.35),
        LOGISTICA("Logistica", "reducao do custo de insumo", 0.12);

        private final String rotulo;
        private final String unidadeEfeito;
        private final double efeitoMaximo;

        Area(String rotulo, String unidadeEfeito, double efeitoMaximo) {
            this.rotulo = rotulo;
            this.unidadeEfeito = unidadeEfeito;
            this.efeitoMaximo = efeitoMaximo;
        }

        public String getRotulo() { return rotulo; }
        public String getUnidadeEfeito() { return unidadeEfeito; }
        public double getEfeitoMaximo() { return efeitoMaximo; }

        /**
         * Efeito do orcamento no turno.
         *
         * A intensidade satura: metade do efeito maximo quando o orcamento
         * iguala a referencia de porte da empresa. Isso garante que estrutura
         * grande demais em empresa pequena so queime caixa.
         */
        public double efeito(double orcamento, double referenciaDePorte) {
            if (orcamento <= 0) {
                return 0.0;
            }
            double referencia = Math.max(referenciaDePorte, 1.0);
            return efeitoMaximo * (orcamento / (orcamento + referencia));
        }
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "empresa_id")
    private Empresa empresa;

    @Enumerated(EnumType.STRING)
    @Column(name = "area", nullable = false, length = 20)
    private Area area;

    @Column(nullable = false)
    private double orcamentoMensal;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Empresa getEmpresa() { return empresa; }
    public void setEmpresa(Empresa empresa) { this.empresa = empresa; }
    public Area getArea() { return area; }
    public void setArea(Area area) { this.area = area; }
    public double getOrcamentoMensal() { return orcamentoMensal; }
    public void setOrcamentoMensal(double orcamentoMensal) { this.orcamentoMensal = orcamentoMensal; }
}
