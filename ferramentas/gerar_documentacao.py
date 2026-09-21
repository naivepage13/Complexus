"""Gerador da documentação viva do Complexus.

Junta duas fontes — o código que existe (ferramentas/inventario.py) e o
catálogo de requisitos escrito à mão (docs/requisitos.toml) — e reescreve:

    docs/REQUISITOS.md   relatório de requisitos funcionais e não funcionais
    docs/INVENTARIO.md   mapa técnico: endpoints, entidades, serviços, páginas
    README.md            blocos de estado do projeto e índice da documentação
    docs/RELATORIO.md    blocos de métricas e de situação dos requisitos
    docs/ROADMAP.md      bloco com o que ainda não foi entregue

Arquivos inteiros são regerados; nos demais, só o conteúdo entre marcadores
<!-- auto:inicio:NOME --> e <!-- auto:fim:NOME --> muda. O texto escrito por
pessoas fora dos marcadores nunca é tocado.

A saída é determinística: depende do código e do catálogo, nunca do relógio.
É o que permite o modo --verificar acusar documentação desatualizada.

Uso:
    python ferramentas/gerar_documentacao.py              reescreve
    python ferramentas/gerar_documentacao.py --verificar  só confere (CI e hook)
"""

from __future__ import annotations

import argparse
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))

from inventario import Inventario, coletar  # noqa: E402
from requisitos import Catalogo, CatalogoInvalido, Requisito, carregar  # noqa: E402

AVISO = (
    "<!-- Arquivo gerado por ferramentas/gerar_documentacao.py. "
    "Nao edite a mao: mude docs/requisitos.toml ou o codigo e rode o gerador. -->"
)


# ---------------------------------------------------------------------------
# Renderização
# ---------------------------------------------------------------------------

def tabela(cabecalho: list[str], linhas: list[list[str]]) -> str:
    """Tabela Markdown. Devolve aviso quando não há linhas."""
    if not linhas:
        return "_Nada a listar._"
    partes = ["| " + " | ".join(cabecalho) + " |",
              "|" + "|".join(["---"] * len(cabecalho)) + "|"]
    partes += ["| " + " | ".join(celula or "-" for celula in linha) + " |" for linha in linhas]
    return "\n".join(partes)


def bloco_estado(inv: Inventario, cat: Catalogo) -> str:
    resumo = cat.resumo()
    return "\n".join([
        tabela(
            ["Indicador", "Valor"],
            [
                ["Versão", f"`{inv.versao}`"],
                ["Requisitos entregues", f"{resumo['implementados']} de {resumo['total']} "
                                         f"({resumo['percentual_entregue']}%)"],
                ["Requisitos funcionais", str(resumo["funcionais"])],
                ["Requisitos não funcionais", str(resumo["nao_funcionais"])],
                ["Testes automatizados", f"{inv.total_testes} "
                                         f"({inv.testes_java} Java + {inv.testes_python} Python)"],
                ["Endpoints da API", f"{inv.endpoints_publicos} públicos + "
                                     f"{inv.endpoints_administrativos} administrativos"],
                ["Entidades persistidas", str(len(inv.entidades))],
                ["Páginas do frontend", str(len(inv.paginas))],
            ],
        ),
        "",
        "Situação por requisito em [docs/REQUISITOS.md](docs/REQUISITOS.md); "
        "mapa do código em [docs/INVENTARIO.md](docs/INVENTARIO.md).",
    ])


