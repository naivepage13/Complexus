// Contas do mapa em pixels. Rodar da raiz: node --test tests/web/
'use strict';

const test = require('node:test');
const assert = require('node:assert');
const Nucleo = require('../../backend/src/main/resources/static/js/mapa-nucleo.js');

const camera = () => ({ ...Nucleo.cameraNoPonto(-47.9, -15.8), kmPorPixel: 8, largura: 400, altura: 240 });

function perto(real, esperado, folga = 1e-6) {
    assert.ok(Math.abs(real - esperado) <= folga, `${real} deveria estar perto de ${esperado}`);
}

test('o centro da camera cai no meio da tela e a conversao e reversivel', () => {
    const [x, y] = Nucleo.paraTela(camera(), -47.9, -15.8);
    perto(x, 200);
    perto(y, 120);

    const [lon, lat] = Nucleo.paraGeo(camera(), ...Nucleo.paraTela(camera(), -43.2, -22.9));
    perto(lon, -43.2);
    perto(lat, -22.9);
});

test('a escala e real: um grau de latitude mede 110,57 km', () => {
    const [, y0] = Nucleo.paraTela(camera(), -47.9, -15);
    const [, y1] = Nucleo.paraTela(camera(), -47.9, -16);
    perto(y1 - y0, 110.57 / 8);
});

test('aproximar mantem fixo o ponto sob o cursor', () => {
    const antes = Nucleo.paraGeo(camera(), 320, 60);
    const nova = Nucleo.mudarEscala(camera(), 2, 320, 60);

    assert.strictEqual(nova.kmPorPixel, 2);
    const [x, y] = Nucleo.paraTela(nova, ...antes);
    perto(x, 320);
    perto(y, 60);
});

test('a escala inicial e a mais proxima em que o pais inteiro cabe', () => {
    assert.strictEqual(Nucleo.escalaQueCabe(400, 240), 24);
    assert.strictEqual(Nucleo.escalaQueCabe(600, 600), 8);
});

test('em tela de celular ainda ha um degrau em que o pais inteiro cabe', () => {
    const escala = Nucleo.escalaQueCabe(125, 200);

    assert.ok(125 * escala >= 4215, `${escala} km por pixel nao mostra o pais na largura`);
    assert.strictEqual(Nucleo.nivel(escala), 'PAIS');
});

test('a hierarquia vai de pais a estados e cidades conforme aproxima', () => {
    assert.strictEqual(Nucleo.nivel(24), 'PAIS');
    assert.strictEqual(Nucleo.nivel(16), 'PAIS');
    assert.strictEqual(Nucleo.nivel(8), 'ESTADOS');
    assert.strictEqual(Nucleo.nivel(4), 'ESTADOS');
    assert.strictEqual(Nucleo.nivel(2), 'CIDADES');
    assert.strictEqual(Nucleo.nivel(0.5), 'CIDADES');
});

test('a linha de pixels e continua e inclui as duas pontas', () => {
    const pixels = [];
    Nucleo.linha(0, 0, 4, 2, (x, y) => pixels.push([x, y]));

    assert.deepStrictEqual(pixels[0], [0, 0]);
    assert.deepStrictEqual(pixels[pixels.length - 1], [4, 2]);
    assert.strictEqual(pixels.length, 5);
    for (let i = 1; i < pixels.length; i++) {
        assert.ok(Math.abs(pixels[i][0] - pixels[i - 1][0]) <= 1);
        assert.ok(Math.abs(pixels[i][1] - pixels[i - 1][1]) <= 1);
    }
});

test('linhas que saem muito da tela sao recortadas sem laco enorme', () => {
    let desenhados = 0;
    Nucleo.linha(-1000000, 5, 1000000, 5, () => desenhados++, 20, 10);

    assert.strictEqual(desenhados, 20);
});

test('o preenchimento pinta os pixels cujo centro esta dentro, respeitando buracos', () => {
    const pintados = new Set();
    const quadrado = [[0, 0], [6, 0], [6, 6], [0, 6]];
    const buraco = [[2, 2], [4, 2], [4, 4], [2, 4]];

    Nucleo.preencher([quadrado, buraco], 10, 10, (y, x0, x1) => {
        for (let x = x0; x <= x1; x++) pintados.add(`${x},${y}`);
    });

    assert.strictEqual(pintados.size, 36 - 4);
    assert.ok(pintados.has('0,0') && pintados.has('5,5'));
    assert.ok(!pintados.has('2,2') && !pintados.has('6,0'));
});

const marcos = [[0, 0, 0], [10, 0, 5], [10, 10, 10]];

test('a posicao no trajeto interpola a perna corrente e para nas pontas', () => {
    assert.deepStrictEqual(Nucleo.posicaoNoTrajeto(marcos, 2.5), [5, 0]);
    assert.deepStrictEqual(Nucleo.posicaoNoTrajeto(marcos, 7.5), [10, 5]);
    assert.deepStrictEqual(Nucleo.posicaoNoTrajeto(marcos, -1), [0, 0]);
    assert.deepStrictEqual(Nucleo.posicaoNoTrajeto(marcos, 99), [10, 10]);
});

test('o veiculo civil vai ate o fim da rota e volta', () => {
    const ida = Nucleo.vaiEVolta(marcos, 22.5);
    assert.deepStrictEqual(ida.posicao, [5, 0]);
    assert.strictEqual(ida.voltando, false);
    assert.strictEqual(ida.horasRestantes, 7.5);

    const volta = Nucleo.vaiEVolta(marcos, 12.5);
    assert.deepStrictEqual(volta.posicao, [10, 5]);
    assert.strictEqual(volta.voltando, true);
    assert.strictEqual(volta.horasRestantes, 7.5);
});

test('horas de jogo seguem o relogio do servidor, nao o do navegador', () => {
    const relogio = Nucleo.relogio({ agora: 1000000, horasJogoPorSegundo: 0.2 }, 5000);

    perto(relogio.horasDesde(1000000, 5000), 0);
    perto(relogio.horasDesde(1000000, 15000), 2);
    perto(relogio.horasAbsolutas(5000), 200);
});

test('distancia ate um segmento considera as pontas', () => {
    perto(Nucleo.distanciaAoSegmento(5, 3, 0, 0, 10, 0), 3);
    perto(Nucleo.distanciaAoSegmento(13, 4, 0, 0, 10, 0), 5);
});

test('ponto dentro do poligono respeita buracos', () => {
    const aneis = [[[0, 0], [6, 0], [6, 6], [0, 6]], [[2, 2], [4, 2], [4, 4], [2, 4]]];

    assert.strictEqual(Nucleo.contem(aneis, 1, 1), true);
    assert.strictEqual(Nucleo.contem(aneis, 3, 3), false);
    assert.strictEqual(Nucleo.contem(aneis, 7, 1), false);
});
