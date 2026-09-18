/* Estatisticas gerais, series historicas, setores e ranking. */

Sessao.exigir();

async function carregar() {
    try {
        const [gerais, serie, ranking] = await Promise.all([
            API.get('/estatisticas/gerais'),
            API.get('/estatisticas/serie?limite=24'),
            API.get('/estatisticas/ranking?limite=20')
        ]);
        desenharIndicadores(gerais);
        desenharSetores(gerais.setores);
        desenharSerie(serie);
        desenharRanking(ranking);
    } catch (erro) {
        Interface.mensagem('#mensagem', erro.message);
    }
}

function desenharIndicadores(gerais) {
    const fechamento = gerais.ultimoFechamento;
    if (!fechamento) {
        Interface.mensagem('#mensagem',
            'Nenhum turno foi processado ainda. Avance um turno no painel para gerar estatisticas.', 'sucesso');
        return;
    }
    document.querySelector('#pib').textContent = Formato.dinheiroCurto(fechamento.pib);
    document.querySelector('#detalhe-turno').textContent =
        `turno ${fechamento.turno} | ${Formato.dataJogo(fechamento.dataJogo)}`;

    document.querySelector('#inflacao').textContent = Formato.percentual(fechamento.inflacaoAnual, 2);
    document.querySelector('#detalhe-juros').textContent =
        `juros ${Formato.percentual(fechamento.taxaJuros, 2)} ao ano`;

    document.querySelector('#desemprego').textContent = Formato.percentual(fechamento.desemprego, 2);
    document.querySelector('#detalhe-empregos').textContent =
        `${Formato.inteiro(fechamento.empregosTotais)} empregos nas empresas simuladas`;

    document.querySelector('#aprovacao').textContent = Formato.numero(fechamento.aprovacaoGoverno, 1);
    document.querySelector('#detalhe-estabilidade').textContent =
        `estabilidade ${Formato.numero(fechamento.estabilidade, 1)} | ${fechamento.leisEmVigor} leis em vigor`;

    document.querySelector('#receita-agregada').textContent = Formato.dinheiroCurto(fechamento.receitaAgregada);
    const lucro = document.querySelector('#detalhe-lucro-agregado');
    lucro.textContent = `lucro ${Formato.dinheiroCurto(fechamento.lucroAgregado)}`;
    lucro.className = `detalhe ${Formato.classe(fechamento.lucroAgregado)}`;

    document.querySelector('#impostos').textContent = Formato.dinheiroCurto(fechamento.impostosArrecadados);
    document.querySelector('#detalhe-dividendos').textContent =
        `dividendos ${Formato.dinheiroCurto(fechamento.dividendosPagos)}`;

    document.querySelector('#indice').textContent = Formato.numero(fechamento.indiceMercado, 1);
    const variacao = document.querySelector('#detalhe-variacao');
    variacao.textContent = `variacao ${Formato.percentual(fechamento.variacaoIndice)}`;
    variacao.className = `detalhe ${Formato.classe(fechamento.variacaoIndice)}`;

    document.querySelector('#capital-investido').textContent = Formato.dinheiroCurto(fechamento.capitalInvestido);
    document.querySelector('#detalhe-investidores').textContent =
        `${fechamento.investidoresAtivos} investidores ativos`;
}

function desenharSetores(setores) {
    const corpo = document.querySelector('#lista-setores');
    if (!setores || !setores.length) {
        corpo.innerHTML = '<tr><td colspan="8" class="suave">Sem dados setoriais.</td></tr>';
        return;
    }
    corpo.innerHTML = setores.map((setor) => `
        <tr>
            <td>${setor.setorRotulo}</td>
            <td class="direita">${setor.empresas}</td>
            <td class="direita">${Formato.dinheiroCurto(setor.receita)}</td>
            <td class="direita ${Formato.classe(setor.lucro)}">${Formato.dinheiroCurto(setor.lucro)}</td>
            <td class="direita">${Formato.percentual(setor.margemMedia, 1)}</td>
            <td class="direita ${Formato.classe(setor.crescimentoMedio)}">${Formato.percentual(setor.crescimentoMedio)}</td>
            <td class="direita">${Formato.inteiro(setor.empregos)}</td>
            <td class="pequeno">${setor.empresaLider || '-'}</td>
        </tr>`).join('');
}

function desenharSerie(serie) {
    const corpo = document.querySelector('#lista-serie');
    if (!serie.length) {
        corpo.innerHTML = '<tr><td colspan="10" class="suave">Nenhum turno processado.</td></tr>';
        return;
    }
    corpo.innerHTML = serie.slice().reverse().map((linha) => `
        <tr>
            <td>${linha.turno}</td>
            <td>${Formato.dataJogo(linha.dataJogo)}</td>
            <td class="direita">${linha.empresasAtivas}</td>
            <td class="direita">${Formato.dinheiroCurto(linha.receitaAgregada)}</td>
            <td class="direita ${Formato.classe(linha.lucroAgregado)}">${Formato.dinheiroCurto(linha.lucroAgregado)}</td>
            <td class="direita">${Formato.dinheiroCurto(linha.impostosArrecadados)}</td>
            <td class="direita">${Formato.numero(linha.indiceMercado, 1)}</td>
            <td class="direita">${Formato.percentual(linha.inflacaoAnual, 1)}</td>
            <td class="direita">${Formato.percentual(linha.taxaJuros, 1)}</td>
            <td class="direita">${Formato.percentual(linha.desemprego, 1)}</td>
        </tr>`).join('');

    Interface.grafico('#grafico-lucro', serie.map((linha) => linha.lucroAgregado));
    Interface.grafico('#grafico-indice', serie.map((linha) => linha.indiceMercado));
}

function desenharRanking(ranking) {
    const corpo = document.querySelector('#lista-ranking');
    if (!ranking.length) {
        corpo.innerHTML = '<tr><td colspan="7" class="suave">Sem empresas ativas.</td></tr>';
        return;
    }
    corpo.innerHTML = ranking.map((empresa, indice) => `
        <tr>
            <td>${indice + 1}</td>
            <td><a href="empresa.html?id=${empresa.empresaId}">${empresa.nome}</a></td>
            <td><span class="etiqueta">${empresa.setor}</span></td>
            <td class="direita">${Formato.dinheiroCurto(empresa.valuation)}</td>
            <td class="direita ${Formato.classe(empresa.crescimentoLucro)}">${Formato.percentual(empresa.crescimentoLucro)}</td>
            <td class="direita">${Formato.percentual(empresa.marketShare, 1)}</td>
            <td class="direita">${Formato.percentual(empresa.indiceLastro, 0)}</td>
        </tr>`).join('');
}

carregar();
