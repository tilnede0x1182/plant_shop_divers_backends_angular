# repositories/orders.py

from .base import BaseRepository
from models.order import Order
from models.order_item import OrderItem
from models.plant import Plant
from decimal import Decimal

class OrderRepository(BaseRepository):
    """
    	Constructeur du repository des commandes.

    	@param db_connection Connection Connexion psycopg2 à PostgreSQL
    """
    def __init__(self, db_connection):
        super().__init__(db_connection, "orders")

    """
    	Mappe une ligne SQL vers un objet Order.

    	@param row tuple Ligne de résultat de la requête
    	@param columns list Noms des colonnes
    	@return Order Instance Order avec les données
    """
    def _map_from_row(self, row, columns):
        col_map = {col: val for col, val in zip(columns, row)}
        return Order(
            id=col_map.get('id'),
            user_id=col_map.get('user_id'),
            total=col_map.get('total'),
            status=col_map.get('status'),
            created_at=col_map.get('created_at')
        )

    """
    	Récupère toutes les commandes pour un utilisateur donné.
    	Triées par date décroissante (plus récentes en premier).

    	@param user_id int Identifiant de l utilisateur
    	@return list Liste d instances Order
    """
    def find_all_for_user(self, user_id):
        with self.db.cursor() as cursor:
            cursor.execute(
                f"SELECT * FROM {self.table_name} WHERE user_id = %s ORDER BY created_at DESC",
                (user_id,)
            )
            rows = cursor.fetchall()
            columns = [desc[0] for desc in cursor.description]
            return [self._map_from_row(row, columns) for row in rows]

    """
    	Crée une commande et ses items de manière transactionnelle.
    	Vérifie le stock et le décrémente pour chaque item.

    	@param user_id int Identifiant de l utilisateur
    	@param items list Liste de dict avec plantId et quantity
    	@return Order Instance Order créée avec total calculé
    	@raises ValueError Si plante non trouvée ou stock insuffisant
    """
    def create_with_items(self, user_id, items):
        order_id = None
        total_price = Decimal('0.0')

        try:
            with self.db.cursor() as cursor:
                # 1. Créer la commande avec un total temporaire
                cursor.execute(
                    "INSERT INTO orders (user_id, total, status) VALUES (%s, %s, %s) RETURNING id",
                    (user_id, total_price, 'confirmed')
                )
                order_id = cursor.fetchone()[0]

                # 2. Traiter chaque item
                for item_data in items:
                    plant_id = item_data['plantId']
                    quantity = item_data['quantity']

                    # Verrouiller la ligne de la plante pour éviter les race conditions sur le stock
                    cursor.execute("SELECT price, stock FROM plants WHERE id = %s FOR UPDATE", (plant_id,))
                    plant_row = cursor.fetchone()
                    if not plant_row:
                        raise ValueError(f"Plante avec l'ID {plant_id} non trouvée.")

                    plant_price, plant_stock = plant_row

                    if plant_stock < quantity:
                        raise ValueError(f"Stock insuffisant pour la plante {plant_id}.")

                    # Ajouter l'item à la commande
                    cursor.execute(
                        "INSERT INTO order_items (order_id, plant_id, quantity, price) VALUES (%s, %s, %s, %s)",
                        (order_id, plant_id, quantity, plant_price)
                    )

                    # Mettre à jour le stock
                    cursor.execute(
                        "UPDATE plants SET stock = stock - %s WHERE id = %s",
                        (quantity, plant_id)
                    )

                    total_price += plant_price * quantity

                # 3. Mettre à jour le total final de la commande
                cursor.execute("UPDATE orders SET total = %s WHERE id = %s", (total_price, order_id))

            self.db.commit()
            return self.find(order_id)

        except Exception as e:
            if self.db:
                self.db.rollback()
            raise e

    """
    	Met à jour le statut d une commande.

    	@param order_id int Identifiant de la commande
    	@param status str Nouveau statut (confirmed, pending, shipped, delivered)
    	@return Order|None Instance Order mise à jour ou None si non trouvée
    """
    def update_status(self, order_id, status):
        with self.db.cursor() as cursor:
            cursor.execute(
                "UPDATE orders SET status = %s WHERE id = %s",
                (status, order_id)
            )
            self.db.commit()
        return self.find(order_id)
