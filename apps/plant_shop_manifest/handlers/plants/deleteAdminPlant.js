const { pool, deleteOrderItemsByPlant } = require('../auth/db');

module.exports = async (req, res, manifest) => {
  try {
    const { id } = req.params;
    const numericId = Number(id);
    if (!Number.isInteger(numericId)) {
      return res.status(400).json({ message: 'Invalid plant id' });
    }

    // Requête SQL directe pour trouver l'id (INTEGER) via true_id
    const { rows } = await pool.query(
      'SELECT id FROM "plant" WHERE true_id = $1 LIMIT 1',
      [numericId]
    );

    if (!rows[0]) {
      return res.status(404).json({ message: 'Plant not found' });
    }

    const plantId = rows[0].id;

    // Supprimer les order_items liés
    await deleteOrderItemsByPlant(plantId);

    // Utiliser l'API Manifest pour supprimer la plante
    await manifest.from('plants').delete(plantId);

    res.status(200).json({ message: 'Plant deleted successfully' });
  } catch (error) {
    console.error('Delete plant error:', error);
    res.status(500).json({ message: 'Internal server error' });
  }
};
