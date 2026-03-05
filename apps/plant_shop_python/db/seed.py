import os
import random
import sys
from decimal import Decimal
import psycopg2
import bcrypt
from dotenv import load_dotenv

# ---------- Lecture .env ----------
# Charge les variables d'environnement depuis le fichier .env
load_dotenv()

# ---------- Constantes ----------
NB_ADMINS = 3
NB_USERS = 20
NB_PLANTS = 50
MAX_ORDERS_PER_USER = 7

PLANT_NAMES = [
	"Rose", "Tulipe", "Lavande", "Orchidée", "Basilic", "Menthe", "Pivoine", "Tournesol",
	"Cactus (Echinopsis)", "Bambou", "Camomille (Matricaria recutita)", "Sauge (Salvia officinalis)",
	"Romarin (Rosmarinus officinalis)", "Thym (Thymus vulgaris)", "Laurier-rose (Nerium oleander)",
	"Aloe vera", "Jasmin (Jasminum officinale)", "Hortensia (Hydrangea macrophylla)",
	"Marguerite (Leucanthemum vulgare)", "Géranium (Pelargonium graveolens)",
	"Fuchsia (Fuchsia magellanica)", "Anémone (Anemone coronaria)", "Azalée (Rhododendron simsii)",
	"Chrysanthème (Chrysanthemum morifolium)", "Digitale pourpre (Digitalis purpurea)",
	"Glaïeul (Gladiolus hortulanus)", "Lys (Lilium candidum)", "Violette (Viola odorata)",
	"Muguet (Convallaria majalis)", "Iris (Iris germanica)", "Lavandin (Lavandula intermedia)",
	"Érable du Japon (Acer palmatum)", "Citronnelle (Cymbopogon citratus)", "Pin parasol (Pinus pinea)",
	"Cyprès (Cupressus sempervirens)", "Olivier (Olea europaea)", "Papyrus (Cyperus papyrus)",
	"Figuier (Ficus carica)", "Eucalyptus (Eucalyptus globulus)", "Acacia (Acacia dealbata)",
	"Bégonia (Begonia semperflorens)", "Calathea (Calathea ornata)", "Dieffenbachia (Dieffenbachia seguine)",
	"Ficus elastica", "Sansevieria (Sansevieria trifasciata)", "Philodendron (Philodendron scandens)",
	"Yucca (Yucca elephantipes)", "Zamioculcas zamiifolia", "Monstera deliciosa",
	"Pothos (Epipremnum aureum)", "Agave (Agave americana)", "Cactus raquette (Opuntia ficus-indica)"
]

# Assurons-nous d'avoir assez de noms de plantes uniques
if NB_PLANTS > len(PLANT_NAMES):
	raise ValueError(f"NB_PLANTS ({NB_PLANTS}) ne peut pas être supérieur au nombre de noms de plantes uniques disponibles ({len(PLANT_NAMES)}).")

FIRST = [
	"Alice", "Bruno", "Cathy", "David", "Emma", "Franck",
	"Gwen", "Hugo", "Inès", "Jules", "Katia", "Léo", "Marie", "Nicolas", "Olivia", "Paul"
]
LAST = [
	"Dupont", "Martin", "Bernard", "Petit", "Robert", "Richard", "Durand", "Moreau", "Roux", "Fournier"
]
EMAIL_DOMAINS = ["gmail.com", "yahoo.com", "hotmail.com"]


# ---------- Helpers ----------
"""
	Génère un entier aléatoire dans l intervalle [min_val, max_val].

	@param min_val int Borne inférieure inclusive
	@param max_val int Borne supérieure inclusive
	@return int Entier aléatoire
"""
def rnd(min_val, max_val):
	return random.randint(min_val, max_val)

"""
	Choisit un élément au hasard dans une liste.

	@param arr list Liste source
	@return any Élément choisi aléatoirement
"""
def pick(arr):
	return random.choice(arr)

"""
	Génère un mot de passe aléatoire simple.
	Format: pw suivi de 9 chiffres.

	@return str Mot de passe généré
"""
def rand_pwd():
	return f"pw{rnd(100000000, 999999999)}"

"""
	Hache un mot de passe en utilisant bcrypt.

	@param p str Mot de passe en clair
	@return str Hash bcrypt du mot de passe
"""
def hash_password(p):
	password_bytes = p.encode('utf-8')
	salt = bcrypt.gensalt()
	return bcrypt.hashpw(password_bytes, salt).decode('utf-8')

"""
	Génère une phrase de type lorem ipsum.
	Composée de 10 à 14 mots aléatoires.

	@return str Phrase générée avec majuscule et point
"""
def lorem_sentence():
	words = ["lorem", "ipsum", "dolor", "sit", "amet", "consectetur", "adipiscing", "elit",
			 "sed", "do", "eiusmod", "tempor", "incididunt", "ut", "labore", "et", "dolore", "magna", "aliqua"]
	n = rnd(10, 14)
	sentence_words = [pick(words) for _ in range(n)]
	sentence = " ".join(sentence_words)
	return sentence.capitalize() + '.'

