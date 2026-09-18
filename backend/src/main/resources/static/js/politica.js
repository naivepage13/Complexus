/* Cargos, mandatos, projetos de lei, votacao e sancao. */

const jogadorPolitico = Sessao.exigir();
let cargos = [];
let tipos = [];
let territorios = { paises: [], estados: [], municipios: [] };
let meusMandatos = [];

async function iniciar() {
    if (!jogadorPolitico) return;
    try {
        [cargos, tipos, territorios] = await Promise.all([
            API.get('/politica/cargos'),
            API.get('/politica/tipos-projeto'),
            API.get('/politica/territorios')
        ]);
        preencherCargos();
        preencherTipos();
        preencherComposicao();
        await Promise.all([carregarMandatos(), carregarProjetos(), carregarLeis()]);
    } catch (erro) {
        Interface.mensagem('#mensagem', erro.message);
    }
}

function opcoesTerritorio(esfera) {
    if (esfera === 'FEDERAL') {
        return territorios.paises.map((p) => ({ id: p.id, rotulo: p.nome }));
    }
    if (esfera === 'ESTADUAL') {
        return territorios.estados.map((e) => ({ id: e.id, rotulo: `${e.nome} (${e.sigla})` }));
    }
    return territorios.municipios.map((m) => ({ id: m.id, rotulo: `${m.nome}/${m.estado}` }));
}

function nomeTerritorio(esfera, id) {
    const opcao = opcoesTerritorio(esfera).find((item) => item.id === id);
    return opcao ? opcao.rotulo : `#${id}`;
}

function preencherCargos() {
    const selecao = document.querySelector('#cargo');
    selecao.innerHTML = cargos
        .filter((cargo) => cargo.eletivo)
        .map((cargo) => `<option value="${cargo.nome}">${cargo.rotulo} (${cargo.esfera})</option>`)
        .join('');
    selecao.addEventListener('change', atualizarTerritorios);
    atualizarTerritorios();
}

function atualizarTerritorios() {
    const escolhido = document.querySelector('#cargo').value;
    const cargo = cargos.find((c) => c.nome === escolhido);
    if (!cargo) return;
    document.querySelector('#territorio').innerHTML = opcoesTerritorio(cargo.esfera)
        .map((item) => `<option value="${item.id}">${item.rotulo}</option>`)
        .join('');
    document.querySelector('#detalhe-cargo').textContent =
        `${cargo.rotulo}: ${cargo.vagas} cadeira(s) por territorio, mandato de ${cargo.duracaoMandatoTurnos} turnos. `
        + `${cargo.legislativo ? 'Propoe e vota projetos.' : ''} `
        + `${cargo.chefiaExecutivo ? 'Sanciona ou veta leis da esfera.' : ''}`;
}

function preencherTipos() {
    const selecao = document.querySelector('#tipo-projeto');
    selecao.innerHTML = tipos.map((tipo) => `<option value="${tipo.nome}">${tipo.descricao}</option>`).join('');
    selecao.addEventListener('change', atualizarDetalheTipo);
    atualizarDetalheTipo();
}

function atualizarDetalheTipo() {
    const escolhido = document.querySelector('#tipo-projeto').value;
    const tipo = tipos.find((t) => t.nome === escolhido);
    if (!tipo) return;
    document.querySelector('#detalhe-tipo').textContent =
        `Esferas: ${tipo.esferas.join(', ')} | parametro entre ${tipo.parametroMinimo} e ${tipo.parametroMaximo} `
        + `(${tipo.unidadeParametro})${tipo.exigeSetor ? ' | exige setor alvo' : ''}.`;
    const parametro = document.querySelector('#parametro-projeto');
    parametro.min = tipo.parametroMinimo;
    parametro.max = tipo.parametroMaximo;
}

async function carregarMandatos() {
    meusMandatos = await API.get(`/politica/mandatos/jogador/${jogadorPolitico.id}`);
    const corpo = document.querySelector('#lista-meus-mandatos');
    if (!meusMandatos.length) {
        corpo.innerHTML = '<tr><td colspan="6" class="suave">Voce ainda nao ocupa cargos.</td></tr>';
    } else {
        corpo.innerHTML = meusMandatos.map((mandato) => `
            <tr>
                <td>${mandato.cargoRotulo}</td>
                <td>${mandato.esfera}</td>
                <td>${nomeTerritorio(mandato.esfera, mandato.territorioId)}</td>
                <td>${mandato.partido || '-'}</td>
                <td class="direita">${Formato.numero(mandato.aprovacao, 1)}</td>
                <td class="direita">turno ${mandato.turnoFim}</td>
            </tr>`).join('');
    }

    document.querySelector('#mandato-projeto').innerHTML = meusMandatos.length
        ? meusMandatos.map((m) => `<option value="${m.id}">${m.cargoRotulo} - ${nomeTerritorio(m.esfera, m.territorioId)}</option>`).join('')
        : '<option value="">Nenhum mandato disponivel</option>';
}

