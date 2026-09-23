"""Catalogo de tipos de unidade.

Cada chave deste catalogo corresponde a um campo do inventario militar em
``combate/paises.json``. Uma instancia de ``Unidade`` no campo de batalha aponta
para um ``TipoUnidade`` daqui, que define todos os seus atributos de combate.

Escala de referencia: 1 ponto de resistencia ~ 1% da capacidade de combate de
uma companhia de infantaria de 100 homens.
"""

from __future__ import annotations

from dataclasses import dataclass, field
from enum import Enum
from types import MappingProxyType
from typing import Mapping

# Uma companhia de infantaria e a entidade atomica terrestre. Individualizar
# 220 mil soldados nao muda o resultado e custa duas ordens de grandeza a mais.
SOLDADOS_POR_COMPANHIA = 100


class Dominio(str, Enum):
    TERRA = "terra"
    AR = "ar"
    MAR = "mar"
    ESTRATEGICO = "estrategico"


class Papel(str, Enum):
    COMBATE = "combate"
    LOGISTICA = "logistica"
    RECONHECIMENTO = "reconhecimento"
    DEFESA_AEREA = "defesa_aerea"


class Fase(str, Enum):
    INTELIGENCIA = "inteligencia"
    AR = "ar"
    PROFUNDO = "profundo"
    MAR = "mar"
    TERRA = "terra"
    CONSEQUENCIAS = "consequencias"


@dataclass(frozen=True)
class TipoUnidade:
    chave: str
    nome: str
    dominio: Dominio
    papel: Papel
    resistencia: float
    defesa: float
    ataque: Mapping[Dominio, float] = field(default_factory=dict)
    fases: tuple[Fase, ...] = ()
    efetivos: int = 0
    deteccao: float = 0.0
    assinatura: float = 5.0
    consumo: float = 0.0
    capacidade_logistica: float = 0.0
    custo_operacional: float = 0.0
    projecao_aerea: float = 0.0
    alvos_atingidos: int = 1  # area de efeito
    consumivel: bool = False
    gate_nuclear: bool = False
    mobilizavel: bool = False

    def poder_contra(self, dominio: Dominio) -> float:
        return self.ataque.get(dominio, 0.0)


def _t(**kwargs) -> TipoUnidade:
    if "ataque" in kwargs:
        kwargs["ataque"] = MappingProxyType(dict(kwargs["ataque"]))
    return TipoUnidade(**kwargs)


