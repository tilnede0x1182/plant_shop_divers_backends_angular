const bcrypt = require('bcrypt');
const { pool, findUserByUuid, serializeUser } = require('../auth/db');

module.exports = async (req, res, manifest) => {
  try {
    const { email, password, name, admin } = req.body;

    if (!email || !password || !name) {
      return res.status(400).json({ message: 'Email, password, and name required' });
    }

    // Check if user already exists via SQL direct
    const { rows } = await pool.query(
      'SELECT id FROM "user" WHERE email = $1 LIMIT 1',
      [email]
    );

    if (rows.length > 0) {
      return res.status(400).json({ message: 'User already exists' });
    }

    // Hash password
    const hashedPassword = await bcrypt.hash(password, 10);

    // Create user
    const user = await manifest.from('users').create({
      email,
      name,
      password: hashedPassword,
      admin: admin === true
    });

    const userRow = await findUserByUuid(user.id);
    res.status(201).json(serializeUser({ ...userRow, email: user.email, name: user.name, admin: user.admin }));
  } catch (error) {
    console.error('Create user error:', error);
    res.status(500).json({ message: 'Internal server error' });
  }
};
