"""Servico analitico do Complexus.

Expoe o modelo setorial em HTTP para o backend Java. Usa apenas a biblioteca
padrao do Python, entao roda com `python analytics/servico_analitico.py` sem
qualquer instalacao previa.

Endpoints
---------
GET  /saude          -> verificacao de disponibilidade
POST /modificadores  -> choques setoriais de um turno
POST /projecao       -> projecao de retorno de um investimento

O backend funciona mesmo com este servico desligado: nesse caso ele usa a
formula equivalente implementada em Java e registra a origem do calculo no
relatorio do turno.
"""

from __future__ import annotations

import argparse
import json
import logging
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer

from modelo_setorial import calcular_todos, projetar_investimento

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s [%(levelname)s] %(message)s",
)
log = logging.getLogger("servico-analitico")

PORTA_PADRAO = 8100


class Manipulador(BaseHTTPRequestHandler):
    """Roteamento minimo sobre o servidor HTTP da biblioteca padrao."""

    server_version = "ComplexusAnalytics/0.2"

    def do_GET(self) -> None:  # noqa: N802 - assinatura exigida pela stdlib
        if self.path.startswith("/saude"):
            self._responder(200, {"status": "ok", "servico": "analitico", "versao": "0.2.0"})
        else:
            self._responder(404, {"erro": "rota nao encontrada", "caminho": self.path})

    def do_POST(self) -> None:  # noqa: N802 - assinatura exigida pela stdlib
        try:
            corpo = self._ler_corpo()
        except ValueError as erro:
            self._responder(400, {"erro": str(erro)})
            return

        try:
            if self.path.startswith("/modificadores"):
                self._responder(200, self._modificadores(corpo))
            elif self.path.startswith("/projecao"):
                self._responder(200, self._projecao(corpo))
            else:
                self._responder(404, {"erro": "rota nao encontrada", "caminho": self.path})
        except (ValueError, KeyError, TypeError) as erro:
            self._responder(422, {"erro": str(erro)})

    # ------------------------------------------------------------------
    # Regras
    # ------------------------------------------------------------------

    def _modificadores(self, corpo: dict) -> dict:
        turno = int(corpo.get("turno", 0))
        inflacao = float(corpo.get("inflacaoAnual", 0.04))
        juros = float(corpo.get("taxaJuros", 0.1075))
        setores = calcular_todos(turno, inflacao, juros)
        log.info("Turno %s: choques calculados para %s setores", turno, len(setores))
        return {"turno": turno, "setores": setores}

    def _projecao(self, corpo: dict) -> dict:
        return projetar_investimento(
            preco_acao=float(corpo["precoAcao"]),
            lucro_mensal=float(corpo["lucroMensal"]),
            payout=float(corpo.get("payout", 0.3)),
            acoes_totais=int(corpo.get("acoesTotais", 1_000_000)),
            crescimento_mensal=float(corpo.get("crescimentoMensal", 0.0)),
            turnos=int(corpo.get("turnos", 12)),
        )

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


def iniciar(porta: int = PORTA_PADRAO) -> None:
    servidor = ThreadingHTTPServer(("127.0.0.1", porta), Manipulador)
    log.info("Servico analitico ouvindo em http://127.0.0.1:%s", porta)
    try:
        servidor.serve_forever()
    except KeyboardInterrupt:
        log.info("Encerrando servico analitico.")
    finally:
        servidor.server_close()


if __name__ == "__main__":
    interpretador = argparse.ArgumentParser(description="Servico analitico do Complexus")
    interpretador.add_argument("--porta", type=int, default=PORTA_PADRAO, help="porta HTTP")
    argumentos = interpretador.parse_args()
    iniciar(argumentos.porta)
