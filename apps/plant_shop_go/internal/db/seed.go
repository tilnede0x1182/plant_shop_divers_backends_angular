package db

import (
	"fmt"
	"log"
	"math"
	"math/rand"
	"os"
	"path/filepath"
	"time"

	"plant_shop_go/internal/models"

	"gorm.io/gorm"

	"github.com/bxcodec/faker/v3"
	"golang.org/x/crypto/bcrypt"
)

// ==============================================================================
// Donnees
// ==============================================================================

const (
	NB_ADMINS           = 3
	NB_USERS            = 20
	NB_PLANTS           = 50
	MAX_ORDERS_PER_USER = 7
)

// hashPass genere un hash bcrypt.
//
// @param password string Mot de passe en clair
// @return string Hash bcrypt
func hashPass(password string) string {
	hash, _ := bcrypt.GenerateFromPassword([]byte(password), 10)
	return string(hash)
}

// randomPrice genere un prix aleatoire entre 5 et 50.
//
// @return float64 Prix arrondi a 2 decimales
func randomPrice() float64 {
	return math.Round((rand.Float64()*45.0+5.0)*100) / 100
}

// randomStock genere un stock aleatoire entre 5 et 30.
//
// @return int Stock
func randomStock() int {
	return rand.Intn(26) + 5
}

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

// reset reinitialise la base de donnees en supprimant toutes les donnees.
//
// @param db *gorm.DB Client GORM de base de donnees
func reset(db *gorm.DB) {
	log.Println("🧹 Nettoyage de la base de données…")
	db.Exec("DELETE FROM order_items")
	db.Exec("DELETE FROM orders")
	db.Exec("DELETE FROM plants")
	db.Exec("DELETE FROM users")
	log.Println("✅ Base de données nettoyée.")
}

// createOneAdmin cree un admin et retourne ses credentials.
//
// @param db *gorm.DB Client GORM
// @param index int Index de l admin
// @return map[string]string Credentials
func createOneAdmin(db *gorm.DB, index int) map[string]string {
	email := fmt.Sprintf("admin%d@planteshop.com", index+1)
	password := "password"
	db.Create(&models.User{Email: email, Password: hashPass(password), Admin: true, Name: faker.Name()})
	return map[string]string{"email": email, "password": password}
}

// createAdmins cree les administrateurs de test.
//
// @param db *gorm.DB Client GORM
// @return []map[string]string Liste des credentials
func createAdmins(db *gorm.DB) []map[string]string {
	log.Println("Creation des administrateurs...")
	var admins []map[string]string
	for idx := 0; idx < NB_ADMINS; idx++ {
		admins = append(admins, createOneAdmin(db, idx))
	}
	log.Printf("%d administrateurs crees.\n", NB_ADMINS)
	return admins
}

// createOneUser cree un user et retourne ses credentials.
//
// @param db *gorm.DB Client GORM
// @return map[string]string Credentials
func createOneUser(db *gorm.DB) map[string]string {
	password := faker.Password()
	email := faker.Email()
	db.Create(&models.User{Email: email, Password: hashPass(password), Admin: false, Name: faker.Name()})
	return map[string]string{"email": email, "password": password}
}

// createUsers cree les utilisateurs de test.
//
// @param db *gorm.DB Client GORM
// @return []map[string]string Liste des credentials
func createUsers(db *gorm.DB) []map[string]string {
	log.Println("Creation des utilisateurs...")
	var users []map[string]string
	for idx := 0; idx < NB_USERS; idx++ {
		users = append(users, createOneUser(db))
	}
	log.Printf("%d utilisateurs crees.\n", NB_USERS)
	return users
}

// createOnePlant cree une plante.
//
// @param db *gorm.DB Client GORM
// @param index int Index de la plante
// @return models.Plant Plante creee
func createOnePlant(db *gorm.DB, index int) models.Plant {
	name := PLANT_NAMES[index%len(PLANT_NAMES)]
	plant := models.Plant{Name: name, Price: randomPrice(), Description: faker.Sentence(), Stock: randomStock()}
	db.Create(&plant)
	return plant
}

