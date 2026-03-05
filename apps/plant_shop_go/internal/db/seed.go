package db

// ==============================================================================
// Importations
// ==============================================================================

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

// ==============================================================================
// Fonctions utilitaires
// ==============================================================================

// hashPassword genere un hash bcrypt.
//
// @param clearPassword string Mot de passe en clair
// @return string Hash bcrypt
func hashPassword(clearPassword string) string {
	hashedPassword, _ := bcrypt.GenerateFromPassword([]byte(clearPassword), 10)
	return string(hashedPassword)
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

// ------------------------------------------------------------------------------
// Fonctions de nettoyage
// ------------------------------------------------------------------------------

// resetDatabase reinitialise la base de donnees en supprimant toutes les donnees.
//
// @param dbConnection *gorm.DB Client GORM de base de donnees
func resetDatabase(dbConnection *gorm.DB) {
	log.Println("Nettoyage de la base de donnees...")
	dbConnection.Exec("DELETE FROM order_items")
	dbConnection.Exec("DELETE FROM orders")
	dbConnection.Exec("DELETE FROM plants")
	dbConnection.Exec("DELETE FROM users")
	log.Println("Base de donnees nettoyee.")
}

// createOneAdmin cree un admin et retourne ses credentials.
//
// @param dbConnection *gorm.DB Client GORM
// @param adminIndex int Index de l admin
// @return map[string]string Credentials
func createOneAdmin(dbConnection *gorm.DB, adminIndex int) map[string]string {
	adminEmail := fmt.Sprintf("admin%d@planteshop.com", adminIndex+1)
	adminPassword := "password"
	dbConnection.Create(&models.User{Email: adminEmail, Password: hashPassword(adminPassword), Admin: true, Name: faker.Name()})
	return map[string]string{"email": adminEmail, "password": adminPassword}
}

// createAdmins cree les administrateurs de test.
//
// @param dbConnection *gorm.DB Client GORM
// @return []map[string]string Liste des credentials
func createAdmins(dbConnection *gorm.DB) []map[string]string {
	log.Println("Creation des administrateurs...")
	var adminCredentials []map[string]string
	for adminIndex := 0; adminIndex < NB_ADMINS; adminIndex++ {
		adminCredentials = append(adminCredentials, createOneAdmin(dbConnection, adminIndex))
	}
	log.Printf("%d administrateurs crees.\n", NB_ADMINS)
	return adminCredentials
}

// createOneUser cree un user et retourne ses credentials.
//
// @param db *gorm.DB Client GORM
// @return map[string]string Credentials
func createOneUser(dbConnection *gorm.DB) map[string]string {
	userPassword := faker.Password()
	userEmail := faker.Email()
	dbConnection.Create(&models.User{Email: userEmail, Password: hashPassword(userPassword), Admin: false, Name: faker.Name()})
	return map[string]string{"email": userEmail, "password": userPassword}
}

// createUsers cree les utilisateurs de test.
//
// @param dbConnection *gorm.DB Client GORM
// @return []map[string]string Liste des credentials
func createUsers(dbConnection *gorm.DB) []map[string]string {
	log.Println("Creation des utilisateurs...")
	var userCredentials []map[string]string
	for userIndex := 0; userIndex < NB_USERS; userIndex++ {
		userCredentials = append(userCredentials, createOneUser(dbConnection))
	}
	log.Printf("%d utilisateurs crees.\n", NB_USERS)
	return userCredentials
}

// createOnePlant cree une plante.
//
// ------------------------------------------------------------------------------
// Fonctions de creation plantes
// ------------------------------------------------------------------------------

// @param dbConnection *gorm.DB Client GORM
// @param plantIndex int Index de la plante
// @return models.Plant Plante creee
func createOnePlant(dbConnection *gorm.DB, plantIndex int) models.Plant {
	plantName := PLANT_NAMES[plantIndex%len(PLANT_NAMES)]
	newPlant := models.Plant{Name: plantName, Price: randomPrice(), Description: faker.Sentence(), Stock: randomStock()}
	dbConnection.Create(&newPlant)
	return newPlant
}

// createPlants cree les plantes de test.
//
// @param dbConnection *gorm.DB Client GORM
// @return []models.Plant Liste des plantes
func createPlants(dbConnection *gorm.DB) []models.Plant {
	log.Println("Creation des plantes...")
	var plantList []models.Plant
	for plantIndex := 0; plantIndex < NB_PLANTS; plantIndex++ {
		plantList = append(plantList, createOnePlant(dbConnection, plantIndex))
	}
	log.Printf("%d plantes creees.\n", NB_PLANTS)
	return plantList
}

// ------------------------------------------------------------------------------
// Fonctions de creation commandes
// ------------------------------------------------------------------------------

// createOrderForUser cree une commande pour un utilisateur.
//
// @param dbConnection *gorm.DB Client GORM
// @param targetUserID uint ID utilisateur
// @param availablePlants []models.Plant Liste des plantes
func createOrderForUser(dbConnection *gorm.DB, targetUserID uint, availablePlants []models.Plant) {
	orderStatuses := []string{"confirmed", "pending", "shipped", "delivered"}
	newOrder := models.Order{UserID: targetUserID, TotalPrice: 0.0, Status: orderStatuses[rand.Intn(len(orderStatuses))]}
	dbConnection.Create(&newOrder)
	orderTotal := addOrderItem(dbConnection, newOrder.ID, availablePlants) + addOrderItem(dbConnection, newOrder.ID, availablePlants)
	dbConnection.Model(&newOrder).Update("total_price", math.Trunc(orderTotal*100)/100)
}

// createOrders cree les commandes de test.
//
// @param dbConnection *gorm.DB Client GORM
// @param availablePlants []models.Plant Liste des plantes
func createOrders(dbConnection *gorm.DB, availablePlants []models.Plant) {
	log.Println("Creation des commandes...")
	var allUsers []models.User
	dbConnection.Find(&allUsers)
	orderCount := 0
	for _, currentUser := range allUsers {
		ordersForUser := rand.Intn(MAX_ORDERS_PER_USER + 1)
		for orderIndex := 0; orderIndex < ordersForUser; orderIndex++ {
			createOrderForUser(dbConnection, currentUser.ID, availablePlants)
			orderCount++
		}
	}
	log.Printf("%d commandes creees.\n", orderCount)
}

// addOrderItem ajoute un item a une commande et retourne le prix.
//
// @param dbConnection *gorm.DB Client GORM de base de donnees
// @param targetOrderID uint ID de la commande
// @param availablePlants []models.Plant Liste des plantes disponibles
// @return float64 Prix de l item ajoute
func addOrderItem(dbConnection *gorm.DB, targetOrderID uint, availablePlants []models.Plant) float64 {
	selectedPlant := availablePlants[rand.Intn(len(availablePlants))]
	if selectedPlant.Stock <= 0 {
		return 0
	}
	itemQuantity := rand.Intn(5) + 1
	if itemQuantity > selectedPlant.Stock {
		itemQuantity = selectedPlant.Stock
	}
	dbConnection.Create(&models.OrderItem{OrderID: targetOrderID, PlantID: selectedPlant.ID, Quantity: itemQuantity})
	dbConnection.Model(&selectedPlant).Update("stock", selectedPlant.Stock-itemQuantity)
	return float64(itemQuantity) * selectedPlant.Price
}

// ------------------------------------------------------------------------------
// Fonctions de generation fichier
// ------------------------------------------------------------------------------

// buildUsersFileContent construit le contenu du fichier users.txt.
//
// @param adminCredentials []map[string]string Credentials admin
// @param userCredentials []map[string]string Credentials utilisateur
// @return string Contenu du fichier
func buildUsersFileContent(adminCredentials, userCredentials []map[string]string) string {
	fileContent := "Administrateurs :\n\n"
	for _, adminCred := range adminCredentials {
		fileContent += fmt.Sprintf("%s %s\n", adminCred["email"], adminCred["password"])
	}
	fileContent += "\nUtilisateurs :\n\n"
	for _, userCred := range userCredentials {
		fileContent += fmt.Sprintf("%s %s\n", userCred["email"], userCred["password"])
	}
	return fileContent
}

// writeUsersFile genere le fichier users.txt.
//
// @param adminCredentials []map[string]string Credentials admin
// @param userCredentials []map[string]string Credentials utilisateur
func writeUsersFile(adminCredentials, userCredentials []map[string]string) {
	log.Println("Generation du fichier users.txt...")
	outputFilePath := filepath.Join(".", "users.txt")
	fileContent := buildUsersFileContent(adminCredentials, userCredentials)
	if writeError := os.WriteFile(outputFilePath, []byte(fileContent), 0644); writeError != nil {
		log.Fatalf("Erreur ecriture users.txt : %v", writeError)
	}
	log.Println("Fichier users.txt genere.")
}

// ==============================================================================
// Fonction principale
// ==============================================================================

// Seed execute le seed complet de la base de donnees.
//
// @param dbConnection *gorm.DB Client GORM de base de donnees
func Seed(dbConnection *gorm.DB) {
	log.Println("Lancement de la seed...")
	rand.Seed(time.Now().UnixNano())
	resetDatabase(dbConnection)
	adminCredentials := createAdmins(dbConnection)
	userCredentials := createUsers(dbConnection)
	plantList := createPlants(dbConnection)
	writeUsersFile(adminCredentials, userCredentials)
	createOrders(dbConnection, plantList)
	log.Println("Seed terminee avec succes!")
}
