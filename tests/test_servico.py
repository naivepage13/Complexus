"""Testes do servico HTTP de combate.

Sobem um servidor real em porta efemera e batem nele, para o contrato de fio
ficar coberto e nao so as funcoes internas.
"""

from __future__ import annotations

import json
import logging
import sys
import threading
import unittest
import urllib.error
import urllib.request
from http.server import ThreadingHTTPServer
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))

from combate.servico import Manipulador, _decimal, _inteiro, _resolver_nacao  # noqa: E402


class ServidorDeTeste:
    """Sobe o servico em porta efemera e devolve a base da URL."""

    def __enter__(self) -> "ServidorDeTeste":
        self.servidor = ThreadingHTTPServer(("127.0.0.1", 0), Manipulador)
        self.base = f"http://127.0.0.1:{self.servidor.server_address[1]}"
        self.thread = threading.Thread(target=self.servidor.serve_forever, daemon=True)
        self.thread.start()
        return self

    def __exit__(self, *_) -> None:
        self.servidor.shutdown()
        self.servidor.server_close()
        self.thread.join(timeout=5)

    def get(self, rota: str) -> tuple[int, dict]:
        try:
            with urllib.request.urlopen(self.base + rota, timeout=30) as resposta:
                return resposta.status, json.loads(resposta.read())
        except urllib.error.HTTPError as erro:
            return erro.code, json.loads(erro.read())

    def post(self, rota: str, corpo: dict) -> tuple[int, dict]:
        requisicao = urllib.request.Request(
            self.base + rota,
            data=json.dumps(corpo).encode("utf-8"),
            headers={"Content-Type": "application/json"},
            method="POST",
        )
        try:
            with urllib.request.urlopen(requisicao, timeout=120) as resposta:
                return resposta.status, json.loads(resposta.read())
        except urllib.error.HTTPError as erro:
            return erro.code, json.loads(erro.read())


class TestContratoHttp(unittest.TestCase):
    @classmethod
    def setUpClass(cls) -> None:
        # O servico loga cada batalha; na suite isso so polui a saida.
        logging.getLogger("servico-combate").setLevel(logging.WARNING)
        cls.ctx = ServidorDeTeste()
        cls.servidor = cls.ctx.__enter__()

    @classmethod
    def tearDownClass(cls) -> None:
        cls.ctx.__exit__()

    def test_saude(self) -> None:
        status, corpo = self.servidor.get("/saude")
        self.assertEqual(status, 200)
        self.assertEqual(corpo["status"], "ok")
        self.assertEqual(corpo["servico"], "combate")

    def test_batalha_por_nome(self) -> None:
        status, corpo = self.servidor.post(
            "/batalha",
            {"atacante": "Brasil", "defensor": "Argentina", "semente": 7, "rodadas": 2},
        )
        self.assertEqual(status, 200)
        self.assertIn("veredito", corpo)
        self.assertIn("duracaoMs", corpo)
        self.assertEqual(corpo["atacante"]["nome"], "Brasil")

    def test_batalha_aceita_nacao_no_corpo(self) -> None:
        """O servico e sem estado: o Java vai mandar a nacao inteira."""
        fixture = json.loads(
            (Path(__file__).resolve().parents[1] / "combate" / "paises.json")
            .read_text(encoding="utf-8")
        )
        nacao = dict(fixture["paises"][1])
        nacao["nome"] = "Nacao-Do-Java"
        status, corpo = self.servidor.post(
            "/batalha",
            {"atacante": "Brasil", "defensor": nacao, "semente": 7, "rodadas": 2},
        )
        self.assertEqual(status, 200)
        self.assertEqual(corpo["defensor"]["nome"], "Nacao-Do-Java")

    def test_contrato_sai_em_camelcase(self) -> None:
        status, corpo = self.servidor.post(
            "/batalha",
            {"atacante": "Brasil", "defensor": "Argentina", "semente": 7, "rodadas": 1},
        )
        self.assertEqual(status, 200)
        for chave in ("frenteFinal", "nuclearEmpregado", "duracaoMs"):
            self.assertIn(chave, corpo)
        for chave in ("baixasHumanas", "unidadesPerdidas", "custoTotal"):
            self.assertIn(chave, corpo["atacante"])
        self.assertNotIn("frente_final", corpo)

    def test_ordem_de_batalha(self) -> None:
        status, corpo = self.servidor.post("/ordem-de-batalha", {"nacao": "Brasil"})
        self.assertEqual(status, 200)
        self.assertGreater(corpo["unidades"], 0)
        self.assertGreater(corpo["efetivos"], 0)
        self.assertIn("infantaria_ativa", corpo["porTipo"])

    def test_rota_desconhecida(self) -> None:
        self.assertEqual(self.servidor.get("/nada")[0], 404)
        self.assertEqual(self.servidor.post("/nada", {})[0], 404)

    def test_nacao_desconhecida(self) -> None:
        status, corpo = self.servidor.post(
            "/batalha", {"atacante": "Atlantida", "defensor": "Brasil"}
        )
        self.assertEqual(status, 422)
        self.assertIn("Atlantida", corpo["erro"])

    def test_campo_ausente_sem_aspas_sobrando(self) -> None:
        status, corpo = self.servidor.post("/batalha", {"atacante": "Brasil"})
        self.assertEqual(status, 422)
        self.assertEqual(corpo["erro"], "campo obrigatorio ausente: defensor")

    def test_rodadas_fora_do_limite(self) -> None:
        status, corpo = self.servidor.post(
            "/batalha",
            {"atacante": "Brasil", "defensor": "Argentina", "rodadas": 9999},
        )
        self.assertEqual(status, 422)
        self.assertIn("rodadas", corpo["erro"])


class TestValidacao(unittest.TestCase):
    def test_resolver_nacao_por_nome_e_por_objeto(self) -> None:
        self.assertEqual(_resolver_nacao({"a": "Brasil"}, "a")["nome"], "Brasil")
        objeto = {"nome": "X", "militar": {}}
        self.assertIs(_resolver_nacao({"a": objeto}, "a"), objeto)

    def test_resolver_nacao_recusa_tipo_invalido(self) -> None:
        with self.assertRaises(TypeError):
            _resolver_nacao({"a": 42}, "a")
        with self.assertRaises(ValueError):
            _resolver_nacao({"a": {"sem": "nome"}}, "a")

    def test_limites(self) -> None:
        self.assertEqual(_inteiro(5, "x", 1, 10), 5)
        with self.assertRaises(ValueError):
            _inteiro(11, "x", 1, 10)
        self.assertEqual(_decimal("1.5", "y", 0.1, 10.0), 1.5)
        with self.assertRaises(ValueError):
            _decimal(0.01, "y", 0.1, 10.0)


if __name__ == "__main__":
    unittest.main()
