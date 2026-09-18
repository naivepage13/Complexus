/* Mercado de acoes, carteira do jogador e envio de ordens. */

const jogadorInvestidor = Sessao.exigir();
let mercadoAtual = [];

async function carregar() {
    if (!jogadorInvestidor) return;
    try {
        const [mercado, carteira, jogador] = await Promise.all([
            API.get('/investimentos/mercado'),
            API.get(`/investimentos/carteira?jogadorId=${jogadorInvestidor.id}`),
            API.get(`/jogadores/${jogadorInvestidor.id}`)
        ]);
        mercadoAtual = mercado;
        desenharMercado(mercado);
        desenharCarteira(carteira);
        desenharExtrato(carteira.extrato);
        preencherSelecao(mercado, carteira.posicoes);

        document.querySelector('#valor-carteira').textContent = Formato.dinheiroCurto(carteira.valorAtual);
        const retorno = document.querySelector('#retorno-carteira');
        retorno.textContent = Formato.dinheiroCurto(carteira.retornoTotal);
        retorno.className = `valor ${Formato.classe(carteira.retornoTotal)}`;
        document.querySelector('#caixa-investidor').textContent = Formato.dinheiroCurto(jogador.saldo);
    } catch (erro) {
        Interface.mensagem('#mensagem', erro.message);
    }
}

function desenharMercado(mercado) {
    const corpo = document.querySelector('#lista-mercado');
    if (!mercado.length) {
        corpo.innerHTML = '<tr><td colspan="8" class="suave">'
            + 'Nenhuma empresa com capital aberto. Abra o capital de uma empresa sua para negociar.</td></tr>';
        return;
    }
    corpo.innerHTML = mercado.map((linha) => `
        <tr>
            <td><a href="empresa.html?id=${linha.empresaId}">${linha.nome}</a></td>
            <td><span class="etiqueta">${linha.setorRotulo}</span></td>
            <td class="direita">${Formato.dinheiro(linha.precoAcao)}</td>
            <td class="direita">${Formato.inteiro(linha.acoesDisponiveis)}</td>
            <td class="direita ${Formato.classe(linha.lucroMensal)}">${Formato.dinheiroCurto(linha.lucroMensal)}</td>
            <td class="direita ${Formato.classe(linha.crescimentoLucro)}">${Formato.percentual(linha.crescimentoLucro)}</td>
            <td class="direita">${Formato.percentual(linha.indiceLastro, 0)}</td>
            <td class="direita">${Formato.dinheiro(linha.dividendoProjetadoPorAcao)}</td>
        </tr>`).join('');
}

function desenharCarteira(carteira) {
    const corpo = document.querySelector('#lista-carteira');
    if (!carteira.posicoes.length) {
        corpo.innerHTML = '<tr><td colspan="8" class="suave">Nenhuma posicao aberta.</td></tr>';
        return;
    }
    corpo.innerHTML = carteira.posicoes.map((posicao) => `
        <tr>
            <td><a href="empresa.html?id=${posicao.empresaId}">${posicao.empresa}</a></td>
            <td class="direita">${Formato.inteiro(posicao.acoes)}</td>
            <td class="direita">${Formato.dinheiro(posicao.precoMedio)}</td>
            <td class="direita">${Formato.dinheiro(posicao.precoAtual)}</td>
            <td class="direita">${Formato.dinheiroCurto(posicao.valorAtual)}</td>
            <td class="direita ${Formato.classe(posicao.resultadoNaoRealizado)}">${Formato.dinheiroCurto(posicao.resultadoNaoRealizado)}</td>
            <td class="direita">${Formato.dinheiroCurto(posicao.dividendosRecebidos)}</td>
            <td class="direita ${Formato.classe(posicao.retornoTotal)}">${Formato.dinheiroCurto(posicao.retornoTotal)}</td>
        </tr>`).join('');
}

function desenharExtrato(extrato) {
    const corpo = document.querySelector('#lista-extrato');
    if (!extrato || !extrato.length) {
        corpo.innerHTML = '<tr><td colspan="6" class="suave">Sem movimentacoes.</td></tr>';
        return;
    }
    corpo.innerHTML = extrato.map((linha) => `
        <tr>
            <td>${linha.turno}</td>
            <td><span class="etiqueta">${linha.tipoRotulo}</span></td>
            <td class="pequeno">${linha.origem}</td>
            <td class="pequeno">${linha.destino}</td>
            <td class="direita">${Formato.dinheiro(linha.valor)}</td>
            <td class="pequeno">${linha.descricao || '-'}</td>
        </tr>`).join('');
}

function preencherSelecao(mercado, posicoes) {
    const opcoes = new Map();
    mercado.forEach((linha) => opcoes.set(linha.empresaId, linha.nome));
    posicoes.forEach((posicao) => opcoes.set(posicao.empresaId, posicao.empresa));

    const selecao = document.querySelector('#empresa-ordem');
    selecao.innerHTML = [...opcoes.entries()]
        .map(([id, nome]) => `<option value="${id}">${nome}</option>`)
        .join('');
    atualizarResumo();
}

function atualizarResumo() {
    const empresaId = Number(document.querySelector('#empresa-ordem').value);
    const quantidade = Number(document.querySelector('#quantidade-ordem').value);
    const empresa = mercadoAtual.find((linha) => linha.empresaId === empresaId);
    const resumo = document.querySelector('#resumo-ordem');
    if (!empresa) {
        resumo.textContent = '';
        return;
    }
    const total = empresa.precoAcao * quantidade;
    resumo.textContent = `Preco de referencia ${Formato.dinheiro(empresa.precoAcao)} `
        + `| valor estimado da ordem ${Formato.dinheiro(total)} (mais 0,5% de custo de transacao).`;
}

document.querySelector('#empresa-ordem').addEventListener('change', atualizarResumo);
document.querySelector('#quantidade-ordem').addEventListener('input', atualizarResumo);

async function enviarOrdem(caminho, rotulo) {
    Interface.mensagem('#mensagem', '');
    try {
        await API.post(caminho, {
            jogadorId: jogadorInvestidor.id,
            empresaId: Number(document.querySelector('#empresa-ordem').value),
            quantidade: Number(document.querySelector('#quantidade-ordem').value)
        });
        Interface.mensagem('#mensagem', `${rotulo} executada com sucesso.`, 'sucesso');
        await carregar();
        await Interface.montarCabecalho();
    } catch (erro) {
        Interface.mensagem('#mensagem', erro.message);
    }
}

document.querySelector('#botao-comprar')
    .addEventListener('click', () => enviarOrdem('/investimentos/comprar', 'Compra'));
document.querySelector('#botao-vender')
    .addEventListener('click', () => enviarOrdem('/investimentos/vender', 'Venda'));

carregar();
