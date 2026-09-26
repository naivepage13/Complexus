/*
 * Conteudo estatico do tutorial, agrupado por categoria macro. Cada categoria
 * vira um painel de accordion; o texto reflete as regras de docs/REGRAS-DO-JOGO.md.
 */

const CATEGORIAS = [
    {
        titulo: 'Primeiros passos',
        html: `
            <p>Cada turno equivale a uma hora real e avanca o calendario do jogo em
            um mes. O relogio no topo mostra o turno atual e a contagem para o
            proximo processamento.</p>
            <ul>
                <li>Comece pelo Painel: ele resume seu caixa, suas empresas, seus
                investimentos e seus mandatos.</li>
                <li>O canal de Atualizacoes mostra o que aconteceu no mundo a cada
                turno: leis aprovadas, empresas fundadas, dividendos pagos.</li>
                <li>Seu caixa pessoal financia tudo: fundar empresa, comprar acoes
                e fazer campanha politica saem dele.</li>
            </ul>`
    },
    {
        titulo: 'Empresas',
        html: `
            <p>Fundar uma empresa custa capital acima do minimo do setor
            escolhido: 70% desse valor vira patrimonio e 30% vira caixa da
            empresa.</p>
            <ul>
                <li><strong>Capacidade de producao</strong> e limitada pelo menor
                entre dois tetos: sua equipe (funcionarios x produtividade) e seu
                patrimonio (giro de ativo do setor). A tela da empresa mostra qual
                dos dois esta te travando.</li>
                <li><strong>Concorrencia</strong>: voce disputa mercado com outras
                empresas do mesmo municipio e setor. Patrimonio, produtividade,
                reputacao e marketing aumentam sua fatia.</li>
                <li><strong>Caixa negativo</strong> vira divida automatica com
                juros; divida acima de 2,5x o patrimonio encerra a empresa por
                insolvencia.</li>
                <li>Setores imobiliario e construcao tambem tem
                <strong>empreendimentos</strong>: obras com parcelas por turno que
                entram como patrimonio ao concluir.</li>
            </ul>`
    },
    {
        titulo: 'Investimentos',
        html: `
            <p>Empresas podem abrir capital e negociar acoes no mercado.</p>
            <ul>
                <li>Compre acoes no primario (direto da empresa) ou no mercado
                secundario (de outros investidores).</li>
                <li>O preco da acao segue o valuation da empresa, suavizado entre
                turnos para evitar saltos bruscos.</li>
                <li>Empresas com politica de dividendos pagam uma parcela do lucro
                aos acionistas a cada turno, proporcional a quantidade de acoes.</li>
                <li>Acompanhe sua carteira e o retorno total (dividendos mais
                valorizacao) na pagina de Investimentos.</li>
            </ul>`
    },
    {
        titulo: 'Politica',
        html: `
            <p>O Brasil simulado tem tres esferas de governo: federal, estadual e
            municipal, cada uma com cargos legislativos, executivos e nomeados.</p>
            <ul>
                <li><strong>Legislativo</strong> (senador, deputados, vereador):
                propoe e vota projetos de lei.</li>
                <li><strong>Executivo</strong> (presidente, governador, prefeito):
                sanciona ou veta o que o legislativo aprova.</li>
                <li>Se houver cadeira vaga, a posse e direta. Com todas ocupadas,
                voce desafia o NPC de menor aprovacao e vence se a aprovacao dele
                estiver abaixo de 45.</li>
                <li>Leis aprovadas tem efeito real: impostos, subsidios,
                regulacao, infraestrutura, programas sociais e zoneamento mudam a
                economia simulada no mesmo turno em que a lei entra em vigor.</li>
            </ul>`
    },
    {
        titulo: 'Mapa',
        html: `
            <p>O Mapa mostra o territorio em tres niveis de zoom: Estados,
            Cidades e Estradas.</p>
            <ul>
                <li>Clicar num estado leva as cidades daquele estado; clicar numa
                cidade abre o Painel de Decisao com PIB, infraestrutura e tropas.</li>
                <li>A aliquota estadual e o investimento em infraestrutura do
                estado sao herdados por todas as cidades filhas: mudar um valor no
                estado atualiza o PIB efetivo das cidades na hora.</li>
                <li>Estradas conectam cidades; bloquear uma rodovia reduz a
                infraestrutura efetiva das cidades ligadas a ela.</li>
            </ul>`
    }
];

function renderizar() {
    const lista = document.querySelector('#lista-tutorial');
    lista.innerHTML = CATEGORIAS.map((categoria, indice) => `
        <div class="accordion-item" data-indice="${indice}">
            <button type="button" class="accordion-cabecalho">
                <span>${categoria.titulo}</span>
                <span class="accordion-seta">&#9660;</span>
            </button>
            <div class="accordion-corpo oculto">${categoria.html}</div>
        </div>`).join('');

    lista.querySelectorAll('.accordion-cabecalho').forEach((botao) => {
        botao.addEventListener('click', () => {
            const item = botao.closest('.accordion-item');
            item.classList.toggle('aberto');
            item.querySelector('.accordion-corpo').classList.toggle('oculto');
        });
    });
}

document.addEventListener('DOMContentLoaded', () => {
    if (!Sessao.exigir()) return;
    renderizar();
});
