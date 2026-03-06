const path = require('path');
const dotenv = require('dotenv');
const { Pool } = require('pg');

dotenv.config({ path: path.resolve(__dirname, '../../.env') });

const {
  DATABASE_URL,
  DB_HOST = 'localhost',
  DB_PORT = '5432',
  DB_USERNAME = 'postgres',
  DB_PASSWORD = 'postgres',
  DB_DATABASE = 'postgres',
  DB_SSL = 'false'
} = process.env;

const pool = new Pool({
  connectionString:
    DATABASE_URL ||
    `postgresql://${DB_USERNAME}:${DB_PASSWORD}@${DB_HOST}:${DB_PORT}/${DB_DATABASE}`,
  ssl: DB_SSL === 'true' ? { rejectUnauthorized: false } : undefined
});

/**
 * Recherche un utilisateur par email
 * @param {string} email Email de l'utilisateur
 * @return {Object|null} Utilisateur trouvé ou null
 */
async function findUserByEmail(email) {
  const query =
    'SELECT id, true_id, email, password, name, admin FROM "user" WHERE email = $1 LIMIT 1';
  const { rows } = await pool.query(query, [email]);
  return rows[0] || null;
}

/**
 * Recherche un utilisateur par true_id
 * @param {number} trueId Identifiant true_id de l'utilisateur
 * @return {Object|null} Utilisateur trouvé ou null
 */
async function findUserByTrueId(trueId) {
  const { rows } = await pool.query(
    'SELECT id, true_id, email, name, admin FROM "user" WHERE true_id = $1 LIMIT 1',
    [trueId]
  );
  return rows[0] || null;
}

/**
 * Recherche l'id interne d'un utilisateur par true_id
 * @param {number} trueId Identifiant true_id de l'utilisateur
 * @return {string|null} UUID de l'utilisateur ou null
 */
async function findUserIdByTrueId(trueId) {
  const { rows } = await pool.query(
    'SELECT id FROM "user" WHERE true_id = $1 LIMIT 1',
    [trueId]
  );
  return rows[0]?.id || null;
}

/**
 * Recherche un admin par email
 * @param {string} email Email de l'admin
 * @return {Object|null} Admin trouvé ou null
 */
async function findAdminByEmail(email) {
  const query =
    'SELECT id, true_id, email, password FROM "admin" WHERE email = $1 LIMIT 1';
  const { rows } = await pool.query(query, [email]);
  const admin = rows[0];
  if (!admin) return null;
  return {
    ...admin,
    name: admin.name || admin.email.split('@')[0],
    admin: true
  };
}

/**
 * Recherche un admin par id
 * @param {string} id UUID de l'admin
 * @return {Object|null} Admin trouvé ou null
 */
async function findAdminById(id) {
  const { rows } = await pool.query(
    'SELECT id, email FROM "admin" WHERE id = $1 LIMIT 1',
    [id]
  );
  const admin = rows[0];
  if (!admin) return null;
  return {
    id: admin.id,
    true_id: admin.id,
    email: admin.email,
    name: admin.email.split('@')[0],  // Use email prefix as name
    admin: true
  };
}

/**
 * Recherche une plante par UUID
 * @param {string} uuid UUID de la plante
 * @return {Object|null} Plante trouvée ou null
 */
async function findPlantByUuid(uuid) {
  const { rows } = await pool.query(
    'SELECT id, true_id, name, price, stock, description, "createdAt", "updatedAt" FROM "plant" WHERE id = $1 LIMIT 1',
    [uuid]
  );
  return rows[0] || null;
}

/**
 * Recherche une plante par true_id
 * @param {number} trueId Identifiant true_id de la plante
 * @return {Object|null} Plante trouvée ou null
 */
async function findPlantByTrueId(trueId) {
  const { rows } = await pool.query(
    'SELECT id, true_id, name, price, stock, description, "createdAt", "updatedAt" FROM "plant" WHERE true_id = $1 LIMIT 1',
    [trueId]
  );
  return rows[0] || null;
}

/**
 * Recherche l'UUID d'une plante par true_id
 * @param {number} trueId Identifiant true_id de la plante
 * @return {string|null} UUID de la plante ou null
 */
