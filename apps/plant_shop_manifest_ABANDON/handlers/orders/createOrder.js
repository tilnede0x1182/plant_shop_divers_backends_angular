module.exports = async (req, res, manifest) => {
  try {
    const currentUser = req.authenticable;
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
      const plant = await manifest.from('plants').findOneById(item.plantId);

      if (!plant) {
        // Rollback: delete the order
        await manifest.from('orders').destroy(order.id);
        return res.status(400).json({ message: `Plant with ID ${item.plantId} not found` });
      }

      if (plant.stock < item.quantity) {
        // Rollback: delete the order
        await manifest.from('orders').destroy(order.id);
        return res.status(400).json({ message: `Insufficient stock for plant: ${plant.name}` });
      }

      // Create order item
      const orderItem = await manifest.from('order-items').create({
        orderId: order.id,
        plantId: plant.id,
        quantity: item.quantity
      });

      // Update plant stock
      await manifest.from('plants').patch(plant.id, {
        stock: plant.stock - item.quantity
      });

      totalPrice += plant.price * item.quantity;
      orderItems.push(orderItem);
    }

    // Update order with total price
    await manifest.from('orders').patch(order.id, {
      totalPrice
    });

    // Get complete order with items and plants
    const completeOrder = await manifest
      .from('orders')
      .with(['orderItems.plant'])
      .findOneById(order.id);

    res.status(201).json(completeOrder);
  } catch (error) {
    console.error('Create order error:', error);
    res.status(500).json({ message: 'Internal server error' });
  }
};
