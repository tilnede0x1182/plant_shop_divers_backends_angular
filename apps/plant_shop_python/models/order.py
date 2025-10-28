# models/order.py

from dataclasses import dataclass
from datetime import datetime
from decimal import Decimal
from typing import List, Optional
from .order_item import OrderItem

@dataclass
class Order:
    """Représente une commande dans la base de données."""
    id: int
    user_id: int
    total: Decimal
    status: str
    created_at: datetime
    items: Optional[List[OrderItem]] = None # Les items sont chargés séparément
