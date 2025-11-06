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

module.exports = {
  pool,
  findUserByEmail,
  findAdminByEmail,
  findPlantByUuid,
  findPlantByTrueId,
  findPlantUuidByTrueId,
  listPlantsWithTrueId,
  serializePlant
};
