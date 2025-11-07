const { pool, findUserIdByTrueId } = require('../auth/db');

module.exports = async (req, res, manifest) => {
  try {
    const { id } = req.params;
    const numericId = Number(id);
    if (!Number.isInteger(numericId)) {
      return res.status(400).json({ message: 'Invalid user id' });
    }

    const userId = await findUserIdByTrueId(numericId);
    if (!userId) {
      return res.status(404).json({ message: 'User not found' });
    }

    // Delete order items for all orders of this user
    await pool.query(
      'DELETE FROM "order_item" WHERE "orderId" IN (SELECT id FROM "order" WHERE "userId" = $1)',
      [userId]
    );

    // Delete all orders of this user
    await pool.query('DELETE FROM "order" WHERE "userId" = $1', [userId]);

    // Delete the user
    await pool.query('DELETE FROM "user" WHERE id = $1', [userId]);

    res.status(200).json({ message: 'User deleted successfully' });
  } catch (error) {
    console.error('Delete user error:', error);
    res.status(500).json({ message: 'Internal server error' });
  }
};
