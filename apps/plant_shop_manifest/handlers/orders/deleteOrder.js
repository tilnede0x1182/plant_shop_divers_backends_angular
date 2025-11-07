const { findOrderUuidByTrueId, deleteOrderItemsByOrder } = require('../auth/db');

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

    await deleteOrderItemsByOrder(orderUuid);
    await manifest.from('orders').delete(orderUuid);

    res.status(200).json({ message: 'Order deleted successfully' });
  } catch (error) {
    console.error('Delete order error:', error);
    res.status(500).json({ message: 'Internal server error' });
  }
};
