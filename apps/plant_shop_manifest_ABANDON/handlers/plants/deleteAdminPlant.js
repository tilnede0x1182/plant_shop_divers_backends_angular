module.exports = async (req, res, manifest) => {
  try {
    const { id } = req.params;

    await manifest.from('plants').destroy(parseInt(id));

    res.status(200).json({ message: 'Plant deleted successfully' });
  } catch (error) {
    console.error('Delete plant error:', error);
    res.status(500).json({ message: 'Internal server error' });
  }
};
