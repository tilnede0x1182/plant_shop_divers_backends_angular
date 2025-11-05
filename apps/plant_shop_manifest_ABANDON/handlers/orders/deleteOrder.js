module.exports = async (req, res, manifest) => {
  try {
    const { id } = req.params;

    // Admin only - enforced by policy
    // Note: OrderItems will be deleted automatically by cascade
    await manifest.from('orders').destroy(parseInt(id));

    res.status(200).json({ message: 'Order deleted successfully' });
  } catch (error) {
    console.error('Delete order error:', error);
    res.status(500).json({ message: 'Internal server error' });
  }
};
