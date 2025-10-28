# controllers/users.py

from flask import Blueprint, request, g
from util.response import json_response, empty_response
from util.security import auth_required, admin_required, hash_password
from repositories.users import UserRepository

users_bp = Blueprint('users', __name__)

def init_users_controller(db_connection):
    repo = UserRepository(db_connection)

    @users_bp.route('/users', methods=['GET'])
    @admin_required
    def list_users():
        """Récupère tous les utilisateurs (admin uniquement)."""
        users = repo.list()
        sanitized = []
        for user in users:
            data = user.__dict__.copy()
            data.pop('password_hash', None)
            sanitized.append(data)
        return json_response(sanitized)

    @users_bp.route('/users', methods=['POST'])
    @admin_required
    def create_user():
        """Crée un utilisateur avec contrôle admin."""
        data = request.get_json() or {}
        if data.get('password'):
            data = data.copy()
            data['password'] = hash_password(data['password'])
        created = repo.create(data)
        payload = created.__dict__.copy()
        payload.pop('password_hash', None)
        return json_response(payload, 201)

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

        user_data = user.__dict__.copy()
        user_data.pop('password_hash', None)
        return json_response(user_data)

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
        payload = updated_user.__dict__.copy()
        payload.pop('password_hash', None)
        return json_response(payload)

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
        sanitized = []
        for user in repo.list():
            data = user.__dict__.copy()
            data.pop('password_hash', None)
            sanitized.append(data)
        return json_response(sanitized)

    @users_bp.route('/admin/users/<int:user_id>', methods=['DELETE'])
    @admin_required
    def admin_delete_user(user_id):
        """Alias admin pour suppression legacy."""
        repo.delete(user_id)
        return empty_response(200)

    return users_bp
