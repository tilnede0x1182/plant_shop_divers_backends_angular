const { listUsersWithTrueId, serializeUser } = require('../../auth/db');

module.exports = async (_req, res) => {
  try {
    const users = await listUsersWithTrueId();
    res.status(200).json(users.map(serializeUser));
  } catch (error) {
    console.error('Get admin users error:', error);
    res.status(500).json({ message: 'Internal server error' });
  }
};