async function carregarProjetos() {
    const projetos = await API.get('/politica/projetos');
    const corpo = document.querySelector('#lista-projetos');
    if (!projetos.length) {
        corpo.innerHTML = '<tr><td colspan="8" class="suave">Nenhum projeto protocolado.</td></tr>';
        return;
    }
    corpo.innerHTML = projetos.map((projeto) => `
        <tr>
            <td>${projeto.titulo}<div class="suave pequeno">${projeto.ementa || ''}</div></td>
            <td class="pequeno">${projeto.autor}<div class="suave">${projeto.autorCargo}</div></td>
            <td>${projeto.esfera}</td>
            <td class="pequeno">${projeto.tipoDescricao}</td>
            <td class="direita">${Formato.numero(projeto.parametro, 4)}</td>
            <td><span class="etiqueta ${classeStatus(projeto.status)}">${projeto.statusRotulo}</span></td>
            <td class="direita pequeno">${projeto.votosSim} sim / ${projeto.votosNao} nao</td>
            <td>${acoesProjeto(projeto)}</td>
        </tr>`).join('');

    corpo.querySelectorAll('[data-acao]').forEach((botao) => {
        botao.addEventListener('click', () => executarAcao(botao.dataset));
    });
}

function classeStatus(status) {
    if (status === 'SANCIONADO') return 'etiqueta-positiva';
    if (status === 'REJEITADO' || status === 'VETADO') return 'etiqueta-negativa';
    if (status === 'EM_VOTACAO' || status === 'APROVADO') return 'etiqueta-alerta';
    return '';
}

function acoesProjeto(projeto) {
    const botoes = [];
    const mandatoAutor = meusMandatos.find((m) => m.id === projeto.autorMandatoId);
    const mandatoNaCasa = meusMandatos.find((m) => m.esfera === projeto.esfera
        && m.territorioId === projeto.territorioId && m.legislativo);
    const chefeExecutivo = meusMandatos.find((m) => m.esfera === projeto.esfera
        && m.territorioId === projeto.territorioId && m.executivo);

    if (projeto.status === 'RASCUNHO' && mandatoAutor) {
        botoes.push(`<button class="botao botao-pequeno" data-acao="pautar" data-projeto="${projeto.id}">Pautar</button>`);
    }
    if (projeto.status === 'EM_VOTACAO' && mandatoNaCasa) {
        botoes.push(`<button class="botao botao-pequeno" data-acao="votar" data-opcao="SIM" data-projeto="${projeto.id}" data-mandato="${mandatoNaCasa.id}">Sim</button>`);
        botoes.push(`<button class="botao botao-pequeno botao-neutro" data-acao="votar" data-opcao="NAO" data-projeto="${projeto.id}" data-mandato="${mandatoNaCasa.id}">Nao</button>`);
    }
    if (projeto.status === 'APROVADO' && chefeExecutivo) {
        botoes.push(`<button class="botao botao-pequeno" data-acao="sancionar" data-projeto="${projeto.id}">Sancionar</button>`);
        botoes.push(`<button class="botao botao-pequeno botao-neutro" data-acao="vetar" data-projeto="${projeto.id}">Vetar</button>`);
    }
    if (projeto.status === 'VETADO' && mandatoNaCasa) {
        botoes.push(`<button class="botao botao-pequeno botao-secundario" data-acao="derrubar" data-projeto="${projeto.id}" data-mandato="${mandatoNaCasa.id}">Derrubar veto</button>`);
    }
    return botoes.length ? `<div class="acoes">${botoes.join('')}</div>` : '<span class="suave pequeno">-</span>';
}

async function executarAcao(dados) {
    Interface.mensagem('#mensagem', '');
    const id = dados.projeto;
    try {
        if (dados.acao === 'pautar') {
            await API.post(`/politica/projetos/${id}/pautar`, { jogadorId: jogadorPolitico.id });
            Interface.mensagem('#mensagem', 'Projeto pautado. A apuracao ocorre no proximo turno.', 'sucesso');
        } else if (dados.acao === 'votar') {
            await API.post(`/politica/projetos/${id}/votar`, {
                jogadorId: jogadorPolitico.id,
                mandatoId: Number(dados.mandato),
                opcao: dados.opcao
            });
            Interface.mensagem('#mensagem', `Voto ${dados.opcao} registrado.`, 'sucesso');
        } else if (dados.acao === 'sancionar' || dados.acao === 'vetar') {
            const sancionar = dados.acao === 'sancionar';
            await API.post(`/politica/projetos/${id}/sancionar`, {
                jogadorId: jogadorPolitico.id,
                sancionar,
                justificativa: sancionar ? null : 'Veto por decisao do executivo.'
            });
            Interface.mensagem('#mensagem', sancionar ? 'Lei sancionada.' : 'Projeto vetado.', 'sucesso');
        } else if (dados.acao === 'derrubar') {
            await API.post(`/politica/projetos/${id}/derrubar-veto`, {
                jogadorId: jogadorPolitico.id,
                mandatoId: Number(dados.mandato)
            });
            Interface.mensagem('#mensagem', 'Veto derrubado e lei promulgada.', 'sucesso');
        }
        await Promise.all([carregarProjetos(), carregarLeis()]);
    } catch (erro) {
        Interface.mensagem('#mensagem', erro.message);
    }
}

