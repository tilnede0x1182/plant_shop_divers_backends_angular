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

def json_response(data, status_code=200):
    """Crée une réponse Flask avec un corps JSON."""
    return Response(
        json.dumps(data, cls=CustomJSONEncoder),
        status=status_code,
        mimetype='application/json'
    )

def empty_response(status_code=204):
    """Crée une réponse Flask vide."""
    return Response(status=status_code)
