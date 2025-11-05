module.exports = async (req, res, manifest) => {
  try {
    const { id } = req.params;
    const currentUser = req.authenticable;

    // Get order with related items and plants
    const order = await manifest
      .from('orders')
      .with(['orderItems.plant'])
      .findOneById(parseInt(id));

    if (!order) {
      return res.status(404).json({ message: 'Order not found' });
    }

    // Check if user owns the order or is admin
    if (order.userId !== currentUser.id && !currentUser.admin) {
      return res.status(403).json({ message: 'Forbidden' });
    }

    res.status(200).json(order);
  } catch (error) {
    console.error('Get order by ID error:', error);
    res.status(500).json({ message: 'Internal server error' });
  }
};
