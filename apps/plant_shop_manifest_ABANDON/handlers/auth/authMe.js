const { findUserByTrueId, serializeUser } = require('./db');
const { getUserFromToken } = require('./tokenUtils');

module.exports = async (req, res, manifest) => {
  try {
    // Extract user from JWT token
    const currentUser = getUserFromToken(req);

    if (!currentUser) {
      return res.status(401).json({ message: 'Not authenticated' });
    }

    // currentUser.id contains true_id from the JWT
    const user = await findUserByTrueId(currentUser.id);

    if (!user) {
      return res.status(404).json({ message: 'User not found' });
    }

    // Return serialized user info (without password)
    res.status(200).json(serializeUser(user));
  } catch (error) {
    console.error('Get current user error:', error);
    res.status(500).json({ message: 'Internal server error' });
  }
};
