"""Testes do sistema de combate.

    python -m unittest discover -s tests

Cobrem as regras que nao podem quebrar sem ninguem perceber: montagem da
ordem de batalha, gate nuclear, reprodutibilidade, logistica e selecao de alvo.
"""

from __future__ import annotations

import copy
import sys
import unittest
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[1] / "src"))

from combate import CATALOGO, carregar_paises, criar_forca, resolver_guerra  # noqa: E402
from combate.catalogo import Fase, Papel  # noqa: E402
from combate.fases import alvos_da_fase, engajar  # noqa: E402
from combate.dados import Dados  # noqa: E402
from combate.logistica import avaliar  # noqa: E402
from combate.motor import _movimento_de_frente  # noqa: E402
from combate.unidades import LIMIAR_INEFICACIA, Unidade  # noqa: E402


class TestOrdemDeBatalha(unittest.TestCase):
    def setUp(self) -> None:
        self.paises = carregar_paises()
        self.brasil = self.paises["Brasil"]

    def test_infantaria_vira_companhias(self) -> None:
        forca = criar_forca(self.brasil)
        companhias = forca.contagem()["infantaria_ativa"]
        self.assertEqual(companhias, 220000 // 100)

    def test_plataformas_sao_individuais(self) -> None:
        forca = criar_forca(self.brasil)
        self.assertEqual(forca.contagem()["tanque_principal"], 350)
        self.assertEqual(forca.contagem()["porta_avioes"], 1)

    def test_reserva_respeita_mobilizacao(self) -> None:
        pais = copy.deepcopy(self.brasil)
        pais["doutrina"]["mobilizacao_reserva"] = 0.5
        forca = criar_forca(pais)
        self.assertEqual(forca.contagem()["infantaria_reserva"], (450000 // 100) // 2)

    def test_cobertura_de_equipamento_penaliza_infantaria(self) -> None:
        forca = criar_forca(self.brasil)
        # 85.000 equipamentos para 220.000 soldados: cobertura parcial.
        self.assertLess(forca.cobertura_equipamento, 1.0)
        infantaria = next(
            u for u in forca.unidades if u.tipo.chave == "infantaria_ativa"
        )
        self.assertLess(forca.bonus_unidade(infantaria), 1.0)


class TestGateNuclear(unittest.TestCase):
    def setUp(self) -> None:
        self.brasil = carregar_paises()["Brasil"]

    def test_sem_permissao_nao_ha_ogivas(self) -> None:
        forca = criar_forca(self.brasil)
        self.assertNotIn("ogiva_nuclear", forca.contagem())

    def test_com_permissao_as_ogivas_existem(self) -> None:
        pais = copy.deepcopy(self.brasil)
        pais["doutrina"]["permitir_nuclear"] = True
        forca = criar_forca(pais)
        self.assertEqual(forca.contagem()["ogiva_nuclear"], 8)

    def test_uso_nuclear_e_registrado_e_punido(self) -> None:
        paises = carregar_paises()
        atacante = copy.deepcopy(paises["Brasil"])
        atacante["doutrina"]["permitir_nuclear"] = True
        com = resolver_guerra(atacante, paises["Argentina"], semente=7)
        sem = resolver_guerra(paises["Brasil"], paises["Argentina"], semente=7)
        self.assertEqual(com.nuclear_empregado, ["Brasil"])
        self.assertEqual(sem.nuclear_empregado, [])
        self.assertLess(
            com.atacante.estabilidade_final, sem.atacante.estabilidade_final
        )


class TestReprodutibilidade(unittest.TestCase):
    def test_mesma_semente_mesmo_resultado(self) -> None:
        paises = carregar_paises()
        a = resolver_guerra(paises["Brasil"], paises["Argentina"], semente=42)
        b = resolver_guerra(paises["Brasil"], paises["Argentina"], semente=42)
        self.assertEqual(a.para_dicionario(), b.para_dicionario())

    def test_sementes_diferentes_divergem(self) -> None:
        paises = carregar_paises()
        a = resolver_guerra(paises["Brasil"], paises["Argentina"], semente=1)
        b = resolver_guerra(paises["Brasil"], paises["Argentina"], semente=2)
        self.assertNotEqual(a.frente_final, b.frente_final)


class TestLogistica(unittest.TestCase):
    def test_sem_tesouro_o_suprimento_cai(self) -> None:
        forca = criar_forca(carregar_paises()["Brasil"])
        rico = avaliar(forca, tesouro=500.0, distancia=1.0)
        quebrado = avaliar(forca, tesouro=0.0, distancia=1.0)
        self.assertGreater(rico.suprimento_efetivo, quebrado.suprimento_efetivo)
        self.assertTrue(quebrado.falta_verba)

    def test_distancia_aperta_o_transporte(self) -> None:
        forca = criar_forca(carregar_paises()["Brasil"])
        perto = avaliar(forca, tesouro=500.0, distancia=1.0)
        longe = avaliar(forca, tesouro=500.0, distancia=4.0)
        self.assertGreater(perto.razao_transporte, longe.razao_transporte)


class TestSelecaoDeAlvo(unittest.TestCase):
    def test_logistica_nao_e_alvo_na_fase_terrestre(self) -> None:
        forca = criar_forca(carregar_paises()["Brasil"])
        alvos = alvos_da_fase(forca, Fase.TERRA)
        self.assertTrue(alvos)
        self.assertFalse(any(u.tipo.papel is Papel.LOGISTICA for u in alvos))

    def test_logistica_e_alvo_na_fase_profunda(self) -> None:
        forca = criar_forca(carregar_paises()["Brasil"])
        alvos = alvos_da_fase(forca, Fase.PROFUNDO)
        self.assertTrue(any(u.tipo.papel is Papel.LOGISTICA for u in alvos))

    def test_destrocos_saem_da_lista_de_alvos(self) -> None:
        tipo = CATALOGO["infantaria_ativa"]
        mortos = [
            Unidade(id=i, tipo=tipo, dono="X", resistencia_atual=0.0, ativa=False)
            for i in range(50)
        ]
        self.assertIsNone(Dados(1).escolher_alvo(mortos, 0.5))
        self.assertEqual(mortos, [])


class TestUnidade(unittest.TestCase):
    def test_sai_de_combate_antes_da_aniquilacao(self) -> None:
        tipo = CATALOGO["infantaria_ativa"]
        u = Unidade(id=1, tipo=tipo, dono="X", resistencia_atual=tipo.resistencia)
        u.receber_dano(tipo.resistencia * (1 - LIMIAR_INEFICACIA) + 1)
        self.assertFalse(u.ativa)
        self.assertGreater(u.resistencia_atual, 0.0)


class TestRacionamento(unittest.TestCase):
    def test_estoque_de_consumiveis_nao_e_gasto_de_uma_vez(self) -> None:
        paises = carregar_paises()
        atacante = criar_forca(paises["Brasil"])
        defensor = criar_forca(paises["Argentina"])
        engajar(atacante, defensor, Fase.PROFUNDO, Dados(3), intel=0.5)
        restantes = sum(
            1 for u in atacante.unidades
            if u.tipo.chave == "missil_simples" and not u.gasta
        )
        self.assertGreater(restantes, 0)


class TestVeredito(unittest.TestCase):
    def test_frente_parada_nao_move(self) -> None:
        self.assertEqual(_movimento_de_frente(1000.0, 1000.0), 0.0)
        self.assertEqual(_movimento_de_frente(0.0, 0.0), 0.0)

    def test_forca_muito_superior_vence(self) -> None:
        paises = carregar_paises()
        relatorio = resolver_guerra(paises["Brasil"], paises["Argentina"], semente=5)
        self.assertIn("vitoria", relatorio.veredito)
        self.assertGreater(
            relatorio.defensor.baixas_humanas, relatorio.atacante.baixas_humanas
        )

    def test_relatorio_exporta_json_serializavel(self) -> None:
        import json

        paises = carregar_paises()
        relatorio = resolver_guerra(paises["Brasil"], paises["Argentina"], semente=8)
        texto = json.dumps(relatorio.para_dicionario(), ensure_ascii=False)
        self.assertIn("veredito", texto)


if __name__ == "__main__":
    unittest.main()
