const {
  findPlantByTrueId,
  findPlantUuidByTrueId,
  findOrderByUuid,
  listOrderItemsWithPlants,
  serializeOrder
} = require('../auth/db');
const { getUserFromToken } = require('../auth/tokenUtils');

module.exports = async (req, res, manifest) => {
  try {
    const currentUser = getUserFromToken(req);
    if (!currentUser) {
      return res.status(401).json({ message: 'Unauthorized' });
    }

    const { items } = req.body;

    if (!items || !Array.isArray(items) || items.length === 0) {
      return res.status(400).json({ message: 'Items array is required' });
    }

    // Validate items format
    for (const item of items) {
      if (!item.plantId || !item.quantity || item.quantity < 1) {
        return res.status(400).json({ message: 'Each item must have plantId and quantity >= 1' });
      }
    }

    // Create the order
    const order = await manifest.from('orders').create({
      userId: currentUser.id,
      status: 'pending',
      totalPrice: 0
    });

    let totalPrice = 0;
    const orderItems = [];

    // Create order items and calculate total
    for (const item of items) {
      const plantUuid = await findPlantUuidByTrueId(item.plantId);
      if (!plantUuid) {
        await manifest.from('orders').delete(order.id);
        return res.status(400).json({ message: `Plant with ID ${item.plantId} not found` });
      }

      const plant = await findPlantByTrueId(item.plantId);

      if (!plant) {
        // Rollback: delete the order
        await manifest.from('orders').delete(order.id);
        return res.status(400).json({ message: `Plant with ID ${item.plantId} not found` });
      }

      const plantPrice = Number(plant.price);
      const plantStock = Number(plant.stock);

      if (plantStock < item.quantity) {
        // Rollback: delete the order
        await manifest.from('orders').delete(order.id);
        return res.status(400).json({ message: `Insufficient stock for plant: ${plant.name}` });
      }

      // Create order item
      const orderItem = await manifest.from('order-items').create({
        orderId: order.id,
        plantId: plantUuid,
        quantity: item.quantity
      });

      // Update plant stock
      await manifest.from('plants').patch(plantUuid, {
        stock: plantStock - item.quantity
      });

      totalPrice += plantPrice * item.quantity;
      orderItems.push(orderItem);
    }

    // Update order with total price
    await manifest.from('orders').patch(order.id, {
      totalPrice
    });

    // Get complete order with items and plants via SQL helpers
    const orderRow = await findOrderByUuid(order.id);
    const itemsRows = await listOrderItemsWithPlants(order.id);
    const completeOrder = serializeOrder(orderRow, itemsRows);

    res.status(201).json(completeOrder);
  } catch (error) {
    console.error('Create order error:', error);
    res.status(500).json({ message: 'Internal server error' });
  }
};
