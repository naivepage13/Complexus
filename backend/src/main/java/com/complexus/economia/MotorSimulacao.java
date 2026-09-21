package com.complexus.economia;

import com.complexus.politica.Estado;
import com.complexus.politica.Municipio;
import com.complexus.politica.Pais;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Motor economico do jogo: funcoes puras que transformam o estado de uma
 * empresa e do mundo no resultado de um mes.
 *
 * Nao acessa banco nem altera entidades. Toda regra de calculo vive aqui para
 * que o balanceamento possa ser ajustado e testado em um unico lugar.
 *
 * <h2>Duas camadas</h2>
 * A operacao acontece por <b>unidade</b> ({@link #simularOperacao}): cada filial
 * disputa o mercado da sua cidade, com a propria equipe, o proprio patrimonio e
 * o preco que a empresa pratica. O resultado financeiro acontece por
 * <b>empresa</b> ({@link #consolidar}): juros da divida, estrutura administrativa
 * e imposto sobre o lucro sao unicos e nao se repetem por filial.
 *
 * <h2>Cadeia de calculo</h2>
 * <ol>
 *   <li>mercado potencial do setor no municipio;</li>
 *   <li>competitividade relativa define a participacao de mercado;</li>
 *   <li>demanda capturada x capacidade instalada define o volume;</li>
 *   <li>preco praticado converte volume em receita;</li>
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
     * Forca competitiva de uma unidade. Patrimonio entra com raiz quadrada para
     * que capital sozinho nao domine o mercado; reputacao, marketing, esforco
     * comercial e preco pesam sobre ele.
     *
     * Metade da elasticidade-preco age aqui, na disputa por cliente do
     * concorrente, e metade no tamanho da demanda capturada: quem baixa o preco
     * tira cliente do vizinho e amplia o proprio mercado.
     */
    public double competitividade(PerfilOperacional perfil) {
        double capital = Math.sqrt(Math.max(perfil.patrimonio(), 1.0));
        double fatorReputacao = 0.5 + perfil.reputacao() / 100.0;
        double fatorMarketing = 1.0 + Math.sqrt(Math.max(perfil.marketingMensal(), 0.0)) / 500.0;
        double fatorComercial = 1.0 + Math.max(perfil.bonusComercial(), 0.0);
        return capital * Math.max(perfil.produtividade(), 0.1) * fatorReputacao * fatorMarketing
                * fatorComercial * atratividadeDePreco(perfil);
    }

    /** Competitividade de uma empresa sem estrutura declarada. */
    public double competitividade(Empresa empresa) {
        return competitividade(PerfilOperacional.neutro(empresa));
    }

    /** Peso do preco na disputa por mercado: metade da elasticidade do setor. */
    private double atratividadeDePreco(PerfilOperacional perfil) {
        double preco = Math.max(perfil.fatorPreco(), 0.1);
        return Math.pow(preco, -perfil.setor().getElasticidadePreco() / 2.0);
    }

    /**
     * Receita mensal maxima que a unidade consegue entregar, a preco de referencia.
     *
     * A producao tem dois tetos e vale o menor deles: a equipe (gente para
     * produzir e vender) e o patrimonio (instalacoes, estoque, terrenos). E o
     * que obriga o jogador a crescer nas duas frentes - contratar sem investir
     * so gera folha ociosa, e investir sem contratar deixa ativo parado.
     */
    public double capacidadeProdutiva(PerfilOperacional perfil) {
        return Math.max(Math.min(capacidadePorEquipe(perfil), capacidadePorCapital(perfil)), 0);
    }

    public double capacidadeProdutiva(Empresa empresa) {
        return capacidadeProdutiva(PerfilOperacional.neutro(empresa));
    }

    /** Teto de producao apenas pela equipe, util para diagnosticar ociosidade. */
    public double capacidadePorEquipe(PerfilOperacional perfil) {
        return perfil.funcionarios() * perfil.setor().getReceitaPorFuncionario() * perfil.produtividade();
    }

    public double capacidadePorEquipe(Empresa empresa) {
        return capacidadePorEquipe(PerfilOperacional.neutro(empresa));
    }

    /** Teto de producao apenas pelo patrimonio. */
    public double capacidadePorCapital(PerfilOperacional perfil) {
        return perfil.patrimonio() * perfil.setor().getGiroAtivoMensal();
    }

    public double capacidadePorCapital(Empresa empresa) {
        return capacidadePorCapital(PerfilOperacional.neutro(empresa));
    }

    /**
     * Calcula o mes de uma unidade.
     *
     * @param participacao    fatia do mercado disputavel conquistada (0 a 1)
     * @param subsidioFracao  subsidio vigente, como fracao da receita
     * @param regulacaoFracao regulacao vigente, como acrescimo ao custo variavel
     */
    public ResultadoOperacional simularOperacao(PerfilOperacional perfil,
                                                double mercadoDisputavel,
                                                double participacao,
                                                ContextoMercado contexto,
                                                Estado estado,
                                                Municipio municipio,
                                                double subsidioFracao,
                                                double regulacaoFracao) {

        Setor setor = perfil.setor();
        ModificadorSetorial modificador = contexto.modificador(setor);

        double ajusteRenda = 1.0 + (contexto.rendaMedia() / contexto.rendaReferencia() - 1.0)
                * setor.getElasticidadeRenda();
        double ajusteJuros = 1.0 - (contexto.taxaJurosAnual() - JUROS_REFERENCIA)
                * setor.getElasticidadeJuros() * 3.0;
        double ajusteConfianca = 0.85 + contexto.estabilidade() / 400.0;
        // A outra metade da elasticidade-preco: preco alto encolhe o proprio mercado.
        double ajustePreco = Math.pow(Math.max(perfil.fatorPreco(), 0.1),
                -setor.getElasticidadePreco() / 2.0);

        double demandaCapturada = mercadoDisputavel * participacao
                * Math.max(ajusteRenda, 0.2)
                * Math.max(ajusteJuros, 0.2)
                * ajusteConfianca
                * ajustePreco
                * (1.0 + modificador.choqueDemanda());

        double capacidade = capacidadeProdutiva(perfil);
        // Volume sai a preco de referencia; o preco praticado vira receita depois.
        double volume = Math.max(Math.min(demandaCapturada, capacidade), 0.0);
        double receita = volume * perfil.fatorPreco();
        double ocupacao = capacidade <= 0 ? 0.0 : Math.min(volume / capacidade, 1.0);

        double custoVariavel = volume * (1.0 - setor.getMargemBase())
                * (1.0 + modificador.choqueCusto())
                * (1.0 + regulacaoFracao)
                * Math.max(perfil.fatorCustoVariavel(), 0.1);
        double folha = perfil.funcionarios() * perfil.salarioMedio() * 1.32; // encargos
        double depreciacao = perfil.patrimonio() * DEPRECIACAO_MENSAL;
        double custoOperacional = custoVariavel + folha + perfil.marketingMensal() + depreciacao;

        double impostoIndireto = receita
                * (estado.getAliquotaEstadual() * baseEstadual(setor)
                + municipio.getAliquotaMunicipal() * baseMunicipal(setor));
        double subsidio = receita * subsidioFracao;

        return new ResultadoOperacional(demandaCapturada, capacidade, volume, receita,
                custoOperacional, impostoIndireto, subsidio, ocupacao);
    }

    /**
     * Fecha o mes da empresa a partir do que as unidades produziram.
     *
     * Juros da divida, estrutura administrativa e imposto de renda entram uma
     * unica vez, na companhia: e o que permite uma filial nova operar no
     * vermelho sem ser tributada como se fosse uma empresa separada.
     *
     * @param juros           juros do mes sobre a divida onerosa
     * @param custoEstrutura  orcamento dos departamentos no mes
     * @param aliquotaImposto aliquota federal sobre o lucro
     */
    public ResultadoMensal consolidar(List<ResultadoOperacional> operacoes, double juros,
                                      double custoEstrutura, double aliquotaImposto) {
        double demanda = 0;
        double capacidade = 0;
        double volume = 0;
        double receita = 0;
        double custo = 0;
        double impostoIndireto = 0;
        double subsidio = 0;
        for (ResultadoOperacional operacao : operacoes) {
            demanda += operacao.demandaCapturada();
            capacidade += operacao.capacidade();
            volume += operacao.volume();
            receita += operacao.receita();
            custo += operacao.custoOperacional();
            impostoIndireto += operacao.impostoIndireto();
            subsidio += operacao.subsidioRecebido();
        }
        double custoTotal = custo + Math.max(juros, 0) + Math.max(custoEstrutura, 0);
        double lucroAntesImposto = receita + subsidio - custoTotal - impostoIndireto;
        double impostoRenda = lucroAntesImposto > 0 ? lucroAntesImposto * aliquotaImposto : 0.0;
        double lucro = lucroAntesImposto - impostoRenda;
        double ocupacao = capacidade <= 0 ? 0.0 : Math.min(volume / capacidade, 1.0);

        return new ResultadoMensal(demanda, capacidade, receita, custoTotal, impostoIndireto,
                impostoRenda, subsidio, lucro, ocupacao);
    }

    /**
     * Calcula o mes de uma empresa de unidade unica, sem estrutura declarada.
     * Atalho usado pelos testes de balanceamento do motor.
     */
    public ResultadoMensal simularMes(Empresa empresa,
                                      double mercadoDisputavel,
                                      double participacao,
                                      ContextoMercado contexto,
                                      Estado estado,
                                      Municipio municipio,
                                      Pais pais,
                                      double subsidioFracao,
                                      double regulacaoFracao) {

        ResultadoOperacional operacao = simularOperacao(PerfilOperacional.neutro(empresa),
                mercadoDisputavel, participacao, contexto, estado, municipio,
                subsidioFracao, regulacaoFracao);
        double juros = empresa.getDivida() * contexto.taxaJurosAnual() / 12.0;
        return consolidar(List.of(operacao), juros, 0.0, pais.getAliquotaImpostoEmpresarial());
    }

    private double baseEstadual(Setor setor) {
        return switch (setor) {
            case ALIMENTICIO -> BASE_ESTADUAL_ALIMENTICIO;
            case IMOBILIARIO -> BASE_ESTADUAL_IMOBILIARIO;
            case CONSTRUCAO -> BASE_ESTADUAL_CONSTRUCAO;
        };
    }

    private double baseMunicipal(Setor setor) {
        return switch (setor) {
            case ALIMENTICIO -> BASE_MUNICIPAL_ALIMENTICIO;
            case IMOBILIARIO -> BASE_MUNICIPAL_IMOBILIARIO;
            case CONSTRUCAO -> BASE_MUNICIPAL_CONSTRUCAO;
        };
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
