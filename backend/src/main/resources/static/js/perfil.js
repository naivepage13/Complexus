/*
 * Pagina de perfil: avatar (guardado so no navegador), dados de cadastro
 * somente leitura e os formularios de redefinir senha e alterar e-mail.
 */

const AVATAR_PADRAO = 'data:image/svg+xml;utf8,' + encodeURIComponent(
    '<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 72 72">'
    + '<rect width="72" height="72" fill="#eef1f6"/>'
    + '<circle cx="36" cy="28" r="14" fill="#c3cbd6"/>'
    + '<path d="M10 66c4-16 18-24 26-24s22 8 26 24" fill="#c3cbd6"/>'
    + '</svg>'
);

function chaveAvatar(jogadorId) {
    return `complexus.avatar.${jogadorId}`;
}

function carregarAvatar(jogadorId) {
    const alvo = document.querySelector('#avatar-preview');
    const guardado = localStorage.getItem(chaveAvatar(jogadorId));
    alvo.src = guardado || AVATAR_PADRAO;
}

function ligarUploadAvatar(jogadorId) {
    document.querySelector('#avatar-arquivo').addEventListener('change', (evento) => {
        const arquivo = evento.target.files[0];
        if (!arquivo) return;
        if (!['image/png', 'image/jpeg'].includes(arquivo.type)) {
            Interface.mensagem('#mensagem', 'Envie uma imagem PNG ou JPEG.', 'erro');
            return;
        }
        const leitor = new FileReader();
        leitor.onload = () => {
            localStorage.setItem(chaveAvatar(jogadorId), leitor.result);
            document.querySelector('#avatar-preview').src = leitor.result;
            Interface.mensagem('#mensagem', 'Avatar atualizado.', 'sucesso');
        };
        leitor.readAsDataURL(arquivo);
    });
}

function preencherDadosDaConta(jogador) {
    document.querySelector('#dado-usuario').textContent = jogador.usuario;
    document.querySelector('#dado-cadastro').textContent = Formato.momento(jogador.criadoEm);
    document.querySelector('#dado-horas').textContent = `${Formato.inteiro(jogador.horasEmJogo)} h`;
    document.querySelector('#dado-email').textContent = jogador.email || 'nao cadastrado';
}

function ligarFormularioEmail(jogadorId) {
    const form = document.querySelector('#form-email');
    const botao = document.querySelector('#botao-email');
    form.addEventListener('submit', async (evento) => {
        evento.preventDefault();
        const novoEmail = document.querySelector('#novo-email').value.trim();
        if (!Validacao.email(novoEmail)) {
            Interface.mensagem('#mensagem', 'Informe um e-mail em formato valido.', 'erro');
            return;
        }
        botao.disabled = true;
        const textoOriginal = botao.textContent;
        botao.textContent = 'Salvando...';
        try {
            const jogador = await API.post(`/jogadores/${jogadorId}/email`, { email: novoEmail });
            Sessao.salvar(jogador);
            preencherDadosDaConta(jogador);
            document.querySelector('#novo-email').value = '';
            Interface.mensagem('#mensagem', 'E-mail atualizado com sucesso.', 'sucesso');
        } catch (erro) {
            Interface.mensagem('#mensagem', erro.message, 'erro');
        } finally {
            botao.disabled = false;
            botao.textContent = textoOriginal;
        }
    });
}

