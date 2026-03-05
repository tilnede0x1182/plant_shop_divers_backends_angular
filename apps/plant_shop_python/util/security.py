# util/security.py

import bcrypt
import uuid
from functools import wraps
from flask import request, g
from util.response import json_response
from typing import Optional, Dict

_SESSIONS: Dict[str, dict] = {}

"""
	Hache un mot de passe en utilisant bcrypt.

	@param password str Mot de passe en clair à hacher
	@return str Hash bcrypt du mot de passe
"""
def hash_password(password: str) -> str:
    return bcrypt.hashpw(password.encode('utf-8'), bcrypt.gensalt()).decode('utf-8')

"""
	Vérifie un mot de passe par rapport à son hash bcrypt.

	@param password str Mot de passe en clair à vérifier
	@param hashed_password str Hash bcrypt stocké
	@return bool True si le mot de passe correspond, False sinon
"""
def check_password(password: str, hashed_password: str) -> bool:
    return bcrypt.checkpw(password.encode('utf-8'), hashed_password.encode('utf-8'))

"""
	Crée un identifiant de session en mémoire et stocke les infos utilisateur.

	@param user_id int Identifiant unique de l utilisateur
	@param is_admin bool Indique si l utilisateur est administrateur
	@param user_name str Nom de l utilisateur
	@param user_email str Email de l utilisateur
	@return str Token UUID hexadécimal de session
"""
def create_session_token(user_id: int, is_admin: bool, user_name: str, user_email: str) -> str:
    token = uuid.uuid4().hex
    _SESSIONS[token] = {
        "id": user_id,
        "admin": bool(is_admin),
        "name": user_name,
        "email": user_email
    }
    return token

"""
	Supprime une session active si elle existe.

	@param token Optional[str] Token de session à invalider (peut être None)
"""
def invalidate_session(token: Optional[str]) -> None:
    if token:
        _SESSIONS.pop(token, None)

"""
	Décorateur pour les routes nécessitant une authentification.
	Injecte l utilisateur dans g.user si le token est valide.

	@param f function Fonction de route à décorer
	@return function Fonction décorée avec vérification d authentification
"""
def auth_required(f):
    @wraps(f)
    def decorated_function(*args, **kwargs):
        token = _extract_token()
        if not token:
            return json_response({"error": "Authentification requise"}, 401)

        session = _SESSIONS.get(token)
        if not session:
            return json_response({"error": "Token invalide ou expiré"}, 401)

        # Injecte l'utilisateur dans le contexte global de la requête (g)
        g.user = session
        return f(*args, **kwargs)
    return decorated_function

"""
	Décorateur pour les routes nécessitant des droits administrateur.
	Combine auth_required et vérifie le flag admin.

	@param f function Fonction de route à décorer
	@return function Fonction décorée avec vérification admin
"""
def admin_required(f):
    @wraps(f)
    @auth_required
    def decorated_function(*args, **kwargs):
        if not g.user or not g.user['admin']:
            return json_response({"error": "Accès interdit. Droits administrateur requis."}, 403)
        return f(*args, **kwargs)
    return decorated_function

"""
	Récupère un jeton depuis les cookies ou l en-tête Authorization.
	Priorise le header Bearer, puis le cookie backend_python.

	@return Optional[str] Token de session ou None si absent
"""
def _extract_token() -> Optional[str]:
    header = request.headers.get('Authorization', '')
    if header.startswith('Bearer '):
        return header.split(' ', 1)[1].strip() or None
    cookie = request.cookies.get('backend_python')
    if cookie:
        cleaned = cookie.strip()
        return cleaned.strip('"') or None
    return None
