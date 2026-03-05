# models/user.py

from dataclasses import dataclass
from datetime import datetime

"""
	Représente un utilisateur dans la base de données.

	@param id Identifiant unique de l utilisateur
	@param name Nom de l utilisateur
	@param email Adresse email
	@param password_hash Hash du mot de passe
	@param is_admin Indique si l utilisateur est administrateur
	@param created_at Date de création du compte
"""
@dataclass
class User:
    id: int
    name: str
    email: str
    password_hash: str  # Le hash du mot de passe
    is_admin: bool
    created_at: datetime
