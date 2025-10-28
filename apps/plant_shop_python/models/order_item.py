# models/order_item.py

from dataclasses import dataclass
from decimal import Decimal
from typing import Optional
from .plant import Plant

@dataclass
class OrderItem:
    """Représente un article dans une commande."""
    id: int
    order_id: int
    plant_id: int
    quantity: int
    price: Decimal
    plant: Optional[Plant] = None # La plante est chargée séparément
