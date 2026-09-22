/* Cadeia produtiva: propostas, contratos em vigor e historico de fornecimento. */

const jogadorCadeia = Sessao.exigir();
const idEmpresaCadeia = new URLSearchParams(window.location.search).get('id');
let empresaCadeia = null;
let panorama = null;

async function carregarCadeia() {
    if (!jogadorCadeia || !idEmpresaCadeia) {
        window.location.href = 'empresas.html';
        return;
    }
    try {
        const [empresa, dados] = await Promise.all([
            API.get(`/empresas/${idEmpresaCadeia}`),
            API.get(`/empresas/${idEmpresaCadeia}/fornecimento`)
        ]);
        empresaCadeia = empresa;
        panorama = dados;
        desenharCabecalho(empresa, dados);
        desenharIndicadores(dados);
        desenharPropostas(dados.contratos);
        desenharContratos(dados.contratos);
        desenharHistorico(dados.contratos);
        await preencherTipos();
    } catch (erro) {
        Interface.mensagem('#mensagem', erro.message);
    }
}

function desenharCabecalho(empresa, dados) {
    document.querySelector('#titulo-empresa').textContent = `Fornecimento de ${empresa.nome}`;
    const fornece = dados.podeFornecer.map((tipo) => tipo.rotulo).join(', ') || 'nada';
    const compra = dados.podeComprar.map((tipo) => tipo.rotulo).join(', ') || 'nada';
    document.querySelector('#subtitulo-empresa').textContent =
        `${empresa.setorRotulo} | fornece: ${fornece} | compra: ${compra}`;
    document.querySelector('#link-empresa').href = `empresa.html?id=${empresa.id}`;

    if (empresa.donoId !== jogadorCadeia.id) {
        document.querySelector('#area-proposta').classList.add('oculto');
        Interface.mensagem('#mensagem',
            'Voce nao controla esta empresa: a pagina esta em modo leitura.', 'erro');
    }
}

function desenharIndicadores(dados) {
    document.querySelector('#capacidade-livre').textContent =
        Formato.dinheiroCurto(dados.capacidadeLivreParaContrato);
    document.querySelector('#insumo-livre').textContent =
        Formato.dinheiroCurto(dados.insumoLivreParaContrato);

    const vigentes = dados.contratos.filter((contrato) => contrato.status === 'ATIVO');
    const fornecendo = vigentes.filter((contrato) => ehFornecedor(contrato));
    const comprando = vigentes.filter((contrato) => !ehFornecedor(contrato));

    const faturamento = fornecendo.reduce((soma, contrato) => soma + contrato.faturamentoMensal, 0);
    document.querySelector('#fornecendo').textContent = Formato.dinheiroCurto(faturamento);
    document.querySelector('#detalhe-fornecendo').textContent =
        `${fornecendo.length} contrato(s) ocupando capacidade`;

    const compra = comprando.reduce((soma, contrato) => soma + contrato.faturamentoMensal, 0);
    const economia = comprando.reduce((soma, contrato) => soma + contrato.economiaMensalDoComprador, 0);
    document.querySelector('#comprando').textContent = Formato.dinheiroCurto(compra);
    const detalhe = document.querySelector('#detalhe-comprando');
    detalhe.textContent = economia >= 0
        ? `economia de ${Formato.dinheiroCurto(economia)} por turno`
        : `premio de ${Formato.dinheiroCurto(-economia)} por turno`;
    detalhe.className = `detalhe ${Formato.classe(economia)}`;
}

function ehFornecedor(contrato) {
    return contrato.fornecedorId === Number(idEmpresaCadeia);
}

function papel(contrato) {
    return ehFornecedor(contrato) ? 'Fornecendo' : 'Comprando';
}

function contraparte(contrato) {
    return ehFornecedor(contrato)
        ? `${contrato.comprador}${contrato.compradorDoSistema ? ' (sistema)' : ''}`
        : `${contrato.fornecedor}${contrato.fornecedorDoSistema ? ' (sistema)' : ''}`;
}

