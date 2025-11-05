module.exports = async (req, res, manifest) => {
  try {
    const currentUser = req.authenticable;

    // Get orders for the current user with related items and plants
    const orders = await manifest
      .from('orders')
      .where([{ userId: currentUser.id }])
      .with(['orderItems.plant'])
      .find();

    res.status(200).json(orders);
  } catch (error) {
    console.error('Get user orders error:', error);
    res.status(500).json({ message: 'Internal server error' });
  }
};
