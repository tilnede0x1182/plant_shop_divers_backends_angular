const {
  findPlantUuidByTrueId,
  findPlantByUuid,
  serializePlant
} = require('../auth/db');

/**
 * Handler mise à jour plante (admin)
 * @param {Object} req Requête HTTP
 * @param {Object} res Réponse HTTP
 * @param {Object} manifest Instance Manifest
 */
module.exports = async (req, res, manifest) => {
  try {
    const { id } = req.params;
    const numericId = Number(id);
    if (!Number.isInteger(numericId)) {
      return res.status(400).json({ message: 'Invalid plant id' });
    }
    const updates = {};

    // Only include fields that are provided
    if (req.body.name !== undefined) updates.name = req.body.name;
    if (req.body.price !== undefined) updates.price = parseFloat(req.body.price);
    if (req.body.stock !== undefined) updates.stock = parseInt(req.body.stock);
    if (req.body.description !== undefined) updates.description = req.body.description;

    const plantUuid = await findPlantUuidByTrueId(numericId);
    if (!plantUuid) {
      return res.status(404).json({ message: 'Plant not found' });
    }

    const plant = await manifest.from('plants').patch(plantUuid, updates);

    if (!plant) {
      return res.status(404).json({ message: 'Plant not found' });
    }

    const updatedPlant = await findPlantByUuid(plant.id);
    res.status(200).json(serializePlant(updatedPlant));
  } catch (error) {
    console.error('Update plant error:', error);
    res.status(500).json({ message: 'Internal server error' });
  }
};
