# models/plant.py

from dataclasses import dataclass
from datetime import datetime
from decimal import Decimal

@dataclass
class Plant:
    """Représente une plante dans la base de données."""
    id: int
    name: str
    description: str
    price: Decimal
    stock: int
    created_at: datetime
