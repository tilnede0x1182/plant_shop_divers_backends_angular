# repositories/plants.py

from .base import BaseRepository
from models.plant import Plant

class PlantRepository(BaseRepository):
    """
    	Constructeur du repository des plantes.

    	@param db_connection Connection Connexion psycopg2 à PostgreSQL
    """
    def __init__(self, db_connection):
        super().__init__(db_connection, "plants")

    """
    	Mappe une ligne SQL vers un objet Plant.

    	@param row tuple Ligne de résultat de la requête
    	@param columns list Noms des colonnes
    	@return Plant Instance Plant avec les données
    """
    def _map_from_row(self, row, columns):
        col_map = {col: val for col, val in zip(columns, row)}
        return Plant(
            id=col_map.get('id'),
            name=col_map.get('name'),
            description=col_map.get('description'),
            price=col_map.get('price'),
            stock=col_map.get('stock'),
            created_at=col_map.get('created_at')
        )

    """
    	Crée une nouvelle plante en base de données.

    	@param plant_data dict Dictionnaire avec name, price, stock, description
    	@return Plant Instance Plant créée
    """
    def create(self, plant_data):
        with self.db.cursor() as cursor:
            cursor.execute(
                "INSERT INTO plants (name, price, stock, description) VALUES (%s, %s, %s, %s) RETURNING id",
                (plant_data['name'], plant_data['price'], plant_data['stock'], plant_data.get('description'))
            )
            plant_id = cursor.fetchone()[0]
            self.db.commit()
            return self.find(plant_id)

    """
    	Met à jour une plante existante.

    	@param plant_id int Identifiant de la plante
    	@param plant_data dict Champs à modifier (name, price, stock, description)
    	@return Plant Instance Plant mise à jour
    """
    def update(self, plant_id, plant_data):
        fields, values = [], []
        for key, value in plant_data.items():
            if key in ['name', 'price', 'stock', 'description']:
                fields.append(f"{key} = %s")
                values.append(value)

        if not fields: return self.find(plant_id)

        values.append(plant_id)

        with self.db.cursor() as cursor:
            cursor.execute(
                f"UPDATE {self.table_name} SET {', '.join(fields)} WHERE id = %s",
                tuple(values)
            )
            self.db.commit()
        return self.find(plant_id)

    """
    	Liste toutes les plantes triées par nom croissant.

    	@return list Liste d instances Plant
    """
    def list(self):
        with self.db.cursor() as cursor:
            cursor.execute(f"SELECT * FROM {self.table_name} ORDER BY name ASC")
            rows = cursor.fetchall()
            columns = [desc[0] for desc in cursor.description]
            return [self._map_from_row(row, columns) for row in rows]
