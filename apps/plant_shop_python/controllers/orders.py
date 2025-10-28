# controllers/orders.py

from flask import Blueprint, request, g
from util.response import json_response, empty_response
from util.security import auth_required, admin_required
from repositories.orders import OrderRepository
from repositories.order_items import OrderItemRepository

orders_bp = Blueprint('orders', __name__)

def init_orders_controller(db_connection):
    order_repo = OrderRepository(db_connection)
    item_repo = OrderItemRepository(db_connection)

    @orders_bp.route('/orders', methods=['GET'])
    @auth_required
    def list_orders():
        """Retourne les commandes liées à l'utilisateur courant."""
        user_id = g.user['id']
        orders = order_repo.find_all_for_user(user_id)

        # Enrichir chaque commande avec ses items
        result = []
        for order in orders:
            items = item_repo.find_all_for_order(order.id)
            order.items = items
            result.append(order.__dict__)

        return json_response(result)

    @orders_bp.route('/orders', methods=['POST'])
    @auth_required
    def create_order():
        """Crée une commande pour l'utilisateur connecté."""
        user_id = g.user['id']
        data = request.get_json() or {}
        items = data.get('items')
        if not items:
            return json_response({"error": "La commande doit contenir des articles"}, 400)

        try:
            new_order = order_repo.create_with_items(user_id, items)
            return json_response(new_order.__dict__, 201)
        except ValueError as e:
            return json_response({"error": str(e)}, 400)
        except Exception as e:
            return json_response({"error": "Erreur interne du serveur"}, 500)

    @orders_bp.route('/orders/<int:order_id>', methods=['PATCH'])
    @admin_required
    def update_order_status(order_id):
        """Met à jour le statut d'une commande (admin)."""
        data = request.get_json() or {}
        status = data.get('status')
        if not status:
            return json_response({"error": "Le statut est requis"}, 400)

        updated_order = order_repo.update_status(order_id, status)
        if not updated_order:
            return json_response({"error": "Commande non trouvée"}, 404)

        return json_response(updated_order.__dict__)

    @orders_bp.route('/orders/<int:order_id>', methods=['DELETE'])
    @admin_required
    def delete_order(order_id):
        """Supprime une commande ainsi que ses items (admin)."""
        # La suppression en cascade est gérée par la DB
        order_repo.delete(order_id)
        return empty_response(200)

    return orders_bp
