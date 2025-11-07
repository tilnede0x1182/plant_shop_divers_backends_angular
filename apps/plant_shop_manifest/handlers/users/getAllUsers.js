const { listUsersWithTrueId, serializeUser } = require('../auth/db');

module.exports = async (req, res) => {
  try {
    const users = await listUsersWithTrueId();
    res.status(200).json(users.map(serializeUser));
  } catch (error) {
    console.error('Get all users error:', error);
    res.status(500).json({ message: 'Internal server error' });
  }
};
