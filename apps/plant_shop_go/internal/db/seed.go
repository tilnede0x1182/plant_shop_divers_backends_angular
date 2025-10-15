// File: cmd/seed/main.go
package db

import (
	"fmt"
	"log"
	"math/rand"
	"os"
	"path/filepath"
	"time"

	// Modifiez ces chemins pour correspondre à votre projet
	"plant_shop_go/internal/db"
	"plant_shop_go/internal/models"

	"github.com/bxcodec/faker/v3"
	"golang.org/x/crypto/bcrypt"
	"gorm.io/gorm"
)

// # Données
const (
	NB_ADMINS           = 3
	NB_USERS            = 20
	NB_PLANTS           = 50
	MAX_ORDERS_PER_USER = 7
)

var PLANT_NAMES = []string{
	"Rose", "Tulipe", "Lavande", "Orchidée", "Basilic", "Menthe",
	"Pivoine", "Tournesol", "Cactus (Echinopsis)", "Bambou",
	"Camomille (Matricaria recutita)", "Sauge (Salvia officinalis)",
	"Romarin (Rosmarinus officinalis)", "Thym (Thymus vulgaris)",
	"Laurier-rose (Nerium oleander)", "Aloe vera", "Jasmin (Jasminum officinale)",
	"Hortensia (Hydrangea macrophylla)", "Marguerite (Leucanthemum vulgare)",
	"Géranium (Pelargonium graveolens)", "Fuchsia (Fuchsia magellanica)",
	"Anémone (Anemone coronaria)", "Azalée (Rhododendron simsii)",
	"Chrysanthème (Chrysanthemum morifolium)", "Digitale pourpre (Digitalis purpurea)",
	"Glaïeul (Gladiolus hortulanus)", "Lys (Lilium candidum)", "Violette (Viola odorata)",
	"Muguet (Convallaria majalis)", "Iris (Iris germanica)", "Lavandin (Lavandula intermedia)",
	"Érable du Japon (Acer palmatum)", "Citronnelle (Cymbopogon citratus)",
	"Pin parasol (Pinus pinea)", "Cyprès (Cupressus sempervirens)", "Olivier (Olea europaea)",
	"Papyrus (Cyperus papyrus)", "Figuier (Ficus carica)", "Eucalyptus (Eucalyptus globulus)",
	"Acacia (Acacia dealbata)", "Bégonia (Begonia semperflorens)", "Calathea (Calathea ornata)",
	"Dieffenbachia (Dieffenbachia seguine)", "Ficus elastica", "Sansevieria (Sansevieria trifasciata)",
	"Philodendron (Philodendron scandens)", "Yucca (Yucca elephantipes)", "Zamioculcas zamiifolia",
	"Monstera deliciosa", "Pothos (Epipremnum aureum)", "Agave (Agave americana)",
	"Cactus raquette (Opuntia ficus-indica)", "Palmier-dattier (Phoenix dactylifera)",
	"Amaryllis (Hippeastrum hybridum)", "Bleuet (Centaurea cyanus)",
	"Cœur-de-Marie (Lamprocapnos spectabilis)", "Croton (Codiaeum variegatum)",
	"Dracaena (Dracaena marginata)", "Hosta (Hosta plantaginea)", "Lierre (Hedera helix)",
	"Mimosa (Acacia dealbata)",
}

// ## Réinitialisation
func reset(db *gorm.DB) {
	log.Println("Réinitialisation de la base de données...")
	// L'ordre est important pour respecter les contraintes de clés étrangères
	db.Exec("DELETE FROM order_items")
	db.Exec("DELETE FROM orders")
	db.Exec("DELETE FROM plants")
	db.Exec("DELETE FROM users")
}

// ## Admins
func createAdmins(db *gorm.DB) []map[string]string {
	log.Println("Création des administrateurs...")
	var admins []map[string]string
	for i := 0; i < NB_ADMINS; i++ {
		email := fmt.Sprintf("admin%d@planteshop.com", i+1)
		password := "password"
		hash, _ := bcrypt.GenerateFromPassword([]byte(password), 10)
		db.Create(&models.User{
			Email:    email,
			Password: string(hash),
			Admin:    true,
			Name:     faker.Name(),
		})
		admins = append(admins, map[string]string{"email": email, "password": password})
	}
	return admins
}

// ## Users
func createUsers(db *gorm.DB) []map[string]string {
	log.Println("Création des utilisateurs...")
	var users []map[string]string
	for i := 0; i < NB_USERS; i++ {
		password := faker.Password()
		email := faker.Email()
		hash, _ := bcrypt.GenerateFromPassword([]byte(password), 10)
		db.Create(&models.User{
			Email:    email,
			Password: string(hash),
			Admin:    false,
			Name:     faker.Name(),
		})
		users = append(users, map[string]string{"email": email, "password": password})
	}
	return users
}

