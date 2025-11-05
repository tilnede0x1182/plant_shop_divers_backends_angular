module.exports = async (req, res, manifest) => {
  try {
    const { id } = req.params;
    const updates = {};

    // Only include fields that are provided
    if (req.body.name !== undefined) updates.name = req.body.name;
    if (req.body.price !== undefined) updates.price = parseFloat(req.body.price);
    if (req.body.stock !== undefined) updates.stock = parseInt(req.body.stock);
    if (req.body.description !== undefined) updates.description = req.body.description;

    const plant = await manifest.from('plants').patch(parseInt(id), updates);

    if (!plant) {
      return res.status(404).json({ message: 'Plant not found' });
    }

    res.status(200).json(plant);
  } catch (error) {
    console.error('Update plant error:', error);
    res.status(500).json({ message: 'Internal server error' });
  }
};
