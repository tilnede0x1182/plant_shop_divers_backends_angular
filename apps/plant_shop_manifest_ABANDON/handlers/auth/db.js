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
    'SELECT id, email, password, name, admin FROM "user" WHERE email = $1 LIMIT 1';
  const { rows } = await pool.query(query, [email]);
  return rows[0] || null;
}

async function findAdminByEmail(email) {
  const query =
    'SELECT id, email, password FROM "admin" WHERE email = $1 LIMIT 1';
  const { rows } = await pool.query(query, [email]);
  const admin = rows[0];
  if (!admin) return null;
  return {
    ...admin,
    name: admin.name || admin.email.split('@')[0],
    admin: true
  };
}

module.exports = {
  pool,
  findUserByEmail,
  findAdminByEmail
};
