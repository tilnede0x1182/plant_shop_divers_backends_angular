# repositories/users.py

from .base import BaseRepository
from models.user import User

class UserRepository(BaseRepository):
    def __init__(self, db_connection):
        super().__init__(db_connection, "users")

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

    def find_by_email_with_password(self, email):
        """Récupère un utilisateur par email, incluant le hash du mot de passe."""
        return self._find_by_field("email", email)

    def _find_by_field(self, field, value):
        with self.db.cursor() as cursor:
            cursor.execute(f"SELECT * FROM {self.table_name} WHERE {field} = %s", (value,))
            row = cursor.fetchone()
            if row:
                columns = [desc[0] for desc in cursor.description]
                return self._map_from_row(row, columns)
            return None

    def list(self):
        """Liste les utilisateurs, admins en premier, puis par nom."""
        with self.db.cursor() as cursor:
            cursor.execute(f"SELECT * FROM {self.table_name} ORDER BY is_admin DESC, name ASC")
            rows = cursor.fetchall()
            columns = [desc[0] for desc in cursor.description]
            return [self._map_from_row(row, columns) for row in rows]

    def create(self, user_data):
        """Crée un nouvel utilisateur."""
        with self.db.cursor() as cursor:
            cursor.execute(
                "INSERT INTO users (name, email, password_hash, is_admin) VALUES (%s, %s, %s, %s) RETURNING id",
                (user_data['name'], user_data['email'], user_data['password'], user_data.get('admin', False))
            )
            user_id = cursor.fetchone()[0]
            self.db.commit()
            return self.find(user_id)

    def update(self, user_id, user_data):
        """Met à jour un utilisateur."""
        fields = []
        values = []
        for key, value in user_data.items():
            if key in ['name', 'email', 'is_admin']: # Champs modifiables
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