def bloco_documentacao() -> str:
    return tabela(
        ["Documento", "Conteúdo", "Origem"],
        [
            ["[docs/RELATORIO.md](docs/RELATORIO.md)",
             "Relatório geral: estado, entregas, decisões e limitações", "Misto"],
            ["[docs/REQUISITOS.md](docs/REQUISITOS.md)",
             "Requisitos funcionais e não funcionais, com rastreabilidade", "Gerado"],
            ["[docs/INVENTARIO.md](docs/INVENTARIO.md)",
             "Mapa técnico: endpoints, entidades, serviços e páginas", "Gerado"],
            ["[docs/ARQUITETURA.md](docs/ARQUITETURA.md)",
             "Camadas, modelo de dados e decisões de arquitetura", "Manual"],
            ["[docs/REGRAS-DO-JOGO.md](docs/REGRAS-DO-JOGO.md)",
             "Fórmulas e regras de economia, política e investimento", "Manual"],
            ["[docs/API.md](docs/API.md)", "Contrato dos endpoints REST", "Manual"],
            ["[docs/AUDITORIA.md](docs/AUDITORIA.md)",
             "Linha de auditoria do jogo e do desenvolvimento", "Manual"],
            ["[docs/ROADMAP.md](docs/ROADMAP.md)", "Próximos ciclos e dívidas técnicas", "Misto"],
            ["[CHANGELOG.md](CHANGELOG.md)", "Histórico de versões", "Manual"],
            ["[docs/requisitos.toml](docs/requisitos.toml)",
             "Catálogo de requisitos: fonte única dos relatórios", "Manual"],
        ],
    )


def bloco_metricas(inv: Inventario) -> str:
    linhas = [[linguagem, f"{total:,}".replace(",", ".")]
              for linguagem, total in sorted(inv.linhas.items(), key=lambda par: -par[1])]
    total_geral = sum(inv.linhas.values())
    linhas.append(["**Total**", f"**{total_geral:,}**".replace(",", ".")])

    return "\n".join([
        f"Versão `{inv.versao}` · {inv.total_testes} testes · "
        f"{len(inv.endpoints)} endpoints · {len(inv.entidades)} entidades · "
        f"{len(inv.servicos)} serviços · {len(inv.paginas)} páginas.",
        "",
        "Linhas de código não vazias, sem contar `legado/` e artefatos de build:",
        "",
        tabela(["Linguagem", "Linhas"], linhas),
    ])


def bloco_requisitos(cat: Catalogo) -> str:
    resumo = cat.resumo()
    partes = [
        tabela(
            ["Situação", "Funcionais", "Não funcionais", "Total"],
            [
                [
                    rotulo,
                    str(sum(1 for r in cat.funcionais if r.status == status)),
                    str(sum(1 for r in cat.nao_funcionais if r.status == status)),
                    str(len(cat.por_status(status))),
                ]
                for status, rotulo in (
                    ("IMPLEMENTADO", "Entregue"),
                    ("PARCIAL", "Parcial"),
                    ("PLANEJADO", "Planejado"),
                )
            ],
        ),
        "",
        f"{resumo['entregues_com_teste']} dos {resumo['implementados']} requisitos entregues "
        "têm teste automatizado declarado.",
    ]

    lacunas = cat.lacunas_de_teste()
    if lacunas:
        partes += [
            "",
            "**Lacunas de cobertura** — entregues sem teste declarado:",
            "",
            tabela(
                ["Requisito", "Título", "Situação"],
                [[r.id, r.titulo, r.rotulo_status] for r in lacunas],
            ),
        ]
    return "\n".join(partes)


def bloco_planejados(cat: Catalogo) -> str:
    pendentes = [r for r in cat.requisitos if r.status in ("PLANEJADO", "PARCIAL")]
    pendentes.sort(key=lambda r: ({"ALTA": 0, "MEDIA": 1, "BAIXA": 2}[r.prioridade], r.tipo, r.numero))
    return tabela(
        ["Prioridade", "Requisito", "Título", "Situação", "Por quê"],
        [[r.prioridade.capitalize(), r.id, r.titulo, r.rotulo_status, r.descricao]
         for r in pendentes],
    )


