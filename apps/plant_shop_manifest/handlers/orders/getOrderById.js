const {
  findOrderUuidByTrueId,
  findOrderByUuid,
  listOrderItemsWithPlants,
  serializeOrder,
  findUserIdByTrueId
} = require('../auth/db');
const { getUserFromToken } = require('../auth/tokenUtils');

/**
 * Handler récupération commande par id
 * @param {Object} req Requête HTTP
 * @param {Object} res Réponse HTTP
 */
module.exports = async (req, res) => {
  try {
    const { id } = req.params;
    const currentUser = getUserFromToken(req);
    if (!currentUser) {
      return res.status(401).json({ message: 'Unauthorized' });
    }

    // Convertir true_id en id INTEGER
    const userId = await findUserIdByTrueId(currentUser.id);
    if (!userId) {
      return res.status(404).json({ message: 'User not found' });
    }

    const numericId = Number(id);
    if (!Number.isInteger(numericId)) {
      return res.status(400).json({ message: 'Invalid order id' });
    }

    const orderUuid = await findOrderUuidByTrueId(numericId);

    if (!orderUuid) {
      return res.status(404).json({ message: 'Order not found' });
    }

    const orderRow = await findOrderByUuid(orderUuid);

    if (!orderRow) {
      return res.status(404).json({ message: 'Order not found' });
    }

    // Check if user owns the order or is admin
    if (orderRow.userId !== userId && !currentUser.admin) {
      return res.status(403).json({ message: 'Forbidden' });
    }

    const itemsRows = await listOrderItemsWithPlants(orderUuid);
    const order = serializeOrder(orderRow, itemsRows);

    res.status(200).json(order);
  } catch (error) {
    console.error('Get order by ID error:', error);
    res.status(500).json({ message: 'Internal server error' });
  }
};
