# controllers/orders.py

from flask import Blueprint, request, g
from util.response import json_response, empty_response
from util.security import auth_required, admin_required
from repositories.orders import OrderRepository
from repositories.order_items import OrderItemRepository

orders_bp = Blueprint('orders', __name__)

def _serialize_item(item):
    """Prépare un OrderItem pour le JSON (avec la plante)."""
    payload = {
        "id": item.id,
        "orderId": item.order_id,
        "plantId": item.plant_id,
        "quantity": item.quantity,
        "price": item.price
    }
    if getattr(item, 'plant', None):
        payload["plant"] = {
            "id": item.plant.id,
            "name": item.plant.name,
            "price": item.plant.price
        }
    return payload

def _serialize_order(order):
    """Convertit une commande et ses items en dictionnaire."""
    items = getattr(order, 'items', []) or []
    return {
        "id": order.id,
        "userId": order.user_id,
        "total": order.total,
        "status": order.status,
        "createdAt": order.created_at,
        "orderItems": [_serialize_item(item) for item in items]
    }

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
        serialized = []
        for order in orders:
            order.items = item_repo.find_all_for_order(order.id)
            serialized.append(_serialize_order(order))
        return json_response(serialized)

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
            new_order.items = item_repo.find_all_for_order(new_order.id)
            return json_response(_serialize_order(new_order), 201)
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

        updated_order.items = item_repo.find_all_for_order(order_id)
        return json_response(_serialize_order(updated_order))

    @orders_bp.route('/orders/<int:order_id>', methods=['DELETE'])
    @admin_required
    def delete_order(order_id):
        """Supprime une commande ainsi que ses items (admin)."""
        # La suppression en cascade est gérée par la DB
        order_repo.delete(order_id)
        return empty_response(200)

    return orders_bp
