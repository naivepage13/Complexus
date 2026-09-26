/*
 * Lista de versoes entregues (ver CHANGELOG.md), com estado de leitura
 * persistido no navegador. Envelope fechado com brilho = nao lido; abrir o
 * card marca como lido e troca para o envelope aberto e neutro.
 */

const CHAVE_LIDAS = 'complexus.novidades.lidas';

const ICONE_ENVELOPE_FECHADO = `
    <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
        <rect x="3" y="5" width="18" height="14" rx="2"></rect>
        <path d="M3 7l9 6 9-6"></path>
    </svg>`;

const ICONE_ENVELOPE_ABERTO = `
    <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
        <path d="M3 8l9-5 9 5"></path>
        <rect x="3" y="8" width="18" height="11" rx="2"></rect>
        <path d="M3 9.5l9 5 9-5"></path>
    </svg>`;

const VERSOES = [
    {
        versao: '0.6.0',
        titulo: 'Organizacao do repositorio',
        data: '25/09/2026',
        itens: [
            'Pasta legado/ removida: os HTML e scripts do prototipo original ja nao sao usados.',
            'Corrigida a divergencia de versao do backend, que tinha ficado presa em 0.4.1.',
            'IniciarComplexus.bat agora sobe o backend de verdade em vez do antigo servidor estatico.',
            'Raiz do projeto renomeada para Complexus.'
        ]
    },
    {
        versao: '0.5.0',
        titulo: 'Integracao do combate e do mapa',
        data: '24/09/2026',
        itens: [
            'Motor de combate por fases (inteligencia, ar, golpe profundo, mar, terra) incorporado ao repositorio.',
            'Mapa interativo hierarquico disponivel em /mapa.html, com zoom semantico e camadas de estado, cidade e estrada.',
            'Leaflet passou a ser servido localmente (vendor/), sem depender de CDN, para o jogo funcionar offline.',
            'Paleta do mapa unificada com o tema claro do restante do jogo.'
        ]
    },
    {
        versao: '0.4.1',
        titulo: 'Inicializacao com um clique',
        data: '24/09/2026',
        itens: [
            'IniciarComplexus.bat sobe o servico analitico e o backend e abre o navegador sozinho.',
            'O script procura automaticamente um JDK 21 ou superior instalado na maquina.'
        ]
    },
    {
        versao: '0.4.0',
        titulo: 'Documentacao que se mantem sozinha',
        data: '21/09/2026',
        itens: [
            'Novo catalogo de requisitos (docs/requisitos.toml) com 34 requisitos rastreados.',
            'Gerador de documentacao que le o codigo e recusa o commit se algo ficar desatualizado.',
            'Relatorio de requisitos e inventario tecnico passaram a ser gerados, nao escritos a mao.'
        ]
    },
    {
        versao: '0.2.1',
        titulo: 'Separacao entre o que o jogador ve e o que fica na auditoria',
        data: '18/09/2026',
        itens: [
            'Novo canal de atualizacoes (/api/atualizacoes) com os fatos publicos do jogo e as acoes do proprio jogador.',
            'Rotas administrativas passaram a exigir o cabecalho X-Admin-Token.',
            'Painel inicial deixou de mostrar relatorio tecnico e ganhou atalhos e o canal de atualizacoes.'
        ]
    }
];

function lerLidas() {
    try {
        return JSON.parse(localStorage.getItem(CHAVE_LIDAS) || '[]');
    } catch (erro) {
        return [];
    }
}

function marcarComoLida(versao) {
    const lidas = lerLidas();
    if (!lidas.includes(versao)) {
        lidas.push(versao);
        localStorage.setItem(CHAVE_LIDAS, JSON.stringify(lidas));
    }
}

function renderizar() {
    const lidas = lerLidas();
    const lista = document.querySelector('#lista-novidades');
    lista.innerHTML = VERSOES.map((release) => {
        const lida = lidas.includes(release.versao);
        const envelope = lida
            ? `<span class="envelope envelope-lido" title="Lida">${ICONE_ENVELOPE_ABERTO}</span>`
            : `<span class="envelope envelope-nao-lido" title="Nao lida">${ICONE_ENVELOPE_FECHADO}</span>`;
        const itens = release.itens.map((item) => `<li>${item}</li>`).join('');
        return `
            <article class="cartao cartao-novidade" data-versao="${release.versao}">
                <div class="novidade-cabecalho">
                    <div class="novidade-titulo">
                        <span class="etiqueta etiqueta-primaria">v${release.versao}</span>
                        <strong>${release.titulo}</strong>
                        <span class="suave pequeno">${release.data}</span>
                    </div>
                    ${envelope}
                </div>
                <div class="novidade-corpo oculto">
                    <ul>${itens}</ul>
                </div>
            </article>`;
    }).join('');

    lista.querySelectorAll('.cartao-novidade').forEach((card) => {
        card.addEventListener('click', () => {
            const versao = card.dataset.versao;
            const corpo = card.querySelector('.novidade-corpo');
            corpo.classList.toggle('oculto');
            if (!lerLidas().includes(versao)) {
                marcarComoLida(versao);
                const envelope = card.querySelector('.envelope');
                envelope.outerHTML = `<span class="envelope envelope-lido" title="Lida">${ICONE_ENVELOPE_ABERTO}</span>`;
            }
        });
    });
}

document.addEventListener('DOMContentLoaded', () => {
    if (!Sessao.exigir()) return;
    renderizar();
});
