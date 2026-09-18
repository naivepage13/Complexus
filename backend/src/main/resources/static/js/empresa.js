/* Gestao de uma empresa: balanco, equipe, parametros, obras e historico. */

const jogadorEmpresa = Sessao.exigir();
const idEmpresa = new URLSearchParams(window.location.search).get('id');
let empresaAtual = null;

async function carregarEmpresa() {
    if (!jogadorEmpresa || !idEmpresa) {
        window.location.href = 'empresas.html';
        return;
    }
    try {
        const empresa = await API.get(`/empresas/${idEmpresa}`);
        empresaAtual = empresa;
        desenharCabecalho(empresa);
        desenharIndicadores(empresa);
        desenharBalanco(empresa);
        desenharHistorico(empresa.historico);
        desenharObras(empresa);
        preencherFormularios(empresa);
    } catch (erro) {
        Interface.mensagem('#mensagem', erro.message);
    }
}

function desenharCabecalho(empresa) {
    document.querySelector('#titulo-empresa').textContent = empresa.nome;
    document.querySelector('#subtitulo-empresa').textContent =
        `${empresa.setorRotulo} | sede em ${empresa.municipio}/${empresa.estado} | `
        + `fundada no turno ${empresa.turnoFundacao} | dono: ${empresa.dono}`;

    const ehDono = empresa.donoId === jogadorEmpresa.id;
    if (!ehDono) {
        document.querySelector('#area-gestao').classList.add('oculto');
        document.querySelector('#area-obras').classList.add('oculto');
    } else if (empresa.setor === 'ALIMENTICIO') {
        document.querySelector('#area-obras').classList.add('oculto');
    }
}

function desenharIndicadores(empresa) {
    document.querySelector('#receita').textContent = Formato.dinheiroCurto(empresa.receitaMensal);
    document.querySelector('#detalhe-custo').textContent =
        `custo ${Formato.dinheiroCurto(empresa.custoMensal)} | impostos ${Formato.dinheiroCurto(empresa.impostosMensais)}`;

    const lucro = document.querySelector('#lucro');
    lucro.textContent = Formato.dinheiroCurto(empresa.lucroMensal);
    lucro.className = `valor ${Formato.classe(empresa.lucroMensal)}`;
    const crescimento = document.querySelector('#detalhe-crescimento');
    crescimento.textContent = `crescimento ${Formato.percentual(empresa.crescimentoLucro)}`;
    crescimento.className = `detalhe ${Formato.classe(empresa.crescimentoLucro)}`;

    document.querySelector('#valuation').textContent = Formato.dinheiroCurto(empresa.valuation);
    document.querySelector('#detalhe-acao').textContent =
        `acao ${Formato.dinheiro(empresa.precoAcao)} | ${empresa.capitalAberto ? 'capital aberto' : 'capital fechado'}`;

    document.querySelector('#lastro').textContent = Formato.percentual(empresa.indiceLastro, 0);
}

function desenharBalanco(empresa) {
    const capacidade = empresa.capacidade || {};
    const linhas = [
        ['Caixa', Formato.dinheiro(empresa.caixa)],
        ['Patrimonio operacional', Formato.dinheiro(empresa.patrimonio)],
        ['Divida', Formato.dinheiro(empresa.divida)],
        ['Patrimonio liquido', Formato.dinheiro(empresa.patrimonioLiquido)],
        ['Lucro acumulado', Formato.dinheiro(empresa.lucroAcumulado)],
        ['Funcionarios', Formato.inteiro(empresa.funcionarios)],
        ['Salario medio', Formato.dinheiro(empresa.salarioMedio)],
        ['Produtividade', Formato.numero(empresa.produtividade, 2)],
        ['Capacidade pela equipe', Formato.dinheiroCurto(capacidade.capacidadePorEquipe)],
        ['Capacidade pelo patrimonio', Formato.dinheiroCurto(capacidade.capacidadePorCapital)],
        ['Gargalo atual', capacidade.gargalo === 'EQUIPE'
            ? 'equipe (contrate ou aumente a produtividade)'
            : 'patrimonio (aporte capital)'],
        ['Equipe sugerida', Formato.inteiro(capacidade.equipeSugerida)],
        ['Ocupacao da capacidade', Formato.percentual(capacidade.ocupacao, 0)],
        ['Reputacao', Formato.numero(empresa.reputacao, 1)],
        ['Participacao de mercado', Formato.percentual(empresa.marketShare, 2)],
        ['Acoes em circulacao', Formato.inteiro(empresa.acoesEmCirculacao)],
        ['Payout', Formato.percentual(empresa.payout, 0)]
    ];
    document.querySelector('#tabela-balanco').innerHTML = linhas
        .map(([rotulo, valor]) => `<tr><th>${rotulo}</th><td class="direita">${valor}</td></tr>`)
        .join('');
}

