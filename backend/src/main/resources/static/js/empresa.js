/* Gestao de uma empresa: balanco, estrutura, equipe, parametros, obras e historico. */

const jogadorEmpresa = Sessao.exigir();
const idEmpresa = new URLSearchParams(window.location.search).get('id');
let empresaAtual = null;
let catalogoEstrutura = null;
let municipiosDisponiveis = [];

async function carregarEmpresa() {
    if (!jogadorEmpresa || !idEmpresa) {
        window.location.href = 'empresas.html';
        return;
    }
    try {
        if (!catalogoEstrutura) {
            catalogoEstrutura = await API.get(`/empresas/${idEmpresa}/estrutura/catalogo`);
            const territorios = await API.get('/politica/territorios');
            municipiosDisponiveis = territorios.municipios || [];
            preencherCatalogo();
        }
        const empresa = await API.get(`/empresas/${idEmpresa}`);
        empresaAtual = empresa;
        desenharCabecalho(empresa);
        desenharIndicadores(empresa);
        desenharBalanco(empresa);
        desenharUnidades(empresa);
        desenharLinhas(empresa);
        desenharDepartamentos(empresa);
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
    document.querySelector('#link-financas').href = `financas.html?id=${empresa.id}`;
    document.querySelector('#link-cadeia').href = `cadeia.html?id=${empresa.id}`;

    const ehDono = empresa.donoId === jogadorEmpresa.id;
    if (!ehDono) {
        document.querySelector('#area-gestao').classList.add('oculto');
        document.querySelector('#area-obras').classList.add('oculto');
        document.querySelector('#area-unidades').classList.add('oculto');
        document.querySelector('#area-portfolio').classList.add('oculto');
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
        ['Unidades ativas', Formato.inteiro((empresa.unidades || []).length)],
        ['Funcionarios', Formato.inteiro(empresa.funcionarios)],
        ['Salario medio', Formato.dinheiro(empresa.salarioMedio)],
        ['Produtividade', Formato.numero(empresa.produtividade, 2)],
        ['Capacidade pela equipe', Formato.dinheiroCurto(capacidade.capacidadePorEquipe)],
        ['Capacidade pelo patrimonio', Formato.dinheiroCurto(capacidade.capacidadePorCapital)],
        ['Capacidade efetiva', Formato.dinheiroCurto(capacidade.capacidadeEfetiva)],
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

    const alavancagem = empresa.patrimonioLiquido > 0 ? empresa.divida / empresa.patrimonioLiquido : 0;
    document.querySelector('#resumo-divida').textContent = empresa.divida > 0
        ? `Divida de ${Formato.dinheiroCurto(empresa.divida)}, `
          + `${Formato.numero(alavancagem, 2)}x o patrimonio liquido. `
          + 'Contratos, taxas e limites ficam na pagina de Financas.'
        : 'Sem divida contratada. As linhas de credito disponiveis ficam na pagina de Financas.';
}

/*
 * Unidades: a tabela mostra onde a empresa opera e qual filial paga a conta.
 * A margem exibida e a operacional - juros, estrutura e imposto de renda sao
 * da companhia, nao da filial, e aparecem no resultado consolidado.
 */
function desenharUnidades(empresa) {
    const unidades = empresa.unidades || [];
    const corpo = document.querySelector('#lista-unidades');
    if (!unidades.length) {
        corpo.innerHTML = '<tr><td colspan="9" class="suave">Nenhuma unidade ativa.</td></tr>';
        return;
    }
    corpo.innerHTML = unidades.map((unidade) => `
        <tr>
            <td>${unidade.nome}${unidade.sede ? ' <span class="etiqueta">sede</span>' : ''}</td>
            <td>${unidade.municipio}/${unidade.estado}</td>
            <td class="direita">${Formato.dinheiroCurto(unidade.patrimonio)}</td>
            <td class="direita">${Formato.inteiro(unidade.funcionarios)}</td>
            <td class="direita">${Formato.numero(unidade.produtividade, 2)}</td>
            <td class="direita">${Formato.percentual(unidade.ocupacao, 0)}</td>
            <td class="direita ${Formato.classe(unidade.margemOperacional)}">${Formato.dinheiroCurto(unidade.margemOperacional)}</td>
            <td class="direita">${Formato.percentual(unidade.marketShare, 1)}</td>
            <td class="direita">${unidade.sede ? '' : `<button class="botao botao-neutro botao-pequeno" data-fechar="${unidade.id}">Fechar</button>`}</td>
        </tr>`).join('');

    corpo.querySelectorAll('[data-fechar]').forEach((botao) => {
        botao.addEventListener('click', () => fecharUnidade(botao.dataset.fechar));
    });

    const opcoes = unidades
        .map((unidade) => `<option value="${unidade.id}">${unidade.nome} (${unidade.municipio})</option>`)
        .join('');
    ['#unidade-alvo', '#origem-transferencia', '#unidade-obra'].forEach((seletor) => {
        const alvo = document.querySelector(seletor);
        if (alvo) alvo.innerHTML = opcoes;
    });
    const destino = document.querySelector('#destino-transferencia');
    if (destino) {
        destino.innerHTML = opcoes;
        if (unidades.length > 1) destino.selectedIndex = 1;
    }

    const ocupadas = unidades.map((unidade) => unidade.municipioId);
    document.querySelector('#municipio-unidade').innerHTML = municipiosDisponiveis
        .filter((municipio) => !ocupadas.includes(municipio.id))
        .map((municipio) => `<option value="${municipio.id}">${municipio.nome}/${municipio.estado}</option>`)
        .join('') || '<option value="">A empresa ja opera em todas as cidades</option>';
    atualizarCustoUnidade();
}

/* Abrir filial custa capital, instalacao e admissoes; o jogador ve antes de clicar. */
function atualizarCustoUnidade() {
    const alvo = document.querySelector('#custo-unidade');
    if (!alvo || !empresaAtual) return;
    const capital = Number(document.querySelector('#capital-unidade').value || 0);
    const funcionarios = Number(document.querySelector('#equipe-unidade').value || 0);
    const instalacao = capital * 0.08;
    const admissao = funcionarios * empresaAtual.salarioMedio * 0.5;
    const total = capital + instalacao + admissao;
    alvo.textContent = `Desembolso: ${Formato.dinheiro(total)} `
        + `(capital ${Formato.dinheiroCurto(capital)} + instalacao ${Formato.dinheiroCurto(instalacao)} `
        + `+ admissoes ${Formato.dinheiroCurto(admissao)}). Caixa: ${Formato.dinheiro(empresaAtual.caixa)}.`;
}

function desenharLinhas(empresa) {
    const linhas = empresa.linhas || [];
    const corpo = document.querySelector('#lista-linhas');
    if (!linhas.length) {
        corpo.innerHTML = '<tr><td colspan="4" class="suave">Sem linha declarada: a empresa vende no padrao do setor.</td></tr>';
    } else {
        corpo.innerHTML = linhas.map((linha) => `
            <tr>
                <td>${linha.nome}</td>
                <td>${linha.posicionamentoRotulo}</td>
                <td class="direita">${Formato.percentual(linha.fatiaMix, 0)}</td>
                <td class="direita"><button class="botao botao-neutro botao-pequeno" data-encerrar="${linha.id}">Encerrar</button></td>
            </tr>`).join('');
        corpo.querySelectorAll('[data-encerrar]').forEach((botao) => {
            botao.addEventListener('click', () => encerrarLinha(botao.dataset.encerrar));
        });
    }

    const declarado = linhas.reduce((soma, linha) => soma + linha.fatiaMix, 0);
    const preco = linhas.reduce((soma, linha) => soma + linha.fatiaMix * linha.fatorPreco, 0)
        + Math.max(1 - declarado, 0);
    document.querySelector('#resumo-mix').textContent =
        `preco medio ${Formato.percentual(preco - 1, 0)} sobre a referencia | `
        + `${Formato.percentual(Math.max(1 - declarado, 0), 0)} do mix no padrao`;
}

function desenharDepartamentos(empresa) {
    const departamentos = empresa.departamentos || [];
    const porArea = Object.fromEntries(departamentos.map((item) => [item.area, item.orcamentoMensal]));
    document.querySelector('#campos-departamentos').innerHTML = catalogoEstrutura.areas
        .map((area) => `
            <div class="campo">
                <label for="dep-${area.nome}">${area.rotulo} (R$/mes)</label>
                <input type="number" id="dep-${area.nome}" min="0" step="5000"
                       value="${Math.round(porArea[area.nome] || 0)}">
                <span class="suave pequeno">${area.unidadeEfeito}, ate ${Formato.numero(area.efeitoMaximo, 2)}</span>
            </div>`).join('');

    const total = departamentos.reduce((soma, item) => soma + item.orcamentoMensal, 0);
    document.querySelector('#resumo-estrutura').textContent =
        `custo fixo de ${Formato.dinheiroCurto(total)} por turno`;
}

function preencherCatalogo() {
    document.querySelector('#posicionamento-linha').innerHTML = catalogoEstrutura.posicionamentos
        .map((item) => `<option value="${item.nome}">${item.rotulo} `
            + `(preco ${Formato.numero(item.fatorPreco, 2)}x, custo ${Formato.numero(item.fatorCusto, 2)}x)</option>`)
        .join('');
}

/*
 * O desempenho da empresa e mostrado por grafico: lucro, receita e margem
 * liquida, um ponto por turno. A tabela abaixo fica como detalhamento.
 */
function desenharHistorico(historico) {
    const corpo = document.querySelector('#lista-historico');
    if (!historico || !historico.length) {
        corpo.innerHTML = '<tr><td colspan="8" class="suave">Nenhum turno processado ainda.</td></tr>';
        Interface.grafico('#grafico-lucro', [], { legenda: '#legenda-lucro' });
        Interface.grafico('#grafico-receita', [], { legenda: '#legenda-receita' });
        Interface.grafico('#grafico-margem', [], { formato: 'percentual', legenda: '#legenda-margem' });
        document.querySelector('#legenda-periodo').textContent = 'aguardando o primeiro fechamento';
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

    const turnos = historico.map((linha) => linha.turno);
    Interface.grafico('#grafico-lucro', historico.map((linha) => linha.lucro),
        { rotulos: turnos, legenda: '#legenda-lucro' });
    Interface.grafico('#grafico-receita', historico.map((linha) => linha.receita),
        { rotulos: turnos, legenda: '#legenda-receita' });
    Interface.grafico('#grafico-margem',
        historico.map((linha) => (linha.receita > 0 ? linha.lucro / linha.receita : 0)),
        { rotulos: turnos, formato: 'percentual', legenda: '#legenda-margem' });

    document.querySelector('#legenda-periodo').textContent =
        `${historico.length} turnos, do turno ${turnos[0]} ao ${turnos[turnos.length - 1]}`;
}

function desenharObras(empresa) {
    const corpo = document.querySelector('#lista-obras');
    const obras = empresa.empreendimentos || [];
    if (!obras.length) {
        corpo.innerHTML = '<tr><td colspan="7" class="suave">Nenhum empreendimento registrado.</td></tr>';
        return;
    }
    corpo.innerHTML = obras.map((obra) => `
        <tr>
            <td>${obra.nome}</td>
            <td>${obra.unidade || '-'}</td>
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

/** Unidade escolhida nos formularios de capital e equipe. */
function unidadeAlvo() {
    const valor = document.querySelector('#unidade-alvo').value;
    return valor ? Number(valor) : null;
}

function fecharUnidade(unidadeId) {
    executar(async () => {
        const resumo = await API.requisitar(
            `/empresas/${idEmpresa}/estrutura/unidades/${unidadeId}?jogadorId=${jogadorEmpresa.id}`,
            { method: 'DELETE' });
        Interface.mensagem('#mensagem',
            `Unidade fechada. Liquidacao de ${Formato.dinheiro(resumo.liquidacao)} `
            + `menos ${Formato.dinheiro(resumo.rescisao)} de rescisoes.`, 'sucesso');
    });
}

function encerrarLinha(linhaId) {
    executar(async () => {
        await API.requisitar(
            `/empresas/${idEmpresa}/estrutura/linhas/${linhaId}?jogadorId=${jogadorEmpresa.id}`,
            { method: 'DELETE' });
        Interface.mensagem('#mensagem', 'Linha encerrada.', 'sucesso');
    });
}

document.querySelector('#formulario-capital').addEventListener('submit', (evento) => {
    evento.preventDefault();
    executar(async () => {
        await API.post(`/empresas/${idEmpresa}/capital`, {
            jogadorId: jogadorEmpresa.id,
            valor: Number(document.querySelector('#valor-capital').value),
            unidadeId: unidadeAlvo()
        });
        Interface.mensagem('#mensagem', 'Capital aportado.', 'sucesso');
    });
});

document.querySelector('#botao-contratar').addEventListener('click', () => {
    executar(async () => {
        await API.post(`/empresas/${idEmpresa}/contratar`, {
            jogadorId: jogadorEmpresa.id,
            quantidade: Number(document.querySelector('#quantidade-equipe').value),
            unidadeId: unidadeAlvo()
        });
        Interface.mensagem('#mensagem', 'Contratacao registrada.', 'sucesso');
    });
});

document.querySelector('#botao-demitir').addEventListener('click', () => {
    executar(async () => {
        await API.post(`/empresas/${idEmpresa}/demitir`, {
            jogadorId: jogadorEmpresa.id,
            quantidade: Number(document.querySelector('#quantidade-equipe').value),
            unidadeId: unidadeAlvo()
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

document.querySelector('#formulario-unidade').addEventListener('submit', (evento) => {
    evento.preventDefault();
    executar(async () => {
        await API.post(`/empresas/${idEmpresa}/estrutura/unidades`, {
            jogadorId: jogadorEmpresa.id,
            municipioId: Number(document.querySelector('#municipio-unidade').value),
            nome: document.querySelector('#nome-unidade').value,
            capital: Number(document.querySelector('#capital-unidade').value),
            funcionarios: Number(document.querySelector('#equipe-unidade').value)
        });
        Interface.mensagem('#mensagem', 'Unidade aberta.', 'sucesso');
    });
});

['#capital-unidade', '#equipe-unidade'].forEach((seletor) => {
    document.querySelector(seletor).addEventListener('input', atualizarCustoUnidade);
});

document.querySelector('#botao-transferir-capital').addEventListener('click', () => {
    executar(async () => {
        const resumo = await API.post(`/empresas/${idEmpresa}/estrutura/unidades/transferir-capital`, {
            jogadorId: jogadorEmpresa.id,
            origemId: Number(document.querySelector('#origem-transferencia').value),
            destinoId: Number(document.querySelector('#destino-transferencia').value),
            valor: Number(document.querySelector('#valor-transferencia').value)
        });
        Interface.mensagem('#mensagem',
            `Capital movido. Perda na mudanca: ${Formato.dinheiro(resumo.perdaNaMudanca)}.`, 'sucesso');
    });
});

document.querySelector('#botao-transferir-equipe').addEventListener('click', () => {
    executar(async () => {
        const resumo = await API.post(`/empresas/${idEmpresa}/estrutura/unidades/transferir-equipe`, {
            jogadorId: jogadorEmpresa.id,
            origemId: Number(document.querySelector('#origem-transferencia').value),
            destinoId: Number(document.querySelector('#destino-transferencia').value),
            quantidade: Number(document.querySelector('#equipe-transferencia').value)
        });
        Interface.mensagem('#mensagem',
            `Equipe movida. Ajuda de custo: ${Formato.dinheiro(resumo.ajudaDeCusto)}.`, 'sucesso');
    });
});

document.querySelector('#formulario-linha').addEventListener('submit', (evento) => {
    evento.preventDefault();
    executar(async () => {
        await API.post(`/empresas/${idEmpresa}/estrutura/linhas`, {
            jogadorId: jogadorEmpresa.id,
            nome: document.querySelector('#nome-linha').value,
            posicionamento: document.querySelector('#posicionamento-linha').value,
            fatiaMix: Number(document.querySelector('#fatia-linha').value)
        });
        Interface.mensagem('#mensagem', 'Linha criada.', 'sucesso');
    });
});

/* Um POST por area: sao poucos campos e cada um gera seu proprio evento. */
document.querySelector('#formulario-departamentos').addEventListener('submit', (evento) => {
    evento.preventDefault();
    executar(async () => {
        for (const area of catalogoEstrutura.areas) {
            const campo = document.querySelector(`#dep-${area.nome}`);
            await API.post(`/empresas/${idEmpresa}/estrutura/departamentos`, {
                jogadorId: jogadorEmpresa.id,
                area: area.nome,
                orcamentoMensal: Number(campo.value || 0)
            });
        }
        Interface.mensagem('#mensagem', 'Orcamentos atualizados.', 'sucesso');
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
            turnosTotais: Number(document.querySelector('#prazo-obra').value),
            unidadeId: Number(document.querySelector('#unidade-obra').value) || null
        });
        Interface.mensagem('#mensagem', 'Obra iniciada.', 'sucesso');
    });
});

carregarEmpresa();
