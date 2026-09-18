/*
 * Painel inicial.
 *
 * Mostra apenas orientacao: onde o jogador esta, quantos ativos ele tem e o
 * que aconteceu no mundo. Desempenho de empresa (lucro, margem, valuation)
 * fica na pagina da propria empresa, em grafico.
 */

const jogadorPainel = Sessao.exigir();

async function carregarPainel() {
    if (!jogadorPainel) return;
    try {
        const [painel, estado, feed] = await Promise.all([
            API.get(`/jogadores/${jogadorPainel.id}/painel`),
            API.get('/jogo/estado'),
            API.get(`/atualizacoes?jogadorId=${jogadorPainel.id}&limite=8`)
        ]);

        document.querySelector('#saudacao').textContent = `Ola, ${painel.jogador.nome}`;
        document.querySelector('#resumo-turno').textContent =
            `Turno ${estado.turnoAtual} | ${Formato.dataJogo(estado.dataJogo)} | `
            + `caixa disponivel ${Formato.dinheiroCurto(painel.jogador.saldo)}`;

        preencherAtalhos(painel);
        desenharFeed(feed);
    } catch (erro) {
        Interface.mensagem('#mensagem', erro.message);
    }
}

/** Os atalhos mostram contagem, nao resultado: servem para navegar. */
function preencherAtalhos(painel) {
    const empresas = painel.empresas.length;
    const posicoes = painel.carteira.length;
    const mandatos = painel.mandatos.length;

    document.querySelector('#atalho-empresas').textContent = empresas === 0
        ? 'funde a sua primeira empresa'
        : `${empresas} ${empresas === 1 ? 'empresa sua' : 'empresas suas'} para administrar`;

    document.querySelector('#atalho-carteira').textContent = posicoes === 0
        ? 'nenhuma posicao aberta'
        : `${posicoes} ${posicoes === 1 ? 'posicao aberta' : 'posicoes abertas'}`;

    document.querySelector('#atalho-mandatos').textContent = mandatos === 0
        ? 'assuma um cargo publico'
        : `${mandatos} ${mandatos === 1 ? 'mandato ativo' : 'mandatos ativos'}`;
}

function desenharFeed(feed) {
    const lista = document.querySelector('#feed');
    if (!feed.length) {
        lista.innerHTML = '<li class="suave">Nenhuma atualizacao ainda.</li>';
        return;
    }
    lista.innerHTML = feed.map((item) => `
        <li class="item-tempo">
            <div class="marca-tempo">
                <span class="etiqueta">${item.categoria}</span>
                <span class="suave pequeno">turno ${item.turno}</span>
            </div>
            <div class="conteudo-tempo">
                <strong>${item.titulo}</strong>
                ${item.propria ? '<span class="etiqueta etiqueta-primaria">sua acao</span>' : ''}
                <p class="pequeno">${item.mensagem || ''}</p>
            </div>
        </li>`).join('');
}

document.querySelector('#avancar-turno').addEventListener('click', async (evento) => {
    const botao = evento.currentTarget;
    botao.disabled = true;
    Interface.mensagem('#mensagem', 'Processando turno...', 'sucesso');
    try {
        const relatorio = await API.post('/jogo/turno/avancar?origem=PAINEL');
        Interface.mensagem('#mensagem', `Turno ${relatorio.turno} processado.`, 'sucesso');
        await carregarPainel();
        await Interface.montarCabecalho();
    } catch (erro) {
        Interface.mensagem('#mensagem', erro.message);
    } finally {
        botao.disabled = false;
    }
});

carregarPainel();
