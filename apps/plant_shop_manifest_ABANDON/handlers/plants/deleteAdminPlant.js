const { findPlantUuidByTrueId } = require('../auth/db');

module.exports = async (req, res, manifest) => {
  try {
    const { id } = req.params;
    const numericId = Number(id);
    if (!Number.isInteger(numericId)) {
      return res.status(400).json({ message: 'Invalid plant id' });
    }

    const plantUuid = await findPlantUuidByTrueId(numericId);
    if (!plantUuid) {
      return res.status(404).json({ message: 'Plant not found' });
    }

    await manifest.from('plants').destroy(plantUuid);

    res.status(200).json({ message: 'Plant deleted successfully' });
  } catch (error) {
    console.error('Delete plant error:', error);
    res.status(500).json({ message: 'Internal server error' });
  }
};