def documento_requisitos(cat: Catalogo, inv: Inventario) -> str:
    resumo = cat.resumo()
    partes = [
        "# Relatório de requisitos",
        "",
        AVISO,
        "",
        f"Projeto **{cat.projeto}**, versão `{inv.versao}`. "
        f"{resumo['implementados']} de {resumo['total']} requisitos entregues "
        f"({resumo['percentual_entregue']}%).",
        "",
        "Cada requisito declara o critério de aceite, os arquivos que o implementam e os "
        "testes que o cobrem. O gerador falha se um arquivo declarado não existir, então a "
        "rastreabilidade não envelhece em silêncio.",
        "",
        "## Panorama",
        "",
        bloco_requisitos(cat),
        "",
        "## Requisitos funcionais",
        "",
        _resumo_por_tipo(cat.funcionais),
        "",
    ]
    partes += [_ficha(r) for r in cat.funcionais]
    partes += [
        "## Requisitos não funcionais",
        "",
        _resumo_por_tipo(cat.nao_funcionais),
        "",
    ]
    partes += [_ficha(r) for r in cat.nao_funcionais]
    return "\n".join(partes).rstrip() + "\n"


def _resumo_por_tipo(requisitos: list[Requisito]) -> str:
    return tabela(
        ["ID", "Título", "Situação", "Prioridade", "Versão", "Testes"],
        [
            [r.id, r.titulo, r.rotulo_status, r.prioridade.capitalize(),
             r.versao or "-", str(len(r.testes)) if r.testes else "0"]
            for r in requisitos
        ],
    )


def _ficha(r: Requisito) -> str:
    linhas = [
        f"### {r.id} — {r.titulo}",
        "",
        f"**Situação:** {r.rotulo_status} · **Prioridade:** {r.prioridade.capitalize()}"
        + (f" · **Entregue em:** `{r.versao}`" if r.versao else ""),
        "",
        r.descricao,
        "",
        f"**Critério de aceite:** {r.criterio}",
        "",
    ]
    if r.implementacao:
        linhas += ["**Implementação:**", ""]
        linhas += [f"- `{caminho}`" for caminho in r.implementacao]
        linhas.append("")
    if r.testes:
        linhas += ["**Testes:**", ""]
        linhas += [f"- `{teste}`" for teste in r.testes]
        linhas.append("")
    else:
        linhas += ["**Testes:** nenhum declarado.", ""]
    if r.observacao:
        linhas += [f"> {r.observacao}", ""]
    return "\n".join(linhas)


def documento_inventario(inv: Inventario) -> str:
    return "\n".join([
        "# Inventário técnico",
        "",
        AVISO,
        "",
        f"Retrato do código na versão `{inv.versao}`, lido direto dos fontes.",
        "",
        "## Endpoints da API",
        "",
        f"{inv.endpoints_publicos} públicos e {inv.endpoints_administrativos} administrativos "
        "(estes exigem o cabeçalho `X-Admin-Token`).",
        "",
        tabela(
            ["Método", "Rota", "Controlador", "Acesso"],
            [[e.metodo, f"`{e.rota}`", e.controlador,
              "administrativo" if e.administrativo else "jogador"] for e in inv.endpoints],
        ),
        "",
        "## Entidades persistidas",
        "",
        tabela(["Classe", "Tabela"], [[e.classe, f"`{e.tabela}`"] for e in inv.entidades]),
        "",
        "## Serviços",
        "",
        tabela(["Serviço"], [[f"`{s}`"] for s in inv.servicos]),
        "",
        "## Páginas do frontend",
        "",
        tabela(["Arquivo"], [[f"`{p}`"] for p in inv.paginas]),
        "",
        "## Testes automatizados",
        "",
        tabela(
            ["Origem", "Quantidade"],
            [["Java (JUnit)", str(inv.testes_java)],
             ["Python (unittest)", str(inv.testes_python)],
             ["**Total**", f"**{inv.total_testes}**"]],
        ),
        "",
    ])


# ---------------------------------------------------------------------------
# Escrita
# ---------------------------------------------------------------------------

