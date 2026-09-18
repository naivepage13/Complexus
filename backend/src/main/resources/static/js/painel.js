/* Painel inicial: consolida jogador, empresas, carteira, mandatos e auditoria. */

const jogadorPainel = Sessao.exigir();

async function carregarPainel() {
    if (!jogadorPainel) return;
    try {
        const [painel, estatisticas, eventos] = await Promise.all([
            API.get(`/jogadores/${jogadorPainel.id}/painel`),
            API.get('/estatisticas/gerais'),
            API.get('/auditoria/eventos?limite=8')
        ]);

        document.querySelector('#patrimonio-total').textContent = Formato.dinheiroCurto(painel.patrimonioTotal);
        document.querySelector('#saldo').textContent = Formato.dinheiroCurto(painel.jogador.saldo);
        document.querySelector('#detalhe-patrimonio').textContent =
            `empresas ${Formato.dinheiroCurto(painel.valorEmpresas)} | carteira ${Formato.dinheiroCurto(painel.valorCarteira)}`;

        const lucro = document.querySelector('#lucro-empresas');
        lucro.textContent = Formato.dinheiroCurto(painel.lucroMensalEmpresas);
        lucro.className = `valor ${Formato.classe(painel.lucroMensalEmpresas)}`;

        const fechamento = estatisticas.ultimoFechamento;
        document.querySelector('#indice-mercado').textContent = fechamento
            ? Formato.numero(fechamento.indiceMercado, 1)
            : Formato.numero(estatisticas.estadoJogo.indiceMercado, 1);
        if (fechamento) {
            const detalhe = document.querySelector('#detalhe-indice');
            detalhe.textContent = `variacao ${Formato.percentual(fechamento.variacaoIndice)} no ultimo turno`;
            detalhe.className = `detalhe ${Formato.classe(fechamento.variacaoIndice)}`;
        }

        preencherEmpresas(painel.empresas);
        preencherCarteira(painel.carteira);
        preencherMandatos(painel.mandatos);
        preencherEventos(eventos);
    } catch (erro) {
        Interface.mensagem('#mensagem', erro.message);
    }
}

function preencherEmpresas(empresas) {
    const corpo = document.querySelector('#lista-empresas');
    if (!empresas.length) {
        corpo.innerHTML = '<tr><td colspan="5" class="suave">Voce ainda nao fundou empresas.</td></tr>';
        return;
    }
    corpo.innerHTML = empresas.map((empresa) => `
        <tr>
            <td><a href="empresa.html?id=${empresa.id}">${empresa.nome}</a></td>
            <td><span class="etiqueta">${empresa.setorRotulo}</span></td>
            <td class="direita ${Formato.classe(empresa.lucroMensal)}">${Formato.dinheiroCurto(empresa.lucroMensal)}</td>
            <td class="direita ${Formato.classe(empresa.crescimentoLucro)}">${Formato.percentual(empresa.crescimentoLucro)}</td>
            <td class="direita">${Formato.dinheiroCurto(empresa.valuation)}</td>
        </tr>`).join('');
}

function preencherCarteira(carteira) {
    const corpo = document.querySelector('#lista-carteira');
    if (!carteira.length) {
        corpo.innerHTML = '<tr><td colspan="4" class="suave">Nenhuma posicao aberta.</td></tr>';
        return;
    }
    corpo.innerHTML = carteira.map((posicao) => `
        <tr>
            <td>${posicao.empresa}</td>
            <td class="direita">${Formato.inteiro(posicao.acoes)}</td>
            <td class="direita">${Formato.dinheiroCurto(posicao.valorAtual)}</td>
            <td class="direita ${Formato.classe(posicao.retornoTotal)}">${Formato.dinheiroCurto(posicao.retornoTotal)}</td>
        </tr>`).join('');
}

function preencherMandatos(mandatos) {
    const corpo = document.querySelector('#lista-mandatos');
    if (!mandatos.length) {
        corpo.innerHTML = '<tr><td colspan="5" class="suave">Voce nao ocupa cargos publicos.</td></tr>';
        return;
    }
    corpo.innerHTML = mandatos.map((mandato) => `
        <tr>
            <td>${mandato.cargoRotulo}</td>
            <td>${mandato.esfera}</td>
            <td>${mandato.partido || '-'}</td>
            <td class="direita">${Formato.numero(mandato.aprovacao, 1)}</td>
            <td class="direita">turno ${mandato.turnoFim}</td>
        </tr>`).join('');
}

function preencherEventos(eventos) {
    const corpo = document.querySelector('#lista-eventos');
    if (!eventos.length) {
        corpo.innerHTML = '<tr><td colspan="3" class="suave">Sem eventos registrados.</td></tr>';
        return;
    }
    corpo.innerHTML = eventos.map((evento) => `
        <tr>
            <td>${evento.turno}</td>
            <td><span class="etiqueta">${evento.acao}</span></td>
            <td class="pequeno">${evento.descricao || '-'}</td>
        </tr>`).join('');
}

document.querySelector('#avancar-turno').addEventListener('click', async (evento) => {
    const botao = evento.currentTarget;
    botao.disabled = true;
    Interface.mensagem('#mensagem', 'Processando turno...', 'sucesso');
    try {
        const relatorio = await API.post('/jogo/turno/avancar?origem=PAINEL');
        Interface.mensagem('#mensagem',
            `Turno ${relatorio.turno} processado: ${relatorio.empresasProcessadas} empresas, `
            + `lucro agregado ${Formato.dinheiroCurto(relatorio.lucroAgregado)}.`, 'sucesso');
        await carregarPainel();
    } catch (erro) {
        Interface.mensagem('#mensagem', erro.message);
    } finally {
        botao.disabled = false;
    }
});

carregarPainel();
