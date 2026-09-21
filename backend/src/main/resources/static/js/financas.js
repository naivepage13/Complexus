/* Financas da empresa: nota de credito, linhas disponiveis e divida contratada. */

const jogadorFinancas = Sessao.exigir();
const idEmpresaFinancas = new URLSearchParams(window.location.search).get('id');
let empresaFinancas = null;
let vitrineAtual = null;

async function carregarFinancas() {
    if (!jogadorFinancas || !idEmpresaFinancas) {
        window.location.href = 'empresas.html';
        return;
    }
    try {
        const [empresa, financas] = await Promise.all([
            API.get(`/empresas/${idEmpresaFinancas}`),
            API.get(`/empresas/${idEmpresaFinancas}/financas`)
        ]);
        empresaFinancas = empresa;
        vitrineAtual = financas;
        desenharCabecalho(empresa, financas);
        desenharIndicadores(empresa, financas);
        desenharAvaliacao(financas.avaliacao);
        desenharLinhas(financas.linhas);
        desenharContratos(financas.contratos);
        desenharEncerrados(financas.historico);
        simular();
    } catch (erro) {
        Interface.mensagem('#mensagem', erro.message);
    }
}

function desenharCabecalho(empresa, financas) {
    document.querySelector('#titulo-empresa').textContent = `Financas de ${empresa.nome}`;
    document.querySelector('#subtitulo-empresa').textContent =
        `${empresa.setorRotulo} | patrimonio livre ${Formato.dinheiroCurto(financas.patrimonioLivre)} `
        + `| ${financas.contratos.length} contrato(s) em aberto`;
    document.querySelector('#link-empresa').href = `empresa.html?id=${empresa.id}`;

    if (empresa.donoId !== jogadorFinancas.id) {
        document.querySelectorAll('#formulario-contrato').forEach((no) => no.classList.add('oculto'));
        Interface.mensagem('#mensagem',
            'Voce nao controla esta empresa: a pagina esta em modo leitura.', 'erro');
    }
}

function desenharIndicadores(empresa, financas) {
    const avaliacao = financas.avaliacao;
    document.querySelector('#nota').textContent = avaliacao.nota;
    document.querySelector('#detalhe-nota').textContent =
        `${avaliacao.notaDescricao} | score ${Formato.numero(avaliacao.score, 0)}/100`;

    document.querySelector('#divida').textContent = Formato.dinheiroCurto(empresa.divida);
    document.querySelector('#detalhe-alavancagem').textContent =
        `${Formato.numero(avaliacao.alavancagem, 2)}x o patrimonio liquido`;

    document.querySelector('#servico-divida').textContent =
        Formato.dinheiroCurto(financas.servicoDaDividaMensal);
    const juros = financas.contratos.reduce((soma, contrato) => soma + contrato.jurosDoMes, 0);
    document.querySelector('#detalhe-juros').textContent =
        `juros ${Formato.dinheiroCurto(juros)} | Selic ${Formato.percentual(avaliacao.taxaBasica, 1)}`;

    document.querySelector('#caixa').textContent = Formato.dinheiroCurto(empresa.caixa);
    document.querySelector('#detalhe-patrimonio').textContent =
        `patrimonio ${Formato.dinheiroCurto(empresa.patrimonio)}`;
}

/*
 * O score aparece decomposto porque a nota sozinha nao diz o que arrumar:
 * cada barra mostra quanto aquele criterio esta entregando dos 100 pontos.
 */
