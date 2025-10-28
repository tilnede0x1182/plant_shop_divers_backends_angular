# util/security.py

import bcrypt
import jwt
from datetime import datetime, timedelta, timezone
from functools import wraps
from flask import request, g
from config import config
from util.response import json_response
from repositories.users import UserRepository

def hash_password(password: str) -> str:
    """Hache un mot de passe."""
    return bcrypt.hashpw(password.encode('utf-8'), bcrypt.gensalt()).decode('utf-8')

def check_password(password: str, hashed_password: str) -> bool:
    """Vérifie un mot de passe par rapport à son hash."""
    return bcrypt.checkpw(password.encode('utf-8'), hashed_password.encode('utf-8'))

def create_jwt(user_id: int, is_admin: bool, user_name: str) -> str:
    """Crée un token JWT pour un utilisateur."""
    payload = {
        'sub': user_id,
        'admin': is_admin,
        'name': user_name,
        'exp': datetime.now(timezone.utc) + timedelta(hours=24) # Expiration dans 24h
    }
    return jwt.encode(payload, config.JWT_SECRET, algorithm='HS256')

def decode_jwt_from_cookie(cookie_value: str):
    """Décode le token JWT depuis la valeur du cookie."""
    if not cookie_value:
        return None
    try:
        return jwt.decode(cookie_value, config.JWT_SECRET, algorithms=['HS256'])
    except jwt.ExpiredSignatureError:
        return None # Le token a expiré
    except jwt.InvalidTokenError:
        return None # Token invalide

def auth_required(f):
    """Décorateur pour les routes nécessitant une authentification."""
    @wraps(f)
    def decorated_function(*args, **kwargs):
        token = request.cookies.get('jwt')
        if not token:
            return json_response({"error": "Authentification requise"}, 401)

        payload = decode_jwt_from_cookie(token)
        if not payload:
            return json_response({"error": "Token invalide ou expiré"}, 401)

        # Injecte l'utilisateur dans le contexte global de la requête (g)
        g.user = {
            "id": payload['sub'],
            "admin": payload['admin'],
            "name": payload['name']
        }
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
