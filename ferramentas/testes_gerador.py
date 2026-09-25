"""Testes da documentação viva.

Cobrem as três garantias que fazem a automação valer:

1. o catálogo de requisitos é íntegro (ids únicos, campos obrigatórios);
2. a rastreabilidade aponta para arquivos que existem de verdade;
3. a documentação no repositório está em dia com o código.

Execução: python -m unittest discover -s ferramentas -p "testes_*.py"
"""

import unittest
from pathlib import Path

from gerar_documentacao import montar_saidas, substituir_bloco
from inventario import coletar
from requisitos import CatalogoInvalido, carregar

RAIZ = Path(__file__).resolve().parent.parent


class TestesCatalogo(unittest.TestCase):

    @classmethod
    def setUpClass(cls):
        cls.catalogo = carregar(RAIZ)

    def test_catalogo_carrega(self):
        self.assertTrue(self.catalogo.requisitos, "o catalogo nao pode estar vazio")
        self.assertTrue(self.catalogo.funcionais)
        self.assertTrue(self.catalogo.nao_funcionais)

    def test_requisitos_tem_id_unico(self):
        ids = [r.id for r in self.catalogo.requisitos]
        self.assertEqual(len(ids), len(set(ids)), "ha id repetido no catalogo")

    def test_implementacao_aponta_para_arquivo_existente(self):
        for requisito in self.catalogo.requisitos:
            for caminho in requisito.implementacao:
                with self.subTest(requisito=requisito.id, caminho=caminho):
                    self.assertTrue((RAIZ / caminho).exists(),
                                    f"{requisito.id} aponta para caminho inexistente")

    def test_entregue_declara_versao_e_implementacao(self):
        for requisito in self.catalogo.requisitos:
            if requisito.status != "IMPLEMENTADO":
                continue
            with self.subTest(requisito=requisito.id):
                self.assertTrue(requisito.versao)
                self.assertTrue(requisito.implementacao)

    def test_resumo_fecha_a_conta(self):
        resumo = self.catalogo.resumo()
        self.assertEqual(
            resumo["total"],
            resumo["implementados"] + resumo["parciais"] + resumo["planejados"],
        )
        self.assertEqual(resumo["total"], resumo["funcionais"] + resumo["nao_funcionais"])


class TestesInventario(unittest.TestCase):

    @classmethod
    def setUpClass(cls):
        cls.inventario = coletar(RAIZ)

    def test_encontra_o_essencial(self):
        self.assertNotEqual(self.inventario.versao, "desconhecida")
        self.assertGreater(len(self.inventario.endpoints), 20)
        self.assertGreater(len(self.inventario.entidades), 10)
        self.assertGreater(self.inventario.testes_java, 0)
        self.assertGreater(self.inventario.testes_python, 0)

    def test_separa_rota_administrativa(self):
        administrativas = [e for e in self.inventario.endpoints if e.administrativo]
        self.assertTrue(administrativas, "as rotas /api/admin deveriam ser reconhecidas")
        for endpoint in administrativas:
            self.assertTrue(endpoint.rota.startswith("/api/admin"))

class TestesGeracao(unittest.TestCase):

    def test_documentacao_esta_atualizada(self):
        """Falha quando o repositorio tem documentacao atrasada em relacao ao codigo."""
        for caminho, esperado in montar_saidas(RAIZ).items():
            with self.subTest(arquivo=caminho.name):
                self.assertTrue(caminho.exists(), f"{caminho.name} nao existe")
                self.assertEqual(
                    caminho.read_text(encoding="utf-8"), esperado,
                    f"{caminho.name} esta desatualizado: "
                    "rode python ferramentas/gerar_documentacao.py",
                )

    def test_saida_e_deterministica(self):
        """Duas geracoes seguidas produzem exatamente o mesmo texto."""
        primeira = {k: v for k, v in montar_saidas(RAIZ).items()}
        segunda = {k: v for k, v in montar_saidas(RAIZ).items()}
        self.assertEqual(primeira, segunda)

    def test_bloco_preserva_texto_fora_dos_marcadores(self):
        texto = "antes\n<!-- auto:inicio:x -->\nvelho\n<!-- auto:fim:x -->\ndepois\n"
        resultado = substituir_bloco(texto, "x", "novo", "teste.md")
        self.assertIn("antes", resultado)
        self.assertIn("depois", resultado)
        self.assertIn("novo", resultado)
        self.assertNotIn("velho", resultado)

    def test_bloco_ausente_e_erro(self):
        with self.assertRaises(CatalogoInvalido):
            substituir_bloco("sem marcador", "x", "novo", "teste.md")


if __name__ == "__main__":
    unittest.main()