function desenharAvaliacao(avaliacao) {
    const rotulos = {
        alavancagem: 'Alavancagem (peso 35%)',
        coberturaDeJuros: 'Cobertura de juros (peso 30%)',
        lastro: 'Lastro patrimonial (peso 15%)',
        historicoDePagamento: 'Historico de pagamento (peso 20%)'
    };
    document.querySelector('#componentes-score').innerHTML = Object.entries(avaliacao.componentes)
        .map(([chave, pontos]) => `
            <div class="espacado">
                <div class="cartao-cabecalho">
                    <span class="pequeno">${rotulos[chave] || chave}</span>
                    <span class="pequeno suave">${Formato.numero(pontos, 0)}/100</span>
                </div>
                <div class="barra"><span style="width:${Math.max(Math.min(pontos, 100), 0)}%"></span></div>
            </div>`).join('');

    // Cobertura negativa significa prejuizo: dizer "-23x" confundiria mais que ajudar.
    const cobertura = avaliacao.coberturaDeJuros;
    document.querySelector('#legenda-score').textContent = cobertura <= 0
        ? 'o resultado do turno nao cobre os juros'
        : `o lucro cobre ${Formato.numero(Math.min(cobertura, 99), 1)}x os juros`;

    const observacoes = avaliacao.observacoes || [];
    document.querySelector('#observacoes').innerHTML = observacoes.length
        ? observacoes.map((texto) => `<li>${texto}</li>`).join('')
        : '<li>Nenhum ponto de atencao no credito desta empresa.</li>';
}

function desenharLinhas(linhas) {
    document.querySelector('#lista-linhas').innerHTML = linhas.map((linha) => `
        <tr>
            <td>${linha.rotulo}</td>
            <td class="suave pequeno">${linha.descricao}</td>
            <td class="direita">${Formato.percentual(linha.taxaAnual, 2)}</td>
            <td class="direita">${linha.prazoMinimo} a ${linha.prazoMaximo}</td>
            <td class="direita">${Formato.dinheiroCurto(linha.limiteDisponivel)}</td>
            <td>${linha.exigeGarantia ? '1,3x em patrimonio' : 'sem garantia'}</td>
        </tr>`).join('');

    document.querySelector('#modalidade').innerHTML = linhas
        .filter((linha) => linha.contratavel)
        .map((linha) => `<option value="${linha.modalidade}">${linha.rotulo}</option>`)
        .join('');
}

function desenharContratos(contratos) {
    const corpo = document.querySelector('#lista-contratos');
    if (!contratos.length) {
        corpo.innerHTML = '<tr><td colspan="8" class="suave">A empresa nao tem divida contratada.</td></tr>';
        return;
    }
    const ehDono = empresaFinancas.donoId === jogadorFinancas.id;
    corpo.innerHTML = contratos.map((contrato) => `
        <tr>
            <td>${contrato.modalidadeRotulo}</td>
            <td><span class="etiqueta">${contrato.statusRotulo}</span>
                ${contrato.parcelasEmAtraso > 0
                    ? `<span class="negativo pequeno">${contrato.parcelasEmAtraso} em atraso</span>` : ''}</td>
            <td class="direita">${Formato.dinheiro(contrato.saldoDevedor)}</td>
            <td class="direita">${Formato.percentual(contrato.taxaAnual, 2)}</td>
            <td class="direita">${Formato.dinheiro(contrato.parcelaDoMes)}</td>
            <td class="direita">${contrato.turnosRestantes}</td>
            <td class="direita">${contrato.garantia > 0 ? Formato.dinheiroCurto(contrato.garantia) : '-'}</td>
            <td class="direita">${ehDono ? `
                <button class="botao botao-neutro botao-pequeno" data-amortizar="${contrato.id}">Amortizar</button>
                <button class="botao botao-neutro botao-pequeno" data-renegociar="${contrato.id}">Renegociar</button>` : ''}</td>
        </tr>`).join('');

    corpo.querySelectorAll('[data-amortizar]').forEach((botao) => {
        botao.addEventListener('click', () => amortizar(botao.dataset.amortizar));
    });
    corpo.querySelectorAll('[data-renegociar]').forEach((botao) => {
        botao.addEventListener('click', () => renegociar(botao.dataset.renegociar));
    });
}

