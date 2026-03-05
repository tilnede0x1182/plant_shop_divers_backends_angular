const {
  findPlantByUuid,
  serializePlant
} = require('../auth/db');

/**
 * Handler création plante (admin)
 * @param {Object} req Requête HTTP
 * @param {Object} res Réponse HTTP
 * @param {Object} manifest Instance Manifest
 */
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
    const plantRow = await findPlantByUuid(plant.id);
    if (!plantRow) {
      return res.status(201).json({
        id: null,
        uuid: plant.id,
        name: plant.name,
        price: plant.price,
        stock: plant.stock,
        description: plant.description || null
      });
    }

    res.status(201).json(serializePlant(plantRow));
  } catch (error) {
    console.error('Create plant error:', error);
    res.status(500).json({ message: 'Internal server error' });
  }
};
