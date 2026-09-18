/*
 * Canal de atualizacoes: feed de fatos publicos do mundo mais as acoes do
 * proprio jogador. Consome /api/atualizacoes, que ja devolve o conteudo
 * filtrado e sem dado interno.
 */

const jogadorFeed = Sessao.exigir();
let atualizacoes = [];
let categoriaAtiva = 'TODAS';

async function carregar() {
    if (!jogadorFeed) return;
    try {
        atualizacoes = await API.get(`/atualizacoes?jogadorId=${jogadorFeed.id}&limite=60`);
        desenhar();
    } catch (erro) {
        Interface.mensagem('#mensagem', erro.message);
    }
}

function desenhar() {
    const lista = document.querySelector('#feed');
    const filtradas = atualizacoes.filter((item) => {
        if (categoriaAtiva === 'TODAS') return true;
        if (categoriaAtiva === 'MINHAS') return item.propria;
        return item.categoria === categoriaAtiva;
    });

    if (!filtradas.length) {
        lista.innerHTML = '<li class="suave">Nada por aqui ainda.</li>';
        return;
    }

    lista.innerHTML = filtradas.map((item) => `
        <li class="item-tempo">
            <div class="marca-tempo">
                <span class="etiqueta ${classeCategoria(item.categoria)}">${item.categoria}</span>
                <span class="suave pequeno">turno ${item.turno}</span>
            </div>
            <div class="conteudo-tempo">
                <strong>${item.titulo}</strong>
                ${item.propria ? '<span class="etiqueta etiqueta-primaria">sua acao</span>' : ''}
                <p class="pequeno">${item.mensagem || ''}</p>
                <span class="suave pequeno">${Formato.momento(item.momento)}</span>
            </div>
        </li>`).join('');
}

function classeCategoria(categoria) {
    if (categoria === 'ECONOMIA') return 'etiqueta-positiva';
    if (categoria === 'POLITICA') return 'etiqueta-primaria';
    return '';
}

document.querySelectorAll('.filtro').forEach((botao) => {
    botao.addEventListener('click', () => {
        document.querySelectorAll('.filtro').forEach((b) => b.classList.remove('ativo'));
        botao.classList.add('ativo');
        categoriaAtiva = botao.dataset.categoria;
        desenhar();
    });
});

carregar();
