module.exports = async (req, res, manifest) => {
  try {
    const plants = await manifest.from('plants').find();

    res.status(200).json(plants);
  } catch (error) {
    console.error('Get admin plants error:', error);
    res.status(500).json({ message: 'Internal server error' });
  }
};
