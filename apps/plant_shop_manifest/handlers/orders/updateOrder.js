const { findOrderUuidByTrueId, findOrderByUuid, listOrderItemsWithPlants, serializeOrder } = require('../auth/db');

/**
 * Handler mise à jour commande
 * @param {Object} req Requête HTTP
 * @param {Object} res Réponse HTTP
 * @param {Object} manifest Instance Manifest
 */
module.exports = async (req, res, manifest) => {
  try {
    const { id } = req.params;
    const numericId = Number(id);
    if (!Number.isInteger(numericId)) {
      return res.status(400).json({ message: 'Invalid order id' });
    }
    const orderUuid = await findOrderUuidByTrueId(numericId);
    if (!orderUuid) {
      return res.status(404).json({ message: 'Order not found' });
    }
    const updates = {};

    // Only include fields that are provided
    if (req.body.status !== undefined) updates.status = req.body.status;
    if (req.body.totalPrice !== undefined) updates.totalPrice = parseFloat(req.body.totalPrice);

    const order = await manifest.from('orders').patch(orderUuid, updates);

    if (!order) {
      return res.status(404).json({ message: 'Order not found' });
    }

    const orderRow = await findOrderByUuid(orderUuid);
    const itemsRows = await listOrderItemsWithPlants(orderUuid);

    res.status(200).json(serializeOrder(orderRow, itemsRows));
  } catch (error) {
    console.error('Update order error:', error);
    res.status(500).json({ message: 'Internal server error' });
  }
};
