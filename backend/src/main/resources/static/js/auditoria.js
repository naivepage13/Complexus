/* Consulta da linha de auditoria, verificacao da cadeia e livro-razao. */

Sessao.exigir();

async function carregar(turno) {
    try {
        const consulta = turno ? `?turno=${turno}` : '?limite=100';
        const [eventos, razao] = await Promise.all([
            API.get(`/auditoria/eventos${consulta}`),
            API.get('/auditoria/razao?limite=100')
        ]);
        desenharEventos(eventos);
        desenharRazao(razao);
        document.querySelector('#total-eventos').textContent = Formato.inteiro(eventos.length);
        if (eventos.length) {
            document.querySelector('#ultimo-hash').textContent = eventos[0].hash.slice(0, 32) + '...';
        }
    } catch (erro) {
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

document.querySelector('#verificar').addEventListener('click', async () => {
    Interface.mensagem('#mensagem', '');
    try {
        const resultado = await API.get('/auditoria/integridade');
        document.querySelector('#total-eventos').textContent = Formato.inteiro(resultado.totalEventos);
        const estado = document.querySelector('#estado-cadeia');
        estado.textContent = resultado.cadeiaIntegra ? 'integra' : 'violada';
        estado.className = `valor ${resultado.cadeiaIntegra ? 'positivo' : 'negativo'}`;
        document.querySelector('#detalhe-cadeia').textContent = resultado.cadeiaIntegra
            ? `${resultado.totalEventos} eventos verificados`
            : `${resultado.falhas.length} elo(s) divergente(s)`;
        document.querySelector('#ultimo-hash').textContent = resultado.ultimoHash.slice(0, 32) + '...';
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
