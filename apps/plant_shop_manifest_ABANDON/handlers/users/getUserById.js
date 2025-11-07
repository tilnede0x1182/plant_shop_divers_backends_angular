const { findUserByTrueId, serializeUser } = require('../auth/db');
const { getUserFromToken } = require('../auth/tokenUtils');

module.exports = async (req, res, manifest) => {
  try {
    const { id } = req.params;
    const numericId = Number(id);

    console.log('📖 getUserById called for ID:', id, '→', numericId);

    if (!Number.isInteger(numericId)) {
      return res.status(400).json({ message: 'Invalid user id' });
    }

    const currentUser = getUserFromToken(req);
    console.log('📖 Current user from token:', currentUser);

    if (!currentUser) {
      console.log('❌ No current user - returning 401');
      return res.status(401).json({ message: 'Unauthorized' });
    }

    // Un utilisateur normal ne peut voir que son propre profil
    // Un admin peut voir n'importe quel profil
    console.log('📖 Checking access: currentUser.admin =', currentUser.admin, ', currentUser.id =', currentUser.id, ', numericId =', numericId);
    if (!currentUser.admin && currentUser.id !== numericId) {
      console.log('❌ Access denied: user can only view own profile');
      return res.status(403).json({ message: 'Forbidden: you can only view your own profile' });
    }

    const userRow = await findUserByTrueId(numericId);
    console.log('📖 User from DB:', userRow);

    if (!userRow) {
      console.log('❌ User not found in DB');
      return res.status(404).json({ message: 'User not found' });
    }

    const serialized = serializeUser(userRow);
    console.log('📖 Sending serialized user:', serialized);

    res.status(200).json(serialized);
  } catch (error) {
    console.error('❌ Get user by ID error:', error);
    res.status(500).json({ message: 'Internal server error' });
  }
};
