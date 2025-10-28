import os
import socket
from dotenv import load_dotenv
import sys

load_dotenv()
PORT = int(os.getenv("SERVER_ADDRESS", "4100"))

def on_starting(server):
    # Vérifie si le port est déjà pris avant le démarrage
    sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    in_use = sock.connect_ex(("0.0.0.0", PORT)) == 0
    sock.close()
    if in_use:
        print(f"❌ Port {PORT} déjà utilisé. Un autre serveur est peut-être en cours d'exécution.")
        sys.exit(0)
    print(f"🚀 Serveur démarré sur http://localhost:{PORT}")

def when_ready(server):
    # Désactive les logs Gunicorn
    server.log.access_log.setLevel("CRITICAL")
    server.log.error_log.setLevel("CRITICAL")
