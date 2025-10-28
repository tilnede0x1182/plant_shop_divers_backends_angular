import os
import sys
import time
import socket
import random
import string
import json
from datetime import datetime
import requests
from dotenv import load_dotenv

# -------- .env --------
# Charge les variables d'environnement depuis le fichier .env
load_dotenv()

# -------- Config --------
PORT = os.getenv("SERVER_ADDRESS", "4100")
BASE = f"http://localhost:{PORT}/api"
ADMIN_EMAIL = "admin1@planteshop.com"
ADMIN_PWD = "password"


# -------- Utilitaires --------
def ts():
    """Retourne un timestamp formaté comme en Java."""
    return datetime.now().strftime("%Y%m%d%H%M%S")

def rand(n):
    """Génère une chaîne alphanumérique aléatoire de longueur n."""
    alpha = string.ascii_lowercase + string.digits
    return ''.join(random.choice(alpha) for _ in range(n))

def wait_for_server(host, port, timeout_ms):
    """Attend que le serveur soit disponible sur un port donné."""
    start_time = time.time() * 1000
    while (time.time() * 1000) - start_time < timeout_ms:
        try:
            with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as s:
                s.settimeout(0.1)  # Timeout de connexion de 100ms
                s.connect((host, port))
            return True
        except (socket.timeout, ConnectionRefusedError):
            time.sleep(0.1)
    return False


