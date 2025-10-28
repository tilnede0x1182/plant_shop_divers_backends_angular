# controllers/plants.py

from flask import Blueprint, request
from util.response import json_response, empty_response
from util.security import admin_required
from repositories.plants import PlantRepository

plants_bp = Blueprint('plants', __name__)

def init_plants_controller(db_connection):
    repo = PlantRepository(db_connection)

    # Routes publiques
    @plants_bp.route('/', methods=['GET'])
    def list_plants():
        plants = repo.list()
        return json_response([p.__dict__ for p in plants])

    @plants_bp.route('/<int:plant_id>', methods=['GET'])
    def get_plant(plant_id):
        plant = repo.find(plant_id)
        if not plant:
            return json_response({"error": "Plante non trouvée"}, 404)
        return json_response(plant.__dict__)

    # Routes admin
    @plants_bp.route('/admin/plants', methods=['GET'])
    @admin_required
    def admin_list_plants():
        # La version admin liste toutes les plantes, même sans stock
        all_plants = repo.list()
        return json_response([p.__dict__ for p in all_plants])

    @plants_bp.route('/admin/plants', methods=['POST'])
    @admin_required
    def create_plant():
        data = request.get_json()
        new_plant = repo.create(data)
        return json_response(new_plant.__dict__, 201)

    @plants_bp.route('/admin/plants/<int:plant_id>', methods=['PATCH'])
    @admin_required
    def update_plant(plant_id):
        data = request.get_json()
        updated_plant = repo.update(plant_id, data)
        return json_response(updated_plant.__dict__)

    @plants_bp.route('/admin/plants/<int:plant_id>', methods=['DELETE'])
    @admin_required
    def delete_plant(plant_id):
        repo.delete(plant_id)
        return empty_response(200) # Le test attend 200

    return plants_bp
