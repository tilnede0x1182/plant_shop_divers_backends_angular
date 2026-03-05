# repositories/order_items.py

from .base import BaseRepository
from models.order_item import OrderItem
from models.plant import Plant # Pour le mapping joint

class OrderItemRepository(BaseRepository):
    """
    	Constructeur du repository des items de commande.

    	@param db_connection Connection Connexion psycopg2 à PostgreSQL
    """
    def __init__(self, db_connection):
        super().__init__(db_connection, "order_items")

    """
    	Mappe une ligne SQL vers un objet OrderItem.
    	Peut inclure les données de la plante associée.

    	@param row tuple Ligne de résultat de la requête
    	@param columns list Noms des colonnes
    	@param with_plant bool Inclure les données de la plante (défaut: False)
    	@return OrderItem Instance OrderItem avec plant si demandé
    """
    def _map_from_row(self, row, columns, with_plant=False):
        col_map = {col: val for col, val in zip(columns, row)}
        order_item = OrderItem(
            id=col_map.get('id'),
            order_id=col_map.get('order_id'),
            plant_id=col_map.get('plant_id'),
            quantity=col_map.get('quantity'),
            price=col_map.get('price')
        )
        if with_plant:
            order_item.plant = Plant(
                id=col_map.get('plant_id'),
                name=col_map.get('plant_name'),
                price=col_map.get('plant_price'),
                description=None, stock=0, created_at=None # Non requis pour l'affichage de la commande
            )
        return order_item

    """
    	Récupère tous les items pour une commande avec les infos plante.
    	Effectue une jointure avec la table plants.

    	@param order_id int Identifiant de la commande
    	@return list Liste d instances OrderItem avec plant attaché
    """
    def find_all_for_order(self, order_id):
        with self.db.cursor() as cursor:
            sql = """
                SELECT oi.*, p.name as plant_name, p.price as plant_price
                FROM order_items oi
                JOIN plants p ON oi.plant_id = p.id
                WHERE oi.order_id = %s
            """
            cursor.execute(sql, (order_id,))
            rows = cursor.fetchall()
            columns = [desc[0] for desc in cursor.description]
            return [self._map_from_row(row, columns, with_plant=True) for row in rows]
