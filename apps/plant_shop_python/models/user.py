# models/user.py

from dataclasses import dataclass
from datetime import datetime

@dataclass
class User:
    """Représente un utilisateur dans la base de données."""
    id: int
    name: str
    email: str
    password_hash: str  # Le hash du mot de passe
    is_admin: bool
    created_at: datetime
