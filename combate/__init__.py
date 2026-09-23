"""Sistema de combate do GeoHistoricalSim.

O motor e a fonte da verdade das regras de guerra. O frontend consome o
resultado (``RelatorioBatalha.para_dicionario()``), nunca reimplementa a regra.
"""

from .catalogo import CATALOGO, Dominio, Fase, Papel, TipoUnidade
from .motor import Beligerante, carregar_paises, resolver_guerra
from .relatorio import BalancoNacional, RelatorioBatalha
from .unidades import ForcaArmada, Unidade, criar_forca

__all__ = [
    "CATALOGO",
    "BalancoNacional",
    "Beligerante",
    "Dominio",
    "Fase",
    "ForcaArmada",
    "Papel",
    "RelatorioBatalha",
    "TipoUnidade",
    "Unidade",
    "carregar_paises",
    "criar_forca",
    "resolver_guerra",
]