// ## Plants
func createPlants(db *gorm.DB) []models.Plant {
	log.Println("Création des plantes...")
	var plants []models.Plant
	max := len(PLANT_NAMES)
	for i := 0; i < NB_PLANTS; i++ {
		base := PLANT_NAMES[i%max]
		name := base
		if NB_PLANTS > max {
			name = fmt.Sprintf("%s %d", base, (i/max)+1)
		}

		// Traduction fidèle : prix en entier (centimes) entre 5.00€ et 50.00€
		priceInCents := (rand.Intn(46) + 5) * 100 // Génère un prix entre 500 et 5000
		desc := faker.Sentence()
		stock := rand.Intn(26) + 5 // 5 à 30

		p := models.Plant{
			Name:        name,
			Price:       priceInCents,
			Description: desc,
			Stock:       stock,
		}
		db.Create(&p)
		plants = append(plants, p)
	}
	return plants
}

// ## Orders
func createOrders(db *gorm.DB, plants []models.Plant) {
	log.Println("Création des commandes...")
	var users []models.User
	db.Find(&users)

	for _, u := range users {
		numberOfOrders := rand.Intn(MAX_ORDERS_PER_USER + 1)
		for i := 0; i < numberOfOrders; i++ {
			createOrderForUser(db, u, plants)
		}
	}
}

func createOrderForUser(db *gorm.DB, user models.User, plants []models.Plant) {
	statuses := []string{"confirmed", "pending", "shipped", "delivered"}
	totalCents := 0

	// 1. Créer la commande avec un total de 0
	order := models.Order{
		UserID:     user.ID,
		TotalPrice: 0,
		Status:     statuses[rand.Intn(len(statuses))],
	}
	db.Create(&order)

	// 2. Ajouter des articles et calculer le total
	// La version NestJS ajoute toujours 2 types d'articles
	for i := 0; i < 2; i++ {
		itemPrice := addItem(db, order.ID, plants)
		totalCents += itemPrice
	}

	// 3. Mettre à jour la commande avec le total final
	if totalCents > 0 {
		db.Model(&order).Update("total_price", totalCents)
	}
}

func addItem(db *gorm.DB, orderID uint, plants []models.Plant) int {
	// Choisir une plante au hasard
	plantIndex := rand.Intn(len(plants))
	p := &plants[plantIndex] // Utiliser un pointeur pour modifier le stock en mémoire

	if p.Stock <= 0 {
		return 0
	}

	qty := rand.Intn(5) + 1 // Quantité entre 1 et 5
	if qty > p.Stock {
		qty = p.Stock
	}

	// Créer l'article de commande
	db.Create(&models.OrderItem{
		OrderID:   orderID,
		PlantID:   p.ID,
		Quantity:  qty,
		UnitPrice: p.Price, // Sauvegarde du prix en centimes au moment de l'achat
	})

	// Mettre à jour le stock en BDD
	db.Model(&models.Plant{}).Where("id = ?", p.ID).Update("stock", gorm.Expr("stock - ?", qty))

	// Mettre à jour le stock en mémoire pour le prochain tour de boucle
	p.Stock -= qty

	return p.Price * qty // Retourner le prix total de l'article en centimes
}

// ## users.txt
func writeUsersFile(admins, users []map[string]string) {
	log.Println("Génération du fichier users.txt...")
	path := filepath.Join(".", "users.txt")
	var txt = "Administrateurs :\n\n"
	for _, a := range admins {
		txt += fmt.Sprintf("%s %s\n", a["email"], a["password"])
	}
	txt += "\nUtilisateurs :\n\n"
	for _, u := range users {
		txt += fmt.Sprintf("%s %s\n", u["email"], u["password"])
	}
	if err := os.WriteFile(path, []byte(txt), 0644); err != nil {
		log.Fatalf("Échec de l'écriture du fichier users.txt : %v", err)
	}
}

// # Main pour exécution directe
func main() {
	log.Println("🚀 Lancement du seed…")
	// Initialise le générateur de nombres aléatoires
	rand.Seed(time.Now().UnixNano())

	// Établit la connexion à la base de données
	conn := db.Connect()
	if conn == nil {
		log.Fatal("La connexion à la base de données a échoué")
	}

	// Exécute les étapes du seed dans l'ordre
	reset(conn)
	admins := createAdmins(conn)
	users := createUsers(conn)
	plants := createPlants(conn)
	writeUsersFile(admins, users)
	createOrders(conn, plants)

	log.Println("✅ Seed terminée. Données créées & users.txt généré.")
}
