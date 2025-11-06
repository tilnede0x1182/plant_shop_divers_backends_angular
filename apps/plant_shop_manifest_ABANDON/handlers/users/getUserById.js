const { findUserIdByTrueId } = require('../auth/db');

module.exports = async (req, res, manifest) => {
  try {
    const { id } = req.params;
    const numericId = Number(id);

    // Trouver l'id INTEGER via true_id
    const userId = await findUserIdByTrueId(numericId);
    if (!userId) {
      return res.status(404).json({ message: 'User not found' });
    }

    const user = await manifest.from('users').findOneById(userId);

    if (!user) {
      return res.status(404).json({ message: 'User not found' });
    }

    // Return user info (without password)
    res.status(200).json({
      id: user.id,
      email: user.email,
      name: user.name,
      admin: user.admin
    });
  } catch (error) {
    console.error('Get user by ID error:', error);
    res.status(500).json({ message: 'Internal server error' });
  }
};
