"""Servico de combate do GeoHistoricalSim.

Expoe o motor de combate em HTTP para o backend Java, no mesmo molde do
servico analitico: apenas biblioteca padrao, entao roda com
`python -m combate.servico` sem qualquer instalacao previa.

Endpoints
---------
GET  /saude             -> verificacao de disponibilidade
POST /batalha           -> resolve uma guerra completa
POST /ordem-de-batalha  -> composicao da forca de uma nacao, sem simular

O servico e **sem estado**: as nacoes chegam no corpo da requisicao. Passar o
nome em vez do objeto resolve pela fixture local (`combate/paises.json`), o que
serve para testar e para o frontend antes do Java assumir os dados.

Diferenca importante em relacao ao servico analitico: **nao existe fallback em
Java**. Reimplementar a simulacao de 26 mil entidades em outra linguagem criaria
duas regras divergentes, que e exatamente o que a decisao de ter o Python como
fonte unica quis evitar. Se este servico estiver fora do ar, a batalha fica
pendente e resolve no turno seguinte.
"""

from __future__ import annotations

import argparse
import json
import logging
import time
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from typing import Mapping

from .fases import densidade_antiaerea, poder_aereo
from .motor import DISTANCIA_ATACANTE, RODADAS_PADRAO, carregar_paises, resolver_guerra
from .unidades import criar_forca

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s [%(levelname)s] %(message)s",
)
log = logging.getLogger("servico-combate")

PORTA_PADRAO = 8200
VERSAO = "0.2.0"
# Teto para nao deixar uma requisicao prender o turno indefinidamente.
RODADAS_MAXIMAS = 30


class Manipulador(BaseHTTPRequestHandler):
    """Roteamento minimo sobre o servidor HTTP da biblioteca padrao."""

    server_version = f"GeoHistoricalSimCombate/{VERSAO}"

    def do_GET(self) -> None:  # noqa: N802 - assinatura exigida pela stdlib
        if self.path.startswith("/saude"):
            self._responder(200, {"status": "ok", "servico": "combate", "versao": VERSAO})
        else:
            self._responder(404, {"erro": "rota nao encontrada", "caminho": self.path})

    def do_POST(self) -> None:  # noqa: N802 - assinatura exigida pela stdlib
        try:
            corpo = self._ler_corpo()
        except ValueError as erro:
            self._responder(400, {"erro": str(erro)})
            return

        try:
            if self.path.startswith("/batalha"):
                self._responder(200, self._batalha(corpo))
            elif self.path.startswith("/ordem-de-batalha"):
                self._responder(200, self._ordem_de_batalha(corpo))
            else:
                self._responder(404, {"erro": "rota nao encontrada", "caminho": self.path})
        except (ValueError, KeyError, TypeError) as erro:
            self._responder(422, {"erro": str(erro)})

    # ------------------------------------------------------------------
    # Regras
    # ------------------------------------------------------------------

    def _batalha(self, corpo: dict) -> dict:
        atacante = _resolver_nacao(corpo, "atacante")
        defensor = _resolver_nacao(corpo, "defensor")
        rodadas = _inteiro(corpo.get("rodadas", RODADAS_PADRAO), "rodadas", 1, RODADAS_MAXIMAS)
        distancia = _decimal(corpo.get("distancia", DISTANCIA_ATACANTE), "distancia", 0.1, 10.0)
        semente = corpo.get("semente")
        if semente is not None:
            semente = _inteiro(semente, "semente", -(2**31), 2**31)

        inicio = time.perf_counter()
        relatorio = resolver_guerra(
            atacante, defensor,
            rodadas=rodadas, semente=semente, distancia=distancia,
        )
        duracao = (time.perf_counter() - inicio) * 1000

        log.info(
            "%s x %s: %s em %.0f ms (frente %.1f)",
            atacante["nome"], defensor["nome"], relatorio.veredito,
            duracao, relatorio.frente_final,
        )
        saida = relatorio.para_dicionario()
        saida["duracaoMs"] = round(duracao, 1)
        return saida

    def _ordem_de_batalha(self, corpo: dict) -> dict:
        nacao = _resolver_nacao(corpo, "nacao")
        forca = criar_forca(nacao)
        contagem = forca.contagem()
        return {
            "nome": forca.dono,
            "unidades": sum(contagem.values()),
            "efetivos": int(forca.efetivos_vivos()),
            "porTipo": contagem,
            "coberturaEquipamento": round(forca.cobertura_equipamento, 3),
            "capacidadeLogistica": round(forca.capacidade_logistica(), 1),
            "consumoPorRodada": round(forca.consumo_total(), 1),
            "custoPorRodada": round(forca.custo_operacional() / 1000.0, 3),
            "poderAereo": round(poder_aereo(forca), 1),
            "densidadeAntiaerea": round(densidade_antiaerea(forca), 3),
        }

    # ------------------------------------------------------------------
    # Infraestrutura
    # ------------------------------------------------------------------

    def _ler_corpo(self) -> dict:
        tamanho = int(self.headers.get("Content-Length") or 0)
        if tamanho == 0:
            return {}
        bruto = self.rfile.read(tamanho).decode("utf-8")
        try:
            return json.loads(bruto)
        except json.JSONDecodeError as erro:
            raise ValueError(f"JSON invalido: {erro}") from erro

    def _responder(self, status: int, corpo: dict) -> None:
        dados = json.dumps(corpo, ensure_ascii=False).encode("utf-8")
        self.send_response(status)
        self.send_header("Content-Type", "application/json; charset=utf-8")
        self.send_header("Content-Length", str(len(dados)))
        self.end_headers()
        self.wfile.write(dados)

    def log_message(self, formato: str, *args) -> None:
        log.debug("%s - %s", self.address_string(), formato % args)


