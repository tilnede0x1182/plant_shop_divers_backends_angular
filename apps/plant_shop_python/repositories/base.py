# repositories/base.py

import psycopg2

class BaseRepository:
    """Classe de base pour les opérations CRUD communes."""
    def __init__(self, db_connection, table_name):
        self.db = db_connection
        self.table_name = table_name

    def _map_from_row(self, row, columns):
        """Méthode à implémenter par les classes filles pour mapper une ligne de DB à un modèle."""
        raise NotImplementedError

    def find(self, item_id):
        """Récupère un enregistrement par son ID."""
        with self.db.cursor() as cursor:
            cursor.execute(f"SELECT * FROM {self.table_name} WHERE id = %s", (item_id,))
            row = cursor.fetchone()
            if row:
                columns = [desc[0] for desc in cursor.description]
                return self._map_from_row(row, columns)
            return None

    def list(self):
        """Récupère tous les enregistrements d'une table."""
        with self.db.cursor() as cursor:
            cursor.execute(f"SELECT * FROM {self.table_name}")
            rows = cursor.fetchall()
            columns = [desc[0] for desc in cursor.description]
            return [self._map_from_row(row, columns) for row in rows]

    def delete(self, item_id):
        """Supprime un enregistrement par son ID."""
        with self.db.cursor() as cursor:
            cursor.execute(f"DELETE FROM {self.table_name} WHERE id = %s", (item_id,))
            return cursor.rowcount > 0
