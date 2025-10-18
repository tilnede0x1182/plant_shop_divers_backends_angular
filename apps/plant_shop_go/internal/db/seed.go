package db

// # Importations
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
	log.Println("🧹 Nettoyage de la base de données…")
	db.Exec("DELETE FROM order_items")
	db.Exec("DELETE FROM orders")
	db.Exec("DELETE FROM plants")
	db.Exec("DELETE FROM users")
	log.Println("✅ Base de données nettoyée.")
}

// ## Admins
func createAdmins(db *gorm.DB) []map[string]string {
	log.Println("👑 Création des administrateurs…")
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
	log.Printf("✅ %d administrateurs créés.\n", NB_ADMINS)
	return admins
}

// ## Users
func createUsers(db *gorm.DB) []map[string]string {
	log.Println("👥 Création des utilisateurs…")
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
	log.Printf("✅ %d utilisateurs créés.\n", NB_USERS)
	return users
}

// ## Plants
func createPlants(db *gorm.DB) []models.Plant {
	log.Println("🌱 Création des plantes…")
	var plants []models.Plant
	max := len(PLANT_NAMES)
	for i := 0; i < NB_PLANTS; i++ {
		base := PLANT_NAMES[i%max]
		name := base
		price := math.Round((rand.Float64()*45.0+5.0)*100) / 100
		desc := faker.Sentence()
		stock := rand.Intn(26) + 5
		p := models.Plant{
			Name:        name,
			Price:       price,
			Description: desc,
			Stock:       stock,
		}
		db.Create(&p)
		plants = append(plants, p)
	}
	log.Printf("✅ %d plantes créées.\n", NB_PLANTS)
	return plants
}

// ## Orders
func createOrders(db *gorm.DB, plants []models.Plant) {
	log.Println("🛒 Création des commandes…")
	var users []models.User
	db.Find(&users)
	statuses := []string{"confirmed", "pending", "shipped", "delivered"}
	totalOrders := 0
	for _, u := range users {
		n := rand.Intn(MAX_ORDERS_PER_USER + 1)
		for i := 0; i < n; i++ {
			total := 0.0
			order := models.Order{
				UserID:     u.ID,
				TotalPrice: 0.0,
				Status:     statuses[rand.Intn(len(statuses))],
			}
			db.Create(&order)
			for j := 0; j < 2; j++ {
				total += addItem(db, order.ID, plants)
			}
			total = math.Trunc(total*100) / 100
			db.Model(&order).Update("total_price", total)
			totalOrders++
		}
	}
	log.Printf("✅ %d commandes créées.\n", totalOrders)
}

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

// ## users.txt
func writeUsersFile(admins, users []map[string]string) {
	log.Println("✍️  Génération du fichier users.txt…")
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
		log.Fatalf("[ERREUR] Écriture users.txt : %v", err)
	}
	log.Println("✅ Fichier users.txt généré.")
}

// # Seed principal
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
