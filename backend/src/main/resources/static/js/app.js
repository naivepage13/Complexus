/*
 * Utilitarios compartilhados por todas as paginas.
 *
 * Responsabilidades: chamadas a API, sessao do jogador no navegador,
 * formatacao de numeros e o relogio do turno no cabecalho.
 */

const API = {
    async requisitar(caminho, opcoes = {}) {
        const resposta = await fetch(`/api${caminho}`, {
            headers: { 'Content-Type': 'application/json' },
            ...opcoes
        });
        const texto = await resposta.text();
        const corpo = texto ? JSON.parse(texto) : null;
        if (!resposta.ok) {
            throw new Error(corpo && corpo.mensagem ? corpo.mensagem : `Erro ${resposta.status}`);
        }
        return corpo;
    },
    get(caminho) {
        return API.requisitar(caminho);
    },
    post(caminho, corpo) {
        return API.requisitar(caminho, { method: 'POST', body: JSON.stringify(corpo || {}) });
    }
};

const Sessao = {
    chave: 'complexus.jogador',
    obter() {
        const bruto = localStorage.getItem(Sessao.chave);
        return bruto ? JSON.parse(bruto) : null;
    },
    salvar(jogador) {
        localStorage.setItem(Sessao.chave, JSON.stringify(jogador));
    },
    encerrar() {
        localStorage.removeItem(Sessao.chave);
        window.location.href = 'login.html';
    },
    /** Redireciona para o login quando nao ha jogador identificado. */
    exigir() {
        const jogador = Sessao.obter();
        if (!jogador) {
            window.location.href = 'login.html';
            return null;
        }
        return jogador;
    }
};

const Formato = {
    dinheiro(valor) {
        if (valor === null || valor === undefined || Number.isNaN(valor)) {
            return '-';
        }
        return Number(valor).toLocaleString('pt-BR', {
            style: 'currency',
            currency: 'BRL',
            maximumFractionDigits: 2
        });
    },
    dinheiroCurto(valor) {
        const numero = Number(valor || 0);
        const absoluto = Math.abs(numero);
        if (absoluto >= 1e12) return `R$ ${(numero / 1e12).toFixed(2)} tri`;
        if (absoluto >= 1e9) return `R$ ${(numero / 1e9).toFixed(2)} bi`;
        if (absoluto >= 1e6) return `R$ ${(numero / 1e6).toFixed(2)} mi`;
        if (absoluto >= 1e3) return `R$ ${(numero / 1e3).toFixed(1)} mil`;
        return Formato.dinheiro(numero);
    },
    percentual(valor, casas = 2) {
        if (valor === null || valor === undefined || Number.isNaN(valor)) {
            return '-';
        }
        return `${(Number(valor) * 100).toFixed(casas)}%`;
    },
    numero(valor, casas = 2) {
        return Number(valor || 0).toLocaleString('pt-BR', {
            minimumFractionDigits: casas,
            maximumFractionDigits: casas
        });
    },
    inteiro(valor) {
        return Number(valor || 0).toLocaleString('pt-BR');
    },
    classe(valor) {
        if (valor > 0) return 'positivo';
        if (valor < 0) return 'negativo';
        return 'suave';
    },
    dataJogo(iso) {
        if (!iso) return '-';
        const [ano, mes] = iso.split('-');
        const meses = ['jan', 'fev', 'mar', 'abr', 'mai', 'jun',
            'jul', 'ago', 'set', 'out', 'nov', 'dez'];
        return `${meses[Number(mes) - 1]}/${ano}`;
    },
    momento(iso) {
        if (!iso) return '-';
        return new Date(iso).toLocaleString('pt-BR');
    }
};

const Validacao = {
    email(valor) {
        return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(String(valor || '').trim());
    }
};

/** Estrutura de navegacao: categoria -> paginas [rotulo, arquivo]. */
const CATEGORIAS = [
    { nome: 'Operacoes', itens: [['Painel', 'index.html'], ['Empresas', 'empresas.html']] },
    { nome: 'Mercado', itens: [['Investimentos', 'investimentos.html'], ['Estatisticas', 'estatisticas.html']] },
    { nome: 'Governo', itens: [['Politica', 'politica.html'], ['Mapa', 'mapa.html']] },
    { nome: 'Informacao', itens: [['Atualizacoes', 'atualizacoes.html']] },
    { nome: 'Conta', itens: [['Perfil', 'perfil.html'], ['Novidades', 'novidades.html'], ['Tutorial', 'tutorial.html'], ['Termos de uso', 'termos.html']] }
];

