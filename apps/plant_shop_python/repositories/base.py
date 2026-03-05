# repositories/base.py

import psycopg2

class BaseRepository:
    """Classe de base pour les opérations CRUD communes."""
    def __init__(self, db_connection, table_name):
        self.db = db_connection
        self.table_name = table_name

    """
    	Méthode abstraite pour mapper une ligne de DB à un modèle.
    	À implémenter par les classes filles.

    	@param row tuple Ligne de résultat de la requête SQL
    	@param columns list Liste des noms de colonnes
    	@return object Instance du modèle correspondant
    """
    def _map_from_row(self, row, columns):
        raise NotImplementedError

    """
    	Récupère un enregistrement par son ID.

    	@param item_id int Identifiant unique de l enregistrement
    	@return object|None Instance du modèle ou None si non trouvé
    """
    def find(self, item_id):
        with self.db.cursor() as cursor:
            cursor.execute(f"SELECT * FROM {self.table_name} WHERE id = %s", (item_id,))
            row = cursor.fetchone()
            if row:
                columns = [desc[0] for desc in cursor.description]
                return self._map_from_row(row, columns)
            return None

    """
    	Récupère tous les enregistrements de la table.

    	@return list Liste d instances du modèle
    """
    def list(self):
        with self.db.cursor() as cursor:
            cursor.execute(f"SELECT * FROM {self.table_name}")
            rows = cursor.fetchall()
            columns = [desc[0] for desc in cursor.description]
            return [self._map_from_row(row, columns) for row in rows]

    """
    	Supprime un enregistrement par son ID.

    	@param item_id int Identifiant de l enregistrement à supprimer
    	@return bool True si supprimé, False sinon
    """
    def delete(self, item_id):
        with self.db.cursor() as cursor:
            cursor.execute(f"DELETE FROM {self.table_name} WHERE id = %s", (item_id,))
            return cursor.rowcount > 0
