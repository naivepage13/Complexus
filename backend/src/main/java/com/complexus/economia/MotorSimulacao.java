package com.complexus.economia;

import com.complexus.politica.Estado;
import com.complexus.politica.Municipio;
import com.complexus.politica.Pais;
import org.springframework.stereotype.Component;

/**
 * Motor economico do jogo: funcoes puras que transformam o estado de uma
 * empresa e do mundo no resultado de um mes.
 *
 * Nao acessa banco nem altera entidades. Toda regra de calculo vive aqui para
 * que o balanceamento possa ser ajustado e testado em um unico lugar.
 *
 * <h2>Cadeia de calculo</h2>
 * <ol>
 *   <li>mercado potencial do setor no municipio;</li>
 *   <li>competitividade relativa define a participacao de mercado;</li>
 *   <li>demanda capturada x capacidade instalada define a receita;</li>
 *   <li>custos, tributos e subsidios definem o lucro;</li>
 *   <li>lucro anualizado e patrimonio definem o valuation e o preco da acao.</li>
 * </ol>
 */
@Component
public class MotorSimulacao {

    /** Fracao da receita que serve de base para tributos estaduais. */
    private static final double BASE_ESTADUAL_ALIMENTICIO = 0.60;
    private static final double BASE_ESTADUAL_IMOBILIARIO = 0.15;
    private static final double BASE_ESTADUAL_CONSTRUCAO = 0.45;

    /** Fracao da receita que serve de base para tributos municipais. */
    private static final double BASE_MUNICIPAL_ALIMENTICIO = 0.10;
    private static final double BASE_MUNICIPAL_IMOBILIARIO = 0.55;
    private static final double BASE_MUNICIPAL_CONSTRUCAO = 0.40;

    private static final double JUROS_REFERENCIA = 0.1075;
    private static final double DEPRECIACAO_MENSAL = 0.004;

    /**
     * Tamanho mensal do mercado do setor no municipio, em R$ de jogo.
     * Depende de populacao, renda, e dos indicadores locais que a politica move.
     */
    public double mercadoPotencial(Setor setor, Municipio municipio, Estado estado, Pais pais) {
        double populacao = Math.max(municipio.getPopulacao(), 1);
        double renda = Math.max(pais.getRendaMedia(), 1);
        double ocupacao = 1.0 - pais.getDesemprego();

        return switch (setor) {
            // Alimentacao: consumo recorrente, pouco sensivel a ciclo.
            case ALIMENTICIO -> populacao * renda * 0.16 * ocupacao;
            // Imobiliario: puxado por demanda local, urbanizacao e valor do terreno.
            case IMOBILIARIO -> populacao * renda * 0.030
                    * (municipio.getDemandaImobiliaria() / 100.0)
                    * (0.6 + municipio.getIndiceUrbanizacao() / 125.0);
            // Construcao: puxada por obra publica estadual e pelo mercado imobiliario.
            case CONSTRUCAO -> populacao * renda * 0.022
                    * (0.5 + estado.getIndiceDesenvolvimento() / 120.0)
                    + estado.getInvestimentoInfraestrutura() * 0.45;
        };
    }

    /**
     * Fatia do mercado que as empresas simuladas conseguem disputar entre si.
     *
     * O mercado potencial de uma cidade e muito maior do que a capacidade das
     * empresas do jogo: o restante e atendido por empresas nao modeladas. Limitar
     * a disputa a capacidade instalada do grupo e o que torna a concorrencia
     * relevante - quem perde participacao fica com capacidade ociosa e prejuizo.
     */
    public double mercadoDisputavel(double mercadoPotencial, double somaCapacidades) {
        return Math.min(mercadoPotencial, Math.max(somaCapacidades, 0));
    }

    /**
     * Forca competitiva da empresa. Patrimonio entra com raiz quadrada para que
     * capital sozinho nao domine o mercado; reputacao e marketing pesam sobre ele.
     */
    public double competitividade(Empresa empresa) {
        double capital = Math.sqrt(Math.max(empresa.getPatrimonio(), 1.0));
        double fatorReputacao = 0.5 + empresa.getReputacao() / 100.0;
        double fatorMarketing = 1.0 + Math.sqrt(Math.max(empresa.getMarketingMensal(), 0.0)) / 500.0;
        return capital * Math.max(empresa.getProdutividade(), 0.1) * fatorReputacao * fatorMarketing;
    }

    /**
     * Receita mensal maxima que a empresa consegue entregar.
     *
     * A producao tem dois tetos e vale o menor deles: a equipe (gente para
     * produzir e vender) e o patrimonio (instalacoes, estoque, terrenos). E o
     * que obriga o jogador a crescer nas duas frentes - contratar sem investir
     * so gera folha ociosa, e investir sem contratar deixa ativo parado.
     */
    public double capacidadeProdutiva(Empresa empresa) {
        Setor setor = empresa.getSetor();
        double porEquipe = empresa.getFuncionarios() * setor.getReceitaPorFuncionario()
                * empresa.getProdutividade();
        double porCapital = empresa.getPatrimonio() * setor.getGiroAtivoMensal();
        return Math.max(Math.min(porEquipe, porCapital), 0);
    }

    /** Teto de producao apenas pela equipe, util para diagnosticar ociosidade. */
    public double capacidadePorEquipe(Empresa empresa) {
        return empresa.getFuncionarios() * empresa.getSetor().getReceitaPorFuncionario()
                * empresa.getProdutividade();
    }