function ligarFormularioSenha(jogadorId) {
    const form = document.querySelector('#form-senha');
    const botao = document.querySelector('#botao-senha');
    form.addEventListener('submit', async (evento) => {
        evento.preventDefault();
        const senhaAtual = document.querySelector('#senha-atual').value;
        const novaSenha = document.querySelector('#senha-nova').value;
        const confirmar = document.querySelector('#senha-confirmar').value;

        if (novaSenha.length < 4) {
            Interface.mensagem('#mensagem', 'A nova senha precisa de ao menos 4 caracteres.', 'erro');
            return;
        }
        if (novaSenha !== confirmar) {
            Interface.mensagem('#mensagem', 'A confirmacao nao coincide com a nova senha.', 'erro');
            return;
        }

        botao.disabled = true;
        const textoOriginal = botao.textContent;
        botao.textContent = 'Salvando...';
        try {
            await API.post(`/jogadores/${jogadorId}/senha`, { senhaAtual, novaSenha });
            form.reset();
            Interface.mensagem('#mensagem', 'Senha redefinida com sucesso.', 'sucesso');
        } catch (erro) {
            Interface.mensagem('#mensagem', erro.message, 'erro');
        } finally {
            botao.disabled = false;
            botao.textContent = textoOriginal;
        }
    });
}

/**
 * Fluxo de exclusao de conta: abre um modal de confirmacao (nunca exclui
 * direto no clique do botao), exige senha atual ou a palavra "EXCLUIR" antes
 * de habilitar o botao final, e limpa a sessao do navegador apos o sucesso.
 */
function ligarExclusaoDeConta(jogadorId) {
    const modal = document.querySelector('#modal-exclusao');
    const campoConfirmacao = document.querySelector('#confirmacao-exclusao');
    const botaoAbrir = document.querySelector('#botao-abrir-exclusao');
    const botaoCancelar = document.querySelector('#botao-cancelar-exclusao');
    const botaoConfirmar = document.querySelector('#botao-confirmar-exclusao');

    const confirmacaoValida = (valor) => {
        const texto = valor.trim();
        return texto.toUpperCase() === 'EXCLUIR' || texto.length >= 4;
    };

    const abrir = () => {
        campoConfirmacao.value = '';
        botaoConfirmar.disabled = true;
        Interface.mensagem('#mensagem-exclusao', '');
        modal.classList.remove('oculto');
        campoConfirmacao.focus();
    };

    const fechar = () => {
        modal.classList.add('oculto');
    };

    botaoAbrir.addEventListener('click', abrir);
    botaoCancelar.addEventListener('click', fechar);
    modal.addEventListener('click', (evento) => {
        if (evento.target === modal) fechar();
    });
    document.addEventListener('keydown', (evento) => {
        if (evento.key === 'Escape' && !modal.classList.contains('oculto')) fechar();
    });

    campoConfirmacao.addEventListener('input', () => {
        botaoConfirmar.disabled = !confirmacaoValida(campoConfirmacao.value);
    });

    botaoConfirmar.addEventListener('click', async () => {
        const valor = campoConfirmacao.value.trim();
        if (!confirmacaoValida(valor)) return;

        const corpo = valor.toUpperCase() === 'EXCLUIR'
            ? { confirmacao: 'EXCLUIR' }
            : { senha: valor };

        botaoConfirmar.disabled = true;
        const textoOriginal = botaoConfirmar.textContent;
        botaoConfirmar.textContent = 'Excluindo...';
        try {
            await API.post(`/jogadores/${jogadorId}/excluir`, corpo);
            localStorage.clear();
            window.location.href = 'login.html';
        } catch (erro) {
            Interface.mensagem('#mensagem-exclusao', erro.message, 'erro');
            botaoConfirmar.disabled = false;
            botaoConfirmar.textContent = textoOriginal;
        }
    });
}

document.addEventListener('DOMContentLoaded', async () => {
    const jogador = Sessao.exigir();
    if (!jogador) return;

    carregarAvatar(jogador.id);
    ligarUploadAvatar(jogador.id);
    ligarFormularioEmail(jogador.id);
    ligarFormularioSenha(jogador.id);
    ligarExclusaoDeConta(jogador.id);

    try {
        const atual = await API.get(`/jogadores/${jogador.id}`);
        Sessao.salvar(atual);
        preencherDadosDaConta(atual);
    } catch (erro) {
        Interface.mensagem('#mensagem', erro.message, 'erro');
    }
});
