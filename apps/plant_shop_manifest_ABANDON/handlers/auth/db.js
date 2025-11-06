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

async function findUserByEmail(email) {
  const query =
    'SELECT id, true_id, email, password, name, admin FROM "user" WHERE email = $1 LIMIT 1';
  const { rows } = await pool.query(query, [email]);
  return rows[0] || null;
}

async function findUserByTrueId(trueId) {
  const { rows } = await pool.query(
    'SELECT id, true_id, email, name, admin FROM "user" WHERE true_id = $1 LIMIT 1',
    [trueId]
  );
  return rows[0] || null;
}

async function findUserIdByTrueId(trueId) {
  const { rows } = await pool.query(
    'SELECT id FROM "user" WHERE true_id = $1 LIMIT 1',
    [trueId]
  );
  return rows[0]?.id || null;
}

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

async function findPlantByUuid(uuid) {
  const { rows } = await pool.query(
    'SELECT id, true_id, name, price, stock, description, "createdAt", "updatedAt" FROM "plant" WHERE id = $1 LIMIT 1',
    [uuid]
  );
  return rows[0] || null;
}

async function findPlantByTrueId(trueId) {
  const { rows } = await pool.query(
    'SELECT id, true_id, name, price, stock, description, "createdAt", "updatedAt" FROM "plant" WHERE true_id = $1 LIMIT 1',
    [trueId]
  );
  return rows[0] || null;
}

async function findPlantUuidByTrueId(trueId) {
  const { rows } = await pool.query(
    'SELECT id FROM "plant" WHERE true_id = $1 LIMIT 1',
    [trueId]
  );
  return rows[0]?.id || null;
}

async function listPlantsWithTrueId() {
  const { rows } = await pool.query(
    'SELECT id, true_id, name, price, stock, description, "createdAt", "updatedAt" FROM "plant" ORDER BY true_id'
  );
  return rows;
}

async function findOrderUuidByTrueId(trueId) {
  const { rows } = await pool.query(
    'SELECT id FROM "order" WHERE true_id = $1 LIMIT 1',
    [trueId]
  );
  return rows[0]?.id || null;
}

async function findOrderByUuid(uuid) {
  const { rows } = await pool.query(
    'SELECT id, true_id, "userId", "totalPrice", status, "createdAt", "updatedAt" FROM "order" WHERE id = $1 LIMIT 1',
    [uuid]
  );
  return rows[0] || null;
}

async function findOrderByTrueId(trueId) {
  const { rows } = await pool.query(
    'SELECT id, true_id, "userId", "totalPrice", status, "createdAt", "updatedAt" FROM "order" WHERE true_id = $1 LIMIT 1',
    [trueId]
  );
  return rows[0] || null;
}

async function listOrderItemsWithPlants(orderUuid) {
  const { rows } = await pool.query(
    'SELECT oi.id, oi.true_id, oi."orderId", oi."plantId", oi.quantity, oi."createdAt", oi."updatedAt", p.id AS plant_id, p.true_id AS plant_true_id, p.name AS plant_name, p.price AS plant_price, p.stock AS plant_stock, p.description AS plant_description FROM "order_item" oi JOIN "plant" p ON p.id = oi."plantId" WHERE oi."orderId" = $1 ORDER BY oi.true_id',
    [orderUuid]
  );
  return rows;
}

async function listOrdersForUser(userUuid) {
  const { rows } = await pool.query(
    'SELECT id, true_id, "userId", "totalPrice", status, "createdAt", "updatedAt" FROM "order" WHERE "userId" = $1 ORDER BY true_id',
    [userUuid]
  );
  return rows;
}

async function deleteOrderItemsByOrder(orderUuid) {
  await pool.query('DELETE FROM "order_item" WHERE "orderId" = $1', [orderUuid]);
}

async function deleteOrderItemsByPlant(plantUuid) {
  await pool.query('DELETE FROM "order_item" WHERE "plantId" = $1', [plantUuid]);
}

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

module.exports = {
  pool,
  findUserByEmail,
  findUserByTrueId,
  findUserIdByTrueId,
  findAdminByEmail,
  findPlantByUuid,
  findPlantByTrueId,
  findPlantUuidByTrueId,
  listPlantsWithTrueId,
  serializePlant,
  findOrderUuidByTrueId,
  findOrderByUuid,
  findOrderByTrueId,
  listOrderItemsWithPlants,
  listOrdersForUser,
  deleteOrderItemsByOrder,
  deleteOrderItemsByPlant,
  serializeOrder
};
