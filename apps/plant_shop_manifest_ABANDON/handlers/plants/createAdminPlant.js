module.exports = async (req, res, manifest) => {
  try {
    const { name, price, stock, description } = req.body;

    if (!name || price === undefined || stock === undefined) {
      return res.status(400).json({ message: 'Name, price, and stock are required' });
    }

    const plant = await manifest.from('plants').create({
      name,
      price: parseFloat(price),
      stock: parseInt(stock),
      description: description || null
    });

    res.status(201).json(plant);
  } catch (error) {
    console.error('Create plant error:', error);
    res.status(500).json({ message: 'Internal server error' });
  }
};