// createPlants cree les plantes de test.
//
// @param db *gorm.DB Client GORM
// @return []models.Plant Liste des plantes
func createPlants(db *gorm.DB) []models.Plant {
	log.Println("Creation des plantes...")
	var plants []models.Plant
	for idx := 0; idx < NB_PLANTS; idx++ {
		plants = append(plants, createOnePlant(db, idx))
	}
	log.Printf("%d plantes creees.\n", NB_PLANTS)
	return plants
}

// createOrderForUser cree une commande pour un utilisateur.
//
// @param db *gorm.DB Client GORM
// @param userID uint ID utilisateur
// @param plants []models.Plant Liste des plantes
func createOrderForUser(db *gorm.DB, userID uint, plants []models.Plant) {
	statuses := []string{"confirmed", "pending", "shipped", "delivered"}
	order := models.Order{UserID: userID, TotalPrice: 0.0, Status: statuses[rand.Intn(len(statuses))]}
	db.Create(&order)
	total := addItem(db, order.ID, plants) + addItem(db, order.ID, plants)
	db.Model(&order).Update("total_price", math.Trunc(total*100)/100)
}

// createOrders cree les commandes de test.
//
// @param db *gorm.DB Client GORM
// @param plants []models.Plant Liste des plantes
func createOrders(db *gorm.DB, plants []models.Plant) {
	log.Println("Creation des commandes...")
	var users []models.User
	db.Find(&users)
	totalOrders := 0
	for _, user := range users {
		nbOrders := rand.Intn(MAX_ORDERS_PER_USER + 1)
		for idx := 0; idx < nbOrders; idx++ {
			createOrderForUser(db, user.ID, plants)
			totalOrders++
		}
	}
	log.Printf("%d commandes creees.\n", totalOrders)
}

// addItem ajoute un item a une commande et retourne le prix.
//
// @param db *gorm.DB Client GORM de base de donnees
// @param orderID uint ID de la commande
// @param plants []models.Plant Liste des plantes disponibles
// @return float64 Prix de l item ajoute
func addItem(db *gorm.DB, orderID uint, plants []models.Plant) float64 {
	p := plants[rand.Intn(len(plants))]
	if p.Stock <= 0 {
		return 0
	}
	qty := rand.Intn(5) + 1
	if qty > p.Stock {
		qty = p.Stock
	}
	db.Create(&models.OrderItem{OrderID: orderID, PlantID: p.ID, Quantity: qty})
	db.Model(&p).Update("stock", p.Stock-qty)
	return float64(qty) * p.Price
}

// buildUsersFileContent construit le contenu du fichier users.txt.
//
// @param admins []map[string]string Credentials admin
// @param users []map[string]string Credentials utilisateur
// @return string Contenu du fichier
func buildUsersFileContent(admins, users []map[string]string) string {
	txt := "Administrateurs :\n\n"
	for _, admin := range admins {
		txt += fmt.Sprintf("%s %s\n", admin["email"], admin["password"])
	}
	txt += "\nUtilisateurs :\n\n"
	for _, user := range users {
		txt += fmt.Sprintf("%s %s\n", user["email"], user["password"])
	}
	return txt
}

// writeUsersFile genere le fichier users.txt.
//
// @param admins []map[string]string Credentials admin
// @param users []map[string]string Credentials utilisateur
func writeUsersFile(admins, users []map[string]string) {
	log.Println("Generation du fichier users.txt...")
	path := filepath.Join(".", "users.txt")
	txt := buildUsersFileContent(admins, users)
	if err := os.WriteFile(path, []byte(txt), 0644); err != nil {
		log.Fatalf("Erreur ecriture users.txt : %v", err)
	}
	log.Println("Fichier users.txt genere.")
}

// Seed execute le seed complet de la base de donnees.
//
// @param db *gorm.DB Client GORM de base de donnees
func Seed(db *gorm.DB) {
	log.Println("🚀 Lancement de la seed…")
	rand.Seed(time.Now().UnixNano())
	reset(db)
	admins := createAdmins(db)
	users := createUsers(db)
	plants := createPlants(db)
	writeUsersFile(admins, users)
	createOrders(db, plants)
	log.Println("🎉 Seed terminée avec succès !")
}
