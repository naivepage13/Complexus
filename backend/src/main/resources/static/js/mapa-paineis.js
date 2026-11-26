/*
 * Painel lateral do mapa: mostra o que foi clicado (pais, estado, cidade,
 * rodovia, porto, aeroporto, veiculo ou unidade militar).
 *
 * Dados de jogo vem ao vivo da API; decisoes de territorio persistem como
 * proposta de lei (POST /politica/projetos) e ordens militares passam por
 * /operacoes (RF-26 e RF-31).
 */
const MapaPaineis = (function () {
    'use strict';

    // Projetos que dao para propor direto do mapa: os que nao exigem setor alvo.
    const TIPOS_SEM_SETOR = new Set([
        'IMPOSTO_EMPRESARIAL', 'IMPOSTO_ESTADUAL', 'IMPOSTO_MUNICIPAL',
        'INVESTIMENTO_INFRAESTRUTURA', 'PROGRAMA_SOCIAL', 'ZONEAMENTO_URBANO'
    ]);

    const linha = (rotulo, valor, id) => `<div class="painel-linha"><span>${rotulo}</span>
        <strong${id ? ` id="${id}"` : ''}>${valor}</strong></div>`;
    const cabecalho = (titulo, tag) => `<h3>${titulo}</h3><p class="painel-tag">${tag}</p>`;
    const nota = (texto) => `<p class="painel-nota">${texto}</p>`;

    /** Tempo ate chegar, no relogio real e no calendario do jogo. */
    function prazo(horasDeJogo, horasPorSegundo) {
        const segundos = Math.max(0, Math.round(horasDeJogo / horasPorSegundo));
        const minutos = Math.floor(segundos / 60);
        const real = minutos >= 60
            ? `${Math.floor(minutos / 60)} h ${minutos % 60} min`
            : minutos > 0 ? `${minutos} min ${segundos % 60} s` : `${segundos} s`;
        const horas = Math.max(0, Math.round(horasDeJogo));
        const jogo = horas >= 24 ? `${Math.floor(horas / 24)} d ${horas % 24} h` : `${horas} h`;
        return `${real} (${jogo} no jogo)`;
    }

    const coordenadas = ([lon, lat]) => `${Formato.numero(lat, 2)}, ${Formato.numero(lon, 2)}`;

    /**
     * @param contexto {jogador, registro, politica: {mandatos, tipos},
     *   comando: {pode(), horasPorSegundo(), horasRestantes(unidade),
     *   viagem(veiculo), pedirDestino(unidadeId), parar(unidadeId)}, aoMudar()}
     */
    function criar(contexto) {
        const { jogador, registro, politica, comando } = contexto;
        const painelEl = document.getElementById('painel-decisao');
        const conteudoEl = document.getElementById('painel-conteudo');
        let atual = null; // { tipo, id }
        let assinaturaUnidade = '';

        function blocoDecisao(esfera, territorioId) {
            const mandatos = politica.mandatos.filter((m) => m.esfera === esfera
                && m.territorioId === territorioId && (m.legislativo || m.executivo));
            if (mandatos.length === 0) {
                return `<hr>${nota(`Você não tem mandato legislativo nem executivo aqui.
                    <a href="politica.html">Assuma um cargo em Política</a> pra propor decisões neste território.`)}`;
            }
            const tipos = politica.tipos.filter((t) => t.esferas.includes(esfera) && TIPOS_SEM_SETOR.has(t.nome));
            return `
                <hr>
                <form class="formulario" id="form-decisao-mapa">
                    <div class="campo">
                        <label for="mandato-decisao-mapa">Mandato proponente</label>
                        <select id="mandato-decisao-mapa">
                            ${mandatos.map((m) => `<option value="${m.id}">${m.cargoRotulo}</option>`).join('')}
                        </select>
                    </div>
                    <div class="campo">
                        <label for="tipo-decisao-mapa">Instrumento</label>
                        <select id="tipo-decisao-mapa">
                            ${tipos.map((t) => `<option value="${t.nome}">${t.descricao}</option>`).join('')}
                        </select>
                    </div>
                    <div class="campo">
                        <label for="parametro-decisao-mapa">Parâmetro</label>
                        <input type="number" id="parametro-decisao-mapa" step="0.001" required>
                    </div>
                    <div class="campo">
                        <label for="titulo-decisao-mapa">Título</label>
                        <input type="text" id="titulo-decisao-mapa" placeholder="Ex.: Redução do ICMS">
                    </div>
                    <button class="botao botao-pequeno" type="submit">Propor projeto</button>
                </form>
                <div id="mensagem-decisao-mapa"></div>
                ${nota(`O projeto entra como rascunho — pauta, vota e sanciona em
                    <a href="politica.html">Política</a>, como qualquer outra lei.`)}
            `;
        }

        function ligarFormularioDecisao() {
            const form = document.getElementById('form-decisao-mapa');
            if (!form) return;
            form.addEventListener('submit', async (evento) => {
                evento.preventDefault();
                Interface.mensagem('#mensagem-decisao-mapa', '');
                try {
                    await API.post('/politica/projetos', {
                        jogadorId: jogador.id,
                        mandatoId: Number(document.getElementById('mandato-decisao-mapa').value),
                        titulo: document.getElementById('titulo-decisao-mapa').value,
                        ementa: '',
                        tipo: document.getElementById('tipo-decisao-mapa').value,
                        setorAlvo: null,
                        parametro: Number(document.getElementById('parametro-decisao-mapa').value)
                    });
                    Interface.mensagem('#mensagem-decisao-mapa',
                        'Projeto protocolado como rascunho. Pauta-o em Política para ir a votação.', 'sucesso');
                    form.reset();
                } catch (erro) {
                    Interface.mensagem('#mensagem-decisao-mapa', erro.message);
                }
            });
        }

        const montadores = {
            pais() {
                const pais = registro.pais;
                if (!pais) return null;
                const comDado = [...registro.estados.values()].filter((e) => e.api).length;
                return cabecalho(pais.nome, 'País')
                    + linha('População', Formato.inteiro(pais.populacao))
                    + linha('Tesouro nacional', Formato.dinheiroCurto(pais.tesouro))
                    + linha('PIB', Formato.dinheiroCurto(pais.pib))
                    + linha('Alíquota federal sobre empresas', Formato.percentual(pais.aliquotaImpostoEmpresarial, 0))
                    + linha('Gasto social mensal', Formato.dinheiroCurto(pais.gastoSocialMensal))
                    + linha('Estabilidade institucional', Formato.numero(pais.estabilidade, 0))
                    + linha('Aprovação do governo', Formato.numero(pais.aprovacaoGoverno, 0))
                    + linha('Desemprego', Formato.percentual(pais.desemprego, 1))
                    + linha('Renda média', Formato.dinheiroCurto(pais.rendaMedia))
                    + linha('Estados com dado no mapa', comDado)
                    + blocoDecisao('FEDERAL', pais.id);
            },
            estado(id) {
                const uf = registro.estados.get(id);
                if (!uf) return null;
                if (!uf.api) {
                    return cabecalho(`${uf.nome} (${uf.sigla})`, 'Estado')
                        + nota(`Este estado ainda não tem território de partida (fora da carga inicial). A
                            fronteira aparece só por completude geográfica; população, tesouro e alíquota entram
                            aqui assim que existir um Estado de verdade nesta UF.`);
                }
                const cidades = [...registro.cidades.values()].filter((c) => c.estadoId === uf.id).length;
                return cabecalho(`${uf.nome} (${uf.sigla})`, 'Estado')
                    + linha('População', Formato.inteiro(uf.api.populacao))
                    + linha('Tesouro estadual', Formato.dinheiroCurto(uf.api.tesouro))
                    + linha('Alíquota estadual', Formato.percentual(uf.api.aliquotaEstadual, 0))
                    + linha('Investimento em infraestrutura',
                        `${Formato.dinheiroCurto(uf.api.investimentoInfraestrutura)}/turno`)
                    + linha('Índice de desenvolvimento', Formato.numero(uf.api.indiceDesenvolvimento, 1))
                    + linha('Municípios no mapa', cidades)
                    + blocoDecisao('ESTADUAL', uf.api.id);
            },
            cidade(id) {
                const cidade = registro.cidades.get(id);
                if (!cidade) return null;
                const uf = registro.estados.get(cidade.estadoId);
                const titulo = cabecalho(`${cidade.nome}${cidade.capital ? ' (capital)' : ''}`,
                    `Município — ${uf ? uf.nome : ''}`);
                if (!cidade.api) {
                    return titulo + nota(`Este município ainda não tem território de partida (fora da carga
                        inicial). O marcador aparece só por completude geográfica; população, tesouro e alíquota
                        entram aqui assim que existir um Município de verdade aqui.`);
                }
                return titulo
                    + linha('População', Formato.inteiro(cidade.api.populacao))
                    + linha('Tesouro municipal', Formato.dinheiroCurto(cidade.api.tesouro))
                    + linha('Alíquota municipal', Formato.percentual(cidade.api.aliquotaMunicipal, 0))
                    + linha('Índice de urbanização', Formato.numero(cidade.api.indiceUrbanizacao, 1))
                    + linha('Demanda imobiliária', Formato.numero(cidade.api.demandaImobiliaria, 0))
                    + linha('Custo do terreno (m²)', Formato.dinheiroCurto(cidade.api.custoTerrenoM2))
                    + linha('Zoneamento', Formato.numero(cidade.api.zoneamento, 0))
                    + blocoDecisao('MUNICIPAL', cidade.api.id);
            },
            estrada(id) {
                const estrada = registro.estradas.get(id);
                if (!estrada) return null;
                const m = estrada.metricas || {};
                const p = (v) => (v === null || v === undefined) ? '—' : v;
                return cabecalho(estrada.nome, `Camada logística — ${estrada.tipo}`)
                    + linha('Índice de desenvolvimento', p(m.indiceDesenvolvimento))
                    + linha('Pavimentação', p(m.pavimentacao))
                    + linha('Capacidade', p(m.capacidade))
                    + nota(`O jogo ainda não modela rodovia como território: não há decisão nem dado
                        associado, e as métricas acima aguardam essa modelagem.`);
            },
            porto(id) {
                return terminal(registro.portos.get(id), 'Porto', 'NAVIO_CARGA');
            },
            aeroporto(id) {
                return terminal(registro.aeroportos.get(id), 'Aeroporto', 'AVIAO_COMERCIAL');
            },
            veiculo(id) {
                const veiculo = registro.veiculos.get(id);
                if (!veiculo) return null;
                const rota = registro.rotas.get(veiculo.rotaId);
                return cabecalho(rota.rotulo, 'Tráfego civil')
                    + linha('Rota', rota.nome)
                    + linha('Sentido', '', 'painel-sentido')
                    + linha('Velocidade', `${Formato.inteiro(rota.velocidadeKmH)} km/h`)
                    + linha('Chega em', '', 'painel-prazo')
                    + nota('Tráfego ilustrativo: ainda não carrega mercadoria nem afeta a economia.');
            },
            unidade(id) {
                const unidade = registro.unidades.get(id);
                if (!unidade) return null;
                const movimento = unidade.emMovimento
                    ? linha('Situação', 'Em deslocamento')
                        + linha('Destino', coordenadas(unidade.trajeto[unidade.trajeto.length - 1]))
                        + linha('Chega em', '', 'painel-prazo')
                    : linha('Situação', 'Parada') + linha('Posição', coordenadas(unidade.posicao));
                const acoes = comando.pode()
                    ? `<div class="painel-acoes">
                           <button class="botao botao-pequeno" type="button" id="botao-ordem">
                               ${unidade.emMovimento ? 'Mudar destino' : 'Dar ordem de movimento'}</button>
                           ${unidade.emMovimento
                               ? '<button class="botao botao-neutro botao-pequeno" type="button" id="botao-parar">Parar</button>'
                               : ''}
                       </div>`
                    : nota('Somente o Presidente da República dá ordens às forças.');
                const alcance = {
                    EXERCITO: 'Vai a qualquer ponto em terra, usando rodovia quando for mais rápido.',
                    MARINHA: 'Vai a qualquer ponto no mar dentro da área de operações, sem cruzar terra.',
                    AERONAUTICA: 'Voa em linha reta a qualquer ponto do teatro de operações.'
                }[unidade.ramo];
                return cabecalho(unidade.nome, `${unidade.ramoRotulo} — ${unidade.tipoRotulo}`)
                    + linha('Velocidade', `${Formato.inteiro(unidade.velocidadeKmH)} km/h`)
                    + movimento + acoes + '<div id="mensagem-ordem"></div>' + nota(alcance);
            }
        };

        function terminal(local, rotulo, tipo) {
            if (!local) return null;
            const rotas = [...registro.rotas.values()]
                .filter((r) => r.tipo === tipo && r.id.split(':').slice(1).includes(local.id));
            return cabecalho(local.nome, rotulo)
                + (rotas.length
                    ? rotas.map((r) => linha('Rota', r.nome)).join('')
                    : nota('Nenhuma rota regular parte daqui.'));
        }

        function assinatura(unidade) {
            return unidade ? `${unidade.emMovimento}:${unidade.partidaEm || ''}` : '';
        }

        function redesenhar() {
            if (!atual) return;
            const html = montadores[atual.tipo](atual.id);
            if (html === null) {
                fechar();
                return;
            }
            painelEl.classList.remove('oculto');
            conteudoEl.innerHTML = html;
            ligarFormularioDecisao();
            if (atual.tipo === 'unidade') {
                assinaturaUnidade = assinatura(registro.unidades.get(atual.id));
                const ordem = document.getElementById('botao-ordem');
                if (ordem) ordem.addEventListener('click', () => comando.pedirDestino(atual.id));
                const parar = document.getElementById('botao-parar');
                if (parar) parar.addEventListener('click', () => comando.parar(atual.id));
            }
            tique();
        }

        /** Atualiza so os campos que mudam com o relogio. */
        function tique() {
            if (!atual) return;
            const prazoEl = document.getElementById('painel-prazo');
            if (atual.tipo === 'veiculo') {
                const veiculo = registro.veiculos.get(atual.id);
                const rota = registro.rotas.get(veiculo.rotaId);
                const viagem = comando.viagem(veiculo);
                const [origem, destino] = rota.nome.split(' - ');
                document.getElementById('painel-sentido').textContent = `para ${viagem.voltando ? origem : destino}`;
                prazoEl.textContent = prazo(viagem.horasRestantes, comando.horasPorSegundo());
            } else if (atual.tipo === 'unidade' && prazoEl) {
                const unidade = registro.unidades.get(atual.id);
                prazoEl.textContent = prazo(comando.horasRestantes(unidade), comando.horasPorSegundo());
            }
        }

        function abrir(tipo, id) {
            atual = { tipo, id };
            redesenhar();
            contexto.aoMudar();
        }

        function fechar() {
            atual = null;
            painelEl.classList.add('oculto');
            conteudoEl.innerHTML = '';
            contexto.aoMudar();
        }

        /** Depois de cada consulta: refaz o painel da unidade so se ela mudou de situacao. */
        function unidadesAtualizadas() {
            if (atual && atual.tipo === 'unidade'
                    && assinatura(registro.unidades.get(atual.id)) !== assinaturaUnidade) {
                redesenhar();
            }
        }

        document.getElementById('fechar-painel').addEventListener('click', fechar);

        return { abrir, fechar, tique, unidadesAtualizadas, selecionado: () => atual };
    }

    return { criar };
})();
