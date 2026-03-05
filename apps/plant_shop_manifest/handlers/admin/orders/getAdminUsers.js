const { listUsersWithTrueId, serializeUser } = require('../../auth/db');

/**
 * Handler liste utilisateurs (admin)
 * @param {Object} _req Requête HTTP
 * @param {Object} res Réponse HTTP
 */
module.exports = async (_req, res) => {
  try {
    const users = await listUsersWithTrueId();
    res.status(200).json(users.map(serializeUser));
  } catch (error) {
    console.error('Get admin users error:', error);
    res.status(500).json({ message: 'Internal server error' });
  }
};