const Interface = {
    /** Escreve uma mensagem de erro ou sucesso no elemento indicado. */
    mensagem(seletor, texto, tipo = 'erro') {
        const alvo = document.querySelector(seletor);
        if (!alvo) return;
        if (!texto) {
            alvo.className = 'oculto';
            alvo.textContent = '';
            return;
        }
        alvo.className = `mensagem mensagem-${tipo}`;
        alvo.textContent = texto;
    },
    /**
     * Monta a navegacao em duas faixas: categorias e paginas da categoria
     * aberta. O menu plano que cada HTML traz (e a engrenagem de
     * configuracoes) e substituido aqui, para a estrutura viver num so lugar.
     */
    marcarMenu() {
        const cabecalho = document.querySelector('.cabecalho');
        const menuAntigo = cabecalho && cabecalho.querySelector('.menu a[href="index.html"]');
        if (!menuAntigo) return; // login e paginas de admin mantem o cabecalho proprio

        const pagina = window.location.pathname.split('/').pop() || 'index.html';
        const alias = { 'empresa.html': 'empresas.html' };
        const alvo = alias[pagina] || pagina;
        const atual = CATEGORIAS.find((c) => c.itens.some((i) => i[1] === alvo)) || CATEGORIAS[0];

        cabecalho.querySelector('.menu').remove();
        const engrenagem = cabecalho.querySelector('.config-menu');
        if (engrenagem) engrenagem.remove();

        const link = (rotulo, href, ativo) =>
            `<a href="${href}"${ativo ? ' class="ativo" aria-current="page"' : ''}>${rotulo}</a>`;
        const faixas = document.createElement('div');
        faixas.className = 'navegacao';
        faixas.innerHTML = `
            <nav class="categorias" aria-label="Categorias">
                <div class="navegacao-conteudo">${CATEGORIAS.map((c) =>
                    `<a href="${c.itens[0][1]}"${c === atual ? ' class="ativo" aria-current="true"' : ''}>${c.nome}</a>`
                ).join('')}</div>
            </nav>
            <nav class="subnav" aria-label="Paginas de ${atual.nome}">
                <div class="navegacao-conteudo">${atual.itens.map((i) => link(i[0], i[1], i[1] === alvo)).join('')}</div>
            </nav>`;
        cabecalho.appendChild(faixas);

        const principal = document.querySelector('main');
        if (principal) {
            if (!principal.id) principal.id = 'conteudo';
            const pular = document.createElement('a');
            pular.className = 'pular';
            pular.href = `#${principal.id}`;
            pular.textContent = 'Pular para o conteudo';
            document.body.prepend(pular);
        }
    },
    /** Mostra nome e saldo do jogador e liga o botao de sair. */
    async montarCabecalho() {
        Interface.marcarMenu();
        const jogador = Sessao.obter();
        const alvo = document.querySelector('#identificacao');
        if (alvo && jogador) {
            try {
                const atual = await API.get(`/jogadores/${jogador.id}`);
                Sessao.salvar(atual);
                alvo.innerHTML = `<span>${atual.nome}</span>
                    <span class="etiqueta etiqueta-primaria">${Formato.dinheiroCurto(atual.saldo)}</span>
                    <button class="botao botao-neutro botao-pequeno" id="sair">Sair</button>`;
                document.querySelector('#sair').addEventListener('click', Sessao.encerrar);
            } catch (erro) {
                alvo.textContent = jogador.nome;
            }
        }
        await Interface.montarRelogio();
    },
    /** Exibe turno corrente, data do jogo e contagem para o proximo turno. */
    async montarRelogio() {
        const alvo = document.querySelector('#relogio-turno');
        if (!alvo) return;
        try {
            const estado = await API.get('/jogo/estado');
            let restante = estado.segundosParaProximoTurno;
            const desenhar = () => {
                const minutos = String(Math.floor(restante / 60)).padStart(2, '0');
                const segundos = String(Math.floor(restante % 60)).padStart(2, '0');
                alvo.innerHTML = `<span class="etiqueta">Turno ${estado.turnoAtual}</span>
                    <span class="etiqueta">${Formato.dataJogo(estado.dataJogo)}</span>
                    <span class="etiqueta">proximo em ${minutos}:${segundos}</span>`;
            };
            desenhar();
            setInterval(() => {
                restante = restante > 0 ? restante - 1 : 0;
                desenhar();
            }, 1000);
        } catch (erro) {
            alvo.textContent = 'Servidor indisponivel';
        }
    },
    /**
     * Grafico de barras simples, sem dependencia externa.
     *
     * @param opcoes {formato: 'dinheiro'|'percentual', rotulos: [], legenda: '#seletor'}
     *               Barras negativas saem em vermelho; o titulo de cada barra
     *               traz o turno e o valor formatado.
     */
    grafico(seletor, valores, opcoes = {}) {
        const alvo = document.querySelector(seletor);
        if (!alvo) return;

        const formatar = (valor) => (opcoes.formato === 'percentual'
            ? Formato.percentual(valor, 1)
            : Formato.dinheiroCurto(valor));

        if (!valores || valores.length === 0) {
            alvo.innerHTML = '<p class="suave pequeno">Sem dados suficientes.</p>';
            if (opcoes.legenda) {
                const legenda = document.querySelector(opcoes.legenda);
                if (legenda) legenda.textContent = 'O primeiro turno processado ja gera o grafico.';
            }
            return;
        }

        const maximo = Math.max(...valores.map((v) => Math.abs(v)), Number.EPSILON);
        alvo.innerHTML = valores
            .map((valor, indice) => {
                const altura = Math.max((Math.abs(valor) / maximo) * 100, 2);
                const classe = valor < 0 ? ' class="negativa"' : '';
                const rotulo = opcoes.rotulos ? `turno ${opcoes.rotulos[indice]}: ` : '';
                return `<div${classe} style="height:${altura}%" title="${rotulo}${formatar(valor)}"></div>`;
            })
            .join('');

        if (opcoes.legenda) {
            const legenda = document.querySelector(opcoes.legenda);
            if (legenda) {
                const menor = Math.min(...valores);
                const maior = Math.max(...valores);
                const ultimo = valores[valores.length - 1];
                legenda.textContent = `ultimo ${formatar(ultimo)} | maior ${formatar(maior)} | menor ${formatar(menor)}`;
            }
        }
    }
};

