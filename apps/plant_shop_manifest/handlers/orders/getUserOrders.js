const { listOrdersForUser, listOrderItemsWithPlants, serializeOrder, findUserIdByTrueId } = require('../auth/db');
const { getUserFromToken } = require('../auth/tokenUtils');

module.exports = async (req, res) => {
  try {
    const currentUser = getUserFromToken(req);
    if (!currentUser) {
      return res.status(401).json({ message: 'Unauthorized' });
    }

    // Convertir true_id en id INTEGER
    const userId = await findUserIdByTrueId(currentUser.id);
    if (!userId) {
      return res.status(404).json({ message: 'User not found' });
    }

    const ordersRows = await listOrdersForUser(userId);

    const orders = [];
    for (const orderRow of ordersRows) {
      const itemsRows = await listOrderItemsWithPlants(orderRow.id);
      orders.push(serializeOrder(orderRow, itemsRows));
    }

    res.status(200).json(orders);
  } catch (error) {
    console.error('Get user orders error:', error);
    res.status(500).json({ message: 'Internal server error' });
  }
};