class Test:
    def __init__(self):
        """Initialise le test avec un timestamp et des sessions HTTP."""
        self.timestamp = ts()
        # Utiliser des sessions requests gère automatiquement les cookies pour nous.
        # C'est l'équivalent du Map<String, String> cookie en plus robuste.
        self.sessions = {
            "admin": requests.Session(),
            "user": requests.Session()
        }
        # Définir le header par défaut pour toutes les requêtes de la session
        for session in self.sessions.values():
            session.headers.update({"Content-Type": "application/json"})

    def call(self, m, p, exp, body, who):
        """Effectue un appel API et attend une réponse de type objet JSON."""
        session = self.sessions.get(who)
        if not session:
            raise ValueError(f"Identifiant de session inconnu : '{who}'")

        try:
            response = session.request(m, BASE + p, json=body)
            code = response.status_code
        except requests.exceptions.RequestException as e:
            raise RuntimeError(f"Erreur de connexion à l'API: {e}")

        print(f"{'✅' if code == exp else '❌'} {m:<7s} {p} [{code}]")

        if code != exp:
            raise RuntimeError(f"API {m} {p} -> {code} (attendu {exp})\n{response.text}")

        content_type = response.headers.get("Content-Type", "")
        txt = response.text.strip()

        if "application/json" in content_type or txt.startswith("{"):
            return response.json() if txt else {}
        return {}

    def call_array(self, m, p, exp, body, who):
        """Effectue un appel API et attend une réponse de type tableau JSON."""
        session = self.sessions.get(who)
        if not session:
            raise ValueError(f"Identifiant de session inconnu : '{who}'")

        try:
            response = session.request(m, BASE + p, json=body)
            code = response.status_code
        except requests.exceptions.RequestException as e:
            raise RuntimeError(f"Erreur de connexion à l'API: {e}")

        print(f"{'✅' if code == exp else '❌'} {m:<7s} {p} [{code}]")

        if code != exp:
            raise RuntimeError(f"API {m} {p} -> {code} (attendu {exp})\n{response.text}")

        content_type = response.headers.get("Content-Type", "")
        txt = response.text.strip()

        if "application/json" in content_type and txt.startswith("["):
            return response.json() if txt else []
        return []

    # -------- Auth --------
    def login(self, mail, pw, who):
        j = {"email": mail, "password": pw}
        self.call("POST", "/auth/login", 201, j, who)

    def register(self, name, mail, pw, who):
        j = {"name": name, "email": mail, "password": pw}
        self.call("POST", "/auth/register", 201, j, who)

    # -------- Assertions --------
    @staticmethod
    def assert_eq(o, k, e):
        if k not in o:
            print(f"❌   ↳ Clé '{k}' manquante dans l'objet JSON")
            raise AssertionError(f"Objet vide – clé {k} recherchée")

        a = o[k]
        ok = (a == e)

        # Utiliser json.dumps pour un affichage correct des chaînes (avec guillemets)
        print(f"{'✅' if ok else '❌'}   ↳ {k}={json.dumps(a)}\n (attendu {json.dumps(e)})")

        if not ok:
            raise AssertionError(f"Assertion échouée pour la clé '{k}'")

    @staticmethod
    def assert_num(o, k):
        val = o.get(k)
        if not isinstance(val, (int, float)):
            raise AssertionError(f"Clé {k} n'est pas numérique ou absente")

    # -------- Modules de Test --------
    def test_plants(self):
        print("\n📌 TEST MODULE: PLANTS (admin)")
        plant_data = {"name": "Test Plant", "price": 10, "stock": 5}
        plant = self.call("POST", "/admin/plants", 201, plant_data, "admin")
        self.assert_num(plant, "id")
        plant_id = plant["id"]
        get = self.call("GET", f"/plants/{plant_id}", 200, None, "admin")
        self.assert_eq(get, "name", plant_data["name"])
        price_update = {"price": 15}
        self.call("PATCH", f"/admin/plants/{plant_id}", 200, price_update, "admin")
        check = self.call("GET", f"/plants/{plant_id}", 200, None, "admin")
        self.assert_eq(check, "price", 15)
        print(f"   ↳ name={check['name']}")
        self.call("DELETE", f"/admin/plants/{plant_id}", 200, None, "admin")

    def test_users(self):
        print("\n📌 TEST MODULE: USERS (admin)")
        email = f"utilisateur_test_{self.timestamp}@example.com"
        user_data = {"email": email, "name": "Utilisateur de test", "password": "pass123"}
        user = self.call("POST", "/users", 201, user_data, "admin")
        user_id = user["id"]
        name_update = {"name": "Tester Update"}
        self.call("PATCH", f"/users/{user_id}", 200, name_update, "admin")
        get = self.call("GET", f"/users/{user_id}", 200, None, "admin")
        self.assert_eq(get, "name", "Tester Update")
        self.call("DELETE", f"/users/{user_id}", 200, None, "admin")

    def test_orders(self):
        print("\n📌 TEST MODULE: ORDERS & ORDER ITEMS")
        plant_name = f"Plante_de_test_{self.timestamp}"
        plant_data = {"name": plant_name, "price": 10, "stock": 5}
        plant = self.call("POST", "/admin/plants", 201, plant_data, "admin")
        self.assert_num(plant, "id")
        pid = plant["id"]

        item = {"plantId": pid, "quantity": 2}
        order_data = {"items": [item]}
        order = self.call("POST", "/orders", 201, order_data, "user")
        self.assert_num(order, "id")
        oid = order["id"]

        status_update = {"status": "shipped"}
        self.call("PATCH", f"/orders/{oid}", 200, status_update, "admin")

        order_list = self.call_array("GET", "/orders", 200, None, "user")
        found = next((o for o in order_list if o["id"] == oid), None)
        if found is None:
            raise RuntimeError("Commande absente")

        self.assert_eq(found, "status", "shipped")
        if "orderItems" not in found or not found["orderItems"]:
            raise RuntimeError("Items absents dans la commande")

        nested_plant = found["orderItems"][0]["plant"]
        self.assert_eq(nested_plant, "name", plant_name)

        self.call("DELETE", f"/orders/{oid}", 200, None, "admin")
        self.call("DELETE", f"/admin/plants/{pid}", 200, None, "admin")

    def test_user_profile(self, email):
        print("\n📌 TEST MODULE: USER PROFILE (user)")
        users = self.call_array("GET", "/users", 200, None, "admin")
        user_obj = next((u for u in users if u["email"] == email), None)
        if user_obj is None:
            raise RuntimeError("Utilisateur de test non trouvé")
        uid = user_obj["id"]

        profile = self.call("GET", f"/users/{uid}", 200, None, "user")
        self.assert_eq(profile, "id", uid)

        new_name = f"Utilisateur_de_test_{self.timestamp}"
        name_update = {"name": new_name}
        self.call("PATCH", f"/users/{uid}", 200, name_update, "user")

        updated = self.call("GET", f"/users/{uid}", 200, None, "user")
        self.assert_eq(updated, "name", new_name)

        admin_update = {"admin": True}
        self.call("PATCH", f"/users/{uid}", 200, admin_update, "user")  # L'API doit ignorer ce champ

        check = self.call("GET", f"/users/{uid}", 200, None, "admin")
        self.assert_eq(check, "admin", False)  # Vérification que l'utilisateur n'est pas devenu admin

    def test_auth_roles(self):
        print("\n📌 TEST MODULE: ROLES")
        bad_plant = {"name": "Bad", "price": 1, "stock": 1}
        self.call("POST", "/admin/plants", 403, bad_plant, "user")

        good_plant = {"name": "Good", "price": 1, "stock": 1}
        plant = self.call("POST", "/admin/plants", 201, good_plant, "admin")
        pid = plant["id"]
        self.call("DELETE", f"/admin/plants/{pid}", 200, None, "admin")

        self.call("GET", "/users", 403, None, "user")

    def test_admin_plants(self):
        print("\n📌 TEST MODULE: ADMIN PLANTS")
        plantes = self.call_array("GET", "/admin/plants", 200, None, "admin")
        print(f"   ↳ {len(plantes)} plantes récupérées")

        plant_data = {"name": f"Plante_admin_{self.timestamp}", "price": 99, "stock": 12}
        p = self.call("POST", "/admin/plants", 201, plant_data, "admin")
        p_id = p["id"]

        price_update = {"price": 123}
        self.call("PATCH", f"/admin/plants/{p_id}", 200, price_update, "admin")
        self.call("DELETE", f"/admin/plants/{p_id}", 200, None, "admin")

    def test_admin_users(self):
        print("\n📌 TEST MODULE: ADMIN USERS")
        email = f"admin_temp_{self.timestamp}@example.com"
        name = f"Admin Temporaire {self.timestamp}"

        temp_admin_data = {"email": email, "name": name, "password": "password", "admin": True}
        temp = self.call("POST", "/users", 201, temp_admin_data, "admin")
        temp_id = temp["id"]

        user_list = self.call_array("GET", "/admin/users", 200, None, "admin")
        cible = next((u for u in user_list if u["email"] == email), None)
        if cible is None:
            raise RuntimeError("L'admin temporaire n'a pas été trouvé dans la liste !")
        self.assert_eq(cible, "name", name)

        nouveau_nom = f"Admin_temp_modifié_{self.timestamp}"
        name_update = {"name": nouveau_nom}
        self.call("PATCH", f"/users/{temp_id}", 200, name_update, "admin")

        user_get = self.call("GET", f"/users/{temp_id}", 200, None, "admin")
        self.assert_eq(user_get, "name", nouveau_nom)

        self.call("DELETE", f"/users/{temp_id}", 200, None, "admin")

    def test_auth_me(self):
        print("\n📌 TEST MODULE: AUTH /me")
        me = self.call("GET", "/auth/me", 200, None, "user")
        mail = me["email"]
        nom = me["name"]
        self.assert_eq(me, "email", mail)
        self.assert_eq(me, "name", nom)
        print(f"   ↳ Utilisateur connecté: {mail} ({nom})")

# -------- Main --------
if __name__ == "__main__":
    try:
        if not wait_for_server("127.0.0.1", int(PORT), 5000):
            print(f"❌ Serveur http://localhost:{PORT} injoignable", file=sys.stderr)
            sys.exit(2)

        t = Test()

        random_tag = rand(4)
        user_email = f"utilisateur_de_test_{t.timestamp}_{random_tag}@example.com"
        user_password = "pass123"

        print(f"🧪 Démarrage des tests: http://localhost:{PORT}/api\n")

        # Connexion des utilisateurs de base pour les tests
        t.login(ADMIN_EMAIL, ADMIN_PWD, "admin")
        t.register("User", user_email, user_password, "user")
        t.login(user_email, user_password, "user")

        # Exécution des suites de tests
        t.test_plants()
        t.test_users()
        t.test_orders()
        t.test_user_profile(user_email)
        t.test_auth_roles()
        t.test_admin_plants()
        t.test_admin_users()
        t.test_auth_me()

        print("\n🎉 Tous les tests ont réussi!")
        sys.exit(0)

    except Exception as e:
        print(f"\n❌ Tests interrompus: {e}", file=sys.stderr)
        # import traceback; traceback.print_exc() # Décommenter pour un débogage détaillé
        sys.exit(1)
