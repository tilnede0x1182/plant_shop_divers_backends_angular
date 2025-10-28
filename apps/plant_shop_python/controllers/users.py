# controllers/users.py

from flask import Blueprint, request, g
from util.response import json_response, empty_response
from util.security import auth_required, admin_required
from repositories.users import UserRepository

users_bp = Blueprint('users', __name__)

def init_users_controller(db_connection):
    repo = UserRepository(db_connection)

    @users_bp.route('/', methods=['POST'])
    @admin_required
    def create_user():
        data = request.get_json()
        # Logique de création...
        new_user = repo.create(data)
        return json_response(new_user.__dict__, 201)

    @users_bp.route('/<int:user_id>', methods=['GET'])
    @auth_required
    def get_user(user_id):
        current_user = g.user
        if not current_user['admin'] and current_user['id'] != user_id:
            return json_response({"error": "Accès interdit"}, 403)

        user = repo.find(user_id)
        if not user:
            return json_response({"error": "Utilisateur non trouvé"}, 404)

        # Exclure le hash du mot de passe
        user_dict = user.__dict__
        del user_dict['password_hash']
        return json_response(user_dict)

    @users_bp.route('/<int:user_id>', methods=['PATCH'])
    @auth_required
    def update_user(user_id):
        current_user = g.user
        if not current_user['admin'] and current_user['id'] != user_id:
            return json_response({"error": "Accès interdit"}, 403)

        data = request.get_json()
        # Un utilisateur non-admin ne peut pas se promouvoir admin
        if not current_user['admin'] and 'admin' in data:
            del data['admin']

        updated_user = repo.update(user_id, data)
        user_dict = updated_user.__dict__
        del user_dict['password_hash']
        return json_response(user_dict)

    # Routes spécifiques à l'administration
    @users_bp.route('/admin/users', methods=['GET'])
    @admin_required
    def admin_list_users():
        users = repo.list()
        # Exclure les hashs de mot de passe
        users_list = [u.__dict__ for u in users]
        for u in users_list:
            del u['password_hash']
        return json_response(users_list)

    @users_bp.route('/admin/users/<int:user_id>', methods=['DELETE'])
    @admin_required
    def admin_delete_user(user_id):
        repo.delete(user_id)
        return empty_response(200)

    return users_bp
