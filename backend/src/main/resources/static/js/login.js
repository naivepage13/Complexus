/* Tela de login e cadastro simplificado. */

const formularioLogin = document.querySelector('#formulario-login');
const formularioCadastro = document.querySelector('#formulario-cadastro');

document.querySelector('#abrir-cadastro').addEventListener('click', () => {
    formularioCadastro.classList.remove('oculto');
});

document.querySelector('#cancelar-cadastro').addEventListener('click', () => {
    formularioCadastro.classList.add('oculto');
});

formularioLogin.addEventListener('submit', async (evento) => {
    evento.preventDefault();
    Interface.mensagem('#mensagem', '');
    try {
        const jogador = await API.post('/jogadores/login', {
            usuario: document.querySelector('#usuario').value,
            senha: document.querySelector('#senha').value
        });
        Sessao.salvar(jogador);
        window.location.href = 'index.html';
    } catch (erro) {
        Interface.mensagem('#mensagem', erro.message);
    }
});

formularioCadastro.addEventListener('submit', async (evento) => {
    evento.preventDefault();
    Interface.mensagem('#mensagem', '');
    try {
        const jogador = await API.post('/jogadores/registrar', {
            usuario: document.querySelector('#novo-usuario').value,
            nome: document.querySelector('#novo-nome').value,
            senha: document.querySelector('#nova-senha').value
        });
        Sessao.salvar(jogador);
        window.location.href = 'index.html';
    } catch (erro) {
        Interface.mensagem('#mensagem', erro.message);
    }
});