document.addEventListener('DOMContentLoaded', () => {
    Interface.montarCabecalho();
});

/**
 * Auto-reload de desenvolvimento: desligado por padrao, so roda quando o
 * proprio navegador tem `localStorage.complexus.devAutoReload = '1'`
 * (ative rodando isso uma vez no console). Recarrega a pagina quando o
 * `Last-Modified` da propria pagina ou de um CSS/JS carregado muda no disco.
 * Nunca entra em builds de verdade nem afeta outros jogadores.
 */
if (localStorage.getItem('complexus.devAutoReload') === '1') {
    document.addEventListener('DOMContentLoaded', () => {
        // Espera o DOMContentLoaded (nao roda no topo do arquivo) para o
        // script da propria pagina (carregado depois de app.js no HTML) ja
        // ter sido parseado e executado, e estar no DOM.
        const arquivosObservados = [
            window.location.pathname,
            ...[...document.querySelectorAll('link[rel="stylesheet"], script[src]')]
                .map((el) => el.href || el.src)
        ].filter((url, indice, lista) => url && lista.indexOf(url) === indice);

        const ultimaModificacao = {};
        setInterval(async () => {
            for (const url of arquivosObservados) {
                try {
                    const resposta = await fetch(url, { method: 'HEAD', cache: 'no-store' });
                    const modificado = resposta.headers.get('Last-Modified');
                    if (!modificado) continue;
                    if (ultimaModificacao[url] === undefined) {
                        ultimaModificacao[url] = modificado;
                    } else if (ultimaModificacao[url] !== modificado) {
                        window.location.reload();
                        return;
                    }
                } catch (erro) {
                    // servidor reiniciando (build em andamento); tenta de novo no proximo ciclo
                }
            }
        }, 1500);
    });
}
