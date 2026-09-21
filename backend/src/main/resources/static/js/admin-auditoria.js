/*
 * Console administrativo da linha de auditoria.
 *
 * Fora do menu do jogo: todas as chamadas vao para /api/admin/auditoria e
 * levam o cabecalho X-Admin-Token. Sem token valido o servidor devolve 403.
 */

const CHAVE_TOKEN = 'complexus.admin.token';

function token() {
    return sessionStorage.getItem(CHAVE_TOKEN) || '';
}

async function apiAdmin(caminho) {
    const resposta = await fetch(`/api/admin${caminho}`, {
        headers: { 'Content-Type': 'application/json', 'X-Admin-Token': token() }
    });
    const texto = await resposta.text();
    const corpo = texto ? JSON.parse(texto) : null;
    if (!resposta.ok) {
        throw new Error(corpo && corpo.mensagem ? corpo.mensagem : `Erro ${resposta.status}`);
    }
    return corpo;
}

function mostrarConteudo(visivel) {
    document.querySelector('#area-conteudo').classList.toggle('oculto', !visivel);
    document.querySelector('#area-credencial').classList.toggle('oculto', visivel);
}

async function carregar(turno) {
    if (!token()) {
        mostrarConteudo(false);
        return;
    }
    try {
        const consulta = turno ? `?turno=${turno}` : '?limite=100';
        const [eventos, razao] = await Promise.all([
            apiAdmin(`/auditoria/eventos${consulta}`),
            apiAdmin('/auditoria/razao?limite=100')
        ]);
        mostrarConteudo(true);
        Interface.mensagem('#mensagem', '');
        desenharEventos(eventos);
        desenharRazao(razao);
        document.querySelector('#total-eventos').textContent = Formato.inteiro(eventos.length);
        if (eventos.length) {
            document.querySelector('#ultimo-hash').textContent = `${eventos[0].hash.slice(0, 32)}...`;
        }
    } catch (erro) {
        mostrarConteudo(false);
        Interface.mensagem('#mensagem', erro.message);
    }
}

function desenharEventos(eventos) {
    const corpo = document.querySelector('#lista-eventos');
    if (!eventos.length) {
        corpo.innerHTML = '<tr><td colspan="8" class="suave">Nenhum evento encontrado.</td></tr>';
        return;
    }
    corpo.innerHTML = eventos.map((evento) => `
        <tr>
            <td>${evento.id}</td>
            <td class="pequeno">${Formato.momento(evento.momento)}</td>
            <td>${evento.turno}</td>
            <td class="pequeno">${evento.ator}</td>
            <td><span class="etiqueta">${evento.acao}</span></td>
            <td class="pequeno">${evento.entidade}${evento.entidadeId ? ` #${evento.entidadeId}` : ''}</td>
            <td class="pequeno">${evento.descricao || '-'}</td>
            <td class="pequeno suave" title="${evento.hash}">${evento.hash.slice(0, 10)}...</td>
        </tr>`).join('');
}

function desenharRazao(razao) {
    const corpo = document.querySelector('#lista-razao');
    if (!razao.length) {
        corpo.innerHTML = '<tr><td colspan="7" class="suave">Sem lancamentos.</td></tr>';
        return;
    }
    corpo.innerHTML = razao.map((linha) => `
        <tr>
            <td>${linha.id}</td>
            <td>${linha.turno}</td>
            <td><span class="etiqueta">${linha.tipoRotulo}</span></td>
            <td class="pequeno">${linha.origem}</td>
            <td class="pequeno">${linha.destino}</td>
            <td class="direita">${Formato.dinheiro(linha.valor)}</td>
            <td class="pequeno">${linha.descricao || '-'}</td>
        </tr>`).join('');
}

document.querySelector('#formulario-token').addEventListener('submit', (evento) => {
    evento.preventDefault();
    sessionStorage.setItem(CHAVE_TOKEN, document.querySelector('#token').value);
    document.querySelector('#token').value = '';
    carregar(null);
});

document.querySelector('#esquecer').addEventListener('click', () => {
    sessionStorage.removeItem(CHAVE_TOKEN);
    mostrarConteudo(false);
    Interface.mensagem('#mensagem', 'Token removido deste navegador.', 'sucesso');
});

document.querySelector('#verificar').addEventListener('click', async () => {
    Interface.mensagem('#mensagem', '');
    try {
        const resultado = await apiAdmin('/auditoria/integridade');
        document.querySelector('#total-eventos').textContent = Formato.inteiro(resultado.totalEventos);
        const estado = document.querySelector('#estado-cadeia');
        estado.textContent = resultado.cadeiaIntegra ? 'integra' : 'violada';
        estado.className = `valor ${resultado.cadeiaIntegra ? 'positivo' : 'negativo'}`;
        document.querySelector('#detalhe-cadeia').textContent = resultado.cadeiaIntegra
            ? `${resultado.totalEventos} eventos verificados`
            : `${resultado.falhas.length} elo(s) divergente(s)`;
        document.querySelector('#ultimo-hash').textContent = `${resultado.ultimoHash.slice(0, 32)}...`;
        Interface.mensagem('#mensagem',
            resultado.cadeiaIntegra
                ? 'Cadeia de auditoria integra.'
                : `Cadeia comprometida em ${resultado.falhas.length} evento(s).`,
            resultado.cadeiaIntegra ? 'sucesso' : 'erro');
    } catch (erro) {
        Interface.mensagem('#mensagem', erro.message);
    }
});

document.querySelector('#filtrar').addEventListener('click', () => {
    const turno = document.querySelector('#filtro-turno').value;
    carregar(turno ? Number(turno) : null);
});

document.querySelector('#limpar').addEventListener('click', () => {
    document.querySelector('#filtro-turno').value = '';
    carregar(null);
});

carregar(null);
