const {
  listPlantsWithTrueId,
  serializePlant
} = require('../../auth/db');

/**
 * Handler liste plantes (admin)
 * @param {Object} _req Requête HTTP
 * @param {Object} res Réponse HTTP
 */
module.exports = async (_req, res) => {
  try {
    const plants = await listPlantsWithTrueId();
    res.status(200).json(plants.map(serializePlant));
  } catch (error) {
    console.error('Get admin plants error:', error);
    res.status(500).json({ message: 'Internal server error' });
  }
};