    /** Teto de producao apenas pelo patrimonio. */
    public double capacidadePorCapital(Empresa empresa) {
        return empresa.getPatrimonio() * empresa.getSetor().getGiroAtivoMensal();
    }

    /**
     * Calcula o mes da empresa.
     *
     * @param participacao   fatia do mercado potencial conquistada (0 a 1)
     * @param subsidioFracao subsidio vigente, como fracao da receita
     * @param regulacaoFracao regulacao vigente, como acrescimo ao custo variavel
     */
    public ResultadoMensal simularMes(Empresa empresa,
                                      double mercadoPotencial,
                                      double participacao,
                                      ContextoMercado contexto,
                                      Estado estado,
                                      Municipio municipio,
                                      Pais pais,
                                      double subsidioFracao,
                                      double regulacaoFracao) {

        Setor setor = empresa.getSetor();
        ModificadorSetorial modificador = contexto.modificador(setor);

        double ajusteRenda = 1.0 + (contexto.rendaMedia() / contexto.rendaReferencia() - 1.0)
                * setor.getElasticidadeRenda();
        double ajusteJuros = 1.0 - (contexto.taxaJurosAnual() - JUROS_REFERENCIA)
                * setor.getElasticidadeJuros() * 3.0;
        double ajusteConfianca = 0.85 + contexto.estabilidade() / 400.0;

        double demandaCapturada = mercadoPotencial * participacao
                * Math.max(ajusteRenda, 0.2)
                * Math.max(ajusteJuros, 0.2)
                * ajusteConfianca
                * (1.0 + modificador.choqueDemanda());

        double capacidade = capacidadeProdutiva(empresa);
        double receita = Math.max(Math.min(demandaCapturada, capacidade), 0.0);
        double ocupacao = capacidade <= 0 ? 0.0 : Math.min(receita / capacidade, 1.0);

        double custoVariavel = receita * (1.0 - setor.getMargemBase())
                * (1.0 + modificador.choqueCusto())
                * (1.0 + regulacaoFracao);
        double folha = empresa.getFuncionarios() * empresa.getSalarioMedio() * 1.32; // encargos
        double depreciacao = empresa.getPatrimonio() * DEPRECIACAO_MENSAL;
        double juros = empresa.getDivida() * contexto.taxaJurosAnual() / 12.0;
        double custoOperacional = custoVariavel + folha + empresa.getMarketingMensal() + depreciacao + juros;

        double baseEstadual = switch (setor) {
            case ALIMENTICIO -> BASE_ESTADUAL_ALIMENTICIO;
            case IMOBILIARIO -> BASE_ESTADUAL_IMOBILIARIO;
            case CONSTRUCAO -> BASE_ESTADUAL_CONSTRUCAO;
        };
        double baseMunicipal = switch (setor) {
            case ALIMENTICIO -> BASE_MUNICIPAL_ALIMENTICIO;
            case IMOBILIARIO -> BASE_MUNICIPAL_IMOBILIARIO;
            case CONSTRUCAO -> BASE_MUNICIPAL_CONSTRUCAO;
        };
        double impostoIndireto = receita
                * (estado.getAliquotaEstadual() * baseEstadual
                + municipio.getAliquotaMunicipal() * baseMunicipal);

        double subsidio = receita * subsidioFracao;
        double lucroAntesImposto = receita + subsidio - custoOperacional - impostoIndireto;
        double impostoRenda = lucroAntesImposto > 0
                ? lucroAntesImposto * pais.getAliquotaImpostoEmpresarial()
                : 0.0;
        double lucro = lucroAntesImposto - impostoRenda;

        return new ResultadoMensal(demandaCapturada, capacidade, receita, custoOperacional,
                impostoIndireto, impostoRenda, subsidio, lucro, ocupacao);
    }

    /**
     * Valor de mercado da empresa.
     *
     * Lastro: o valuation nunca cai abaixo de 60 por cento do patrimonio liquido,
     * e o premio sobre esse piso vem do lucro anualizado multiplicado pelo
     * multiplo do setor, corrigido pela confianca do mercado.
     */
    public double calcularValuation(Empresa empresa, double lucroMensal, ContextoMercado contexto) {
        double patrimonioLiquido = empresa.patrimonioLiquido();
        double lucroAnualizado = lucroMensal * 12.0;
        double confianca = contexto.modificador(empresa.getSetor()).confianca()
                * (0.8 + contexto.estabilidade() / 250.0);
        double premio = Math.max(lucroAnualizado, 0.0) * empresa.getSetor().getMultiploValuation() * confianca;
        double piso = Math.max(patrimonioLiquido * 0.6, 0.0);
        return Math.max(piso, patrimonioLiquido * 0.85 + premio);
    }

    /**
     * Preco da acao com suavizacao: o mercado converge 40 por cento ao valor
     * justo por turno, evitando saltos bruscos entre meses.
     */
    public double calcularPrecoAcao(Empresa empresa, double valuation) {
        double acoes = Math.max(empresa.getAcoesTotais(), 1);
        double precoJusto = valuation / acoes;
        double precoAtual = empresa.getPrecoAcao();
        if (precoAtual <= 0) {
            return precoJusto;
        }
        return precoAtual + (precoJusto - precoAtual) * 0.4;
    }

    /** Variacao percentual do lucro entre dois meses, tratando base zero ou negativa. */
    public double calcularCrescimento(double lucroAtual, double lucroAnterior) {
        if (lucroAnterior == 0) {
            return lucroAtual > 0 ? 1.0 : 0.0;
        }
        return (lucroAtual - lucroAnterior) / Math.abs(lucroAnterior);
    }
}
