"""Inventário do código do Complexus.

Percorre o repositório e devolve o que existe de fato: versão, endpoints,
entidades, serviços, páginas, testes e tamanho por linguagem. É a metade
"realidade" da documentação automática — a outra metade é o catálogo de
requisitos, escrito à mão em docs/requisitos.toml.

Funções puras: recebem a raiz do projeto e devolvem dados. Nada é escrito aqui.
Somente biblioteca padrão.
"""

from __future__ import annotations

import re
import subprocess
from dataclasses import dataclass, field
from pathlib import Path

#: Pastas que não contam como código do projeto.
IGNORADAS = {".git", "target", "data", "__pycache__", ".venv", "venv", ".claude"}

#: Extensão -> rótulo de linguagem nos relatórios.
LINGUAGENS = {
    ".java": "Java",
    ".py": "Python",
    ".html": "HTML",
    ".css": "CSS",
    ".js": "JavaScript",
    ".toml": "Configuração",
    ".yml": "Configuração",
    ".xml": "Configuração",
}

_MAPEAMENTO_RAIZ = re.compile(r'@RequestMapping\("([^"]+)"\)')
_MAPEAMENTO_ROTA = re.compile(r"@(Get|Post|Put|Delete|Patch)Mapping(?:\(\"([^\"]*)\"\))?")
_TABELA = re.compile(r'@Table\(name = "([^"]+)"')
_CLASSE = re.compile(r"(?:class|interface|enum|record)\s+(\w+)")
_TESTE_JAVA = re.compile(r"@Test\b")
_TESTE_PYTHON = re.compile(r"^\s*def (test_\w+)", re.MULTILINE)
_VERSAO_POM = re.compile(r"<artifactId>complexus-backend</artifactId>\s*<version>([^<]+)</version>")


@dataclass(frozen=True)
class Endpoint:
    metodo: str
    rota: str
    controlador: str

    @property
    def administrativo(self) -> bool:
        return self.rota.startswith("/api/admin")


@dataclass(frozen=True)
class Entidade:
    classe: str
    tabela: str


@dataclass
class Inventario:
    versao: str = "desconhecida"
    commit: str = ""
    endpoints: list[Endpoint] = field(default_factory=list)
    entidades: list[Entidade] = field(default_factory=list)
    servicos: list[str] = field(default_factory=list)
    paginas: list[str] = field(default_factory=list)
    testes_java: int = 0
    testes_python: int = 0
    linhas: dict[str, int] = field(default_factory=dict)

    @property
    def total_testes(self) -> int:
        return self.testes_java + self.testes_python

    @property
    def endpoints_publicos(self) -> int:
        return sum(1 for e in self.endpoints if not e.administrativo)

    @property
    def endpoints_administrativos(self) -> int:
        return sum(1 for e in self.endpoints if e.administrativo)


def coletar(raiz: Path) -> Inventario:
    """Monta o inventário completo do projeto."""
    inventario = Inventario(versao=_versao(raiz), commit=_commit(raiz))

    for arquivo in _arquivos(raiz):
        texto = _ler(arquivo)
        if texto is None:
            continue

        _contar_linhas(inventario, arquivo, texto)

        if arquivo.suffix == ".java":
            if "src/test/" in arquivo.as_posix():
                inventario.testes_java += len(_TESTE_JAVA.findall(texto))
            else:
                _coletar_java(inventario, arquivo, texto)
        elif arquivo.suffix == ".py" and arquivo.name.startswith("testes_"):
            inventario.testes_python += len(_TESTE_PYTHON.findall(texto))
        elif arquivo.suffix == ".html":
            inventario.paginas.append(arquivo.relative_to(raiz).as_posix())

    inventario.endpoints.sort(key=lambda e: (e.rota, e.metodo))
    inventario.entidades.sort(key=lambda e: e.classe)
    inventario.servicos.sort()
    inventario.paginas.sort()
    return inventario


def _coletar_java(inventario: Inventario, arquivo: Path, texto: str) -> None:
    nome = arquivo.stem

    if "@RestController" in texto:
        base = _MAPEAMENTO_RAIZ.search(texto)
        prefixo = base.group(1) if base else ""
        for verbo, sufixo in _MAPEAMENTO_ROTA.findall(texto):
            rota = prefixo + (sufixo or "")
            inventario.endpoints.append(Endpoint(verbo.upper(), rota or "/", nome))

    if "@Entity" in texto:
        tabela = _TABELA.search(texto)
        classe = _CLASSE.search(texto)
        inventario.entidades.append(
            Entidade(classe.group(1) if classe else nome, tabela.group(1) if tabela else "?")
        )

    if "@Service" in texto:
        inventario.servicos.append(nome)


def _contar_linhas(inventario: Inventario, arquivo: Path, texto: str) -> None:
    rotulo = LINGUAGENS.get(arquivo.suffix)
    if not rotulo:
        return
    linhas = sum(1 for linha in texto.splitlines() if linha.strip())
    inventario.linhas[rotulo] = inventario.linhas.get(rotulo, 0) + linhas


def _arquivos(raiz: Path):
    for caminho in sorted(raiz.rglob("*")):
        if not caminho.is_file():
            continue
        if any(parte in IGNORADAS for parte in caminho.relative_to(raiz).parts):
            continue
        yield caminho


def _ler(arquivo: Path) -> str | None:
    try:
        return arquivo.read_text(encoding="utf-8")
    except (UnicodeDecodeError, OSError):
        return None


def _versao(raiz: Path) -> str:
    pom = raiz / "backend" / "pom.xml"
    if not pom.exists():
        return "desconhecida"
    encontrado = _VERSAO_POM.search(re.sub(r"\s+", " ", pom.read_text(encoding="utf-8")))
    return encontrado.group(1) if encontrado else "desconhecida"


def _commit(raiz: Path) -> str:
    """Hash curto do commit atual. Vazio fora de um repositório Git."""
    try:
        resultado = subprocess.run(
            ["git", "rev-parse", "--short", "HEAD"],
            cwd=raiz, capture_output=True, text=True, timeout=10, check=False,
        )
        return resultado.stdout.strip() if resultado.returncode == 0 else ""
    except (OSError, subprocess.SubprocessError):
        return ""
