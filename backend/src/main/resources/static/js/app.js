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
    /** Marca o item do menu correspondente a pagina aberta. */
    marcarMenu() {
        const pagina = window.location.pathname.split('/').pop() || 'index.html';
        document.querySelectorAll('.menu a, .config-dropdown a').forEach((link) => {
            if (link.getAttribute('href') === pagina) {
                link.classList.add('ativo');
            }
        });
    },
    /** Liga o icone de engrenagem do cabecalho ao seu menu suspenso. */
    montarMenuConfiguracoes() {
        const botao = document.querySelector('#botao-config');
        const dropdown = document.querySelector('#dropdown-config');
        if (!botao || !dropdown) return;

        const fechar = () => {
            dropdown.classList.add('oculto');
            botao.classList.remove('aberto');
            botao.setAttribute('aria-expanded', 'false');
        };
        const alternar = (evento) => {
            evento.stopPropagation();
            const abrindo = dropdown.classList.contains('oculto');
            if (abrindo) {
                dropdown.classList.remove('oculto');
                botao.classList.add('aberto');
                botao.setAttribute('aria-expanded', 'true');
            } else {
                fechar();
            }
        };

        botao.addEventListener('click', alternar);
        document.addEventListener('click', (evento) => {
            if (!dropdown.contains(evento.target)) fechar();
        });
        document.addEventListener('keydown', (evento) => {
            if (evento.key === 'Escape') fechar();
        });
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
    Interface.montarMenuConfiguracoes();
});
