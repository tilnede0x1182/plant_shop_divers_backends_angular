# controllers/auth.py

from flask import Blueprint, request, g
from util.response import json_response, empty_response
from util.security import hash_password, check_password, create_jwt
from repositories.users import UserRepository
import psycopg2

auth_bp = Blueprint('auth', __name__)

def init_auth_controller(db_connection):
    user_repo = UserRepository(db_connection)

    @auth_bp.route('/register', methods=['POST'])
    def register():
        data = request.get_json()
        name = data.get('name')
        email = data.get('email')
        password = data.get('password')

        if not email or not password:
            return json_response({"error": "Email et mot de passe requis"}, 400)

        if user_repo.find_by_email_with_password(email):
            return json_response({"error": "Cet email est déjà utilisé"}, 409)

        hashed_password = hash_password(password)
        user_data = {'name': name, 'email': email, 'password': hashed_password}

        user_repo.create(user_data)
        return json_response({"message": "Utilisateur créé"}, 201)

    @auth_bp.route('/login', methods=['POST'])
    def login():
        data = request.get_json()
        email = data.get('email')
        password = data.get('password')

        if not email or not password:
            return json_response({"error": "Email et mot de passe requis"}, 400)

        user = user_repo.find_by_email_with_password(email)
        if not user or not check_password(password, user.password_hash):
            return json_response({"error": "Identifiants invalides"}, 401)

        token = create_jwt(user.id, user.is_admin, user.name)

        # Le test Java attend un corps JSON, même vide.
        resp = json_response({}, 201)
        resp.set_cookie('jwt', token, httponly=True, samesite='Lax', max_age=3600*24)
        return resp

    @auth_bp.route('/logout', methods=['POST'])
    def logout():
        resp = empty_response(204)
        resp.delete_cookie('jwt', path='/')
        return resp

    @auth_bp.route('/me', methods=['GET'])
    def me():
        from util.security import auth_required

        @auth_required
        def get_me():
            return json_response({
                "id": g.user['id'],
                "name": g.user['name'],
                "admin": g.user['admin']
            })

        return get_me()

    return auth_bp
