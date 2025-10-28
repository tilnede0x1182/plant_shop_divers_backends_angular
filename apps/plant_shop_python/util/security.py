# util/security.py

import bcrypt
import uuid
from functools import wraps
from flask import request, g
from util.response import json_response
from typing import Optional, Dict

_SESSIONS: Dict[str, dict] = {}

def hash_password(password: str) -> str:
    """Hache un mot de passe."""
    return bcrypt.hashpw(password.encode('utf-8'), bcrypt.gensalt()).decode('utf-8')

def check_password(password: str, hashed_password: str) -> bool:
    """Vérifie un mot de passe par rapport à son hash."""
    return bcrypt.checkpw(password.encode('utf-8'), hashed_password.encode('utf-8'))

def create_session_token(user_id: int, is_admin: bool, user_name: str, user_email: str) -> str:
    """Crée un identifiant de session en mémoire."""
    token = uuid.uuid4().hex
    _SESSIONS[token] = {
        "id": user_id,
        "admin": bool(is_admin),
        "name": user_name,
        "email": user_email
    }
    return token

def invalidate_session(token: Optional[str]) -> None:
    """Supprime une session active si elle existe."""
    if token:
        _SESSIONS.pop(token, None)

def auth_required(f):
    """Décorateur pour les routes nécessitant une authentification."""
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

def admin_required(f):
    """Décorateur pour les routes nécessitant des droits d'administrateur."""
    @wraps(f)
    @auth_required
    def decorated_function(*args, **kwargs):
        if not g.user or not g.user['admin']:
            return json_response({"error": "Accès interdit. Droits administrateur requis."}, 403)
        return f(*args, **kwargs)
    return decorated_function

def _extract_token() -> Optional[str]:
    """Récupère un jeton depuis les cookies ou l'en-tête Authorization."""
    header = request.headers.get('Authorization', '')
    if header.startswith('Bearer '):
        return header.split(' ', 1)[1].strip() or None
    cookie = request.cookies.get('backend_python')
    if cookie:
        cleaned = cookie.strip()
        return cleaned.strip('"') or None
    return None
