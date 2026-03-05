# models/order.py

from dataclasses import dataclass
from datetime import datetime
from decimal import Decimal
from typing import List, Optional
from .order_item import OrderItem

"""
	Représente une commande dans la base de données.

	@param id Identifiant unique de la commande
	@param user_id Identifiant de l utilisateur ayant passé la commande
	@param total Montant total de la commande
	@param status Statut de la commande (pending, confirmed, shipped, delivered)
	@param created_at Date de création de la commande
	@param items Liste des articles de la commande (chargés séparément)
"""
@dataclass
class Order:
    id: int
    user_id: int
    total: Decimal
    status: str
    created_at: datetime
    items: Optional[List[OrderItem]] = None # Les items sont chargés séparément
