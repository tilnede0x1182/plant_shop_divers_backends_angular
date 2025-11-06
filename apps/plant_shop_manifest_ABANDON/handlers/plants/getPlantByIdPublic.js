const {
  findPlantByTrueId,
  serializePlant
} = require('../auth/db');

module.exports = async (req, res) => {
  try {
    const { id } = req.params;
    const numericId = Number(id);
    if (!Number.isInteger(numericId)) {
      return res.status(400).json({ message: 'Invalid plant id' });
    }

    const plant = await findPlantByTrueId(numericId);

    if (!plant) {
      return res.status(404).json({ message: 'Plant not found' });
    }

    res.status(200).json(serializePlant(plant));
  } catch (error) {
    console.error('Get plant by ID error:', error);
    res.status(500).json({ message: 'Internal server error' });
  }
};
