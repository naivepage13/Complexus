import random

# 1. O MODELO MAIS SIMPLES POSSÍVEL
class Pais:
    def __init__(self, nome, poder):
        self.nome = nome
        self.poder = poder
        self.estabilidade = 100

# 2. A LISTA DE JOGADORES (BANCO DE DADOS INICIAL)
paises = [
    Pais("Brasil", 70),
    Pais("Argentina", 50),
    Pais("França", 80),
    Pais("Egito", 45)
]

# 3. O LOOP DO JOGO
def iniciar_jogo():
    print("=== GeoHistoricalSim v0.1 ===")
    
    while True:
        print("\nPaíses no mundo:")
        for i, p in enumerate(paises):
            print(f"{i} - {p.nome} (Poder: {p.poder} | Estabilidade: {p.estabilidade}%)")
        
        escolha = input("\nEscolha o número do seu país (ou 'sair'): ")
        if escolha.lower() == 'sair': break
        
        meu_pais = paises[int(escolha)]
        alvo = random.choice([p for p in paises if p != meu_pais])
        
        print(f"\nVOCÊ ({meu_pais.nome}) ATACOU {alvo.nome}!")
        
        # Lógica simples: Poder + Sorte
        if (meu_pais.poder + random.randint(1, 20)) > (alvo.poder + random.randint(1, 20)):
            print("Vitória! O inimigo perdeu estabilidade.")
            alvo.estabilidade -= 20
        else:
            print("Derrota! Sua estabilidade caiu.")
            meu_pais.estabilidade -= 15

iniciar_jogo()