CATALOGO: Mapping[str, TipoUnidade] = MappingProxyType({
    # --- Terrestre ---------------------------------------------------------
    "infantaria_ativa": _t(
        chave="infantaria_ativa", nome="Companhia de Infantaria",
        dominio=Dominio.TERRA, papel=Papel.COMBATE,
        resistencia=100, defesa=1.0, efetivos=SOLDADOS_POR_COMPANHIA,
        ataque={Dominio.TERRA: 12.0, Dominio.AR: 1.0},
        fases=(Fase.TERRA,), deteccao=2.0, assinatura=8.0,
        consumo=10.0, custo_operacional=0.5,
    ),
    "infantaria_reserva": _t(
        chave="infantaria_reserva", nome="Companhia de Reserva",
        dominio=Dominio.TERRA, papel=Papel.COMBATE,
        resistencia=80, defesa=0.7, efetivos=SOLDADOS_POR_COMPANHIA,
        ataque={Dominio.TERRA: 7.0, Dominio.AR: 0.5},
        fases=(Fase.TERRA,), deteccao=1.0, assinatura=9.0,
        consumo=9.0, custo_operacional=0.35, mobilizavel=True,
    ),
    "tanque_principal": _t(
        chave="tanque_principal", nome="Tanque de Batalha Principal",
        dominio=Dominio.TERRA, papel=Papel.COMBATE,
        resistencia=60, defesa=3.0, efetivos=4,
        ataque={Dominio.TERRA: 30.0},
        fases=(Fase.TERRA,), deteccao=1.5, assinatura=6.0,
        consumo=6.0, custo_operacional=0.3,
    ),
    "tanque_reconhecimento": _t(
        chave="tanque_reconhecimento", nome="Blindado de Reconhecimento",
        dominio=Dominio.TERRA, papel=Papel.RECONHECIMENTO,
        resistencia=30, defesa=1.6, efetivos=3,
        ataque={Dominio.TERRA: 12.0},
        fases=(Fase.INTELIGENCIA, Fase.TERRA), deteccao=6.0, assinatura=3.0,
        consumo=3.0, custo_operacional=0.15,
    ),
    "caminhao_transporte": _t(
        chave="caminhao_transporte", nome="Caminhao de Transporte",
        dominio=Dominio.TERRA, papel=Papel.LOGISTICA,
        resistencia=15, defesa=0.5, efetivos=2,
        fases=(), assinatura=5.0,
        consumo=2.0, capacidade_logistica=8.0, custo_operacional=0.05,
    ),
    "bateria_antiaerea": _t(
        chave="bateria_antiaerea", nome="Bateria Antiaerea",
        dominio=Dominio.TERRA, papel=Papel.DEFESA_AEREA,
        resistencia=45, defesa=1.4, efetivos=30,
        ataque={Dominio.AR: 28.0},
        fases=(Fase.AR, Fase.PROFUNDO), deteccao=7.0, assinatura=4.0,
        consumo=5.0, custo_operacional=0.4,
    ),

    # --- Aerea -------------------------------------------------------------
    "caca_ataque": _t(
        chave="caca_ataque", nome="Caca de Ataque",
        dominio=Dominio.AR, papel=Papel.COMBATE,
        resistencia=40, defesa=1.2, efetivos=1,
        ataque={Dominio.AR: 35.0, Dominio.TERRA: 25.0, Dominio.MAR: 20.0},
        fases=(Fase.AR, Fase.PROFUNDO, Fase.MAR, Fase.TERRA),
        deteccao=8.0, assinatura=7.0, consumo=12.0, custo_operacional=1.2,
    ),
    "helicoptero": _t(
        chave="helicoptero", nome="Helicoptero de Ataque",
        dominio=Dominio.AR, papel=Papel.COMBATE,
        resistencia=25, defesa=0.9, efetivos=3,
        ataque={Dominio.TERRA: 22.0, Dominio.MAR: 8.0, Dominio.AR: 6.0},
        fases=(Fase.TERRA, Fase.MAR),
        deteccao=5.0, assinatura=6.0, consumo=7.0, custo_operacional=0.6,
    ),
    "aeronave_transporte": _t(
        chave="aeronave_transporte", nome="Aeronave de Transporte",
        dominio=Dominio.AR, papel=Papel.LOGISTICA,
        resistencia=30, defesa=0.6, efetivos=6,
        fases=(), deteccao=1.0, assinatura=9.0,
        consumo=10.0, capacidade_logistica=25.0, custo_operacional=0.8,
    ),
    "patrulha_maritima": _t(
        chave="patrulha_maritima", nome="Aeronave de Patrulha Maritima",
        dominio=Dominio.AR, papel=Papel.RECONHECIMENTO,
        resistencia=30, defesa=0.7, efetivos=8,
        ataque={Dominio.MAR: 14.0},
        fases=(Fase.INTELIGENCIA, Fase.MAR),
        deteccao=14.0, assinatura=8.0, consumo=8.0, custo_operacional=0.7,
    ),

    # --- Naval -------------------------------------------------------------
    "submarino": _t(
        chave="submarino", nome="Submarino",
        dominio=Dominio.MAR, papel=Papel.COMBATE,
        resistencia=55, defesa=2.0, efetivos=40,
        ataque={Dominio.MAR: 40.0, Dominio.TERRA: 10.0},
        fases=(Fase.MAR, Fase.PROFUNDO),
        deteccao=6.0, assinatura=1.5, consumo=9.0, custo_operacional=1.5,
    ),
    "navio_guerra": _t(
        chave="navio_guerra", nome="Navio de Guerra",
        dominio=Dominio.MAR, papel=Papel.COMBATE,
        resistencia=120, defesa=2.5, efetivos=200,
        ataque={Dominio.MAR: 35.0, Dominio.AR: 25.0, Dominio.TERRA: 15.0},
        fases=(Fase.MAR, Fase.AR, Fase.PROFUNDO),
        deteccao=10.0, assinatura=12.0, consumo=14.0, custo_operacional=2.0,
    ),
    "porta_avioes": _t(
        chave="porta_avioes", nome="Porta-Avioes",
        dominio=Dominio.MAR, papel=Papel.COMBATE,
        resistencia=300, defesa=3.0, efetivos=1500,
        ataque={Dominio.MAR: 20.0, Dominio.AR: 15.0},
        fases=(Fase.MAR, Fase.AR),
        deteccao=16.0, assinatura=20.0, consumo=40.0, custo_operacional=8.0,
        projecao_aerea=0.15,
    ),

    # --- Drones ------------------------------------------------------------
    "drone_kamikaze": _t(
        chave="drone_kamikaze", nome="Drone Kamikaze",
        dominio=Dominio.AR, papel=Papel.COMBATE,
        resistencia=5, defesa=0.3, efetivos=0,
        ataque={Dominio.TERRA: 18.0, Dominio.MAR: 12.0},
        fases=(Fase.PROFUNDO,),
        deteccao=1.0, assinatura=1.2, consumo=0.5, custo_operacional=0.05,
        consumivel=True,
    ),
    "drone_reconhecimento": _t(
        chave="drone_reconhecimento", nome="Drone de Reconhecimento",
        dominio=Dominio.AR, papel=Papel.RECONHECIMENTO,
        resistencia=8, defesa=0.4, efetivos=0,
        fases=(Fase.INTELIGENCIA,),
        deteccao=10.0, assinatura=1.5, consumo=0.6, custo_operacional=0.03,
    ),

    # --- Misseis e armas estrategicas --------------------------------------
    "missil_simples": _t(
        chave="missil_simples", nome="Missil Simples",
        dominio=Dominio.ESTRATEGICO, papel=Papel.COMBATE,
        resistencia=1, defesa=0.2, efetivos=0,
        ataque={Dominio.TERRA: 25.0, Dominio.MAR: 20.0},
        fases=(Fase.PROFUNDO,),
        assinatura=2.0, consumo=0.0, custo_operacional=0.1, consumivel=True,
    ),
    "missil_balistico": _t(
        chave="missil_balistico", nome="Missil Balistico",
        dominio=Dominio.ESTRATEGICO, papel=Papel.COMBATE,
        resistencia=1, defesa=0.2, efetivos=0,
        ataque={Dominio.TERRA: 70.0, Dominio.ESTRATEGICO: 60.0},
        fases=(Fase.PROFUNDO,),
        assinatura=4.0, consumo=0.0, custo_operacional=1.0, consumivel=True,
    ),
    "missil_hipersonico": _t(
        chave="missil_hipersonico", nome="Missil Hipersonico",
        dominio=Dominio.ESTRATEGICO, papel=Papel.COMBATE,
        resistencia=1, defesa=0.2, efetivos=0,
        ataque={Dominio.TERRA: 110.0, Dominio.MAR: 90.0},
        fases=(Fase.PROFUNDO,),
        assinatura=1.0, consumo=0.0, custo_operacional=3.0, consumivel=True,
    ),
    "ogiva_nuclear": _t(
        chave="ogiva_nuclear", nome="Ogiva Nuclear",
        dominio=Dominio.ESTRATEGICO, papel=Papel.COMBATE,
        resistencia=1, defesa=0.2, efetivos=0,
        ataque={Dominio.TERRA: 5000.0, Dominio.ESTRATEGICO: 5000.0},
        fases=(Fase.PROFUNDO,),
        assinatura=4.0, consumo=0.0, custo_operacional=10.0,
        alvos_atingidos=80, consumivel=True, gate_nuclear=True,
    ),
})

