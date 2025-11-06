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

    // Admin only - enforced by policy
    await manifest.from('users').delete(userId);

    res.status(200).json({ message: 'User deleted successfully' });
  } catch (error) {
    console.error('Delete user error:', error);
    res.status(500).json({ message: 'Internal server error' });
  }
};
