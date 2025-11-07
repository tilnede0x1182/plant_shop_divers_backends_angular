// Script de seed adapté pour Manifest - inspiré de prisma/seed.ts
const { Client } = require('pg');
const bcrypt = require('bcrypt');
const { writeFileSync } = require('fs');
const { join } = require('path');

// Configuration
const NB_ADMINS = 3;
const NB_USERS = 20;
const NB_PLANTS = 50;
const MAX_ORDERS_PER_USER = 7;

const PLANT_NAMES = [
  'Rose', 'Tulipe', 'Lavande', 'Orchidée', 'Basilic', 'Menthe', 'Pivoine',
  'Tournesol', 'Cactus (Echinopsis)', 'Bambou', 'Camomille (Matricaria recutita)',
  'Sauge (Salvia officinalis)', 'Romarin (Rosmarinus officinalis)',
  'Thym (Thymus vulgaris)', 'Laurier-rose (Nerium oleander)', 'Aloe vera',
  'Jasmin (Jasminum officinale)', 'Hortensia (Hydrangea macrophylla)',
  'Marguerite (Leucanthemum vulgare)', 'Géranium (Pelargonium graveolens)',
  'Fuchsia (Fuchsia magellanica)', 'Anémone (Anemone coronaria)',
  'Azalée (Rhododendron simsii)', 'Chrysanthème (Chrysanthemum morifolium)',
  'Digitale pourpre (Digitalis purpurea)', 'Glaïeul (Gladiolus hortulanus)',
  'Lys (Lilium candidum)', 'Violette (Viola odorata)',
  'Muguet (Convallaria majalis)', 'Iris (Iris germanica)',
  'Lavandin (Lavandula intermedia)', 'Érable du Japon (Acer palmatum)',
  'Citronnelle (Cymbopogon citratus)', 'Pin parasol (Pinus pinea)',
  'Cyprès (Cupressus sempervirens)', 'Olivier (Olea europaea)',
  'Papyrus (Cyperus papyrus)', 'Figuier (Ficus carica)',
  'Eucalyptus (Eucalyptus globulus)', 'Acacia (Acacia dealbata)',
  'Bégonia (Begonia semperflorens)', 'Calathea (Calathea ornata)',
  'Dieffenbachia (Dieffenbachia seguine)', 'Ficus elastica',
  'Sansevieria (Sansevieria trifasciata)', 'Philodendron (Philodendron scandens)',
  'Yucca (Yucca elephantipes)', 'Zamioculcas zamiifolia', 'Monstera deliciosa',
  'Pothos (Epipremnum aureum)', 'Agave (Agave americana)',
  'Cactus raquette (Opuntia ficus-indica)', 'Palmier-dattier (Phoenix dactylifera)',
  'Amaryllis (Hippeastrum hybridum)', 'Bleuet (Centaurea cyanus)',
  'Cœur-de-Marie (Lamprocapnos spectabilis)', 'Croton (Codiaeum variegatum)',
  'Dracaena (Dracaena marginata)', 'Hosta (Hosta plantaginea)',
  'Lierre (Hedera helix)', 'Mimosa (Acacia dealbata)',
];

// Helpers faker-like
const randomInt = (min, max) => Math.floor(Math.random() * (max - min + 1)) + min;
const randomElement = (arr) => arr[Math.floor(Math.random() * arr.length)];

const fakeEmail = () => {
  const names = ['john', 'jane', 'bob', 'alice', 'charlie', 'david', 'emma', 'frank'];
  const domains = ['example.com', 'test.com', 'mail.com', 'demo.com'];
  return `${randomElement(names)}${randomInt(1, 999)}@${randomElement(domains)}`;
};

const fakeName = () => {
  const firstNames = ['Jean', 'Marie', 'Pierre', 'Sophie', 'Luc', 'Anne', 'Paul', 'Claire'];
  const lastNames = ['Dupont', 'Martin', 'Bernard', 'Dubois', 'Thomas', 'Robert', 'Petit', 'Richard'];
  return `${randomElement(firstNames)} ${randomElement(lastNames)}`;
};

const fakePassword = () => {
  const chars = 'abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789';
  let pwd = '';
  for (let i = 0; i < 12; i++) pwd += chars[randomInt(0, chars.length - 1)];
  return pwd;
};

const fakeSentence = () => {
  const words = ['Une', 'belle', 'plante', 'verte', 'pour', 'votre', 'jardin', 'intérieur', 'parfaite', 'magnifique'];
  const length = randomInt(8, 12);
  let sentence = '';
  for (let i = 0; i < length; i++) sentence += randomElement(words) + ' ';
  return sentence.trim() + '.';
};

// Classe SeedService
class SeedService {
  constructor() {
    this.client = new Client({
      connectionString: process.env.DATABASE_URL,
    });
  }

  async connect() {
    await this.client.connect();
  }

  async disconnect() {
    await this.client.end();
  }

  async reset() {
    console.log('🗑️  Nettoyage des tables...');
    await this.client.query('DELETE FROM "order_item"');
    await this.client.query('DELETE FROM "order"');
    await this.client.query('DELETE FROM "plant"');
    await this.client.query('DELETE FROM "user"');
    await this.client.query('DELETE FROM "admin"');
  }