async function findPlantUuidByTrueId(trueId) {
  const { rows } = await pool.query(
    'SELECT id FROM "plant" WHERE true_id = $1 LIMIT 1',
    [trueId]
  );
  return rows[0]?.id || null;
}

/**
 * Liste toutes les plantes avec leur true_id
 * @return {Array} Liste des plantes
 */
async function listPlantsWithTrueId() {
  const { rows } = await pool.query(
    'SELECT id, true_id, name, price, stock, description, "createdAt", "updatedAt" FROM "plant" ORDER BY name ASC'
  );
  return rows;
}

/**
 * Liste tous les utilisateurs avec leur true_id
 * @return {Array} Liste des utilisateurs
 */
async function listUsersWithTrueId() {
  const { rows } = await pool.query(
    'SELECT id, true_id, email, name, admin, "createdAt", "updatedAt" FROM "user" ORDER BY true_id'
  );
  return rows;
}

/**
 * Recherche l'UUID d'une commande par true_id
 * @param {number} trueId Identifiant true_id de la commande
 * @return {string|null} UUID de la commande ou null
 */
async function findOrderUuidByTrueId(trueId) {
  const { rows } = await pool.query(
    'SELECT id FROM "order" WHERE true_id = $1 LIMIT 1',
    [trueId]
  );
  return rows[0]?.id || null;
}

/**
 * Recherche une commande par UUID
 * @param {string} uuid UUID de la commande
 * @return {Object|null} Commande trouvée ou null
 */
async function findOrderByUuid(uuid) {
  const { rows } = await pool.query(
    'SELECT id, true_id, "userId", "totalPrice", status, "createdAt", "updatedAt" FROM "order" WHERE id = $1 LIMIT 1',
    [uuid]
  );
  return rows[0] || null;
}

/**
 * Recherche une commande par true_id
 * @param {number} trueId Identifiant true_id de la commande
 * @return {Object|null} Commande trouvée ou null
 */
async function findOrderByTrueId(trueId) {
  const { rows } = await pool.query(
    'SELECT id, true_id, "userId", "totalPrice", status, "createdAt", "updatedAt" FROM "order" WHERE true_id = $1 LIMIT 1',
    [trueId]
  );
  return rows[0] || null;
}

/**
 * Liste les items d'une commande avec les plantes associées
 * @param {string} orderUuid UUID de la commande
 * @return {Array} Liste des items avec plantes
 */
async function listOrderItemsWithPlants(orderUuid) {
  const { rows } = await pool.query(
    'SELECT oi.id, oi.true_id, oi."orderId", oi."plantId", oi.quantity, oi."createdAt", oi."updatedAt", p.id AS plant_id, p.true_id AS plant_true_id, p.name AS plant_name, p.price AS plant_price, p.stock AS plant_stock, p.description AS plant_description FROM "order_item" oi JOIN "plant" p ON p.id = oi."plantId" WHERE oi."orderId" = $1 ORDER BY oi.true_id',
    [orderUuid]
  );
  return rows;
}

/**
 * Liste les commandes d'un utilisateur
 * @param {string} userUuid UUID de l'utilisateur
 * @return {Array} Liste des commandes
 */
async function listOrdersForUser(userUuid) {
  const { rows } = await pool.query(
    'SELECT id, true_id, "userId", "totalPrice", status, "createdAt", "updatedAt" FROM "order" WHERE "userId" = $1 ORDER BY true_id DESC',
    [userUuid]
  );
  return rows;
}

/**
 * Supprime les items d'une commande
 * @param {string} orderUuid UUID de la commande
 */
async function deleteOrderItemsByOrder(orderUuid) {
  await pool.query('DELETE FROM "order_item" WHERE "orderId" = $1', [orderUuid]);
}

/**
 * Supprime les items liés à une plante
 * @param {string} plantUuid UUID de la plante
 */
async function deleteOrderItemsByPlant(plantUuid) {
  await pool.query('DELETE FROM "order_item" WHERE "plantId" = $1', [plantUuid]);
}