# ----------------------------------------------------------------------
# Validacao de entrada
# ----------------------------------------------------------------------

def _resolver_nacao(corpo: Mapping, campo: str) -> dict:
    """Aceita o objeto completo da nacao ou o nome, buscando na fixture."""
    valor = corpo.get(campo)
    if valor is None:
        # ValueError e nao KeyError: str(KeyError("x")) sai com aspas extras.
        raise ValueError(f"campo obrigatorio ausente: {campo}")
    if isinstance(valor, str):
        paises = carregar_paises()
        if valor not in paises:
            raise ValueError(
                f"nacao desconhecida em {campo}: {valor}. "
                f"Disponiveis na fixture: {', '.join(sorted(paises))}"
            )
        return paises[valor]
    if isinstance(valor, dict):
        if "nome" not in valor:
            raise ValueError(f"{campo} precisa ter o campo 'nome'")
        return valor
    raise TypeError(f"{campo} deve ser um nome ou um objeto de nacao")


def _inteiro(valor, campo: str, minimo: int, maximo: int) -> int:
    numero = int(valor)
    if not minimo <= numero <= maximo:
        raise ValueError(f"{campo} deve estar entre {minimo} e {maximo}")
    return numero


def _decimal(valor, campo: str, minimo: float, maximo: float) -> float:
    numero = float(valor)
    if not minimo <= numero <= maximo:
        raise ValueError(f"{campo} deve estar entre {minimo} e {maximo}")
    return numero


def iniciar(porta: int = PORTA_PADRAO) -> None:
    servidor = ThreadingHTTPServer(("127.0.0.1", porta), Manipulador)
    log.info("Servico de combate ouvindo em http://127.0.0.1:%s", porta)
    try:
        servidor.serve_forever()
    except KeyboardInterrupt:
        log.info("Encerrando servico de combate.")
    finally:
        servidor.server_close()


if __name__ == "__main__":
    interpretador = argparse.ArgumentParser(
        description="Servico de combate do GeoHistoricalSim"
    )
    interpretador.add_argument("--porta", type=int, default=PORTA_PADRAO, help="porta HTTP")
    argumentos = interpretador.parse_args()
    iniciar(argumentos.porta)
