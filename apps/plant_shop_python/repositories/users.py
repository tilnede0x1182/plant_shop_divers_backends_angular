# repositories/users.py

from .base import BaseRepository
from models.user import User

class UserRepository(BaseRepository):
    """
    	Constructeur du repository des utilisateurs.

    	@param db_connection Connection Connexion psycopg2 à PostgreSQL
    """
    def __init__(self, db_connection):
        super().__init__(db_connection, "users")

    """
    	Mappe une ligne SQL vers un objet User.

    	@param row tuple Ligne de résultat de la requête
    	@param columns list Noms des colonnes
    	@return User Instance User avec les données
    """
    def _map_from_row(self, row, columns):
        col_map = {col: val for col, val in zip(columns, row)}
        return User(
            id=col_map.get('id'),
            name=col_map.get('name'),
            email=col_map.get('email'),
            password_hash=col_map.get('password_hash'),
            is_admin=col_map.get('is_admin'),
            created_at=col_map.get('created_at')
        )

    """
    	Récupère un utilisateur par email, incluant le hash du mot de passe.
    	Utilisé pour l authentification.

    	@param email str Adresse email à rechercher
    	@return User|None Instance User ou None si non trouvé
    """
    def find_by_email_with_password(self, email):
        return self._find_by_field("email", email)

    """
    	Recherche un utilisateur par un champ donné.

    	@param field str Nom du champ SQL (email, name, etc)
    	@param value any Valeur à rechercher
    	@return User|None Instance User ou None si non trouvé
    """
    def _find_by_field(self, field, value):
        with self.db.cursor() as cursor:
            cursor.execute(f"SELECT * FROM {self.table_name} WHERE {field} = %s", (value,))
            row = cursor.fetchone()
            if row:
                columns = [desc[0] for desc in cursor.description]
                return self._map_from_row(row, columns)
            return None

    """
    	Liste les utilisateurs, admins en premier, puis par nom.

    	@return list Liste d instances User
    """
    def list(self):
        with self.db.cursor() as cursor:
            cursor.execute(f"SELECT * FROM {self.table_name} ORDER BY is_admin DESC, name ASC")
            rows = cursor.fetchall()
            columns = [desc[0] for desc in cursor.description]
            return [self._map_from_row(row, columns) for row in rows]

    """
    	Crée un nouvel utilisateur en base de données.

    	@param user_data dict Dictionnaire avec name, email, password, admin
    	@return User Instance User créée
    """
    def create(self, user_data):
        with self.db.cursor() as cursor:
            cursor.execute(
                "INSERT INTO users (name, email, password_hash, is_admin) VALUES (%s, %s, %s, %s) RETURNING id",
                (user_data['name'], user_data['email'], user_data['password'], user_data.get('admin', False))
            )
            user_id = cursor.fetchone()[0]
            self.db.commit()
            return self.find(user_id)

    """
    	Met à jour un utilisateur existant.

    	@param user_id int Identifiant de l utilisateur
    	@param user_data dict Champs à modifier (name, email, admin)
    	@return User Instance User mise à jour
    """
    def update(self, user_id, user_data):
        fields = []
        values = []
        for key, value in user_data.items():
            if key == 'admin':
                fields.append("is_admin = %s")
                values.append(value)
            elif key in ['name', 'email', 'is_admin']:
                fields.append(f"{key} = %s")
                values.append(value)

        if not fields:
            return self.find(user_id)

        values.append(user_id)

        with self.db.cursor() as cursor:
            cursor.execute(
                f"UPDATE {self.table_name} SET {', '.join(fields)} WHERE id = %s",
                tuple(values)
            )
            self.db.commit()
        return self.find(user_id)