async function carregarLeis() {
    const leis = await API.get('/politica/leis');
    const corpo = document.querySelector('#lista-leis');
    if (!leis.length) {
        corpo.innerHTML = '<tr><td colspan="5" class="suave">Nenhuma lei em vigor.</td></tr>';
        return;
    }
    corpo.innerHTML = leis.map((lei) => `
        <tr>
            <td>${lei.titulo}</td>
            <td class="pequeno">${lei.tipoDescricao}</td>
            <td>${lei.esfera}</td>
            <td class="direita">${lei.turnoVigencia ?? '-'}</td>
            <td class="pequeno">${lei.efeitoAplicado || '-'}</td>
        </tr>`).join('');
}

function preencherComposicao() {
    const selecao = document.querySelector('#territorio-composicao');
    const opcoes = [
        ...territorios.paises.map((p) => ({ valor: `FEDERAL:${p.id}`, rotulo: `Congresso - ${p.nome}` })),
        ...territorios.estados.map((e) => ({ valor: `ESTADUAL:${e.id}`, rotulo: `Assembleia - ${e.nome}` })),
        ...territorios.municipios.map((m) => ({ valor: `MUNICIPAL:${m.id}`, rotulo: `Camara - ${m.nome}` }))
    ];
    selecao.innerHTML = opcoes.map((o) => `<option value="${o.valor}">${o.rotulo}</option>`).join('');
    selecao.addEventListener('change', carregarComposicao);
    carregarComposicao();
}

async function carregarComposicao() {
    const [esfera, territorioId] = document.querySelector('#territorio-composicao').value.split(':');
    const mandatos = await API.get(`/politica/mandatos?esfera=${esfera}&territorioId=${territorioId}`);
    const corpo = document.querySelector('#lista-composicao');
    if (!mandatos.length) {
        corpo.innerHTML = '<tr><td colspan="5" class="suave">Sem mandatos ativos.</td></tr>';
        return;
    }
    corpo.innerHTML = mandatos.map((mandato) => `
        <tr>
            <td>${mandato.titular}</td>
            <td>${mandato.cargoRotulo}</td>
            <td>${mandato.partido || '-'}</td>
            <td class="direita">${Formato.numero(mandato.aprovacao, 1)}</td>
            <td>${mandato.npc ? '<span class="etiqueta">NPC</span>' : '<span class="etiqueta etiqueta-primaria">Jogador</span>'}</td>
        </tr>`).join('');
}

document.querySelector('#formulario-posse').addEventListener('submit', async (evento) => {
    evento.preventDefault();
    Interface.mensagem('#mensagem', '');
    try {
        await API.post('/politica/mandatos', {
            jogadorId: jogadorPolitico.id,
            cargo: document.querySelector('#cargo').value,
            territorioId: Number(document.querySelector('#territorio').value),
            partido: document.querySelector('#partido').value
        });
        Interface.mensagem('#mensagem', 'Posse registrada.', 'sucesso');
        await Promise.all([carregarMandatos(), carregarComposicao()]);
    } catch (erro) {
        Interface.mensagem('#mensagem', erro.message);
    }
});

document.querySelector('#formulario-projeto').addEventListener('submit', async (evento) => {
    evento.preventDefault();
    Interface.mensagem('#mensagem', '');
    const setor = document.querySelector('#setor-projeto').value;
    try {
        await API.post('/politica/projetos', {
            jogadorId: jogadorPolitico.id,
            mandatoId: Number(document.querySelector('#mandato-projeto').value),
            titulo: document.querySelector('#titulo-projeto').value,
            ementa: document.querySelector('#ementa-projeto').value,
            tipo: document.querySelector('#tipo-projeto').value,
            setorAlvo: setor || null,
            parametro: Number(document.querySelector('#parametro-projeto').value)
        });
        Interface.mensagem('#mensagem', 'Projeto protocolado como rascunho. Pauta-o para ir a votacao.', 'sucesso');
        await carregarProjetos();
    } catch (erro) {
        Interface.mensagem('#mensagem', erro.message);
    }
});

iniciar();