def substituir_bloco(texto: str, nome: str, conteudo: str, arquivo: str) -> str:
    """Troca o conteúdo entre os marcadores do bloco. Exige que existam."""
    inicio = f"<!-- auto:inicio:{nome} -->"
    fim = f"<!-- auto:fim:{nome} -->"

    posicao_inicio = texto.find(inicio)
    posicao_fim = texto.find(fim)
    if posicao_inicio == -1 or posicao_fim == -1:
        raise CatalogoInvalido(f"{arquivo}: marcadores do bloco '{nome}' nao encontrados")
    if posicao_fim < posicao_inicio:
        raise CatalogoInvalido(f"{arquivo}: bloco '{nome}' com marcadores fora de ordem")

    return (
        texto[: posicao_inicio + len(inicio)]
        + "\n"
        + conteudo.strip()
        + "\n"
        + texto[posicao_fim:]
    )


def montar_saidas(raiz: Path) -> dict[Path, str]:
    """Calcula o conteúdo final de cada arquivo gerado, sem escrever nada."""
    inv = coletar(raiz)
    cat = carregar(raiz)

    saidas: dict[Path, str] = {
        raiz / "docs" / "REQUISITOS.md": documento_requisitos(cat, inv),
        raiz / "docs" / "INVENTARIO.md": documento_inventario(inv),
    }

    blocos = {
        raiz / "README.md": [
            ("estado", bloco_estado(inv, cat)),
            ("documentacao", bloco_documentacao()),
        ],
        raiz / "docs" / "RELATORIO.md": [
            ("metricas", bloco_metricas(inv)),
            ("requisitos", bloco_requisitos(cat)),
        ],
        raiz / "docs" / "ROADMAP.md": [
            ("planejados", bloco_planejados(cat)),
        ],
    }

    for caminho, definicoes in blocos.items():
        if not caminho.exists():
            raise CatalogoInvalido(f"arquivo esperado nao existe: {caminho}")
        texto = caminho.read_text(encoding="utf-8")
        for nome, conteudo in definicoes:
            texto = substituir_bloco(texto, nome, conteudo, caminho.name)
        saidas[caminho] = texto

    return saidas


def executar(raiz: Path, verificar: bool) -> int:
    try:
        saidas = montar_saidas(raiz)
    except CatalogoInvalido as erro:
        print(f"ERRO: {erro}", file=sys.stderr)
        return 2

    desatualizados: list[str] = []
    for caminho, conteudo in saidas.items():
        atual = caminho.read_text(encoding="utf-8") if caminho.exists() else None
        if atual == conteudo:
            continue
        relativo = caminho.relative_to(raiz).as_posix()
        desatualizados.append(relativo)
        if not verificar:
            caminho.parent.mkdir(parents=True, exist_ok=True)
            caminho.write_text(conteudo, encoding="utf-8")

    if verificar:
        if desatualizados:
            print("Documentacao desatualizada:", file=sys.stderr)
            for relativo in desatualizados:
                print(f"  - {relativo}", file=sys.stderr)
            print("\nRode: python ferramentas/gerar_documentacao.py", file=sys.stderr)
            return 1
        print("Documentacao em dia.")
        return 0

    if desatualizados:
        print("Documentacao atualizada:")
        for relativo in desatualizados:
            print(f"  - {relativo}")
    else:
        print("Documentacao ja estava em dia.")
    return 0


def main() -> int:
    interpretador = argparse.ArgumentParser(description="Gera a documentacao viva do Complexus")
    interpretador.add_argument(
        "--verificar", action="store_true",
        help="nao escreve; falha se algum arquivo estiver desatualizado",
    )
    interpretador.add_argument(
        "--raiz", type=Path, default=Path(__file__).resolve().parent.parent,
        help="raiz do repositorio (padrao: pasta acima de ferramentas/)",
    )
    argumentos = interpretador.parse_args()
    return executar(argumentos.raiz, argumentos.verificar)


if __name__ == "__main__":
    raise SystemExit(main())
