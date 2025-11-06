const { listOrdersForUser, listOrderItemsWithPlants, serializeOrder } = require('../auth/db');

module.exports = async (req, res) => {
  try {
    const currentUser = req.authenticable;
    const ordersRows = await listOrdersForUser(currentUser.id);

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