# ---------- Main ----------
"""
	Fonction principale du script de seeding.
	Crée admins, utilisateurs, plantes et commandes de test.
	Génère un fichier users.txt avec les identifiants.
"""
def main():
	db = None
	try:
		# Connexion à la base de données
		db_url = os.getenv("DATABASE_URL")
		if not db_url or not db_url.startswith("postgresql://"):
			raise ValueError("DATABASE_URL mal formatée ou manquante dans .env")

		db = psycopg2.connect(dsn=db_url)
		cursor = db.cursor()

		# Nettoyage
		print("🧹 Nettoyage de la base de données…")
		cursor.execute("TRUNCATE order_items, orders, plants, users RESTART IDENTITY CASCADE")
		print("✅ Base vidée.")

		# ---------- Users ----------
		admin_ids = []
		user_ids = []
		creds_out = ["Administrateurs:\n"]

		# Pour garantir des emails uniques
		generated_emails = set()

		# Admins
		print("👑 Création des administrateurs…")
		for i in range(NB_ADMINS):
			name = f"{pick(FIRST)} {pick(LAST)}"
			email = f"admin{i+1}@planteshop.com"
			pwd = "password"

			cursor.execute(
				"INSERT INTO users(name, email, password_hash, is_admin) VALUES (%s, %s, %s, %s) RETURNING id",
				(name, email, hash_password(pwd), True)
			)
			admin_id = cursor.fetchone()[0]
			admin_ids.append(admin_id)
			creds_out.append(f"{email} {pwd}")
		print(f"✅ {len(admin_ids)} admins.")

		creds_out.extend(["", "Utilisateurs:\n"])

		# Users
		print("👥 Création des utilisateurs…")
		for _ in range(NB_USERS):
			while True: # Boucle pour garantir un email unique
				first = pick(FIRST)
				last = pick(LAST)
				email = f"{first.lower()}.{last.lower()}{rnd(1,99)}@{pick(EMAIL_DOMAINS)}"
				if email not in generated_emails:
					generated_emails.add(email)
					break

			pwd = rand_pwd()
			name = f"{first} {last}"

			cursor.execute(
				"INSERT INTO users(name, email, password_hash, is_admin) VALUES (%s, %s, %s, %s) RETURNING id",
				(name, email, hash_password(pwd), False)
			)
			user_id = cursor.fetchone()[0]
			user_ids.append(user_id)
			creds_out.append(f"{email} {pwd}")
		print(f"✅ {len(user_ids)} utilisateurs.")

		# ---------- Plants ----------
		print("🌱 Création des plantes…")
		plants = []

		# Mélanger et sélectionner les noms de plantes pour garantir l'unicité sans numéros
		selected_plant_names = random.sample(PLANT_NAMES, NB_PLANTS)

		for name in selected_plant_names:
			price = rnd(5, 50)
			stock = rnd(5, 30)

			cursor.execute(
				"INSERT INTO plants(name, description, price, stock) VALUES (%s, %s, %s, %s) RETURNING id",
				(name, lorem_sentence(), Decimal(price), stock)
			)
			plant_id = cursor.fetchone()[0]
			plants.append({'id': plant_id, 'price': price, 'stock': stock})
		print(f"✅ {len(plants)} plantes.")

		# ---------- Orders & items ----------
		status_arr = ["confirmed", "pending", "shipped", "delivered"]
		total_orders = 0

		print("🛒 Création des commandes…")
		for user_id in user_ids:
			nb_orders = rnd(0, MAX_ORDERS_PER_USER)
			for _ in range(nb_orders):
				cursor.execute(
					"INSERT INTO orders(user_id, total, status) VALUES (%s, %s, %s) RETURNING id",
					(user_id, Decimal(0), pick(status_arr))
				)
				order_id = cursor.fetchone()[0]

				total = Decimal(0)
				# Crée 1 ou 2 items par commande
				for _ in range(rnd(1, 2)):
					# Sélectionne une plante avec du stock disponible
					available_plants = [p for p in plants if p['stock'] > 0]
					if not available_plants:
						break

					plant_info = pick(available_plants)
					qty = min(rnd(1, 5), plant_info['stock'])

					cursor.execute(
						"INSERT INTO order_items(order_id, plant_id, quantity, price) VALUES (%s, %s, %s, %s)",
						(order_id, plant_info['id'], qty, Decimal(plant_info['price']))
					)

					# Décrémente le stock en mémoire et dans la DB
					plant_info['stock'] -= qty
					cursor.execute(
						"UPDATE plants SET stock = stock - %s WHERE id = %s",
						(qty, plant_info['id'])
					)

					total += Decimal(plant_info['price'] * qty)

				# Met à jour le total de la commande
				cursor.execute(
					"UPDATE orders SET total = %s WHERE id = %s",
					(total, order_id)
				)
				total_orders += 1
		print(f"✅ {total_orders} commandes.")

		# ---------- users.txt ----------
		with open("users.txt", "w", encoding="utf-8") as f:
			for line in creds_out:
				f.write(line + "\n")
		print(f"✍️ Fichier users.txt généré ({len(creds_out)} lignes).")

		db.commit()
		print("🎉 Seed terminée !")

	except (Exception, psycopg2.DatabaseError) as error:
		print(f"Erreur: {error}", file=sys.stderr)
		if db:
			db.rollback()
	finally:
		if db:
			cursor.close()
			db.close()

if __name__ == "__main__":
	main()
