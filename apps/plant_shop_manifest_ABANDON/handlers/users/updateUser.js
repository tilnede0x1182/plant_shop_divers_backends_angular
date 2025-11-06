const { findUserIdByTrueId } = require('../auth/db');

module.exports = async (req, res, manifest) => {
  try {
    const { id } = req.params;
    const numericId = Number(id);
    const updates = {};

    // Trouver l'id INTEGER via true_id
    const userId = await findUserIdByTrueId(numericId);
    if (!userId) {
      return res.status(404).json({ message: 'User not found' });
    }

    // Only include fields that are provided
    if (req.body.name !== undefined) updates.name = req.body.name;
    if (req.body.email !== undefined) updates.email = req.body.email;
    if (req.body.admin !== undefined) updates.admin = req.body.admin;

    const user = await manifest.from('users').patch(userId, updates);

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
    console.error('Update user error:', error);
    res.status(500).json({ message: 'Internal server error' });
  }
};
