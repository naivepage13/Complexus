// Mapa interativo hierárquico: Estados -> Cidades -> Estradas, com zoom
// semântico. Implementa a RF-26: a geometria continua em GeoJSON estático
// (o backend não guarda coordenadas), mas todo dado de jogo — população,
// tesouro, alíquotas, índices — vem ao vivo de /api/politica/territorios, e
// as decisões do painel persistem de verdade via /api/politica/projetos
// (o mesmo fluxo de proposta de lei da página Política, exigindo mandato).
(function () {
    'use strict';

    const jogadorMapa = Sessao.exigir();
    if (!jogadorMapa) return;

    // Hierarquia de zoom semântico País > Estado > Cidade, com renderização
    // mutuamente exclusiva entre os três: cada camada só existe (é montada em
    // Leaflet) dentro da sua própria faixa — ao cruzar a fronteira de um
    // nível pro outro, a camada anterior é desmontada por completo, não só
    // escondida (ver criarCamadaLazy).
    const ZOOM = {
        min: 4,        // País: visão geral do Brasil inteiro, só o marcador do país
        estados: 6,    // a partir daqui, os 27 estados entram (país sai de cena)
        cidades: 8,    // a partir daqui, cidades entram em cena
        max: 10,       // nível mais profundo permitido: Cidades (sem zoom de rua)
        flyToEstado: 7 // zoom alvo do flyTo ao clicar num estado sem cidades mapeadas
    };

    const URLS = {
        estados: 'dados/mapa/estados.geojson',
        cidades: 'dados/mapa/cidades.geojson',
        estradas: 'dados/mapa/estradas.geojson'
    };

    // Cores neutras e de baixo contraste para as malhas territoriais — de
    // propósito: a identidade visual do território fica sutil pra não
    // competir com a estrada, que é o destaque absoluto na tela.
    const CORES = {
        estado: '#8a93a3',
        estadoArea: '#eef1f6',
        cidade: '#d1495b',
        estrada: '#c77700'
    };

    // Tipos de projeto que dá pra propor direto do painel do mapa: só os que
    // não exigem setor alvo (SUBSIDIO_SETORIAL e REGULACAO_SETORIAL ficam de
    // fora — pedem um formulário maior e já têm espaço dedicado em Política).
    const TIPOS_SEM_SETOR = new Set([
        'IMPOSTO_EMPRESARIAL', 'IMPOSTO_ESTADUAL', 'IMPOSTO_MUNICIPAL',
        'INVESTIMENTO_INFRAESTRUTURA', 'PROGRAMA_SOCIAL', 'ZONEAMENTO_URBANO'
    ]);

    function normalizar(texto) {
        return (texto || '').normalize('NFD').replace(/[̀-ͯ]/g, '').toLowerCase().trim();
    }

    // ---------------------------------------------------------------
    // Estado global do mapa: geometria estática casada com o registro vivo
    // da API. Nenhum campo de jogo é guardado aqui além do que a API devolve
    // — o painel sempre lê o valor corrente, nunca uma cópia recalculada no
    // cliente (era assim que a versão anterior "herdava" alíquota pra
    // cidade; a herança de verdade já acontece no motor de simulação do
    // backend, então o mapa só mostra o que a API diz).
    // ---------------------------------------------------------------
    const registro = { estados: new Map(), cidades: new Map(), estradas: new Map(), pais: null };
    let mandatosJogador = [];
    let tiposProjeto = [];

    function mandatosNoTerritorio(esfera, territorioId) {
        return mandatosJogador.filter((m) => m.esfera === esfera
            && m.territorioId === territorioId && (m.legislativo || m.executivo));
    }

    function tiposDaEsfera(esfera) {
        return tiposProjeto.filter((t) => t.esferas.includes(esfera) && TIPOS_SEM_SETOR.has(t.nome));
    }

    // ---------------------------------------------------------------
    // Painel de decisão (lateral)
    // ---------------------------------------------------------------
    const painelEl = document.getElementById('painel-decisao');
    const painelConteudoEl = document.getElementById('painel-conteudo');
    let painelAtual = null; // { tipo, id }

    function formatarMoeda(valor) {
        return Formato.dinheiroCurto(valor);
    }

    function fecharPainel() {
        painelAtual = null;
        painelEl.classList.add('oculto');
        painelConteudoEl.innerHTML = '';
    }
    window.fecharPainel = fecharPainel;

    function abrirPainelPais() {
        painelAtual = { tipo: 'pais' };
        renderizarPainel();
    }

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

    // Bloco de formulário compacto que propõe uma lei pro território aberto
    // no painel — é o que persiste a decisão de verdade (POST
    // /politica/projetos), no lugar da mutação só-no-cliente da versão
    // anterior. Exige que o jogador já tenha mandato ali; sem mandato, só
    // convida a assumir um cargo em Política.
    function blocoDecisao(esfera, territorioId) {
        const mandatos = mandatosNoTerritorio(esfera, territorioId);
        if (mandatos.length === 0) {
            return `
                <hr>
                <p class="painel-nota">Você não tem mandato legislativo nem executivo aqui.
                <a href="politica.html">Assuma um cargo em Política</a> pra propor decisões neste território.</p>
            `;
        }
        const tipos = tiposDaEsfera(esfera);
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
            <p class="painel-nota">O projeto entra como rascunho — pauta, vota e sanciona em
            <a href="politica.html">Política</a>, como qualquer outra lei.</p>
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
                    jogadorId: jogadorMapa.id,
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

    function renderizarPainel() {
        if (!painelAtual) return;
        painelEl.classList.remove('oculto');

        if (painelAtual.tipo === 'pais') {
            const pais = registro.pais;
            if (!pais || !pais.api) return fecharPainel();
            const numEstados = [...registro.estados.values()].filter((e) => e.api).length;
            painelConteudoEl.innerHTML = `
                <h3>${pais.api.nome}</h3>
                <p class="painel-tag">País</p>
                <div class="painel-linha"><span>População</span><strong>${Formato.inteiro(pais.api.populacao)}</strong></div>
                <div class="painel-linha"><span>Tesouro nacional</span><strong>${formatarMoeda(pais.api.tesouro)}</strong></div>
                <div class="painel-linha"><span>PIB</span><strong>${formatarMoeda(pais.api.pib)}</strong></div>
                <div class="painel-linha"><span>Alíquota federal sobre empresas</span><strong>${Formato.percentual(pais.api.aliquotaImpostoEmpresarial, 0)}</strong></div>
                <div class="painel-linha"><span>Gasto social mensal</span><strong>${formatarMoeda(pais.api.gastoSocialMensal)}</strong></div>
                <div class="painel-linha"><span>Estabilidade institucional</span><strong>${Formato.numero(pais.api.estabilidade, 0)}</strong></div>
                <div class="painel-linha"><span>Aprovação do governo</span><strong>${Formato.numero(pais.api.aprovacaoGoverno, 0)}</strong></div>
                <div class="painel-linha"><span>Desemprego</span><strong>${Formato.percentual(pais.api.desemprego, 1)}</strong></div>
                <div class="painel-linha"><span>Renda média</span><strong>${formatarMoeda(pais.api.rendaMedia)}</strong></div>
                <div class="painel-linha"><span>Estados com dado no mapa</span><strong>${numEstados}</strong></div>
                ${blocoDecisao('FEDERAL', pais.api.id)}
            `;
            ligarFormularioDecisao();
            return;
        }

        if (painelAtual.tipo === 'estado') {
            const uf = registro.estados.get(painelAtual.id);
            if (!uf) return fecharPainel();
            if (!uf.api) {
                painelConteudoEl.innerHTML = `
                    <h3>${uf.nome} (${uf.sigla})</h3>
                    <p class="painel-tag">Estado</p>
                    <p class="painel-nota">Este estado ainda não tem território de partida (fora da carga
                    inicial — ver SeedDados em docs/requisitos.toml, RF-26). A fronteira aparece só por
                    completude geográfica; população, tesouro e alíquota entram aqui assim que existir um
                    Estado de verdade nesta UF.</p>
                `;
                return;
            }
            const numCidades = [...registro.cidades.values()].filter((c) => c.estadoId === uf.id).length;
            painelConteudoEl.innerHTML = `
                <h3>${uf.nome} (${uf.sigla})</h3>
                <p class="painel-tag">Estado</p>
                <div class="painel-linha"><span>População</span><strong>${Formato.inteiro(uf.api.populacao)}</strong></div>
                <div class="painel-linha"><span>Tesouro estadual</span><strong>${formatarMoeda(uf.api.tesouro)}</strong></div>
                <div class="painel-linha"><span>Alíquota estadual</span><strong>${Formato.percentual(uf.api.aliquotaEstadual, 0)}</strong></div>
                <div class="painel-linha"><span>Investimento em infraestrutura</span><strong>${formatarMoeda(uf.api.investimentoInfraestrutura)}/turno</strong></div>
                <div class="painel-linha"><span>Índice de desenvolvimento</span><strong>${Formato.numero(uf.api.indiceDesenvolvimento, 1)}</strong></div>
                <div class="painel-linha"><span>Municípios no mapa</span><strong>${numCidades}</strong></div>
                ${blocoDecisao('ESTADUAL', uf.api.id)}
            `;
            ligarFormularioDecisao();
            return;
        }

        if (painelAtual.tipo === 'cidade') {
            const cidade = registro.cidades.get(painelAtual.id);
            if (!cidade) return fecharPainel();
            const uf = registro.estados.get(cidade.estadoId);
            if (!cidade.api) {
                painelConteudoEl.innerHTML = `
                    <h3>${cidade.nome}${cidade.capital ? ' (capital)' : ''}</h3>
                    <p class="painel-tag">Município — ${uf ? uf.nome : ''}</p>
                    <p class="painel-nota">Este município ainda não tem território de partida (fora da carga
                    inicial — ver SeedDados em docs/requisitos.toml, RF-26). O marcador aparece só por
                    completude geográfica; população, tesouro e alíquota entram aqui assim que existir um
                    Município de verdade aqui.</p>
                `;
                return;
            }
            painelConteudoEl.innerHTML = `
                <h3>${cidade.nome}${cidade.capital ? ' (capital)' : ''}</h3>
                <p class="painel-tag">Município — ${uf ? uf.nome : cidade.api.estado}</p>
                <div class="painel-linha"><span>População</span><strong>${Formato.inteiro(cidade.api.populacao)}</strong></div>
                <div class="painel-linha"><span>Tesouro municipal</span><strong>${formatarMoeda(cidade.api.tesouro)}</strong></div>
                <div class="painel-linha"><span>Alíquota municipal</span><strong>${Formato.percentual(cidade.api.aliquotaMunicipal, 0)}</strong></div>
                <div class="painel-linha"><span>Índice de urbanização</span><strong>${Formato.numero(cidade.api.indiceUrbanizacao, 1)}</strong></div>
                <div class="painel-linha"><span>Demanda imobiliária</span><strong>${Formato.numero(cidade.api.demandaImobiliaria, 0)}</strong></div>
                <div class="painel-linha"><span>Custo do terreno (m²)</span><strong>${formatarMoeda(cidade.api.custoTerrenoM2)}</strong></div>
                <div class="painel-linha"><span>Zoneamento</span><strong>${Formato.numero(cidade.api.zoneamento, 0)}</strong></div>
                ${blocoDecisao('MUNICIPAL', cidade.api.id)}
            `;
            ligarFormularioDecisao();
            return;
        }

        if (painelAtual.tipo === 'estrada') {
            const estrada = registro.estradas.get(painelAtual.id);
            if (!estrada) return fecharPainel();
            // metricas fica com placeholders (null) no geojson — o backend
            // ainda não modela infraestrutura viária como território. A
            // estrutura já está pronta pra receber dado real do motor de
            // simulação quando essa modelagem existir.
            const m = estrada.metricas || {};
            const p = (v) => (v === null || v === undefined) ? '—' : v;
            const linhaConecta = estrada.cidadesConectadas
                ? `<div class="painel-linha"><span>Conecta</span><strong>${estrada.cidadesConectadas
                    .map((id) => registro.cidades.get(id)?.nome ?? id).join(' ↔ ')}</strong></div>`
                : '';
            painelConteudoEl.innerHTML = `
                <h3>${estrada.nome}</h3>
                <p class="painel-tag">Camada logística — ${estrada.tipo}</p>
                ${linhaConecta}
                <div class="painel-linha"><span>Índice de desenvolvimento</span><strong>${p(m.indiceDesenvolvimento)}</strong></div>
                <div class="painel-linha"><span>Pavimentação</span><strong>${p(m.pavimentacao)}</strong></div>
                <div class="painel-linha"><span>Capacidade</span><strong>${p(m.capacidade)}</strong></div>
                <p class="painel-nota">Camada ilustrativa: o backend ainda não modela infraestrutura viária
                como território, então não há decisão nem dado real associado a rodovia — as métricas acima
                são placeholders prontos para receber o dado do motor de simulação quando essa modelagem
                existir (ver RF-26 em docs/requisitos.toml).</p>
            `;
            return;
        }
    }

    // ---------------------------------------------------------------
    // Geometria: bbox + interseção, para o lazy loading espacial
    // ---------------------------------------------------------------
    const PROFUNDIDADE_POR_TIPO = { Point: 0, LineString: 1, Polygon: 2, MultiLineString: 2, MultiPolygon: 3 };

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
    // Mapa Leaflet
    // ---------------------------------------------------------------
    const map = L.map('mapa-mapa', {
        minZoom: ZOOM.min,
        maxZoom: ZOOM.max, // nível mais profundo estritamente em Cidades
        zoomControl: true,
        // A animação de zoom do Leaflet trava no navegador embutido usado
        // pra testar (ver observação da RF-24 em docs/requisitos.toml); com
        // ela desligada o flyTo/drill-down volta a completar normalmente.
        zoomAnimation: false
    }).setView([-14.235, -51.9253], ZOOM.min);

    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
        attribution: '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a>',
        maxZoom: ZOOM.max
    }).addTo(map);

    // O cabecalho do jogo e montado por js/app.js depois de uma chamada a API,
    // entao a altura do container muda depois que o Leaflet ja mediu e os
    // ladrilhos ficam presos num quadrado pequeno.
    [0, 250, 1000].forEach((atraso) => setTimeout(() => map.invalidateSize(), atraso));
    window.addEventListener('resize', () => map.invalidateSize());

    const indicadorZoomEl = document.getElementById('indicador-zoom');
    function atualizarIndicadorZoom() {
        const zoom = map.getZoom();
        let nivel = 'País';
        if (zoom >= ZOOM.max) nivel = 'Cidades (nível máximo)';
        else if (zoom >= ZOOM.cidades) nivel = 'Estados + Cidades';
        else if (zoom >= ZOOM.estados) nivel = 'Estados';
        indicadorZoomEl.textContent = `Zoom ${zoom} — ${nivel}`;
    }
    map.on('zoomend', atualizarIndicadorZoom);

    // Preenchimento e borda de baixo contraste, cor neutra — o território é
    // só contexto geográfico; quem chama atenção na tela é a estrada.
    function estiloEstado() {
        return { color: CORES.estado, weight: 1, fillColor: CORES.estadoArea, fillOpacity: 0.35 };
    }

    function aoRenderizarPais(feature) {
        const marcador = L.marker(
            [feature.geometry.coordinates[1], feature.geometry.coordinates[0]],
            { icon: L.divIcon({ className: 'marcador-pais', html: 'Brasil', iconSize: [70, 26] }) }
        );
        marcador.on('click', (ev) => {
            L.DomEvent.stopPropagation(ev);
            abrirPainelPais();
        });
        return marcador;
    }

    function aoRenderizarEstado(feature) {
        const props = feature.properties;

        const layer = L.geoJSON(feature, { style: estiloEstado }).getLayers()[0];
        layer.bindTooltip(props.nome, { sticky: true });
        layer.on('click', (ev) => {
            L.DomEvent.stopPropagation(ev);
            abrirPainelEstado(props.id);
            if (map.getZoom() >= ZOOM.cidades) return;
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

    function limitesDasCidadesDoEstado(estadoId) {
        const cidades = [...registro.cidades.values()].filter((c) => c.estadoId === estadoId);
        if (cidades.length === 0) return null;
        return L.latLngBounds(cidades.map((c) => [c.coordenadas[1], c.coordenadas[0]]));
    }

    function aoRenderizarCidade(feature) {
        const props = feature.properties;

        const [lon, lat] = feature.geometry.coordinates;
        const marker = L.circleMarker([lat, lon], {
            radius: props.capital ? 8 : 5,
            color: CORES.cidade,
            fillColor: CORES.cidade,
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

        // MultiLineString: array de segmentos (cada um array de [lon,lat]) —
        // os segmentos vêm do OSM sem ordem ponta-a-ponta, mas o Leaflet
        // desenha cada um independente, então isso não importa visualmente.
        const coords = feature.geometry.type === 'MultiLineString'
            ? feature.geometry.coordinates.map((segmento) => segmento.map(([lon, lat]) => [lat, lon]))
            : feature.geometry.coordinates.map(([lon, lat]) => [lat, lon]);
        const linha = L.polyline(coords, { color: CORES.estrada, weight: 3, opacity: 0.9 });
        linha.on('mouseover', () => linha.setStyle({ weight: 7 }));
        linha.on('mouseout', () => linha.setStyle({ weight: 3 }));
        linha.on('click', (ev) => {
            L.DomEvent.stopPropagation(ev);
            abrirPainelEstrada(props.id);
        });
        return linha;
    }

    // País não tem geometria própria no jogo (não há malha territorial de
    // fronteira nacional aqui) — um único marcador no centro geográfico do
    // Brasil representa a entidade, visível só na faixa de zoom "País".
    const paisFC = {
        type: 'FeatureCollection',
        features: [{
            type: 'Feature',
            properties: { id: 'brasil' },
            geometry: { type: 'Point', coordinates: [-51.9253, -14.235] }
        }]
    };

    async function carregar() {
        try {
            const [estadosFC, cidadesFC, estradasFC, territorios, meusMandatos, tipos] = await Promise.all([
                fetch(URLS.estados).then((r) => r.json()),
                fetch(URLS.cidades).then((r) => r.json()),
                fetch(URLS.estradas).then((r) => r.json()),
                API.get('/politica/territorios'),
                API.get(`/politica/mandatos/jogador/${jogadorMapa.id}`),
                API.get('/politica/tipos-projeto')
            ]);
            mandatosJogador = meusMandatos;
            tiposProjeto = tipos;
            registro.pais = { id: 'brasil', api: territorios.paises[0] };

            // Dados de domínio são registrados uma única vez, na íntegra —
            // a hierarquia não pode depender de quais features já foram
            // materializadas no mapa (lazy loading é só visual). As
            // funções aoRenderizar* só leem este registro. `api` guarda o
            // registro vivo da partida casado por nome/sigla; sem match
            // (território fora da carga inicial), o painel simplesmente
            // não abre pra aquela feature.
            estadosFC.features.forEach((f) => {
                const api = territorios.estados.find((e) => normalizar(e.sigla) === normalizar(f.properties.sigla));
                registro.estados.set(f.properties.id, { ...f.properties, api });
            });
            cidadesFC.features.forEach((f) => {
                const estadoPai = estadosFC.features.find((e) => e.properties.id === f.properties.estadoId);
                const siglaEstado = estadoPai ? estadoPai.properties.sigla : '';
                const api = territorios.municipios.find((m) => normalizar(m.nome) === normalizar(f.properties.nome)
                    && normalizar(m.estado) === normalizar(siglaEstado));
                registro.cidades.set(f.properties.id, {
                    ...f.properties,
                    coordenadas: f.geometry.coordinates, // [lon, lat]
                    api
                });
            });
            estradasFC.features.forEach((f) => registro.estradas.set(f.properties.id, { ...f.properties }));

            // País: só no nível mais afastado — some por completo assim que
            // os estados entram em cena (exclusão mútua entre as camadas).
            criarCamadaLazy(map, paisFC, {
                zoomMin: ZOOM.min,
                zoomMax: ZOOM.estados - 1,
                aoRenderizar: aoRenderizarPais
            });

            // Estados: entram no nível "Estados" e continuam visíveis até o
            // zoom máximo (contexto atrás das cidades), mas deixam de
            // capturar clique de drill-down ao entrar no nível de cidade.
            criarCamadaLazy(map, estadosFC, {
                zoomMin: ZOOM.estados,
                zoomMax: ZOOM.max,
                aoRenderizar: aoRenderizarEstado
            });

            // Cidades: lazy loading real — só entram no nível mais profundo,
            // quando a feature está dentro do viewport.
            criarCamadaLazy(map, cidadesFC, {
                zoomMin: ZOOM.cidades,
                zoomMax: ZOOM.max,
                aoRenderizar: aoRenderizarCidade
            });

            // Estradas: destaque visual da malha logística — visíveis em
            // Estado e Cidade, nunca em País (regra explícita da RF-26).
            criarCamadaLazy(map, estradasFC, {
                zoomMin: ZOOM.estados,
                zoomMax: ZOOM.max,
                aoRenderizar: aoRenderizarEstrada
            });

            atualizarIndicadorZoom();
        } catch (erro) {
            Interface.mensagem('#mensagem', erro.message);
        }
    }

    carregar();
    map.on('click', fecharPainel);
})();
