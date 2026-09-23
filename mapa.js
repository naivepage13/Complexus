// Mapa interativo hierárquico: Estados -> Cidades -> Estradas.
// Zoom semântico + lazy loading espacial (client-side) + herança de estado.
(function () {
    'use strict';

    const ZOOM = {
        min: 5,        // visão geral (todos os estados)
        cidades: 8,    // a partir daqui, cidades e estradas entram em cena
        max: 10,       // nível mais profundo permitido: Cidades (sem zoom de rua)
        flyToEstado: 9 // zoom alvo do flyTo ao clicar num estado
    };

    const URLS = {
        estados: 'data/mapa/estados.geojson',
        cidades: 'data/mapa/cidades.geojson',
        estradas: 'data/mapa/estradas.geojson'
    };

    // ---------------------------------------------------------------
    // Estado global do mapa (hierarquia + regra de herança de negócio)
    // ---------------------------------------------------------------
    const registro = {
        estados: new Map(),   // id -> propriedades (mutáveis: aliquotaEstadual, etc.)
        cidades: new Map(),   // id -> propriedades
        estradas: new Map(),  // id -> propriedades
        ouvintes: new Set()
    };

    function notificarMudanca() {
        registro.ouvintes.forEach((fn) => fn());
    }

    function observarMudancas(fn) {
        registro.ouvintes.add(fn);
    }

    // Uma decisão macrorregional (ex.: imposto estadual) recalcula todas as
    // cidades filhas daquele estado. Nenhuma cidade guarda o valor herdado:
    // ele é sempre derivado do estado pai no momento do cálculo.
    function calcularEfeitosHerdados(cidadeId) {
        const cidade = registro.cidades.get(cidadeId);
        if (!cidade) return null;
        const uf = registro.estados.get(cidade.estadoId);
        const aliquota = uf ? uf.aliquotaEstadual : 0;
        const investimento = uf ? uf.investimentoInfraestrutura : 0;

        let infraestruturaEfetiva = cidade.infraestrutura + investimento * 0.4;
        for (const estrada of registro.estradas.values()) {
            if (estrada.status === 'bloqueada' && estrada.cidadesConectadas.includes(cidadeId)) {
                infraestruturaEfetiva -= 12;
            }
        }

        return {
            pibEfetivo: cidade.pibMunicipal * (1 - aliquota),
            infraestruturaEfetiva: Math.max(0, Math.min(100, infraestruturaEfetiva)),
            aliquotaAplicada: aliquota
        };
    }

    // Limites que envolvem as cidades filhas — usado para o flyTo caber
    // exatamente a área com cidades, em vez do centro geométrico do
    // polígono, que em estados grandes/irregulares (ex.: MG) pode ficar
    // longe de onde as cidades realmente estão.
    function limitesDasCidadesDoEstado(estadoId) {
        const cidades = [...registro.cidades.values()].filter((c) => c.estadoId === estadoId);
        if (cidades.length === 0) return null;
        return L.latLngBounds(cidades.map((c) => [c.coordenadas[1], c.coordenadas[0]]));
    }

    function aplicarDecisaoEstadual(estadoId, novaAliquota) {
        const uf = registro.estados.get(estadoId);
        if (!uf) return;
        uf.aliquotaEstadual = Math.max(0, Math.min(0.5, novaAliquota));
        notificarMudanca();
    }

    function alternarBloqueioEstrada(estradaId) {
        const estrada = registro.estradas.get(estradaId);
        if (!estrada) return;
        estrada.status = estrada.status === 'bloqueada' ? 'normal' : 'bloqueada';
        notificarMudanca();
    }

    function expandirRodovia(estradaId) {
        const estrada = registro.estradas.get(estradaId);
        if (!estrada) return;
        estrada.capacidade = Math.min(100, estrada.capacidade + 15);
        notificarMudanca();
    }

    // ---------------------------------------------------------------
    // Geometria: bbox + interseção, para o lazy loading espacial
    // ---------------------------------------------------------------
    const PROFUNDIDADE_POR_TIPO = { Point: 0, LineString: 1, Polygon: 2, MultiPolygon: 3 };

    function bboxDeGeometria(geometria) {
        let minX = Infinity, minY = Infinity, maxX = -Infinity, maxY = -Infinity;
        (function visitar(coords, profundidade) {
            if (profundidade === 0) {
                const [x, y] = coords;
                if (x < minX) minX = x;
                if (x > maxX) maxX = x;
                if (y < minY) minY = y;
                if (y > maxY) maxY = y;
            } else {
                coords.forEach((c) => visitar(c, profundidade - 1));
            }
        })(geometria.coordinates, PROFUNDIDADE_POR_TIPO[geometria.type] ?? 1);
        return [minX, minY, maxX, maxY];
    }

    function bboxIntersecta(a, b) {
        return a[0] <= b[2] && a[2] >= b[0] && a[1] <= b[3] && a[3] >= b[1];
    }

    // ---------------------------------------------------------------
    // Camada com lazy loading espacial: só materializa em Leaflet as
    // features cuja bbox cruza a viewport atual E cujo zoom está na
    // faixa configurada. Ao sair da vista, a layer é removida (não só
    // escondida) — simula o que um tile/feature server faria por trás.
    // ---------------------------------------------------------------
    function criarCamadaLazy(map, featureCollection, opcoes) {
        const grupo = L.layerGroup();
        const camadasAtivas = new Map();
        const zoomMin = opcoes.zoomMin ?? ZOOM.min;
        const zoomMax = opcoes.zoomMax ?? ZOOM.max;

        function atualizar() {
            const zoom = map.getZoom();
            if (zoom < zoomMin || zoom > zoomMax) {
                grupo.clearLayers();
                camadasAtivas.clear();
                return;
            }

            const limites = map.getBounds();
            const bboxTela = [limites.getWest(), limites.getSouth(), limites.getEast(), limites.getNorth()];
            const idsVisiveis = new Set();

            for (const feature of featureCollection.features) {
                if (opcoes.filtro && !opcoes.filtro(feature)) continue;
                const bbox = bboxDeGeometria(feature.geometry);
                if (!bboxIntersecta(bbox, bboxTela)) continue;

                const id = feature.properties.id;
                idsVisiveis.add(id);
                if (!camadasAtivas.has(id)) {
                    const camada = opcoes.aoRenderizar(feature);
                    camadasAtivas.set(id, camada);
                    grupo.addLayer(camada);
                }
            }

            for (const [id, camada] of camadasAtivas) {
                if (!idsVisiveis.has(id)) {
                    grupo.removeLayer(camada);
                    camadasAtivas.delete(id);
                }
            }
        }

        grupo.addTo(map);
        map.on('moveend zoomend', atualizar);
        atualizar();
        return { grupo, atualizar, camadasAtivas };
    }

    // ---------------------------------------------------------------
    // Painel de decisão (lateral)
    // ---------------------------------------------------------------
    const painelEl = document.getElementById('painel-decisao');
    const painelConteudoEl = document.getElementById('painel-conteudo');
    let painelAtual = null; // { tipo, id } — para re-render em notificarMudanca()

    function formatarMoeda(valor) {
        if (valor >= 1e9) return 'R$ ' + (valor / 1e9).toFixed(1) + ' bi';
        if (valor >= 1e6) return 'R$ ' + (valor / 1e6).toFixed(1) + ' mi';
        return 'R$ ' + valor.toLocaleString('pt-BR');
    }

    function fecharPainel() {
        painelAtual = null;
        painelEl.classList.add('hidden');
        painelConteudoEl.innerHTML = '';
    }
    window.fecharPainel = fecharPainel;

    function abrirPainelEstado(estadoId) {
        painelAtual = { tipo: 'estado', id: estadoId };
        renderizarPainel();
    }

    function abrirPainelCidade(cidadeId) {
        painelAtual = { tipo: 'cidade', id: cidadeId };
        renderizarPainel();
    }

    function abrirPainelEstrada(estradaId) {
        painelAtual = { tipo: 'estrada', id: estradaId };
        renderizarPainel();
    }

    function renderizarPainel() {
        if (!painelAtual) return;
        painelEl.classList.remove('hidden');

        if (painelAtual.tipo === 'estado') {
            const uf = registro.estados.get(painelAtual.id);
            if (!uf) return fecharPainel();
            const numCidades = [...registro.cidades.values()].filter((c) => c.estadoId === uf.id).length;
            painelConteudoEl.innerHTML = `
                <h3>${uf.nome} (${uf.sigla})</h3>
                <p class="painel-tag">Estado</p>
                <div class="painel-linha"><span>Índice de desenvolvimento</span><strong>${uf.indiceDesenvolvimento}</strong></div>
                <div class="painel-linha"><span>Investimento em infraestrutura</span><strong>${uf.investimentoInfraestrutura}</strong></div>
                <div class="painel-linha"><span>Cidades administradas</span><strong>${numCidades}</strong></div>
                <hr>
                <label for="input-aliquota">Alíquota estadual (afeta todas as cidades filhas)</label>
                <input type="range" id="input-aliquota" min="0" max="0.5" step="0.01" value="${uf.aliquotaEstadual}">
                <div class="painel-linha"><span>Valor atual</span><strong id="valor-aliquota">${(uf.aliquotaEstadual * 100).toFixed(0)}%</strong></div>
                <button id="btn-aplicar-aliquota">Aplicar decisão estadual</button>
                <p class="painel-nota">Clique numa cidade deste estado (zoom máximo) para ver o efeito imediato no PIB efetivo.</p>
            `;
            const slider = document.getElementById('input-aliquota');
            const valorEl = document.getElementById('valor-aliquota');
            slider.addEventListener('input', () => {
                valorEl.textContent = (Number(slider.value) * 100).toFixed(0) + '%';
            });
            document.getElementById('btn-aplicar-aliquota').addEventListener('click', () => {
                aplicarDecisaoEstadual(uf.id, Number(slider.value));
            });
            return;
        }

        if (painelAtual.tipo === 'cidade') {
            const cidade = registro.cidades.get(painelAtual.id);
            if (!cidade) return fecharPainel();
            const uf = registro.estados.get(cidade.estadoId);
            const efeitos = calcularEfeitosHerdados(cidade.id);
            painelConteudoEl.innerHTML = `
                <h3>${cidade.nome}${cidade.capital ? ' (capital)' : ''}</h3>
                <p class="painel-tag">Cidade — ${uf ? uf.nome : '—'}</p>
                <div class="painel-linha"><span>PIB municipal (bruto)</span><strong>${formatarMoeda(cidade.pibMunicipal)}</strong></div>
                <div class="painel-linha"><span>Alíquota estadual aplicada</span><strong>${(efeitos.aliquotaAplicada * 100).toFixed(0)}%</strong></div>
                <div class="painel-linha destaque"><span>PIB efetivo (pós-imposto)</span><strong>${formatarMoeda(efeitos.pibEfetivo)}</strong></div>
                <hr>
                <div class="painel-linha"><span>Infraestrutura local</span><strong>${cidade.infraestrutura}</strong></div>
                <div class="painel-linha destaque"><span>Infraestrutura efetiva</span><strong>${efeitos.infraestruturaEfetiva.toFixed(0)}</strong></div>
                <div class="painel-linha"><span>Tropas guarnecidas</span><strong>${cidade.tropasGuarnecidas.toLocaleString('pt-BR')}</strong></div>
                <p class="painel-nota">Valores em destaque herdam a decisão do estado (${uf ? uf.nome : '—'}) e o estado das rodovias conectadas.</p>
            `;
            return;
        }

        if (painelAtual.tipo === 'estrada') {
            const estrada = registro.estradas.get(painelAtual.id);
            if (!estrada) return fecharPainel();
            const nomesCidades = estrada.cidadesConectadas
                .map((id) => registro.cidades.get(id)?.nome ?? id)
                .join(' ↔ ');
            painelConteudoEl.innerHTML = `
                <h3>${estrada.nome}</h3>
                <p class="painel-tag">Camada logística — ${estrada.tipo}</p>
                <div class="painel-linha"><span>Conecta</span><strong>${nomesCidades}</strong></div>
                <div class="painel-linha"><span>Capacidade</span><strong>${estrada.capacidade}</strong></div>
                <div class="painel-linha"><span>Status</span><strong class="${estrada.status === 'bloqueada' ? 'status-bloqueada' : 'status-normal'}">${estrada.status}</strong></div>
                <hr>
                <button id="btn-expandir-rodovia">Expandir rodovia (+15 capacidade)</button>
                <button id="btn-bloquear-rodovia" class="btn-perigo">${estrada.status === 'bloqueada' ? 'Reabrir suprimentos' : 'Bloquear suprimentos'}</button>
                <p class="painel-nota">Bloquear a rodovia reduz a infraestrutura efetiva das cidades conectadas.</p>
            `;
            document.getElementById('btn-expandir-rodovia').addEventListener('click', () => expandirRodovia(estrada.id));
            document.getElementById('btn-bloquear-rodovia').addEventListener('click', () => alternarBloqueioEstrada(estrada.id));
            return;
        }
    }

    observarMudancas(renderizarPainel);

    // ---------------------------------------------------------------
    // Mapa Leaflet
    // ---------------------------------------------------------------
    const map = L.map('mapa-mapa', {
        minZoom: ZOOM.min,
        maxZoom: ZOOM.max, // nível mais profundo estritamente em Cidades
        zoomControl: true
    }).setView([-21.5, -45.5], ZOOM.min);

    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
        attribution: '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a>',
        maxZoom: ZOOM.max
    }).addTo(map);

    const indicadorZoomEl = document.getElementById('indicador-zoom');
    function atualizarIndicadorZoom() {
        const zoom = map.getZoom();
        let nivel = 'Estados';
        if (zoom >= ZOOM.max) nivel = 'Cidades (nível máximo)';
        else if (zoom >= ZOOM.cidades) nivel = 'Estados + Cidades';
        indicadorZoomEl.textContent = `Zoom ${zoom} — ${nivel}`;
    }
    map.on('zoomend', atualizarIndicadorZoom);

    function estiloEstado() {
        return { color: '#44ff44', weight: 1.5, fillColor: '#1e3d1e', fillOpacity: 0.35 };
    }

    function aoRenderizarEstado(feature) {
        const props = feature.properties;

        const layer = L.geoJSON(feature, { style: estiloEstado }).getLayers()[0];
        layer.bindTooltip(props.nome, { sticky: true });
        layer.on('click', (ev) => {
            L.DomEvent.stopPropagation(ev);
            // O painel do estado (e a decisão de imposto) continua acessível
            // mesmo já dentro do nível de cidade — só o flyTo de drill-down
            // não deve se repetir a cada clique de fundo.
            abrirPainelEstado(props.id);
            if (map.getZoom() >= ZOOM.cidades) return;
            // Mira no aglomerado de cidades filhas, não no centro geométrico
            // do polígono: em estados grandes/irregulares (ex.: MG) o centro
            // do polígono pode ficar longe de onde as cidades realmente
            // estão. O zoom nunca fica abaixo do nível de Cidades.
            const limitesCidades = limitesDasCidadesDoEstado(props.id);
            let alvoCentro = layer.getBounds().getCenter();
            let alvoZoom = ZOOM.flyToEstado;
            if (limitesCidades) {
                alvoCentro = limitesCidades.getCenter();
                const zoomParaCaber = map.getBoundsZoom(limitesCidades, false, [40, 40]);
                alvoZoom = Math.max(ZOOM.cidades, Math.min(zoomParaCaber, ZOOM.max));
            }
            map.flyTo(alvoCentro, alvoZoom, { duration: 0.8 });
        });
        return layer;
    }

    function aoRenderizarCidade(feature) {
        const props = feature.properties;

        const [lon, lat] = feature.geometry.coordinates;
        const marker = L.circleMarker([lat, lon], {
            radius: props.capital ? 8 : 5,
            color: '#a3123f',
            fillColor: '#a3123f',
            fillOpacity: 0.9,
            weight: 1.5
        });
        marker.bindTooltip(props.nome);
        marker.on('click', (ev) => {
            L.DomEvent.stopPropagation(ev);
            abrirPainelCidade(props.id);
        });
        return marker;
    }

    function aoRenderizarEstrada(feature) {
        const props = feature.properties;

        const coords = feature.geometry.coordinates.map(([lon, lat]) => [lat, lon]);
        const linha = L.polyline(coords, { color: '#a86a00', weight: 3, opacity: 0.9 });
        linha.on('mouseover', () => linha.setStyle({ weight: 7 }));
        linha.on('mouseout', () => linha.setStyle({ weight: 3 }));
        linha.on('click', (ev) => {
            L.DomEvent.stopPropagation(ev);
            abrirPainelEstrada(props.id);
        });
        return linha;
    }

    // Recarrega o estilo das estradas quando o status muda (ex.: bloqueio).
    observarMudancas(() => {
        camadaEstradas?.camadasAtivas.forEach((linha, id) => {
            const estrada = registro.estradas.get(id);
            if (!estrada) return;
            linha.setStyle({ color: estrada.status === 'bloqueada' ? '#a3123f' : '#a86a00' });
        });
    });

    let camadaEstradas = null;

    Promise.all([
        fetch(URLS.estados).then((r) => r.json()),
        fetch(URLS.cidades).then((r) => r.json()),
        fetch(URLS.estradas).then((r) => r.json())
    ]).then(([estadosFC, cidadesFC, estradasFC]) => {
        // Dados de domínio são registrados uma única vez, na íntegra — a
        // hierarquia e a herança de estado não podem depender de quais
        // features já foram materializadas no mapa (lazy loading é só
        // visual). As funções aoRenderizar* só leem este registro.
        estadosFC.features.forEach((f) => registro.estados.set(f.properties.id, { ...f.properties }));
        cidadesFC.features.forEach((f) => registro.cidades.set(f.properties.id, {
            ...f.properties,
            coordenadas: f.geometry.coordinates // [lon, lat]
        }));
        estradasFC.features.forEach((f) => registro.estradas.set(f.properties.id, { ...f.properties }));

        // Estados: visíveis em toda a faixa de zoom (contexto), mas deixam
        // de capturar clique de drill-down ao entrar no nível de cidade.
        criarCamadaLazy(map, estadosFC, {
            zoomMin: ZOOM.min,
            zoomMax: ZOOM.max,
            aoRenderizar: aoRenderizarEstado
        });

        // Cidades e estradas: lazy loading real — só entram quando o zoom
        // atinge o nível de estado E a feature está dentro do viewport.
        criarCamadaLazy(map, cidadesFC, {
            zoomMin: ZOOM.cidades,
            zoomMax: ZOOM.max,
            aoRenderizar: aoRenderizarCidade
        });

        camadaEstradas = criarCamadaLazy(map, estradasFC, {
            zoomMin: ZOOM.cidades,
            zoomMax: ZOOM.max,
            aoRenderizar: aoRenderizarEstrada
        });

        atualizarIndicadorZoom();
    });

    map.on('click', fecharPainel);
})();
