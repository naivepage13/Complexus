import random

def resolver_combate(atacante, defensor):
    # Regra de Logística: Manter o exército custa 1% do Poder Militar em bilhões
    custo_operacional = atacante.poder_militar * 0.01
    
    penalidade_logistica = 0
    if atacante.tesouro < custo_operacional:
        penalidade_logistica = 15 # Reduz 15 pontos de força por falta de verba
        print(f"⚠️ {atacante.nome} está com problemas de suprimento! (-{penalidade_logistica} forca)")
    
    # Consumo de recursos
    atacante.tesouro -= custo_operacional
    
    # Cálculo de força com os novos modificadores
    bonus_sorte = random.randint(1, 20)
    forca_final = (atacante.poder_militar - penalidade_logistica) + bonus_sorte
    
    print(f"Força Final de Ataque de {atacante.nome}: {forca_final}")
    
    # Comparação (Simplificada para o exemplo)
    if forca_final > defensor.poder_militar:
        print(f"🏆 {atacante.nome} venceu a batalha!")
        defensor.estabilidade -= 10
    else:
        print(f"❌ {defensor.nome} defendeu com sucesso!")
        atacante.estabilidade -= 5

        