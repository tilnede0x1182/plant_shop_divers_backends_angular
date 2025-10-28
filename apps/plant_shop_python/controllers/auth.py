# controllers/auth.py

from flask import Blueprint, request, g
from util.response import json_response, empty_response
from util.security import hash_password, check_password, create_session_token, invalidate_session
from repositories.users import UserRepository

auth_bp = Blueprint('auth', __name__)

def _serialize_user(user):
    """Prépare un utilisateur sans données sensibles."""
    return {
        "id": user.id,
        "name": user.name,
        "email": user.email,
        "admin": user.is_admin
    }

def _attach_session_cookie(response, token):
    """Ajoute le cookie d'authentification backend_python."""
    options = dict(httponly=True, samesite='Lax', max_age=3600 * 24, path='/')
    response.set_cookie('backend_python', token, **options)

def init_auth_controller(db_connection):
    user_repo = UserRepository(db_connection)

    @auth_bp.route('/register', methods=['POST'])
    def register():
        """Crée un compte utilisateur standard."""
        data = request.get_json() or {}
        email, password = data.get('email'), data.get('password')
        if not email or not password:
            return json_response({"error": "Email et mot de passe requis"}, 400)
        if user_repo.find_by_email_with_password(email):
            return json_response({"error": "Cet email est déjà utilisé"}, 409)
        user_payload = {
            'name': data.get('name'),
            'email': email,
            'password': hash_password(password)
        }
        created_user = user_repo.create(user_payload)
        return json_response({"user": _serialize_user(created_user)}, 201)

    @auth_bp.route('/login', methods=['POST'])
    def login():
        """Authentifie un utilisateur et installe le jeton."""
        data = request.get_json() or {}
        email, password = data.get('email'), data.get('password')
        if not email or not password:
            return json_response({"error": "Email et mot de passe requis"}, 400)
        user = user_repo.find_by_email_with_password(email)
        if not user or not check_password(password, user.password_hash):
            return json_response({"error": "Identifiants invalides"}, 401)
        token = create_session_token(user.id, user.is_admin, user.name, user.email)
        resp = json_response({"token": token, "user": _serialize_user(user)}, 201)
        _attach_session_cookie(resp, token)
        return resp

    @auth_bp.route('/logout', methods=['POST'])
    def logout():
        """Supprime les cookies d'authentification."""
        token = request.cookies.get('backend_python')
        invalidate_session(token)
        response = empty_response(204)
        response.delete_cookie('backend_python', path='/')
        return response

    @auth_bp.route('/me', methods=['GET'])
    def me():
        from util.security import auth_required

        @auth_required
        def get_me():
            return json_response({
                "id": g.user['id'],
                "name": g.user['name'],
                "admin": g.user['admin'],
                "email": g.user['email']
            })

        return get_me()

    return auth_bp
