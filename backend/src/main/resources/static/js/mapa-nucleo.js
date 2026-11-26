/*
 * Contas do mapa em pixels: projecao em escala real, rasterizacao sem
 * suavizacao, posicao ao longo de trajetos e testes de clique.
 *
 * Funcoes puras, sem DOM, para poderem ser testadas com `node --test`.
 */
(function (raiz) {
    'use strict';

    // Projecao equirretangular com paralelo de referencia em 15 graus sul, no
    // meio do Brasil: o "mundo" e medido em quilometros a partir do canto
    // noroeste, e cada pixel do jogo vale uma quantidade fixa deles.
    const ORIGEM = { lon: -75, lat: 6 };
    const KM_POR_GRAU_LAT = 110.57;
    const KM_POR_GRAU_LON = 111.32 * Math.cos(15 * Math.PI / 180);
    const BRASIL = { oeste: -74.0, leste: -34.8, norte: 5.3, sul: -33.8 };

    /** Quilometros por pixel de jogo em cada degrau de zoom, do mais aberto ao mais fechado. */
    const ESCALAS = [48, 24, 16, 8, 4, 2, 1, 0.5];

    function paraMundo(lon, lat) {
        return [(lon - ORIGEM.lon) * KM_POR_GRAU_LON, (ORIGEM.lat - lat) * KM_POR_GRAU_LAT];
    }

    function cameraNoPonto(lon, lat) {
        const [centroX, centroY] = paraMundo(lon, lat);
        return { centroX, centroY };
    }

    /** Geografico -> pixel do jogo (com fracao). */
    function paraTela(camera, lon, lat) {
        const [x, y] = paraMundo(lon, lat);
        return [
            (x - camera.centroX) / camera.kmPorPixel + camera.largura / 2,
            (y - camera.centroY) / camera.kmPorPixel + camera.altura / 2
        ];
    }

    function paraGeo(camera, px, py) {
        const x = camera.centroX + (px - camera.largura / 2) * camera.kmPorPixel;
        const y = camera.centroY + (py - camera.altura / 2) * camera.kmPorPixel;
        return [x / KM_POR_GRAU_LON + ORIGEM.lon, ORIGEM.lat - y / KM_POR_GRAU_LAT];
    }

    /** Troca o zoom mantendo no lugar o ponto que esta sob (px, py). */
    function mudarEscala(camera, kmPorPixel, px, py) {
        const dx = px - camera.largura / 2;
        const dy = py - camera.altura / 2;
        return {
            ...camera,
            kmPorPixel,
            centroX: camera.centroX + dx * (camera.kmPorPixel - kmPorPixel),
            centroY: camera.centroY + dy * (camera.kmPorPixel - kmPorPixel)
        };
    }

    function escalaQueCabe(largura, altura) {
        const larguraKm = (BRASIL.leste - BRASIL.oeste) * KM_POR_GRAU_LON;
        const alturaKm = (BRASIL.norte - BRASIL.sul) * KM_POR_GRAU_LAT;
        const cabem = ESCALAS.filter((k) => largura * k >= larguraKm && altura * k >= alturaKm);
        return cabem.length ? cabem[cabem.length - 1] : ESCALAS[0];
    }

    function centroDoBrasil() {
        return cameraNoPonto((BRASIL.oeste + BRASIL.leste) / 2, (BRASIL.norte + BRASIL.sul) / 2);
    }

    /** Hierarquia semantica Pais > Estados > Cidades. */
    function nivel(kmPorPixel) {
        if (kmPorPixel >= 16) return 'PAIS';
        if (kmPorPixel >= 4) return 'ESTADOS';
        return 'CIDADES';
    }

    // ---------------------------------------------------------------
    // Rasterizacao
    // ---------------------------------------------------------------

    /** Recorta o segmento a tela (Liang-Barsky); null quando fica todo fora. */
    function recortar(x0, y0, x1, y1, largura, altura) {
        const dx = x1 - x0;
        const dy = y1 - y0;
        let entrada = 0;
        let saida = 1;
        const bordas = [[-dx, x0], [dx, largura - 1 - x0], [-dy, y0], [dy, altura - 1 - y0]];
        for (const [p, q] of bordas) {
            if (p === 0) {
                if (q < 0) return null;
            } else {
                const t = q / p;
                if (p < 0) entrada = Math.max(entrada, t);
                else saida = Math.min(saida, t);
            }
        }
        if (entrada > saida) return null;
        return [x0 + entrada * dx, y0 + entrada * dy, x0 + saida * dx, y0 + saida * dy];
    }

    /** Linha de Bresenham: chama pintar(x, y) para cada pixel. */
    function linha(x0, y0, x1, y1, pintar, largura, altura) {
        if (largura !== undefined) {
            const corte = recortar(x0, y0, x1, y1, largura, altura);
            if (!corte) return;
            [x0, y0, x1, y1] = corte;
        }
        let x = Math.round(x0);
        let y = Math.round(y0);
        const fimX = Math.round(x1);
        const fimY = Math.round(y1);
        const dx = Math.abs(fimX - x);
        const dy = -Math.abs(fimY - y);
        const passoX = x < fimX ? 1 : -1;
        const passoY = y < fimY ? 1 : -1;
        let erro = dx + dy;
        for (;;) {
            pintar(x, y);
            if (x === fimX && y === fimY) return;
            const dobro = 2 * erro;
            if (dobro >= dy) {
                erro += dy;
                x += passoX;
            }
            if (dobro <= dx) {
                erro += dx;
                y += passoY;
            }
        }
    }

    /**
     * Preenchimento por linhas de varredura, regra do par-impar. Pinta os pixels
     * cujo centro esta dentro, chamando faixa(y, xInicial, xFinal).
     */
    function preencher(aneis, largura, altura, faixa) {
        let topo = Infinity;
        let base = -Infinity;
        for (const anel of aneis) {
            for (const [, y] of anel) {
                if (y < topo) topo = y;
                if (y > base) base = y;
            }
        }
        const primeira = Math.max(0, Math.floor(topo));
        const ultima = Math.min(altura - 1, Math.ceil(base));
        for (let y = primeira; y <= ultima; y++) {
            const centro = y + 0.5;
            const cortes = [];
            for (const anel of aneis) {
                for (let i = 0, j = anel.length - 1; i < anel.length; j = i++) {
                    const [ax, ay] = anel[i];
                    const [bx, by] = anel[j];
                    if ((ay > centro) !== (by > centro)) {
                        cortes.push(ax + (centro - ay) * (bx - ax) / (by - ay));
                    }
                }
            }
            cortes.sort((a, b) => a - b);
            for (let i = 0; i + 1 < cortes.length; i += 2) {
                const x0 = Math.max(0, Math.ceil(cortes[i] - 0.5));
                const x1 = Math.min(largura - 1, Math.ceil(cortes[i + 1] - 0.5) - 1);
                if (x1 >= x0) faixa(y, x0, x1);
            }
        }
    }

    // ---------------------------------------------------------------
    // Movimento
    // ---------------------------------------------------------------

    /** Posicao [lon, lat] em um trajeto de marcos [lon, lat, hora]. */
    function posicaoNoTrajeto(marcos, horas) {
        if (horas <= 0) return [marcos[0][0], marcos[0][1]];
        let baixo = 1;
        let alto = marcos.length;
        while (baixo < alto) {
            const meio = (baixo + alto) >> 1;
            if (marcos[meio][2] > horas) alto = meio;
            else baixo = meio + 1;
        }
        if (baixo >= marcos.length) {
            const fim = marcos[marcos.length - 1];
            return [fim[0], fim[1]];
        }
        const a = marcos[baixo - 1];
        const b = marcos[baixo];
        const f = (horas - a[2]) / (b[2] - a[2]);
        return [a[0] + (b[0] - a[0]) * f, a[1] + (b[1] - a[1]) * f];
    }

    /** Veiculo civil em ida e volta continua. */
    function vaiEVolta(marcos, horas) {
        const duracao = marcos[marcos.length - 1][2];
        const noCiclo = ((horas % (2 * duracao)) + 2 * duracao) % (2 * duracao);
        const voltando = noCiclo > duracao;
        const naRota = voltando ? 2 * duracao - noCiclo : noCiclo;
        return {
            posicao: posicaoNoTrajeto(marcos, naRota),
            voltando,
            horasRestantes: voltando ? naRota : duracao - naRota
        };
    }

    /**
     * Relogio de movimento. O instante oficial e o do servidor; o navegador so
     * conta quanto tempo passou desde a resposta.
     */
    function relogio(resposta, recebidoEm) {
        const noServidor = (agora) => resposta.agora + (agora - recebidoEm);
        return {
            horasDesde: (partidaEm, agora) =>
                (noServidor(agora) - partidaEm) / 1000 * resposta.horasJogoPorSegundo,
            horasAbsolutas: (agora) => noServidor(agora) / 1000 * resposta.horasJogoPorSegundo
        };
    }

    // ---------------------------------------------------------------
    // Cliques
    // ---------------------------------------------------------------

    function distanciaAoSegmento(px, py, ax, ay, bx, by) {
        const dx = bx - ax;
        const dy = by - ay;
        const comprimento = dx * dx + dy * dy;
        const t = comprimento === 0 ? 0
            : Math.max(0, Math.min(1, ((px - ax) * dx + (py - ay) * dy) / comprimento));
        return Math.hypot(px - ax - t * dx, py - ay - t * dy);
    }

    function contem(aneis, x, y) {
        let dentro = false;
        for (const anel of aneis) {
            for (let i = 0, j = anel.length - 1; i < anel.length; j = i++) {
                const [ax, ay] = anel[i];
                const [bx, by] = anel[j];
                if ((ay > y) !== (by > y) && x < (bx - ax) * (y - ay) / (by - ay) + ax) {
                    dentro = !dentro;
                }
            }
        }
        return dentro;
    }

    const MapaNucleo = {
        ESCALAS, paraMundo, cameraNoPonto, centroDoBrasil, paraTela, paraGeo, mudarEscala,
        escalaQueCabe, nivel, linha, preencher, posicaoNoTrajeto, vaiEVolta, relogio,
        distanciaAoSegmento, contem
    };

    if (typeof module !== 'undefined' && module.exports) module.exports = MapaNucleo;
    else raiz.MapaNucleo = MapaNucleo;
})(this);
