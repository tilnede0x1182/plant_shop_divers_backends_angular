# controllers/plants.py

from flask import Blueprint, request
from util.response import json_response, empty_response
from util.security import admin_required
from repositories.plants import PlantRepository

plants_bp = Blueprint('plants', __name__)

"""
	Initialise le contrôleur des plantes avec la connexion DB.
	Enregistre les routes publiques et admin pour la gestion des plantes.

	@param db_connection Connection Connexion psycopg2 à PostgreSQL
	@return Blueprint Blueprint Flask plants_bp avec les routes enregistrées
"""
def init_plants_controller(db_connection):
    repo = PlantRepository(db_connection)

    # Routes publiques
    """
    	Liste les plantes disponibles pour le catalogue public.

    	@return Response JSON array des plantes avec leurs attributs
    """
    @plants_bp.route('/plants', methods=['GET'])
    def list_plants():
        plants = repo.list()
        return json_response([p.__dict__ for p in plants])

    """
    	Retourne une plante par son identifiant.

    	@param plant_id int Identifiant unique de la plante
    	@return Response JSON de la plante (200) ou erreur (404)
    """
    @plants_bp.route('/plants/<int:plant_id>', methods=['GET'])
    def get_plant(plant_id):
        plant = repo.find(plant_id)
        if not plant:
            return json_response({"error": "Plante non trouvée"}, 404)
        return json_response(plant.__dict__)

    # Routes admin
    """
    	Liste de gestion des plantes pour l administration.
    	Retourne toutes les plantes, même sans stock.

    	@return Response JSON array des plantes (200)
    """
    @plants_bp.route('/admin/plants', methods=['GET'])
    @admin_required
    def admin_list_plants():
        # La version admin liste toutes les plantes, même sans stock
        all_plants = repo.list()
        return json_response([p.__dict__ for p in all_plants])

    """
    	Crée une nouvelle plante côté administration.

    	@return Response JSON de la plante créée (201)
    """
    @plants_bp.route('/admin/plants', methods=['POST'])
    @admin_required
    def create_plant():
        data = request.get_json() or {}
        new_plant = repo.create(data)
        return json_response(new_plant.__dict__, 201)

    """
    	Met à jour une plante existante (admin).

    	@param plant_id int Identifiant de la plante à modifier
    	@return Response JSON de la plante mise à jour (200)
    """
    @plants_bp.route('/admin/plants/<int:plant_id>', methods=['PATCH'])
    @admin_required
    def update_plant(plant_id):
        data = (request.get_json() or {})
        updated_plant = repo.update(plant_id, data)
        return json_response(updated_plant.__dict__)

    """
    	Supprime une plante (admin).

    	@param plant_id int Identifiant de la plante à supprimer
    	@return Response Réponse vide (200)
    """
    @plants_bp.route('/admin/plants/<int:plant_id>', methods=['DELETE'])
    @admin_required
    def delete_plant(plant_id):
        repo.delete(plant_id)
        return empty_response(200) # Le test attend 200

    return plants_bp