function desenharEncerrados(historico) {
    const encerrados = historico.filter((contrato) =>
        contrato.status === 'QUITADO' || contrato.status === 'EXECUTADO');
    const corpo = document.querySelector('#lista-encerrados');
    if (!encerrados.length) {
        corpo.innerHTML = '<tr><td colspan="6" class="suave">Nenhum contrato encerrado ate aqui.</td></tr>';
        return;
    }
    corpo.innerHTML = encerrados.map((contrato) => `
        <tr>
            <td>${contrato.modalidadeRotulo}</td>
            <td><span class="etiqueta">${contrato.statusRotulo}</span></td>
            <td class="direita">${Formato.dinheiroCurto(contrato.principal)}</td>
            <td class="direita">${Formato.dinheiroCurto(contrato.jurosPagos)}</td>
            <td class="direita">${Formato.dinheiroCurto(contrato.amortizado)}</td>
            <td class="direita">${contrato.turnoContratacao}</td>
        </tr>`).join('');
}

/* Simula a primeira parcela antes de assinar: e o numero que decide o prazo. */
function simular() {
    const alvo = document.querySelector('#simulacao-contrato');
    if (!alvo || !vitrineAtual) return;
    const modalidade = document.querySelector('#modalidade').value;
    const linha = vitrineAtual.linhas.find((item) => item.modalidade === modalidade);
    if (!linha) return;
    const valor = Number(document.querySelector('#valor-contrato').value || 0);
    const prazo = Math.max(Number(document.querySelector('#prazo-contrato').value || 1), 1);
    const taxaMensal = linha.taxaAnual / 12;
    const primeiraParcela = valor / prazo + valor * taxaMensal;
    const ultimaParcela = valor / prazo + (valor / prazo) * taxaMensal;
    const jurosTotais = valor * taxaMensal * (prazo + 1) / 2;

    alvo.textContent = `Taxa ${Formato.percentual(linha.taxaAnual, 2)} ao ano | `
        + `primeira parcela ${Formato.dinheiro(primeiraParcela)} | `
        + `ultima ${Formato.dinheiro(ultimaParcela)} | `
        + `juros no total ${Formato.dinheiro(jurosTotais)} | `
        + `limite ${Formato.dinheiroCurto(linha.limiteDisponivel)}`
        + (linha.exigeGarantia ? ` | garantia ${Formato.dinheiro(valor * 1.3)}` : '');
}

async function executar(acao) {
    Interface.mensagem('#mensagem', '');
    try {
        await acao();
        await carregarFinancas();
        await Interface.montarCabecalho();
    } catch (erro) {
        Interface.mensagem('#mensagem', erro.message);
    }
}

function amortizar(contratoId) {
    const valor = Number(window.prompt('Quanto amortizar agora? (R$)', '50000'));
    if (!valor) return;
    executar(async () => {
        await API.post(`/empresas/${idEmpresaFinancas}/financas/contratos/${contratoId}/amortizar`,
            { jogadorId: jogadorFinancas.id, valor });
        Interface.mensagem('#mensagem', 'Amortizacao registrada.', 'sucesso');
    });
}

function renegociar(contratoId) {
    const novoPrazo = Number(window.prompt('Novo prazo, em turnos (precisa ser maior que o atual)', '24'));
    if (!novoPrazo) return;
    executar(async () => {
        await API.post(`/empresas/${idEmpresaFinancas}/financas/contratos/${contratoId}/renegociar`,
            { jogadorId: jogadorFinancas.id, novoPrazo });
        Interface.mensagem('#mensagem',
            'Divida renegociada: prazo maior, taxa maior e comissao somada ao saldo.', 'sucesso');
    });
}

document.querySelector('#formulario-contrato').addEventListener('submit', (evento) => {
    evento.preventDefault();
    executar(async () => {
        await API.post(`/empresas/${idEmpresaFinancas}/financas/contratos`, {
            jogadorId: jogadorFinancas.id,
            modalidade: document.querySelector('#modalidade').value,
            valor: Number(document.querySelector('#valor-contrato').value),
            prazoTurnos: Number(document.querySelector('#prazo-contrato').value)
        });
        Interface.mensagem('#mensagem', 'Credito contratado: o valor ja esta no caixa.', 'sucesso');
    });
});

['#modalidade', '#valor-contrato', '#prazo-contrato'].forEach((seletor) => {
    document.querySelector(seletor).addEventListener('input', simular);
});

carregarFinancas();
