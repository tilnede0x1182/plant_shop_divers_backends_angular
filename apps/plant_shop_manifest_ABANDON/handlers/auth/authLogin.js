const path = require('path');
const dotenv = require('dotenv');
const { Pool } = require('pg');
const bcrypt = require('bcrypt');
const { generateUserToken } = require('./tokenUtils');

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

module.exports = async (req, res, manifest) => {
  console.log('🔥 authLogin handler called!', req.body);
  try {
    const { email, password } = req.body;
    console.log('📧 Email:', email, 'Password:', password ? '***' : 'missing');

    if (!email || !password) {
      console.log('❌ Email or password missing');
      return res.status(400).json({ message: 'Email and password required' });
    }

    // Find user by email via direct SQL (bypass Manifest SDK filtering)
    console.log('🔍 Searching for user with email via SQL:', email);
    const user = await findUserByEmail(email);
    console.log('👤 User found:', user ? `ID ${user.id}` : 'NOT FOUND');
    if (!user) {
      return res.status(401).json({ message: 'Invalid credentials' });
    }

    // Verify password
    console.log('🔐 Verifying password...');
    const isValidPassword = await bcrypt.compare(password, user.password);
    console.log('🔐 Password valid:', isValidPassword);
    if (!isValidPassword) {
      return res.status(401).json({ message: 'Invalid credentials' });
    }

    // Generate JWT token
    console.log('🎫 Generating JWT token...');
    const token = generateUserToken(user);
    console.log('🎫 Token generated:', token ? 'YES' : 'NO');

    // Set cookie
    res.cookie('jwt', token, {
      httpOnly: true,
      secure: process.env.NODE_ENV === 'production',
      sameSite: 'strict',
      maxAge: 24 * 60 * 60 * 1000 // 24 hours
    });

    console.log('✅ Login successful, sending response');
    // Return user info (without password)
    res.status(201).json({
      user: {
        id: user.id,
        email: user.email,
        name: user.name,
        admin: user.admin
      }
    });
  } catch (error) {
    console.error('❌ Login error:', error.message);
    console.error('❌ Stack:', error.stack);
    res.status(500).json({ message: 'Internal server error' });
  }
};