/* Quem responde e a parte que nao propos: so ela ve os botoes de aceitar e recusar. */
function desenharPropostas(contratos) {
    const propostas = contratos.filter((contrato) => contrato.status === 'PROPOSTO');
    const corpo = document.querySelector('#lista-propostas');
    if (!propostas.length) {
        corpo.innerHTML = '<tr><td colspan="7" class="suave">Nenhuma proposta aberta.</td></tr>';
        return;
    }
    corpo.innerHTML = propostas.map((contrato) => {
        const souORespondente = contrato.propostoPor === 'FORNECEDOR'
            ? !ehFornecedor(contrato)
            : ehFornecedor(contrato);
        const acoes = souORespondente
            ? `<button class="botao botao-pequeno" data-aceitar="${contrato.id}">Aceitar</button>
               <button class="botao botao-neutro botao-pequeno" data-recusar="${contrato.id}">Recusar</button>`
            : '<span class="suave pequeno">aguardando resposta</span>';
        return `
        <tr>
            <td>${contrato.tipoRotulo}</td>
            <td>${papel(contrato)}</td>
            <td>${contraparte(contrato)}</td>
            <td class="direita">${Formato.dinheiroCurto(contrato.volumeMensal)}</td>
            <td class="direita">${Formato.numero(contrato.precoRelativo, 2)}x</td>
            <td class="direita">${contrato.prazoTurnos}</td>
            <td class="direita">${acoes}</td>
        </tr>`;
    }).join('');

    corpo.querySelectorAll('[data-aceitar]').forEach((botao) => {
        botao.addEventListener('click', () => responder(botao.dataset.aceitar, 'aceitar'));
    });
    corpo.querySelectorAll('[data-recusar]').forEach((botao) => {
        botao.addEventListener('click', () => responder(botao.dataset.recusar, 'recusar'));
    });
}

function desenharContratos(contratos) {
    const vigentes = contratos.filter((contrato) => contrato.status === 'ATIVO');
    const corpo = document.querySelector('#lista-contratos');
    if (!vigentes.length) {
        corpo.innerHTML = '<tr><td colspan="8" class="suave">Nenhum contrato em vigor.</td></tr>';
        return;
    }
    const ehDono = empresaCadeia.donoId === jogadorCadeia.id;
    corpo.innerHTML = vigentes.map((contrato) => {
        const efeito = ehFornecedor(contrato)
            ? `+${Formato.dinheiroCurto(contrato.faturamentoMensal)} de receita`
            : `${contrato.economiaMensalDoComprador >= 0 ? '-' : '+'}`
              + `${Formato.dinheiroCurto(Math.abs(contrato.economiaMensalDoComprador))} de custo`;
        return `
        <tr>
            <td>${contrato.tipoRotulo}</td>
            <td>${papel(contrato)}</td>
            <td>${contraparte(contrato)}</td>
            <td class="direita">${Formato.dinheiroCurto(contrato.volumeMensal)}</td>
            <td class="direita">${Formato.numero(contrato.precoRelativo, 2)}x</td>
            <td class="direita">${efeito}</td>
            <td class="direita">${contrato.turnosRestantes}
                ${contrato.falhasDeEntrega > 0
                    ? `<span class="negativo pequeno">${contrato.falhasDeEntrega} falha(s)</span>` : ''}</td>
            <td class="direita">${ehDono
                ? `<button class="botao botao-neutro botao-pequeno" data-romper="${contrato.id}">Romper</button>`
                : ''}</td>
        </tr>`;
    }).join('');

    corpo.querySelectorAll('[data-romper]').forEach((botao) => {
        botao.addEventListener('click', () => romper(botao.dataset.romper));
    });
}

function desenharHistorico(contratos) {
    const encerrados = contratos.filter((contrato) =>
        ['CONCLUIDO', 'ROMPIDO', 'RECUSADO'].includes(contrato.status));
    const corpo = document.querySelector('#lista-historico');
    if (!encerrados.length) {
        corpo.innerHTML = '<tr><td colspan="6" class="suave">Nada encerrado ate aqui.</td></tr>';
        return;
    }
    corpo.innerHTML = encerrados.map((contrato) => `
        <tr>
            <td>${contrato.tipoRotulo}</td>
            <td>${papel(contrato)}</td>
            <td>${contraparte(contrato)}</td>
            <td><span class="etiqueta">${contrato.statusRotulo}</span></td>
            <td class="direita">${Formato.dinheiroCurto(contrato.totalFaturado)}</td>
            <td class="direita">${contrato.falhasDeEntrega}</td>
        </tr>`).join('');
}

async function preencherTipos() {
    const fornecer = document.querySelector('#papel').value === 'fornecer';
    const tipos = fornecer ? panorama.podeFornecer : panorama.podeComprar;
    const seletor = document.querySelector('#tipo-insumo');
    seletor.innerHTML = tipos.length
        ? tipos.map((tipo) => `<option value="${tipo.nome}">${tipo.rotulo}</option>`).join('')
        : '<option value="">Nenhum insumo disponivel para este setor</option>';
    await carregarParceiros();
}

async function carregarParceiros() {
    const tipo = document.querySelector('#tipo-insumo').value;
    const seletor = document.querySelector('#contraparte');
    if (!tipo) {
        seletor.innerHTML = '';
        simular([]);
        return;
    }
    const fornecer = document.querySelector('#papel').value === 'fornecer';
    try {
        const parceiros = await API.get(
            `/empresas/${idEmpresaCadeia}/fornecimento/parceiros?tipo=${tipo}&comoFornecedor=${fornecer}`);
        seletor.innerHTML = parceiros.length
            ? parceiros.map((parceiro) => {
                const espaco = fornecer
                    ? parceiro.insumoDisponivelParaContrato
                    : parceiro.capacidadeLivreParaContrato;
                return `<option value="${parceiro.empresaId}" data-espaco="${espaco}">`
                    + `${parceiro.nome}${parceiro.doSistema ? ' (sistema)' : ''} - `
                    + `${parceiro.municipio}, cabe ${Formato.dinheiroCurto(espaco)}</option>`;
            }).join('')
            : '<option value="">Nenhuma empresa compativel no momento</option>';
        simular(parceiros);
    } catch (erro) {
        Interface.mensagem('#mensagem', erro.message);
    }
}

