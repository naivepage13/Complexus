// Mapa tatico em pixels (RF-29 a RF-31). O desenho acontece em uma imagem de
// baixa resolucao, em que cada pixel vale uma quantidade fixa de quilometros,
// ampliada sem suavizacao. As contas ficam em mapa-nucleo.js e o painel
// lateral em mapa-paineis.js. Nada aqui busca recurso fora do proprio jogo.
(function () {
    'use strict';

    const jogador = Sessao.exigir();
    if (!jogador) return;

    const N = MapaNucleo;
    const PIXEL_NA_TELA = 3;       // pixels de tela por pixel do jogo
    const QUADRO_MS = 66;          // ~15 quadros por segundo bastam para pixels
    const CONSULTA_MS = 5000;      // de quanto em quanto tempo perguntar pelas forcas
    const LIMITES = { oeste: -80, leste: -25, norte: 10, sul: -38 };
    const CHAVE_CAMADAS = 'complexus.mapa.camadas';

    const URLS = {
        estados: 'dados/mapa/estados.geojson',
        cidades: 'dados/mapa/cidades.geojson',
        estradas: 'dados/mapa/estradas.geojson'
    };

    const CORES = {
        mar: '#7fb2d9', rotaMaritima: '#cfe6f7', rotaAerea: '#f4f8fb',
        terra: '#cfdfae', terraComDado: '#dce9b4', selecao: '#f1dc82', divisa: '#85995c',
        rodovia: '#b0651f', rodoviaSelecionada: '#5a2d06',
        cidade: '#8c2f39', porto: '#12466e', aeroporto: '#4b3f72',
        caminhao: '#6e2f00', navio: '#ffffff', aviao: '#ffffff',
        EXERCITO: '#2f6b1d', MARINHA: '#0a2a57', AERONAUTICA: '#6b2f9c',
        contornoClaro: '#ffffff', contornoEscuro: '#1c2430'
    };

    // Desenhos em pixel art; '#' e um pixel cheio.
    const SPRITES = {
        caminhao: ['###..', '#####', '#####', '.#.#.'],
        navio: ['..#..', '..##.', '#####', '.###.'],
        aviao: ['..#..', '..#..', '#####', '..#..', '.###.'],
        porto: ['..#..', '.###.', '..#..', '#.#.#', '.###.'],
        aeroporto: ['.###.', '#.#.#', '#####', '#.#.#', '.###.'],
        EXERCITO: ['..###..', '..#####', '#######', '#.#.#.#'],
        MARINHA: ['...#...', '..###..', '#######', '.#####.'],
        SUBMARINO: ['...##..', '...#...', '.#####.', '#######', '.#####.'],
        AERONAUTICA: ['...#...', '..###..', '#######', '...#...', '..###..'],
        destino: ['#...#', '.#.#.', '..#..', '.#.#.', '#...#'],
        ponto: ['##', '##']
    };

    const registro = {
        pais: null,
        estados: new Map(), cidades: new Map(), estradas: new Map(),
        portos: new Map(), aeroportos: new Map(), rotas: new Map(),
        veiculos: new Map(), unidades: new Map()
    };
    const politica = { mandatos: [], tipos: [] };

    const containerEl = document.getElementById('mapa-mapa');
    const tela = document.getElementById('tela-mapa');
    const contexto = tela.getContext('2d');
    const base = document.createElement('canvas');
    const contextoBase = base.getContext('2d');
    const dicaEl = document.getElementById('dica-mapa');
    const avisoEl = document.getElementById('aviso-ordem');

    let camera = null;
    let ampliacao = PIXEL_NA_TELA;
    let densidade = 1;
    let baseSuja = true;
    let relogio = null;
    let horasPorSegundo = 0;
    let podeComandar = false;
    let ordemPara = null;          // id da unidade aguardando destino
    let enviandoOrdem = false;
    let alvos = [];                // o que foi desenhado e pode ser clicado

    // ---------------------------------------------------------------
    // Camadas
    // ---------------------------------------------------------------
    const filtros = [...document.querySelectorAll('#mapa-filtros input')];
    const camadas = {};

    function carregarCamadas() {
        let salvas = {};
        try {
            salvas = JSON.parse(localStorage.getItem(CHAVE_CAMADAS)) || {};
        } catch (erro) {
            salvas = {};
        }
        filtros.forEach((caixa) => {
            const nome = caixa.dataset.camada;
            camadas[nome] = salvas[nome] !== false;
            caixa.checked = camadas[nome];
            caixa.addEventListener('change', () => {
                camadas[nome] = caixa.checked;
                try {
                    localStorage.setItem(CHAVE_CAMADAS, JSON.stringify(camadas));
                } catch (erro) {
                    // sem armazenamento local o filtro vale so para esta visita
                }
                baseSuja = true;
                desenhar();
            });
        });
    }

    const nivelAtual = () => N.nivel(camera.kmPorPixel);

    /** Rodovias, caminhoes e divisas so aparecem de Estados para dentro. */
    function visivel(camada) {
        if (!camadas[camada]) return false;
        const nivel = nivelAtual();
        if (camada === 'cidades') return nivel === 'CIDADES';
        if (['estados', 'rodovias', 'caminhoes'].includes(camada)) return nivel !== 'PAIS';
        return true;
    }

    // ---------------------------------------------------------------
    // Geometria em quilometros, convertida uma vez so
    // ---------------------------------------------------------------
    const emKm = (pontos) => pontos.map(([lon, lat]) => N.paraMundo(lon, lat));

    function naTela(pontosKm) {
        const { centroX, centroY, kmPorPixel, largura, altura } = camera;
        return pontosKm.map(([x, y]) => [
            (x - centroX) / kmPorPixel + largura / 2,
            (y - centroY) / kmPorPixel + altura / 2
        ]);
    }

    function caixaDe(grupos) {
        const caixa = [Infinity, Infinity, -Infinity, -Infinity];
        grupos.forEach((pontos) => pontos.forEach(([x, y]) => {
            caixa[0] = Math.min(caixa[0], x);
            caixa[1] = Math.min(caixa[1], y);
            caixa[2] = Math.max(caixa[2], x);
            caixa[3] = Math.max(caixa[3], y);
        }));
        return caixa;
    }

    function naVista([x0, y0, x1, y1]) {
        const meiaLargura = camera.largura / 2 * camera.kmPorPixel;
        const meiaAltura = camera.altura / 2 * camera.kmPorPixel;
        return x1 >= camera.centroX - meiaLargura && x0 <= camera.centroX + meiaLargura
            && y1 >= camera.centroY - meiaAltura && y0 <= camera.centroY + meiaAltura;
    }

    /** Centro de area do maior anel, para o rotulo do estado. */
    function pontoDeRotulo(poligonos) {
        let melhor = { area: 0, ponto: poligonos[0].aneis[0][0] };
        poligonos.forEach(({ aneis }) => {
            const anel = aneis[0];
            let area = 0;
            let x = 0;
            let y = 0;
            for (let i = 0, j = anel.length - 1; i < anel.length; j = i++) {
                const cruzado = anel[j][0] * anel[i][1] - anel[i][0] * anel[j][1];
                area += cruzado;
                x += (anel[j][0] + anel[i][0]) * cruzado;
                y += (anel[j][1] + anel[i][1]) * cruzado;
            }
            if (Math.abs(area) > melhor.area) {
                melhor = { area: Math.abs(area), ponto: [x / (3 * area), y / (3 * area)] };
            }
        });
        return melhor.ponto;
    }

    // ---------------------------------------------------------------
    // Camada de fundo: um pixel por vez, so quando camera ou filtro mudam
    // ---------------------------------------------------------------
    const corDeImagem = (hex) => {
        const n = parseInt(hex.slice(1), 16);
        return (0xff000000 | ((n & 0xff) << 16) | (n & 0xff00) | (n >> 16)) >>> 0;
    };

    function desenharBase() {
        const { largura, altura } = camera;
        const imagem = contextoBase.createImageData(largura, altura);
        const pixels = new Uint32Array(imagem.data.buffer);
        const selecionado = paineis.selecionado() || {};

        const ponto = (x, y, cor) => {
            if (x >= 0 && y >= 0 && x < largura && y < altura) pixels[y * largura + x] = cor;
        };
        /** Linha continua (passo 1) ou pontilhada (um pixel a cada `passo`). */
        const tracar = (pontos, hex, passo = 1, grossa = false) => {
            const cor = corDeImagem(hex);
            let contador = 0;
            for (let i = 1; i < pontos.length; i++) {
                N.linha(pontos[i - 1][0], pontos[i - 1][1], pontos[i][0], pontos[i][1], (x, y) => {
                    if (contador++ % passo !== 0) return;
                    ponto(x, y, cor);
                    if (grossa) {
                        ponto(x + 1, y, cor);
                        ponto(x, y + 1, cor);
                    }
                }, largura, altura);
            }
        };

        pixels.fill(corDeImagem(CORES.mar));

        const estadosNaVista = [...registro.estados.values()].filter((e) => naVista(e.caixa));
        const mostraDivisas = visivel('estados');
        estadosNaVista.forEach((estado) => {
            let hex = CORES.terra;
            if (mostraDivisas && estado.api) hex = CORES.terraComDado;
            if (selecionado.tipo === 'estado' && selecionado.id === estado.id) hex = CORES.selecao;
            const cor = corDeImagem(hex);
            estado.poligonos.forEach(({ aneis }) => {
                N.preencher(aneis.map(naTela), largura, altura,
                    (y, x0, x1) => pixels.fill(cor, y * largura + x0, y * largura + x1 + 1));
            });
        });
        if (mostraDivisas) {
            estadosNaVista.forEach((estado) => estado.poligonos.forEach(({ aneis }) => {
                aneis.forEach((anel) => tracar(naTela([...anel, anel[0]]), CORES.divisa));
            }));
        }

        registro.rotas.forEach((rota) => {
            if (rota.tipo === 'NAVIO_CARGA' && visivel('navios')) {
                tracar(naTela(rota.km), CORES.rotaMaritima, 3);
            }
            if (rota.tipo === 'AVIAO_COMERCIAL' && visivel('avioes')) {
                tracar(naTela(rota.km), CORES.rotaAerea, 6);
            }
        });

        if (visivel('rodovias')) {
            registro.estradas.forEach((estrada) => {
                const marcada = selecionado.tipo === 'estrada' && selecionado.id === estrada.id;
                estrada.trechos.forEach((trecho) => {
                    if (!naVista(trecho.caixa)) return;
                    tracar(naTela(trecho.km), marcada ? CORES.rodoviaSelecionada : CORES.rodovia, 1, marcada);
                });
            });
        }

        contextoBase.putImageData(imagem, 0, 0);

        if (visivel('cidades')) {
            registro.cidades.forEach((cidade) => {
                const [x, y] = N.paraTela(camera, ...cidade.coordenadas).map(Math.round);
                const lado = cidade.capital ? 5 : 3;
                contextoBase.fillStyle = CORES.contornoClaro;
                contextoBase.fillRect(x - (lado + 1) / 2, y - (lado + 1) / 2, lado + 2, lado + 2);
                contextoBase.fillStyle = CORES.cidade;
                contextoBase.fillRect(x - (lado - 1) / 2, y - (lado - 1) / 2, lado, lado);
            });
        }
        baseSuja = false;
    }

    // ---------------------------------------------------------------
    // Quadro: fundo ampliado + o que se move + rotulos
    // ---------------------------------------------------------------
    function sprite(nome, x, y, cor, contorno) {
        const linhas = SPRITES[nome];
        const x0 = Math.round(x) - Math.floor(linhas[0].length / 2);
        const y0 = Math.round(y) - Math.floor(linhas.length / 2);
        const pintar = (dx, dy) => contexto.fillRect((x0 + dx) * ampliacao, (y0 + dy) * ampliacao,
            ampliacao, ampliacao);
        const cheios = [];
        linhas.forEach((texto, dy) => [...texto].forEach((c, dx) => {
            if (c === '#') cheios.push([dx, dy]);
        }));
        contexto.fillStyle = contorno;
        cheios.forEach(([dx, dy]) => {
            pintar(dx - 1, dy);
            pintar(dx + 1, dy);
            pintar(dx, dy - 1);
            pintar(dx, dy + 1);
        });
        contexto.fillStyle = cor;
        cheios.forEach(([dx, dy]) => pintar(dx, dy));
    }

    function dentroDaTela(x, y) {
        return x > -8 && y > -8 && x < camera.largura + 8 && y < camera.altura + 8;
    }

    /** Desenha um marcador e o registra como alvo de clique. */
    function marcar(alvo, nomeSprite, lonLat, cor, contorno) {
        const [x, y] = N.paraTela(camera, ...lonLat);
        if (!dentroDaTela(x, y)) return;
        sprite(nomeSprite, x, y, cor, contorno);
        alvos.push({ ...alvo, x, y });
    }

    /** Na visao do pais, o que e civil vira um ponto para as forcas aparecerem. */
    const reduzido = (nome) => (nivelAtual() === 'PAIS' ? 'ponto' : nome);

    /** Posicao atual da unidade; ao fim do trajeto ela passa a estar parada. */
    function posicaoDe(unidade, agora) {
        if (!unidade.emMovimento) return unidade.posicao;
        const horas = relogio.horasDesde(unidade.partidaEm, agora);
        if (horas >= unidade.trajeto[unidade.trajeto.length - 1][2]) {
            const fim = unidade.trajeto[unidade.trajeto.length - 1];
            unidade.emMovimento = false;
            unidade.posicao = [fim[0], fim[1]];
            return unidade.posicao;
        }
        return N.posicaoNoTrajeto(unidade.trajeto, horas);
    }

    const camadaDaUnidade = (unidade) => unidade.ramo.toLowerCase();

    function desenharCaminho(unidade, posicao, destacado) {
        const horas = relogio.horasDesde(unidade.partidaEm, Date.now());
        const restante = [posicao, ...unidade.trajeto.filter((m) => m[2] > horas)]
            .map(([lon, lat]) => N.paraTela(camera, lon, lat));
        contexto.fillStyle = CORES[unidade.ramo];
        const passo = destacado ? 2 : 4;
        let contador = 0;
        for (let i = 1; i < restante.length; i++) {
            N.linha(restante[i - 1][0], restante[i - 1][1], restante[i][0], restante[i][1], (x, y) => {
                if (contador++ % passo === 0) {
                    contexto.fillRect(x * ampliacao, y * ampliacao, ampliacao, ampliacao);
                }
            }, camera.largura, camera.altura);
        }
        const fim = unidade.trajeto[unidade.trajeto.length - 1];
        const [x, y] = N.paraTela(camera, fim[0], fim[1]);
        sprite('destino', x, y, CORES[unidade.ramo], CORES.contornoClaro);
    }

    function rotulo(texto, lonLat, deslocamento = 0, tamanho = 12) {
        const [x, y] = N.paraTela(camera, ...lonLat);
        if (!dentroDaTela(x, y)) return;
        contexto.font = `bold ${Math.round(tamanho * densidade)}px Consolas, monospace`;
        contexto.textAlign = 'center';
        contexto.textBaseline = 'middle';
        contexto.lineWidth = 3 * densidade;
        contexto.strokeStyle = CORES.contornoClaro;
        const py = y * ampliacao + deslocamento * densidade;
        contexto.strokeText(texto, x * ampliacao, py);
        contexto.fillStyle = CORES.contornoEscuro;
        contexto.fillText(texto, x * ampliacao, py);
    }

    function desenhar() {
        if (!camera || !relogio) return;
        if (baseSuja) desenharBase();
        const agora = Date.now();
        const selecionado = paineis.selecionado() || {};
        alvos = [];

        contexto.imageSmoothingEnabled = false;
        contexto.drawImage(base, 0, 0, camera.largura * ampliacao, camera.altura * ampliacao);

        if (visivel('portos')) {
            registro.portos.forEach((porto) => marcar({ tipo: 'porto', id: porto.id, nome: porto.nome },
                reduzido('porto'), porto.posicao, CORES.porto, CORES.contornoClaro));
        }
        if (visivel('aeroportos')) {
            registro.aeroportos.forEach((a) => marcar({ tipo: 'aeroporto', id: a.id, nome: `Aeroporto ${a.nome}` },
                reduzido('aeroporto'), a.posicao, CORES.aeroporto, CORES.contornoClaro));
        }

        const unidades = [...registro.unidades.values()].filter((u) => visivel(camadaDaUnidade(u)));
        unidades.forEach((unidade) => {
            const posicao = posicaoDe(unidade, agora);
            if (unidade.emMovimento) {
                desenharCaminho(unidade, posicao,
                    selecionado.tipo === 'unidade' && selecionado.id === unidade.id);
            }
        });

        const civis = {
            CAMINHAO: ['caminhoes', 'caminhao', CORES.caminhao, CORES.contornoClaro],
            NAVIO_CARGA: ['navios', 'navio', CORES.navio, CORES.contornoEscuro],
            AVIAO_COMERCIAL: ['avioes', 'aviao', CORES.aviao, CORES.contornoEscuro]
        };
        registro.veiculos.forEach((veiculo) => {
            const rota = registro.rotas.get(veiculo.rotaId);
            const [camada, desenho, cor, contorno] = civis[rota.tipo];
            if (!visivel(camada)) return;
            marcar({ tipo: 'veiculo', id: veiculo.id, nome: `${rota.rotulo}: ${rota.nome}` },
                reduzido(desenho), viagem(veiculo, agora).posicao, cor, contorno);
        });

        unidades.forEach((unidade) => {
            const desenho = unidade.tipo === 'SUBMARINO' ? 'SUBMARINO' : unidade.ramo;
            const posicao = posicaoDe(unidade, agora);
            marcar({ tipo: 'unidade', id: unidade.id, nome: unidade.nome },
                desenho, posicao, CORES[unidade.ramo], CORES.contornoClaro);
            if (selecionado.tipo === 'unidade' && selecionado.id === unidade.id) {
                const [x, y] = N.paraTela(camera, ...posicao).map(Math.round);
                contexto.strokeStyle = CORES.contornoEscuro;
                contexto.lineWidth = ampliacao;
                contexto.strokeRect((x - 6) * ampliacao, (y - 5) * ampliacao, 13 * ampliacao, 11 * ampliacao);
            }
        });

        desenharRotulos(unidades, selecionado, agora);
    }

    function desenharRotulos(unidades, selecionado, agora) {
        const nivel = nivelAtual();
        if (nivel === 'PAIS' && registro.pais) {
            rotulo('BRASIL', [-53, -10.5], 0, 16);
        }
        if (nivel === 'ESTADOS' && visivel('estados')) {
            registro.estados.forEach((estado) => rotulo(estado.sigla, estado.rotulo));
        }
        if (visivel('cidades')) {
            registro.cidades.forEach((cidade) => rotulo(cidade.nome, cidade.coordenadas, -16));
        }
        unidades.forEach((unidade) => {
            const marcada = selecionado.tipo === 'unidade' && selecionado.id === unidade.id;
            if (marcada || nivel === 'CIDADES') rotulo(unidade.nome, posicaoDe(unidade, agora), 22);
        });
    }

    function viagem(veiculo, agora = Date.now()) {
        const rota = registro.rotas.get(veiculo.rotaId);
        return N.vaiEVolta(rota.marcos, relogio.horasAbsolutas(agora) + veiculo.deslocamentoHoras);
    }

    // ---------------------------------------------------------------
    // Camera
    // ---------------------------------------------------------------
    function limitar() {
        const [xMin, yMin] = N.paraMundo(LIMITES.oeste, LIMITES.norte);
        const [xMax, yMax] = N.paraMundo(LIMITES.leste, LIMITES.sul);
        camera.centroX = Math.min(xMax, Math.max(xMin, camera.centroX));
        camera.centroY = Math.min(yMax, Math.max(yMin, camera.centroY));
    }

    function atualizarIndicadores() {
        const nivel = nivelAtual();
        const textos = {
            PAIS: 'Nível: País — aproxime para ver estados e rodovias',
            ESTADOS: 'Nível: Estados — aproxime para ver cidades',
            CIDADES: 'Nível: Cidades'
        };
        document.getElementById('indicador-zoom').textContent = textos[nivel];

        // Barra de escala com um numero redondo perto de 100 pixels de tela.
        const kmPorPixelDeTela = camera.kmPorPixel * densidade / ampliacao;
        const bruto = 100 * kmPorPixelDeTela;
        const potencia = Math.pow(10, Math.floor(Math.log10(bruto)));
        const redondo = [5, 2, 1].map((n) => n * potencia).find((n) => n <= bruto);
        document.getElementById('escala-barra').style.width = `${redondo / kmPorPixelDeTela}px`;
        document.getElementById('escala-texto').textContent =
            `${Formato.inteiro(redondo)} km · 1 pixel = ${String(camera.kmPorPixel).replace('.', ',')} km`;

        filtros.forEach((caixa) => {
            const nome = caixa.dataset.camada;
            const oculta = (nome === 'cidades' && nivel !== 'CIDADES')
                || (['estados', 'rodovias', 'caminhoes'].includes(nome) && nivel === 'PAIS');
            caixa.parentElement.classList.toggle('inativa', oculta);
            caixa.parentElement.title = oculta ? 'Aproxime o mapa para ver esta camada' : '';
        });
        document.getElementById('zoom-mais').disabled = camera.kmPorPixel === N.ESCALAS[N.ESCALAS.length - 1];
        document.getElementById('zoom-menos').disabled = camera.kmPorPixel === N.ESCALAS[0];
    }

    function moveu() {
        limitar();
        baseSuja = true;
        atualizarIndicadores();
        desenhar();
    }

    /** Um degrau de zoom (direcao +1 aproxima), ancorado em um ponto da imagem. */
    function zoom(direcao, x = camera.largura / 2, y = camera.altura / 2) {
        const atual = N.ESCALAS.indexOf(camera.kmPorPixel);
        const novo = Math.min(N.ESCALAS.length - 1, Math.max(0, atual + direcao));
        if (novo === atual) return;
        camera = N.mudarEscala(camera, N.ESCALAS[novo], x, y);
        moveu();
    }

    function verPais() {
        camera = {
            ...camera, ...N.centroDoBrasil(),
            kmPorPixel: N.escalaQueCabe(camera.largura, camera.altura)
        };
        moveu();
    }

    function redimensionar() {
        const largura = containerEl.clientWidth;
        const altura = containerEl.clientHeight;
        if (!largura || !altura) return;
        densidade = window.devicePixelRatio || 1;
        ampliacao = Math.max(2, Math.round(PIXEL_NA_TELA * densidade));
        tela.width = Math.round(largura * densidade);
        tela.height = Math.round(altura * densidade);
        base.width = Math.ceil(tela.width / ampliacao);
        base.height = Math.ceil(tela.height / ampliacao);
        camera = {
            ...(camera || N.centroDoBrasil()),
            largura: base.width,
            altura: base.height,
            kmPorPixel: camera ? camera.kmPorPixel : N.escalaQueCabe(base.width, base.height)
        };
        moveu();
    }

    // ---------------------------------------------------------------
    // Cliques
    // ---------------------------------------------------------------
    /** Posicao do ponteiro em pixels do jogo. */
    function noJogo(evento) {
        const caixa = tela.getBoundingClientRect();
        return [
            (evento.clientX - caixa.left) * densidade / ampliacao,
            (evento.clientY - caixa.top) * densidade / ampliacao
        ];
    }

    /** Tudo que esta sob o ponto, do mais especifico ao mais geral. */
    function sob(x, y) {
        const prioridade = { unidade: 0, veiculo: 1, cidade: 2, porto: 3, aeroporto: 3 };
        const marcadores = [...alvos];
        if (visivel('cidades')) {
            registro.cidades.forEach((cidade) => {
                const [cx, cy] = N.paraTela(camera, ...cidade.coordenadas);
                marcadores.push({ tipo: 'cidade', id: cidade.id, nome: cidade.nome, x: cx, y: cy });
            });
        }
        const achados = marcadores
            .map((alvo) => ({ ...alvo, distancia: Math.hypot(alvo.x - x, alvo.y - y) }))
            .filter((alvo) => alvo.distancia <= 4.5)
            .sort((a, b) => prioridade[a.tipo] - prioridade[b.tipo] || a.distancia - b.distancia);

        const [lon, lat] = N.paraGeo(camera, x, y);
        const [kmX, kmY] = N.paraMundo(lon, lat);
        if (visivel('rodovias')) {
            const alcance = 2 * camera.kmPorPixel;
            for (const estrada of registro.estradas.values()) {
                const perto = estrada.trechos.some(({ km, caixa }) =>
                    kmX >= caixa[0] - alcance && kmX <= caixa[2] + alcance
                    && kmY >= caixa[1] - alcance && kmY <= caixa[3] + alcance
                    && km.some((p, i) => i > 0
                        && N.distanciaAoSegmento(kmX, kmY, km[i - 1][0], km[i - 1][1], p[0], p[1]) <= alcance));
                if (perto) achados.push({ tipo: 'estrada', id: estrada.id, nome: estrada.nome });
            }
        }
        const estado = [...registro.estados.values()].find((e) =>
            e.poligonos.some(({ aneis }) => N.contem(aneis, kmX, kmY)));
        if (estado && nivelAtual() === 'PAIS') {
            achados.push({ tipo: 'pais', id: 'brasil', nome: 'Brasil' });
        } else if (estado) {
            achados.push({ tipo: 'estado', id: estado.id, nome: estado.nome });
        }
        return achados;
    }

    function clicar(x, y) {
        if (ordemPara !== null) {
            enviarOrdem(...N.paraGeo(camera, x, y));
            return;
        }
        const achados = sob(x, y);
        if (achados.length === 0) {
            paineis.fechar();
            return;
        }
        // Clicar de novo no mesmo lugar passa para o item que esta embaixo.
        const atual = paineis.selecionado();
        const indice = atual ? achados.findIndex((a) => a.tipo === atual.tipo && a.id === atual.id) : -1;
        const escolhido = achados[(indice + 1) % achados.length];
        paineis.abrir(escolhido.tipo, escolhido.id);

        if (escolhido.tipo === 'estado' && nivelAtual() === 'ESTADOS') {
            const capital = [...registro.cidades.values()]
                .find((c) => c.estadoId === escolhido.id && c.capital);
            if (capital) {
                camera = { ...camera, ...N.cameraNoPonto(...capital.coordenadas), kmPorPixel: 2 };
                moveu();
            }
        }
    }

    function dica(evento) {
        const [x, y] = noJogo(evento);
        const achados = ordemPara === null ? sob(x, y) : [];
        if (achados.length === 0) {
            dicaEl.classList.add('oculto');
            tela.classList.remove('sobre-alvo');
            return;
        }
        const caixa = containerEl.getBoundingClientRect();
        const extras = achados.filter((a) => !['estado', 'pais'].includes(a.tipo)).length - 1;
        dicaEl.textContent = achados[0].nome + (extras > 0 ? ` (+${extras})` : '');
        dicaEl.style.left = `${evento.clientX - caixa.left + 14}px`;
        dicaEl.style.top = `${evento.clientY - caixa.top + 14}px`;
        dicaEl.classList.remove('oculto');
        tela.classList.add('sobre-alvo');
    }

    let arrasto = null;
    tela.addEventListener('pointerdown', (evento) => {
        if (evento.button !== 0 || !camera) return;
        tela.setPointerCapture(evento.pointerId);
        arrasto = { x: evento.clientX, y: evento.clientY, moveu: false };
    });
    tela.addEventListener('pointermove', (evento) => {
        if (!camera || !relogio) return;
        if (!arrasto) {
            dica(evento);
            return;
        }
        const dx = evento.clientX - arrasto.x;
        const dy = evento.clientY - arrasto.y;
        if (!arrasto.moveu && Math.hypot(dx, dy) < 5) return;
        const kmPorPixelDeTela = camera.kmPorPixel * densidade / ampliacao;
        camera.centroX -= dx * kmPorPixelDeTela;
        camera.centroY -= dy * kmPorPixelDeTela;
        arrasto = { x: evento.clientX, y: evento.clientY, moveu: true };
        dicaEl.classList.add('oculto');
        moveu();
    });
    tela.addEventListener('pointerup', (evento) => {
        if (arrasto && !arrasto.moveu && relogio) clicar(...noJogo(evento));
        arrasto = null;
    });
    tela.addEventListener('pointercancel', () => { arrasto = null; });
    tela.addEventListener('pointerleave', () => dicaEl.classList.add('oculto'));

    let ultimaRoda = 0;
    tela.addEventListener('wheel', (evento) => {
        evento.preventDefault();
        if (!camera || Date.now() - ultimaRoda < 120 || evento.deltaY === 0) return;
        ultimaRoda = Date.now();
        zoom(evento.deltaY < 0 ? 1 : -1, ...noJogo(evento));
    }, { passive: false });

    tela.addEventListener('keydown', (evento) => {
        const setas = { ArrowLeft: [-1, 0], ArrowRight: [1, 0], ArrowUp: [0, -1], ArrowDown: [0, 1] };
        if (setas[evento.key]) {
            evento.preventDefault();
            camera.centroX += setas[evento.key][0] * 20 * camera.kmPorPixel;
            camera.centroY += setas[evento.key][1] * 20 * camera.kmPorPixel;
            moveu();
        } else if (evento.key === '+' || evento.key === '=') {
            zoom(1);
        } else if (evento.key === '-') {
            zoom(-1);
        }
    });
    document.addEventListener('keydown', (evento) => {
        if (evento.key !== 'Escape' || evento.target.closest('input, select, textarea')) return;
        if (ordemPara !== null) encerrarOrdem();
        else paineis.fechar();
    });
    document.getElementById('zoom-mais').addEventListener('click', () => zoom(1));
    document.getElementById('zoom-menos').addEventListener('click', () => zoom(-1));
    document.getElementById('zoom-pais').addEventListener('click', verPais);

    // ---------------------------------------------------------------
    // Ordens
    // ---------------------------------------------------------------
    function pedirDestino(unidadeId) {
        ordemPara = unidadeId;
        const unidade = registro.unidades.get(unidadeId);
        avisoEl.textContent = `Clique no mapa para escolher o destino de ${unidade.nome}. Esc cancela.`;
        avisoEl.classList.remove('oculto');
        tela.classList.add('ordenando');
        dicaEl.classList.add('oculto');
        tela.focus();
    }

    function encerrarOrdem() {
        ordemPara = null;
        avisoEl.classList.add('oculto');
        tela.classList.remove('ordenando');
    }

    async function enviarOrdem(longitude, latitude) {
        if (enviandoOrdem) return;
        enviandoOrdem = true;
        const unidadeId = ordemPara;
        try {
            await API.post('/operacoes/ordens',
                { jogadorId: jogador.id, unidadeId, longitude, latitude });
            await consultarSituacao();
            encerrarOrdem();
            paineis.abrir('unidade', unidadeId);
        } catch (erro) {
            // Destino recusado: o aviso explica e o jogador pode clicar outro ponto.
            avisoEl.textContent = `${erro.message} Clique em outro ponto ou aperte Esc.`;
        } finally {
            enviandoOrdem = false;
        }
    }

    async function parar(unidadeId) {
        try {
            await API.post(`/operacoes/unidades/${unidadeId}/parar`, { jogadorId: jogador.id });
            await consultarSituacao();
            paineis.abrir('unidade', unidadeId);
        } catch (erro) {
            Interface.mensagem('#mensagem-ordem', erro.message);
        }
    }

    // ---------------------------------------------------------------
    // Dados
    // ---------------------------------------------------------------
    const paineis = MapaPaineis.criar({
        jogador,
        registro,
        politica,
        comando: {
            pode: () => podeComandar,
            horasPorSegundo: () => horasPorSegundo,
            horasRestantes: (u) => u.trajeto[u.trajeto.length - 1][2]
                - relogio.horasDesde(u.partidaEm, Date.now()),
            viagem,
            pedirDestino,
            parar
        },
        aoMudar: () => {
            const atual = paineis && paineis.selecionado();
            if (ordemPara !== null && (!atual || atual.id !== ordemPara)) encerrarOrdem();
            baseSuja = true;
            desenhar();
        }
    });

    function aplicarSituacao(resposta) {
        relogio = N.relogio(resposta, Date.now());
        horasPorSegundo = resposta.horasJogoPorSegundo;
        podeComandar = resposta.podeComandar;
        resposta.unidades.forEach((u) => registro.unidades.set(u.id, u));
    }

    async function consultarSituacao() {
        aplicarSituacao(await API.get(`/operacoes/situacao?jogadorId=${jogador.id}`));
        paineis.unidadesAtualizadas();
    }

    const normalizar = (texto) => (texto || '')
        .normalize('NFD').replace(/[̀-ͯ]/g, '').toLowerCase().trim();

    /**
     * Junta a geometria estatica (arquivos) com o registro vivo da API,
     * casando por sigla (estado) ou nome e sigla (municipio).
     */
    function montarTerritorios(estados, cidades, estradas, territorios) {
        registro.pais = territorios.paises[0];
        estados.features.forEach((f) => {
            const partes = f.geometry.type === 'Polygon' ? [f.geometry.coordinates] : f.geometry.coordinates;
            const poligonos = partes.map((aneis) => ({ aneis: aneis.map(emKm) }));
            const rotuloKm = pontoDeRotulo(poligonos);
            const [lon, lat] = N.paraGeo({ centroX: 0, centroY: 0, kmPorPixel: 1, largura: 0, altura: 0 },
                ...rotuloKm);
            registro.estados.set(f.properties.id, {
                ...f.properties,
                poligonos,
                caixa: caixaDe(poligonos.map((p) => p.aneis[0])),
                rotulo: [lon, lat],
                api: territorios.estados.find((e) => normalizar(e.sigla) === normalizar(f.properties.sigla))
            });
        });
        cidades.features.forEach((f) => {
            const estado = registro.estados.get(f.properties.estadoId);
            registro.cidades.set(f.properties.id, {
                ...f.properties,
                coordenadas: f.geometry.coordinates,
                api: territorios.municipios.find((m) => normalizar(m.nome) === normalizar(f.properties.nome)
                    && normalizar(m.estado) === normalizar(estado && estado.sigla))
            });
        });
        estradas.features.forEach((f) => {
            const trechos = f.geometry.coordinates.map((segmento) => {
                const km = emKm(segmento);
                return { km, caixa: caixaDe([km]) };
            });
            registro.estradas.set(f.properties.id, { ...f.properties, trechos });
        });
    }

    function montarMalha(malha) {
        malha.portos.forEach((p) => registro.portos.set(p.id, p));
        malha.aeroportos.forEach((a) => registro.aeroportos.set(a.id, a));
        malha.rotas.forEach((r) => registro.rotas.set(r.id, { ...r, km: emKm(r.marcos) }));
        malha.veiculos.forEach((v) => registro.veiculos.set(v.id, v));
    }

    function animar() {
        let ultimo = 0;
        const passo = (instante) => {
            if (instante - ultimo >= QUADRO_MS) {
                ultimo = instante;
                desenhar();
                paineis.unidadesAtualizadas();
            }
            requestAnimationFrame(passo);
        };
        requestAnimationFrame(passo);
        setInterval(() => paineis.tique(), 1000);
        setInterval(async () => {
            try {
                await consultarSituacao();
                Interface.mensagem('#mensagem', '');
            } catch (erro) {
                Interface.mensagem('#mensagem', `Sem contato com o servidor: ${erro.message}`);
            }
        }, CONSULTA_MS);
    }

    const lerArquivo = async (url) => {
        const resposta = await fetch(url);
        if (!resposta.ok) throw new Error(`Não foi possível ler ${url}`);
        return resposta.json();
    };

    async function carregar() {
        carregarCamadas();
        try {
            const [estados, cidades, estradas, territorios, mandatos, tipos, malha, situacao] =
                await Promise.all([
                    lerArquivo(URLS.estados),
                    lerArquivo(URLS.cidades),
                    lerArquivo(URLS.estradas),
                    API.get('/politica/territorios'),
                    API.get(`/politica/mandatos/jogador/${jogador.id}`),
                    API.get('/politica/tipos-projeto'),
                    API.get('/operacoes/malha'),
                    API.get(`/operacoes/situacao?jogadorId=${jogador.id}`)
                ]);
            politica.mandatos = mandatos;
            politica.tipos = tipos;
            montarTerritorios(estados, cidades, estradas, territorios);
            montarMalha(malha);
            aplicarSituacao(situacao);
            redimensionar();
            new ResizeObserver(redimensionar).observe(containerEl);
            animar();
        } catch (erro) {
            Interface.mensagem('#mensagem', erro.message);
        }
    }

    carregar();
})();
