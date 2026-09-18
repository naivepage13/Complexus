/* Listagem de empresas, catalogo de setores e fundacao de nova empresa. */

const jogadorEmpresas = Sessao.exigir();
let setoresCarregados = [];

async function iniciar() {
    if (!jogadorEmpresas) return;
    try {
        const [setores, territorios] = await Promise.all([
            API.get('/empresas/setores'),
            API.get('/politica/territorios')
        ]);
        setoresCarregados = setores;
        preencherSetores(setores);
        preencherMunicipios(territorios.municipios);
        await carregarEmpresas();
    } catch (erro) {
        Interface.mensagem('#mensagem', erro.message);
    }
}

function preencherSetores(setores) {
    const selecao = document.querySelector('#setor');
    const filtro = document.querySelector('#filtro-setor');
    selecao.innerHTML = setores.map((s) => `<option value="${s.nome}">${s.rotulo}</option>`).join('');
    filtro.innerHTML = '<option value="">Todos os setores</option>'
        + setores.map((s) => `<option value="${s.nome}">${s.rotulo}</option>`).join('');

    document.querySelector('#lista-setores').innerHTML = setores.map((s) => `
        <tr>
            <td>${s.rotulo}</td>
            <td class="direita">${Formato.percentual(s.margemBase, 1)}</td>
            <td class="direita">${Formato.dinheiroCurto(s.receitaPorFuncionario)}</td>
            <td class="direita">${Formato.percentual(s.giroAtivoMensal, 1)}</td>
            <td class="direita">${Formato.numero(s.multiploValuation, 1)}x</td>
            <td class="direita">${Formato.dinheiroCurto(s.capitalMinimo)}</td>
            <td class="direita">${Formato.numero(s.elasticidadeJuros, 2)}</td>
        </tr>`).join('');

    selecao.addEventListener('change', atualizarCapitalMinimo);
    document.querySelector('#capital').addEventListener('input', sugerirEquipe);
    atualizarCapitalMinimo();
}

function atualizarCapitalMinimo() {
    const escolhido = document.querySelector('#setor').value;
    const setor = setoresCarregados.find((s) => s.nome === escolhido);
    if (!setor) return;
    const capital = document.querySelector('#capital');
    capital.min = setor.capitalMinimo;
    if (!capital.value || Number(capital.value) < setor.capitalMinimo) {
        capital.value = setor.capitalMinimo;
    }
    sugerirEquipe();
}

/*
 * A producao e limitada pelo menor teto entre equipe e patrimonio. Como 70% do
 * capital inicial vira patrimonio, da para sugerir a equipe que aproveita toda
 * a estrutura sem gerar folha ociosa.
 */
function sugerirEquipe() {
    const setor = setoresCarregados.find((s) => s.nome === document.querySelector('#setor').value);
    if (!setor) return;
    const capital = Number(document.querySelector('#capital').value || setor.capitalMinimo);
    const patrimonio = capital * 0.7;
    const sugestao = Math.max(Math.round(patrimonio * setor.giroAtivoMensal / setor.receitaPorFuncionario), 1);
    document.querySelector('#capital-minimo').textContent =
        `Setor ${setor.rotulo}: capital minimo ${Formato.dinheiro(setor.capitalMinimo)} | `
        + `equipe sugerida para esse capital: ${sugestao} funcionarios `
        + `(teto de ${Formato.dinheiroCurto(patrimonio * setor.giroAtivoMensal)} de receita por mes).`;
    document.querySelector('#funcionarios').value = sugestao;
}

function preencherMunicipios(municipios) {
    document.querySelector('#municipio').innerHTML = municipios
        .map((m) => `<option value="${m.id}">${m.nome} (${m.estado})</option>`)
        .join('');
}

async function carregarEmpresas() {
    const setor = document.querySelector('#filtro-setor').value;
    const somenteMinhas = document.querySelector('#filtro-minhas').checked;
    const parametros = [];
    if (setor) parametros.push(`setor=${setor}`);
    if (somenteMinhas) parametros.push(`jogadorId=${jogadorEmpresas.id}`);
    const consulta = parametros.length ? `?${parametros.join('&')}` : '';

    const empresas = await API.get(`/empresas${consulta}`);
    const corpo = document.querySelector('#lista-empresas');
    if (!empresas.length) {
        corpo.innerHTML = '<tr><td colspan="9" class="suave">Nenhuma empresa encontrada.</td></tr>';
        return;
    }
    corpo.innerHTML = empresas.map((empresa) => `
        <tr>
            <td><a href="empresa.html?id=${empresa.id}">${empresa.nome}</a></td>
            <td><span class="etiqueta">${empresa.setorRotulo}</span></td>
            <td class="pequeno">${empresa.municipio}/${empresa.estado}</td>
            <td class="pequeno">${empresa.dono}</td>
            <td class="direita">${Formato.dinheiroCurto(empresa.receitaMensal)}</td>
            <td class="direita ${Formato.classe(empresa.lucroMensal)}">${Formato.dinheiroCurto(empresa.lucroMensal)}</td>
            <td class="direita">${Formato.percentual(empresa.marketShare, 1)}</td>
            <td class="direita">${Formato.dinheiroCurto(empresa.valuation)}</td>
            <td class="direita">
                ${empresa.capitalAberto ? '<span class="etiqueta etiqueta-primaria">na bolsa</span>' : ''}
            </td>
        </tr>`).join('');
}

document.querySelector('#filtro-setor').addEventListener('change', carregarEmpresas);
document.querySelector('#filtro-minhas').addEventListener('change', carregarEmpresas);

document.querySelector('#formulario-fundacao').addEventListener('submit', async (evento) => {
    evento.preventDefault();
    Interface.mensagem('#mensagem', '');
    try {
        const empresa = await API.post('/empresas', {
            jogadorId: jogadorEmpresas.id,
            nome: document.querySelector('#nome').value,
            setor: document.querySelector('#setor').value,
            municipioId: Number(document.querySelector('#municipio').value),
            capitalInicial: Number(document.querySelector('#capital').value),
            funcionarios: Number(document.querySelector('#funcionarios').value)
        });
        Interface.mensagem('#mensagem', `Empresa ${empresa.nome} fundada com sucesso.`, 'sucesso');
        document.querySelector('#nome').value = '';
        await carregarEmpresas();
        await Interface.montarCabecalho();
    } catch (erro) {
        Interface.mensagem('#mensagem', erro.message);
    }
});

iniciar();
