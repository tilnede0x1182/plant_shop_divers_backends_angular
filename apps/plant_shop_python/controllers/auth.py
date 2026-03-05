# controllers/auth.py

from flask import Blueprint, request, g
from util.response import json_response, empty_response
from util.security import hash_password, check_password, create_session_token, invalidate_session
from repositories.users import UserRepository

auth_bp = Blueprint('auth', __name__)

"""
	Prépare un utilisateur pour la réponse JSON sans données sensibles.

	@param user User Objet utilisateur depuis la base de données
	@return dict Dictionnaire avec id, name, email, admin
"""
def _serialize_user(user):
    return {
        "id": user.id,
        "name": user.name,
        "email": user.email,
        "admin": user.is_admin
    }

"""
	Ajoute le cookie d authentification backend_python à la réponse.

	@param response Response Objet réponse Flask
	@param token str Token de session à stocker dans le cookie
"""
def _attach_session_cookie(response, token):
    options = dict(httponly=True, samesite='Lax', max_age=3600 * 24, path='/')
    response.set_cookie('backend_python', token, **options)

"""
	Initialise le contrôleur d authentification avec la connexion DB.
	Enregistre les routes /register, /login, /logout, /me sur le Blueprint.

	@param db_connection Connection Connexion psycopg2 à PostgreSQL
	@return Blueprint Blueprint Flask auth_bp avec les routes enregistrées
"""
def init_auth_controller(db_connection):
    user_repo = UserRepository(db_connection)

    """
    	Crée un compte utilisateur standard.
    	Valide email/password, vérifie l unicité de l email.

    	@return Response JSON avec l utilisateur créé (201) ou erreur (400/409)
    """
    @auth_bp.route('/register', methods=['POST'])
    def register():
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

    """
    	Authentifie un utilisateur et installe le jeton de session.
    	Vérifie les identifiants et crée une session en mémoire.

    	@return Response JSON avec token et user (201) ou erreur (400/401)
    """
    @auth_bp.route('/login', methods=['POST'])
    def login():
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

    """
    	Supprime les cookies d authentification et invalide la session.

    	@return Response Réponse vide (204)
    """
    @auth_bp.route('/logout', methods=['POST'])
    def logout():
        token = request.cookies.get('backend_python')
        invalidate_session(token)
        response = empty_response(204)
        response.delete_cookie('backend_python', path='/')
        return response

    """
    	Retourne les informations de l utilisateur connecté.
    	Nécessite une authentification valide.

    	@return Response JSON avec id, name, admin, email (200) ou erreur (401)
    """
    @auth_bp.route('/me', methods=['GET'])
    def me():
        from util.security import auth_required

        """
        	Fonction interne protégée par auth_required.
        	Retourne les données de session de l utilisateur.

        	@return Response JSON avec les informations utilisateur
        """
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
