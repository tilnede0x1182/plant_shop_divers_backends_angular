module.exports = async (req, res, manifest) => {
  try {
    const { id } = req.params;

    const plant = await manifest.from('plants').findOneById(parseInt(id));

    if (!plant) {
      return res.status(404).json({ message: 'Plant not found' });
    }

    res.status(200).json(plant);
  } catch (error) {
    console.error('Get plant by ID error:', error);
    res.status(500).json({ message: 'Internal server error' });
  }
};
