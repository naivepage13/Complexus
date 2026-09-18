"""Testes do modelo setorial. Execucao: python -m unittest discover analytics"""

import unittest

from modelo_setorial import SETORES, calcular_setor, calcular_todos, projetar_investimento


class TestesModeloSetorial(unittest.TestCase):

    def test_resultado_e_deterministico(self):
        primeiro = calcular_setor("ALIMENTICIO", 7)
        segundo = calcular_setor("ALIMENTICIO", 7)
        self.assertEqual(primeiro, segundo)

    def test_turnos_diferentes_geram_choques_diferentes(self):
        self.assertNotEqual(calcular_setor("CONSTRUCAO", 3), calcular_setor("CONSTRUCAO", 4))

    def test_todos_os_setores_sao_calculados(self):
        resultado = calcular_todos(12)
        self.assertEqual(set(resultado), set(SETORES))
        for choques in resultado.values():
            self.assertIn("choqueDemanda", choques)
            self.assertIn("choqueCusto", choques)
            self.assertIn("confianca", choques)

    def test_confianca_fica_no_intervalo(self):
        for turno in range(0, 60):
            for setor in SETORES:
                confianca = calcular_setor(setor, turno, 0.25, 0.35).confianca
                self.assertGreaterEqual(confianca, 0.5)
                self.assertLessEqual(confianca, 1.5)

    def test_juros_altos_derrubam_a_demanda_imobiliaria(self):
        neutro = calcular_setor("IMOBILIARIO", 5, 0.04, 0.1075)
        aperto = calcular_setor("IMOBILIARIO", 5, 0.04, 0.30)
        self.assertLess(aperto.choqueDemanda, neutro.choqueDemanda)

    def test_inflacao_alta_pressiona_custos(self):
        neutro = calcular_setor("CONSTRUCAO", 5, 0.04, 0.1075)
        pressao = calcular_setor("CONSTRUCAO", 5, 0.20, 0.1075)
        self.assertGreater(pressao.choqueCusto, neutro.choqueCusto)

    def test_setor_desconhecido_falha(self):
        with self.assertRaises(ValueError):
            calcular_setor("MINERACAO", 1)

    def test_projecao_acumula_dividendos(self):
        projecao = projetar_investimento(
            preco_acao=10.0,
            lucro_mensal=1_000_000.0,
            payout=0.3,
            acoes_totais=1_000_000,
            crescimento_mensal=0.02,
            turnos=6,
        )
        self.assertEqual(len(projecao["serie"]), 6)
        self.assertGreater(projecao["dividendosAcumulados"], 0)
        self.assertGreater(projecao["precoFinal"], projecao["precoInicial"])

    def test_projecao_rejeita_acoes_invalidas(self):
        with self.assertRaises(ValueError):
            projetar_investimento(10.0, 100.0, 0.3, 0, 0.01, 3)


if __name__ == "__main__":
    unittest.main()