/*
 * A simulacao mostra o teto dos dois lados antes de enviar: o proprio limite e
 * o espaco que sobra na contraparte. Sem isso a recusa so apareceria no envio.
 */
function simular() {
    const alvo = document.querySelector('#simulacao');
    if (!alvo || !panorama) return;
    const fornecer = document.querySelector('#papel').value === 'fornecer';
    const volume = Number(document.querySelector('#volume').value || 0);
    const preco = Number(document.querySelector('#preco').value || 1);
    const prazo = Number(document.querySelector('#prazo').value || 0);
    const opcao = document.querySelector('#contraparte').selectedOptions[0];
    const espacoParceiro = opcao ? Number(opcao.dataset.espaco || 0) : 0;
    const meuLimite = fornecer ? panorama.capacidadeLivreParaContrato : panorama.insumoLivreParaContrato;

    const efeito = fornecer
        ? `receita de ${Formato.dinheiro(volume * preco)} por turno, ocupando ${Formato.dinheiroCurto(volume)} de capacidade`
        : `custo de ${Formato.dinheiro(volume * preco)} por turno, `
          + `${preco <= 1 ? 'economizando' : 'pagando a mais'} `
          + `${Formato.dinheiro(Math.abs(volume * (1 - preco)))} contra o insumo de mercado`;

    alvo.textContent = `${efeito}. Total no prazo: ${Formato.dinheiro(volume * preco * prazo)}. `
        + `Seu limite: ${Formato.dinheiroCurto(meuLimite)} | espaco da contraparte: `
        + `${Formato.dinheiroCurto(espacoParceiro)}.`;
}

async function executar(acao) {
    Interface.mensagem('#mensagem', '');
    try {
        await acao();
        await carregarCadeia();
        await Interface.montarCabecalho();
    } catch (erro) {
        Interface.mensagem('#mensagem', erro.message);
    }
}

function responder(contratoId, acao) {
    executar(async () => {
        const contrato = await API.post(
            `/empresas/${idEmpresaCadeia}/fornecimento/contratos/${contratoId}/${acao}`,
            { jogadorId: jogadorCadeia.id });
        Interface.mensagem('#mensagem',
            acao === 'aceitar' ? 'Contrato em vigor a partir deste turno.' : 'Proposta recusada.',
            acao === 'aceitar' ? 'sucesso' : 'erro');
        return contrato;
    });
}

function romper(contratoId) {
    executar(async () => {
        const resumo = await API.post(
            `/empresas/${idEmpresaCadeia}/fornecimento/contratos/${contratoId}/romper`,
            { jogadorId: jogadorCadeia.id });
        Interface.mensagem('#mensagem',
            `Contrato rompido. Multa de ${Formato.dinheiro(resumo.multa)} paga a ${resumo.indenizada}.`,
            'sucesso');
    });
}

document.querySelector('#formulario-proposta').addEventListener('submit', (evento) => {
    evento.preventDefault();
    executar(async () => {
        const contrato = await API.post(`/empresas/${idEmpresaCadeia}/fornecimento/propostas`, {
            jogadorId: jogadorCadeia.id,
            contraparteId: Number(document.querySelector('#contraparte').value),
            tipo: document.querySelector('#tipo-insumo').value,
            comoFornecedor: document.querySelector('#papel').value === 'fornecer',
            volumeMensal: Number(document.querySelector('#volume').value),
            precoRelativo: Number(document.querySelector('#preco').value),
            prazoTurnos: Number(document.querySelector('#prazo').value)
        });
        const resposta = contrato.status === 'ATIVO'
            ? 'Proposta aceita na hora: o contrato ja esta em vigor.'
            : contrato.status === 'RECUSADO'
                ? 'A contraparte recusou o preco proposto.'
                : 'Proposta enviada. Falta a contraparte responder.';
        Interface.mensagem('#mensagem', resposta,
            contrato.status === 'RECUSADO' ? 'erro' : 'sucesso');
    });
});

document.querySelector('#papel').addEventListener('change', preencherTipos);
document.querySelector('#tipo-insumo').addEventListener('change', carregarParceiros);
['#volume', '#preco', '#prazo', '#contraparte'].forEach((seletor) => {
    document.querySelector(seletor).addEventListener('input', () => simular());
});

carregarCadeia();