# Mapeia o inventario de combate/paises.json para as chaves do catalogo.
MAPA_INVENTARIO: Mapping[str, str] = MappingProxyType({
    "terrestre.soldados_ativos": "infantaria_ativa",
    "terrestre.soldados_reserva": "infantaria_reserva",
    "terrestre.tanques_principais": "tanque_principal",
    "terrestre.tanques_reconhecimento": "tanque_reconhecimento",
    "terrestre.caminhoes_transporte": "caminhao_transporte",
    "aerea.cacas_ataque": "caca_ataque",
    "aerea.helicopteros": "helicoptero",
    "aerea.aeronaves_transporte": "aeronave_transporte",
    "aerea.aeronaves_patrulha_maritima": "patrulha_maritima",
    "naval.submarinos": "submarino",
    "naval.navios_guerra": "navio_guerra",
    "naval.porta_avioes": "porta_avioes",
    "drones.kamikase": "drone_kamikaze",
    "drones.reconhecimento": "drone_reconhecimento",
    "misseis.simples": "missil_simples",
    "misseis.balisticos": "missil_balistico",
    "misseis.hipersonicos": "missil_hipersonico",
    "misseis.ogivas_nucleares": "ogiva_nuclear",
    "defesas.baterias_antiaereas": "bateria_antiaerea",
})

# Campos do inventario que nao viram entidades: modulam outras unidades.
CAMPOS_MODIFICADORES = ("terrestre.equipamento_infantaria",)
