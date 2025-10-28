# app.py

import psycopg2
from flask import Flask, Blueprint
from flask_cors import CORS
from config import config
from routes import register_routes

# Création de l'application Flask
app = Flask(__name__)

# Configuration de CORS pour autoriser les requêtes du frontend Angular
CORS(app, supports_credentials=True, origins=[
    "http://localhost:8300", # Port de développement Angular
    "http://localhost:4150"  # Port de production SSR
])

# Connexion à la base de données
try:
    db_connection = psycopg2.connect(dsn=config.DATABASE_URL)
    print("🔌 Connexion à la base de données réussie.")
except psycopg2.OperationalError as e:
    print(f"❌ Erreur de connexion à la base de données : {e}")
    exit(1)

# Créer un Blueprint global pour le préfixe /api
api_bp = Blueprint('api', __name__, url_prefix='/api')

# Enregistrer les routes sur ce Blueprint
register_routes(api_bp, db_connection)

# Enregistrer le Blueprint principal sur l'application
app.register_blueprint(api_bp)

@app.route('/')
def index():
    """Route racine pour vérifier que le serveur est en ligne."""
    return "👋 Serveur Python PlantShop en ligne !"

# Point d'entrée pour l'exécution du serveur
if __name__ == '__main__':
    from waitress import serve
    try:
        serve(app, host='0.0.0.0', port=config.SERVER_PORT)
    except OSError as e:
        if "Address already in use" in str(e):
            print(f"❌ Le port {config.SERVER_PORT} est déjà utilisé. Fermez le processus ou changez le port.")
        else:
            print(f"❌ Erreur lors du démarrage du serveur : {e}")
    finally:
        if db_connection:
            db_connection.close()
            print("🔌 Connexion à la base de données fermée.")
