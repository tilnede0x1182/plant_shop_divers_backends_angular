# controllers/users.py

from flask import Blueprint, request, g
from util.response import json_response, empty_response
from util.security import auth_required, admin_required, hash_password
from repositories.users import UserRepository

users_bp = Blueprint('users', __name__)

def _serialize_user(user):
    """Retourne un utilisateur sans hash ni champs internes."""
    return {
        "id": user.id,
        "name": user.name,
        "email": user.email,
        "admin": bool(user.is_admin),
        "createdAt": user.created_at
    }

def init_users_controller(db_connection):
    repo = UserRepository(db_connection)

    @users_bp.route('/users', methods=['GET'])
    @admin_required
    def list_users():
        """Récupère tous les utilisateurs (admin uniquement)."""
        users = repo.list()
        return json_response([_serialize_user(user) for user in users])

    @users_bp.route('/users', methods=['POST'])
    @admin_required
    def create_user():
        """Crée un utilisateur avec contrôle admin."""
        data = request.get_json() or {}
        if data.get('password'):
            data = data.copy()
            data['password'] = hash_password(data['password'])
        created = repo.create(data)
        return json_response(_serialize_user(created), 201)

    @users_bp.route('/users/<int:user_id>', methods=['GET'])
    @auth_required
    def get_user(user_id):
        """Retourne un utilisateur si autorisé."""
        current_user = g.user
        if not current_user['admin'] and current_user['id'] != user_id:
            return json_response({"error": "Accès interdit"}, 403)

        user = repo.find(user_id)
        if not user:
            return json_response({"error": "Utilisateur non trouvé"}, 404)

        return json_response(_serialize_user(user))

    @users_bp.route('/users/<int:user_id>', methods=['PATCH'])
    @auth_required
    def update_user(user_id):
        """Met à jour un utilisateur autorisé."""
        current_user = g.user
        if not current_user['admin'] and current_user['id'] != user_id:
            return json_response({"error": "Accès interdit"}, 403)

        data = request.get_json() or {}
        # Un utilisateur non-admin ne peut pas se promouvoir admin
        if not current_user['admin'] and 'admin' in data:
            del data['admin']

        updated_user = repo.update(user_id, data)
        return json_response(_serialize_user(updated_user))

    @users_bp.route('/users/<int:user_id>', methods=['DELETE'])
    @admin_required
    def delete_user(user_id):
        """Supprime un utilisateur (admin uniquement)."""
        repo.delete(user_id)
        return empty_response(200)

    # Routes spécifiques à l'administration
    @users_bp.route('/admin/users', methods=['GET'])
    @admin_required
    def admin_list_users():
        """Alias admin pour compatibilité legacy."""
        return json_response([_serialize_user(user) for user in repo.list()])

    @users_bp.route('/admin/users/<int:user_id>', methods=['DELETE'])
    @admin_required
    def admin_delete_user(user_id):
        """Alias admin pour suppression legacy."""
        repo.delete(user_id)
        return empty_response(200)

    return users_bp
