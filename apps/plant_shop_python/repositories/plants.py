# repositories/plants.py

from .base import BaseRepository
from models.plant import Plant

class PlantRepository(BaseRepository):
    def __init__(self, db_connection):
        super().__init__(db_connection, "plants")

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

    def create(self, plant_data):
        with self.db.cursor() as cursor:
            cursor.execute(
                "INSERT INTO plants (name, price, stock, description) VALUES (%s, %s, %s, %s) RETURNING id",
                (plant_data['name'], plant_data['price'], plant_data['stock'], plant_data.get('description'))
            )
            plant_id = cursor.fetchone()[0]
            self.db.commit()
            return self.find(plant_id)

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

    def list(self):
        """Liste toutes les plantes triées par nom croissant."""
        with self.db.cursor() as cursor:
            cursor.execute(f"SELECT * FROM {self.table_name} ORDER BY name ASC")
            rows = cursor.fetchall()
            columns = [desc[0] for desc in cursor.description]
            return [self._map_from_row(row, columns) for row in rows]
