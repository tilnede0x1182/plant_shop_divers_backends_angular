const { findUserByTrueId, serializeUser } = require('../auth/db');

module.exports = async (req, res, manifest) => {
  try {
    const { id } = req.params;
    const numericId = Number(id);
    if (!Number.isInteger(numericId)) {
      return res.status(400).json({ message: 'Invalid user id' });
    }

    const userRow = await findUserByTrueId(numericId);
    if (!userRow) {
      return res.status(404).json({ message: 'User not found' });
    }

    // ensure latest data using manifest if needed
    const user = await manifest.from('users').findOneById(userRow.id);

    if (!user) {
      return res.status(404).json({ message: 'User not found' });
    }

    res.status(200).json(serializeUser({ ...userRow, email: user.email, name: user.name, admin: user.admin }));
  } catch (error) {
    console.error('Get user by ID error:', error);
    res.status(500).json({ message: 'Internal server error' });
  }
};