/**
 * Recherche l'UUID d'un utilisateur par true_id
 * @param {number} trueId Identifiant true_id de l'utilisateur
 * @return {string|null} UUID de l'utilisateur ou null
 */
async function findUserUuidByTrueId(trueId) {
  const { rows } = await pool.query(
    'SELECT id FROM "user" WHERE true_id = $1 LIMIT 1',
    [trueId]
  );
  return rows[0]?.id || null;
}

/**
 * Recherche un utilisateur complet par true_id (avec dates)
 * @param {number} trueId Identifiant true_id de l'utilisateur
 * @return {Object|null} Utilisateur trouvé ou null
 */
async function findUserByTrueId(trueId) {
  const { rows } = await pool.query(
    'SELECT id, true_id, email, name, admin, "createdAt", "updatedAt" FROM "user" WHERE true_id = $1 LIMIT 1',
    [trueId]
  );
  return rows[0] || null;
}

/**
 * Recherche un utilisateur par UUID
 * @param {string} uuid UUID de l'utilisateur
 * @return {Object|null} Utilisateur trouvé ou null
 */
async function findUserByUuid(uuid) {
  const { rows } = await pool.query(
    'SELECT id, true_id, email, name, admin, "createdAt", "updatedAt" FROM "user" WHERE id = $1 LIMIT 1',
    [uuid]
  );
  return rows[0] || null;
}

/**
 * Sérialise une plante pour l'API
 * @param {Object} row Ligne de la base de données
 * @return {Object|null} Plante sérialisée ou null
 */
function serializePlant(row) {
  if (!row) return null;
  return {
    id: row.true_id,
    uuid: row.id,
    name: row.name,
    price: row.price,
    stock: row.stock,
    description: row.description,
    createdAt: row.createdAt,
    updatedAt: row.updatedAt
  };
}

/**
 * Sérialise une commande avec ses items pour l'API
 * @param {Object} orderRow Ligne de commande
 * @param {Array} itemsRows Lignes des items
 * @return {Object|null} Commande sérialisée ou null
 */
function serializeOrder(orderRow, itemsRows = []) {
  if (!orderRow) return null;

  const orderItems = itemsRows.map((item) => ({
    id: item.true_id,
    uuid: item.id,
    orderId: orderRow.true_id,
    quantity: Number(item.quantity),
    createdAt: item.createdAt,
    updatedAt: item.updatedAt,
    plant: {
      id: item.plant_true_id,
      uuid: item.plant_id,
      name: item.plant_name,
      price: item.plant_price,
      stock: item.plant_stock,
      description: item.plant_description
    }
  }));

  return {
    id: orderRow.true_id,
    uuid: orderRow.id,
    userId: orderRow.userId,
    totalPrice: Number(orderRow.totalPrice),
    status: orderRow.status,
    createdAt: orderRow.createdAt,
    updatedAt: orderRow.updatedAt,
    orderItems
  };
}

/**
 * Sérialise un utilisateur pour l'API
 * @param {Object} row Ligne de la base de données
 * @return {Object|null} Utilisateur sérialisé ou null
 */
function serializeUser(row) {
  if (!row) return null;
  // Convert true_id to number if it's a string (from BIGSERIAL)
  const trueId = typeof row.true_id === 'number' ? row.true_id : parseInt(row.true_id, 10);
  return {
    id: trueId,
    uuid: row.id,
    email: row.email,
    name: row.name,
    admin: row.admin,
    createdAt: row.createdAt,
    updatedAt: row.updatedAt
  };
}

module.exports = {
  pool,
  findUserByEmail,
  findUserByTrueId,
  findUserIdByTrueId,
  findUserByUuid,
  findAdminByEmail,
  findAdminById,
  findPlantByUuid,
  findPlantByTrueId,
  findPlantUuidByTrueId,
  listPlantsWithTrueId,
  listUsersWithTrueId,
  serializePlant,
  findOrderUuidByTrueId,
  findOrderByUuid,
  findOrderByTrueId,
  listOrderItemsWithPlants,
  listOrdersForUser,
  deleteOrderItemsByOrder,
  deleteOrderItemsByPlant,
  serializeOrder,
  serializeUser
};