function desenharHistorico(historico) {
    const corpo = document.querySelector('#lista-historico');
    if (!historico || !historico.length) {
        corpo.innerHTML = '<tr><td colspan="8" class="suave">Nenhum turno processado ainda.</td></tr>';
        Interface.grafico('#grafico-lucro', []);
        return;
    }
    corpo.innerHTML = historico.slice().reverse().map((linha) => `
        <tr>
            <td>${linha.turno}</td>
            <td class="direita">${Formato.dinheiroCurto(linha.receita)}</td>
            <td class="direita">${Formato.dinheiroCurto(linha.custo)}</td>
            <td class="direita">${Formato.dinheiroCurto(linha.impostos)}</td>
            <td class="direita ${Formato.classe(linha.lucro)}">${Formato.dinheiroCurto(linha.lucro)}</td>
            <td class="direita ${Formato.classe(linha.crescimentoLucro)}">${Formato.percentual(linha.crescimentoLucro)}</td>
            <td class="direita">${Formato.dinheiro(linha.precoAcao)}</td>
            <td class="direita">${Formato.percentual(linha.marketShare, 1)}</td>
        </tr>`).join('');

    Interface.grafico('#grafico-lucro', historico.map((linha) => linha.lucro));
    document.querySelector('#legenda-grafico').textContent =
        `${historico.length} turnos registrados, do turno ${historico[0].turno} ao ${historico[historico.length - 1].turno}.`;
}

function desenharObras(empresa) {
    const corpo = document.querySelector('#lista-obras');
    const obras = empresa.empreendimentos || [];
    if (!obras.length) {
        corpo.innerHTML = '<tr><td colspan="6" class="suave">Nenhum empreendimento registrado.</td></tr>';
        return;
    }
    corpo.innerHTML = obras.map((obra) => `
        <tr>
            <td>${obra.nome}</td>
            <td>${obra.tipoRotulo}</td>
            <td><span class="etiqueta">${obra.status}</span></td>
            <td class="direita">${Formato.dinheiroCurto(obra.custoTotal)}</td>
            <td class="direita">${Formato.dinheiroCurto(obra.valorEstimado)}</td>
            <td class="direita">${Formato.percentual(obra.percentualConcluido, 0)}</td>
        </tr>`).join('');
}

function preencherFormularios(empresa) {
    document.querySelector('#marketing').value = Math.round(empresa.marketingMensal);
    document.querySelector('#salario').value = Math.round(empresa.salarioMedio);
    document.querySelector('#payout').value = empresa.payout;
}

async function executar(acao) {
    Interface.mensagem('#mensagem', '');
    try {
        await acao();
        await carregarEmpresa();
        await Interface.montarCabecalho();
    } catch (erro) {
        Interface.mensagem('#mensagem', erro.message);
    }
}

document.querySelector('#formulario-capital').addEventListener('submit', (evento) => {
    evento.preventDefault();
    executar(async () => {
        await API.post(`/empresas/${idEmpresa}/capital`, {
            jogadorId: jogadorEmpresa.id,
            valor: Number(document.querySelector('#valor-capital').value)
        });
        Interface.mensagem('#mensagem', 'Capital aportado.', 'sucesso');
    });
});

document.querySelector('#botao-contratar').addEventListener('click', () => {
    executar(async () => {
        await API.post(`/empresas/${idEmpresa}/contratar`, {
            jogadorId: jogadorEmpresa.id,
            quantidade: Number(document.querySelector('#quantidade-equipe').value)
        });
        Interface.mensagem('#mensagem', 'Contratacao registrada.', 'sucesso');
    });
});

document.querySelector('#botao-demitir').addEventListener('click', () => {
    executar(async () => {
        await API.post(`/empresas/${idEmpresa}/demitir`, {
            jogadorId: jogadorEmpresa.id,
            quantidade: Number(document.querySelector('#quantidade-equipe').value)
        });
        Interface.mensagem('#mensagem', 'Desligamento registrado.', 'sucesso');
    });
});

document.querySelector('#formulario-gestao').addEventListener('submit', (evento) => {
    evento.preventDefault();
    executar(async () => {
        await API.post(`/empresas/${idEmpresa}/gestao`, {
            jogadorId: jogadorEmpresa.id,
            marketingMensal: Number(document.querySelector('#marketing').value),
            salarioMedio: Number(document.querySelector('#salario').value),
            payout: Number(document.querySelector('#payout').value)
        });
        Interface.mensagem('#mensagem', 'Parametros atualizados.', 'sucesso');
    });
});

document.querySelector('#formulario-ipo').addEventListener('submit', (evento) => {
    evento.preventDefault();
    executar(async () => {
        await API.post(`/empresas/${idEmpresa}/ipo`, {
            jogadorId: jogadorEmpresa.id,
            fracaoOfertada: Number(document.querySelector('#fracao-ipo').value)
        });
        Interface.mensagem('#mensagem', 'Capital aberto: as acoes ja aparecem no mercado.', 'sucesso');
    });
});

document.querySelector('#formulario-obra').addEventListener('submit', (evento) => {
    evento.preventDefault();
    executar(async () => {
        await API.post(`/empresas/${idEmpresa}/empreendimentos`, {
            jogadorId: jogadorEmpresa.id,
            nome: document.querySelector('#nome-obra').value,
            tipo: document.querySelector('#tipo-obra').value,
            custoTotal: Number(document.querySelector('#custo-obra').value),
            turnosTotais: Number(document.querySelector('#prazo-obra').value)
        });
        Interface.mensagem('#mensagem', 'Obra iniciada.', 'sucesso');
    });
});

carregarEmpresa();
