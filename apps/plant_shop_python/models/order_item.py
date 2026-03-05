# models/order_item.py

from dataclasses import dataclass
from decimal import Decimal
from typing import Optional
from .plant import Plant

"""
	Représente un article dans une commande.

	@param id Identifiant unique de l article
	@param order_id Identifiant de la commande parente
	@param plant_id Identifiant de la plante commandée
	@param quantity Quantité commandée
	@param price Prix unitaire au moment de la commande
	@param plant Instance de la plante (chargée séparément)
"""
@dataclass
class OrderItem:
    id: int
    order_id: int
    plant_id: int
    quantity: int
    price: Decimal
    plant: Optional[Plant] = None # La plante est chargée séparément
