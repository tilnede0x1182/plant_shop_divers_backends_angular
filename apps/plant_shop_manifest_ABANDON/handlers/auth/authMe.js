const { pool, findUserByTrueId, serializeUser } = require('./db');
const { getUserFromToken } = require('./tokenUtils');

module.exports = async (req, res, manifest) => {
  try {
    console.log('👤 /auth/me called');

    // Extract user from JWT token
    const currentUser = getUserFromToken(req);
    console.log('👤 Current user from token:', currentUser);

    if (!currentUser) {
      console.log('❌ No user in token - returning 401');
      return res.status(401).json({ message: 'Not authenticated' });
    }

    let user = null;

    // Check if admin or user based on entitySlug
    if (currentUser.entitySlug === 'admins') {
      // Query admin table
      const { rows } = await pool.query(
        'SELECT id AS true_id, email, email AS name, true AS admin FROM "admin" WHERE id = $1 LIMIT 1',
        [currentUser.id]
      );
      user = rows[0] ? { ...rows[0], id: rows[0].true_id } : null;
    } else {
      // Query user table
      user = await findUserByTrueId(currentUser.id);
    }

    console.log('👤 User from DB:', user);

    if (!user) {
      console.log('❌ User not found in DB - returning 404');
      return res.status(404).json({ message: 'User not found' });
    }

    const serialized = serializeUser(user);
    console.log('👤 Sending serialized user:', serialized);

    // Return serialized user info (without password)
    res.status(200).json(serialized);
  } catch (error) {
    console.error('❌ Get current user error:', error);
    res.status(500).json({ message: 'Internal server error' });
  }
};
