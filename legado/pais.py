class Pais:
    def __init__(self, nome, pib, estabilidade, poder_militar, tesouro):
        self.nome = nome
        self.pib = pib  # Em bilhões
        self.estabilidade = estabilidade  # 0 a 100
        self.poder_militar = poder_militar # Escala de poder bruto
        self.em_guerra = False
        self.tesouro = tesouro      # Dinheiro em caixa para a guerra

    def status(self):
        print(f"País: {self.nome} | PIB: ${self.pib}B | Estabilidade: {self.estabilidade}% | Poder: {self.poder_militar}")

# Testando a criação dos primeiros países do seu simulador
brasil = Pais("Brasil", 1600, 85, 500)
argentina = Pais("Argentina", 480, 70, 300)

brasil.status()
argentina.status()