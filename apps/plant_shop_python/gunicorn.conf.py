# import os
# import socket
# from dotenv import load_dotenv
# import sys

# load_dotenv()
# PORT = int(os.getenv("SERVER_ADDRESS", "4100"))

# def on_starting(server):
# 	# Vérifie si le port est déjà pris avant le démarrage
# 	sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
# 	in_use = sock.connect_ex(("0.0.0.0", PORT)) == 0
# 	sock.close()
# 	if in_use:
# 		print(f"❌ Port {PORT} déjà utilisé. Un autre serveur est peut-être en cours d'exécution.")
# 		sys.exit(0)
# 	print(f"🚀 Serveur démarré sur http://localhost:{PORT}")

# def when_ready(server):
# 	# Désactive les logs Gunicorn
# 	# server.log.access_log.setLevel("CRITICAL")
# 	# server.log.error_log.setLevel("CRITICAL")
# 	server.log.access_log.setLevel("INFO")
# 	server.log.error_log.setLevel("INFO")



import os
import socket
import sys
from dotenv import load_dotenv

load_dotenv()
PORT = int(os.getenv("SERVER_ADDRESS", "4100"))

"""
	Hook Gunicorn appelé avant le démarrage du serveur.
	Vérifie si le port est déjà utilisé et quitte proprement si c est le cas.

	@param server object Instance du serveur Gunicorn
"""
def on_starting(server):
    sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    in_use = sock.connect_ex(("0.0.0.0", PORT)) == 0
    sock.close()
    if in_use:
        print(f"❌ Port {PORT} déjà utilisé. Serveur arrêté calmement.")
        sys.exit(0)
    print(f"🚀 Serveur démarré sur http://localhost:{PORT}")

"""
	Hook Gunicorn appelé quand le serveur est prêt.
	Configure les niveaux de log pour accès et erreurs.

	@param server object Instance du serveur Gunicorn
"""
def when_ready(server):
    # Active explicitement le log d'accès
    server.log.access_log.setLevel("INFO")
    server.log.error_log.setLevel("INFO")

"""
	Hook Gunicorn appelé à l arrêt du serveur.
	Affiche un message de confirmation d arrêt.

	@param server object Instance du serveur Gunicorn
"""
def on_exit(server):
    print("🔌 Serveur arrêté calmement.")

# Ajout explicite de la sortie vers stdout pour tous les logs
accesslog = '-'
errorlog = '-'
loglevel = 'info'
