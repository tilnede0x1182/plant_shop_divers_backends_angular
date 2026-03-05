# models/plant.py

from dataclasses import dataclass
from datetime import datetime
from decimal import Decimal

"""
	Représente une plante dans la base de données.

	@param id Identifiant unique de la plante
	@param name Nom de la plante
	@param description Description de la plante
	@param price Prix de la plante
	@param stock Quantité en stock
	@param created_at Date de création
"""
@dataclass
class Plant:
    id: int
    name: str
    description: str
    price: Decimal
    stock: int
    created_at: datetime
