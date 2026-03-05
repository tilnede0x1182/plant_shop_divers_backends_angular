# util/response.py

import json
from decimal import Decimal
from datetime import datetime
from flask import Response

class CustomJSONEncoder(json.JSONEncoder):
    """Encodeur JSON personnalisé pour gérer les types Decimal et datetime."""
    def default(self, obj):
        if isinstance(obj, Decimal):
            return float(obj)
        if isinstance(obj, datetime):
            return obj.isoformat() + 'Z' # Format ISO 8601 avec Z pour UTC
        return super().default(obj)

"""
	Crée une réponse Flask avec un corps JSON.

	@param data dict|list Données à sérialiser en JSON
	@param status_code int Code HTTP de la réponse (défaut: 200)
	@return Response Objet Response Flask avec Content-Type application/json
"""
def json_response(data, status_code=200):
    return Response(
        json.dumps(data, cls=CustomJSONEncoder),
        status=status_code,
        mimetype='application/json'
    )

"""
	Crée une réponse Flask vide (sans corps).

	@param status_code int Code HTTP de la réponse (défaut: 204 No Content)
	@return Response Objet Response Flask sans contenu
"""
def empty_response(status_code=204):
    return Response(status=status_code)
