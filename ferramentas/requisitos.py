"""Leitura e validação do catálogo de requisitos.

O catálogo (docs/requisitos.toml) é a fonte única: tudo o que os relatórios
dizem sobre requisitos sai daqui. A validação é o que impede a documentação de
mentir — um requisito que aponta para um arquivo inexistente quebra a geração.

Somente biblioteca padrão (tomllib, disponível desde o Python 3.11).
"""

from __future__ import annotations

import tomllib
from dataclasses import dataclass, field
from pathlib import Path

STATUS_VALIDOS = ("IMPLEMENTADO", "PARCIAL", "PLANEJADO")
PRIORIDADES_VALIDAS = ("ALTA", "MEDIA", "BAIXA")

#: Ordem de exibição nos relatórios.
ORDEM_STATUS = {"IMPLEMENTADO": 0, "PARCIAL": 1, "PLANEJADO": 2}
ORDEM_PRIORIDADE = {"ALTA": 0, "MEDIA": 1, "BAIXA": 2}

ROTULO_STATUS = {
    "IMPLEMENTADO": "entregue",
    "PARCIAL": "parcial",
    "PLANEJADO": "planejado",
}


class CatalogoInvalido(Exception):
    """O catálogo tem erro estrutural: a geração não pode continuar."""


@dataclass(frozen=True)
class Requisito:
    id: str
    titulo: str
    status: str
    prioridade: str
    versao: str
    descricao: str
    criterio: str
    implementacao: tuple[str, ...] = ()
    testes: tuple[str, ...] = ()
    observacao: str = ""

    @property
    def funcional(self) -> bool:
        return self.id.startswith("RF-")

    @property
    def tipo(self) -> str:
        return "RF" if self.funcional else "RNF"

    @property
    def numero(self) -> int:
        return int(self.id.split("-")[1])

    @property
    def coberto_por_teste(self) -> bool:
        return bool(self.testes)

    @property
    def rotulo_status(self) -> str:
        return ROTULO_STATUS.get(self.status, self.status.lower())


@dataclass
class Catalogo:
    projeto: str
    responsavel: str
    requisitos: list[Requisito] = field(default_factory=list)

    @property
    def funcionais(self) -> list[Requisito]:
        return [r for r in self.requisitos if r.funcional]

    @property
    def nao_funcionais(self) -> list[Requisito]:
        return [r for r in self.requisitos if not r.funcional]

    def por_status(self, status: str) -> list[Requisito]:
        return [r for r in self.requisitos if r.status == status]

    def resumo(self) -> dict[str, int]:
        """Contagem por status, mais totais e cobertura."""
        entregues = len(self.por_status("IMPLEMENTADO"))
        contagem = {
            "total": len(self.requisitos),
            "funcionais": len(self.funcionais),
            "nao_funcionais": len(self.nao_funcionais),
            "implementados": entregues,
            "parciais": len(self.por_status("PARCIAL")),
            "planejados": len(self.por_status("PLANEJADO")),
            "entregues_com_teste": sum(
                1 for r in self.requisitos if r.status == "IMPLEMENTADO" and r.coberto_por_teste
            ),
        }
        contagem["percentual_entregue"] = (
            round(entregues * 100 / contagem["total"]) if contagem["total"] else 0
        )
        return contagem

    def lacunas_de_teste(self) -> list[Requisito]:
        """Requisitos entregues sem nenhum teste declarado."""
        return [
            r for r in self.requisitos
            if r.status in ("IMPLEMENTADO", "PARCIAL") and not r.coberto_por_teste
        ]


def carregar(raiz: Path) -> Catalogo:
    """Lê e valida o catálogo. Levanta CatalogoInvalido em qualquer erro."""
    caminho = raiz / "docs" / "requisitos.toml"
    if not caminho.exists():
        raise CatalogoInvalido(f"catalogo nao encontrado: {caminho}")

    with caminho.open("rb") as arquivo:
        bruto = tomllib.load(arquivo)

    meta = bruto.get("meta", {})
    catalogo = Catalogo(
        projeto=meta.get("projeto", "Projeto"),
        responsavel=meta.get("responsavel", ""),
    )

    erros: list[str] = []
    vistos: set[str] = set()

    for indice, item in enumerate(bruto.get("requisito", []), start=1):
        identificador = item.get("id", f"<sem id na posicao {indice}>")

        for campo in ("id", "titulo", "status", "prioridade", "descricao", "criterio"):
            if not item.get(campo):
                erros.append(f"{identificador}: campo obrigatorio ausente ou vazio: {campo}")

        if identificador in vistos:
            erros.append(f"{identificador}: id repetido")
        vistos.add(identificador)

        if not (identificador.startswith("RF-") or identificador.startswith("RNF-")):
            erros.append(f"{identificador}: id deve comecar com RF- ou RNF-")

        status = item.get("status", "")
        if status and status not in STATUS_VALIDOS:
            erros.append(f"{identificador}: status invalido '{status}' (use {STATUS_VALIDOS})")

        prioridade = item.get("prioridade", "")
        if prioridade and prioridade not in PRIORIDADES_VALIDAS:
            erros.append(f"{identificador}: prioridade invalida '{prioridade}'")

        if status == "IMPLEMENTADO" and not item.get("versao"):
            erros.append(f"{identificador}: requisito entregue precisa declarar a versao")

        if status == "IMPLEMENTADO" and not item.get("implementacao"):
            erros.append(f"{identificador}: requisito entregue precisa apontar a implementacao")

        # Rastreabilidade honesta: o caminho declarado tem de existir de fato.
        for caminho_declarado in item.get("implementacao", []):
            if not (raiz / caminho_declarado).exists():
                erros.append(
                    f"{identificador}: implementacao aponta para arquivo inexistente: {caminho_declarado}"
                )

        catalogo.requisitos.append(
            Requisito(
                id=identificador,
                titulo=item.get("titulo", ""),
                status=status,
                prioridade=prioridade,
                versao=item.get("versao", ""),
                descricao=item.get("descricao", ""),
                criterio=item.get("criterio", ""),
                implementacao=tuple(item.get("implementacao", [])),
                testes=tuple(item.get("testes", [])),
                observacao=item.get("observacao", ""),
            )
        )

    if erros:
        raise CatalogoInvalido("catalogo de requisitos invalido:\n  - " + "\n  - ".join(erros))

    catalogo.requisitos.sort(key=lambda r: (r.tipo, r.numero))
    return catalogo
