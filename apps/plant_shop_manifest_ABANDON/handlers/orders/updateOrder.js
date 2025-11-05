module.exports = async (req, res, manifest) => {
  try {
    const { id } = req.params;
    const updates = {};

    // Only include fields that are provided
    if (req.body.status !== undefined) updates.status = req.body.status;
    if (req.body.totalPrice !== undefined) updates.totalPrice = parseFloat(req.body.totalPrice);

    const order = await manifest.from('orders').patch(parseInt(id), updates);

    if (!order) {
      return res.status(404).json({ message: 'Order not found' });
    }

    res.status(200).json(order);
  } catch (error) {
    console.error('Update order error:', error);
    res.status(500).json({ message: 'Internal server error' });
  }
};