  async createAdmins() {
    console.log(`👨‍💼 Création de ${NB_ADMINS} admins...`);
    const admins = [];
    for (let idx = 0; idx < NB_ADMINS; idx++) {
      const email = `admin${idx + 1}@planteshop.com`;
      const password = 'password';
      const hashedPassword = await bcrypt.hash(password, 10);
      const name = fakeName();

      // Insérer dans la table user avec admin=true
      await this.client.query(
        'INSERT INTO "user" (email, password, admin, name, "createdAt", "updatedAt") VALUES ($1, $2, $3, $4, NOW(), NOW())',
        [email, hashedPassword, true, name]
      );

      // Insérer AUSSI dans la table admin (pour le panel admin)
      await this.client.query(
        'INSERT INTO "admin" (email, password, "createdAt", "updatedAt") VALUES ($1, $2, NOW(), NOW())',
        [email, hashedPassword]
      );

      admins.push({ email, password });
    }
    return admins;
  }

  async createUsers() {
    console.log(`👤 Création de ${NB_USERS} utilisateurs...`);
    const users = [];
    for (let idx = 0; idx < NB_USERS; idx++) {
      const email = fakeEmail();
      const password = fakePassword();
      const hashedPassword = await bcrypt.hash(password, 10);
      const name = fakeName();

      await this.client.query(
        'INSERT INTO "user" (email, password, admin, name, "createdAt", "updatedAt") VALUES ($1, $2, $3, $4, NOW(), NOW())',
        [email, hashedPassword, false, name]
      );
      users.push({ email, password });
    }
    return users;
  }

  async createPlants() {
    console.log(`🌱 Création de ${NB_PLANTS} plantes...`);
    const plants = [];
    const max = PLANT_NAMES.length;

    for (let idx = 0; idx < NB_PLANTS; idx++) {
      const base = PLANT_NAMES[idx % max];
      const name = NB_PLANTS > max ? `${base} ${Math.floor(idx / max) + 1}` : base;
      const price = randomInt(5, 50);
      const stock = randomInt(5, 30);
      const description = fakeSentence();

      const result = await this.client.query(
        'INSERT INTO "plant" (name, price, stock, description, "createdAt", "updatedAt") VALUES ($1, $2, $3, $4, NOW(), NOW()) RETURNING id, price, stock',
        [name, price, stock, description]
      );
      plants.push(result.rows[0]);
    }
    return plants;
  }

  async createOrders(plants) {
    console.log(`📦 Création des commandes...`);
    const usersResult = await this.client.query('SELECT id FROM "user"');
    const users = usersResult.rows;

    for (const user of users) {
      const numberOfOrders = randomInt(0, MAX_ORDERS_PER_USER);
      for (let idx = 0; idx < numberOfOrders; idx++) {
        await this.createOrderForUser(user.id, plants);
      }
    }
  }

  async createOrderForUser(userId, plants) {
    const status = randomElement(['confirmed', 'pending', 'shipped', 'delivered']);
    const orderResult = await this.client.query(
      'INSERT INTO "order" ("userId", "totalPrice", status, "createdAt", "updatedAt") VALUES ($1, $2, $3, NOW(), NOW()) RETURNING id',
      [userId, 0, status]
    );
    const orderId = orderResult.rows[0].id;

    let total = 0;
    for (let iter = 0; iter < 2; iter++) {
      total += await this.addItem(orderId, plants);
    }

    await this.client.query(
      'UPDATE "order" SET "totalPrice" = $1 WHERE id = $2',
      [total, orderId]
    );
  }

  async addItem(orderId, plants) {
    const plant = randomElement(plants);
    if (!plant.stock) return 0;

    const qty = Math.min(randomInt(1, 5), plant.stock);
    if (!qty) return 0;

    await this.client.query(
      'INSERT INTO "order_item" ("orderId", "plantId", quantity, "createdAt", "updatedAt") VALUES ($1, $2, $3, NOW(), NOW())',
      [orderId, plant.id, qty]
    );

    await this.client.query(
      'UPDATE "plant" SET stock = stock - $1 WHERE id = $2',
      [qty, plant.id]
    );

    plant.stock -= qty;
    return plant.price * qty;
  }

  writeUsersFile(admins, users) {
    const path = join(__dirname, 'users.txt');
    let txt = 'Administrateurs :\n\n';
    admins.forEach((admin) => (txt += `${admin.email} ${admin.password}\n`));
    txt += '\nUtilisateurs :\n\n';
    users.forEach((u) => (txt += `${u.email} ${u.password}\n`));
    writeFileSync(path, txt, 'utf8');
    console.log(`📄 Fichier users.txt créé: ${path}`);
  }

  async run() {
    try {
      await this.connect();
      await this.reset();
      const admins = await this.createAdmins();
      const users = await this.createUsers();
      const plants = await this.createPlants();
      this.writeUsersFile(admins, users);
      await this.createOrders(plants);
      console.log('✅ Seed terminée. Données créées & users.txt généré.');
    } catch (error) {
      console.error('❌ Erreur lors du seed:', error);
      throw error;
    } finally {
      await this.disconnect();
    }
  }
}

// Lancement
new SeedService().run().catch((err) => {
  console.error(err);
  process.exit(1);
});
