const { findUserByTrueId, serializeUser } = require('../auth/db');
const { getUserFromToken } = require('../auth/tokenUtils');

module.exports = async (req, res, manifest) => {
  try {
    const { id } = req.params;
    const numericId = Number(id);
    if (!Number.isInteger(numericId)) {
      return res.status(400).json({ message: 'Invalid user id' });
    }

    const currentUser = getUserFromToken(req);
    if (!currentUser) {
      return res.status(401).json({ message: 'Unauthorized' });
    }

    // Un utilisateur normal ne peut voir que son propre profil
    // Un admin peut voir n'importe quel profil
    if (!currentUser.admin && currentUser.id !== numericId) {
      return res.status(403).json({ message: 'Forbidden: you can only view your own profile' });
    }

    const userRow = await findUserByTrueId(numericId);
    if (!userRow) {
      return res.status(404).json({ message: 'User not found' });
    }

    res.status(200).json(serializeUser(userRow));
  } catch (error) {
    console.error('Get user by ID error:', error);
    res.status(500).json({ message: 'Internal server error' });
  }
};
