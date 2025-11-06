const {
  findOrderUuidByTrueId,
  findOrderByUuid,
  listOrderItemsWithPlants,
  serializeOrder
} = require('../auth/db');

module.exports = async (req, res) => {
  try {
    const { id } = req.params;
    const currentUser = req.authenticable;
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
    if (orderRow.userId !== currentUser.id && !currentUser.admin) {
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
