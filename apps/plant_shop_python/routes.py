# routes.py

from flask import Blueprint
from controllers.auth import init_auth_controller
from controllers.plants import init_plants_controller
from controllers.users import init_users_controller
from controllers.orders import init_orders_controller

def register_routes(app, db_connection):
    """Enregistre toutes les routes de l'application."""

    # Création des Blueprints avec la connexion DB
    auth_bp = init_auth_controller(db_connection)
    plants_bp = init_plants_controller(db_connection)
    users_bp = init_users_controller(db_connection)
    orders_bp = init_orders_controller(db_connection)

    # Enregistrement des Blueprints sur l'application Flask
    # Le préfixe /api est géré globalement dans app.py
    app.register_blueprint(auth_bp, url_prefix='/auth')
    app.register_blueprint(plants_bp, url_prefix='/') # Gère /plants et /admin/plants
    app.register_blueprint(users_bp, url_prefix='/') # Gère /users et /admin/users
    app.register_blueprint(orders_bp, url_prefix='/orders')
