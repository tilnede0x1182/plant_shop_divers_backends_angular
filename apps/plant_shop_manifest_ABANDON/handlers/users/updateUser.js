const { pool, findUserIdByTrueId, findUserByTrueId, serializeUser } = require('../auth/db');
const { getUserFromToken } = require('../auth/tokenUtils');

module.exports = async (req, res, manifest) => {
  try {
    const { id } = req.params;
    const numericId = Number(id);
    const currentUser = getUserFromToken(req);

    if (!currentUser) {
      return res.status(401).json({ message: 'Unauthorized' });
    }

    // Trouver l'id INTEGER via true_id
    const userId = await findUserIdByTrueId(numericId);
    if (!userId) {
      return res.status(404).json({ message: 'User not found' });
    }

    const setClauses = [];
    const values = [];
    let paramIndex = 1;

    // Only include fields that are provided
    if (req.body.name !== undefined) {
      setClauses.push(`name = $${paramIndex++}`);
      values.push(req.body.name);
    }
    if (req.body.email !== undefined) {
      setClauses.push(`email = $${paramIndex++}`);
      values.push(req.body.email);
    }

    // Only admins can change admin status
    if (req.body.admin !== undefined && currentUser?.admin) {
      setClauses.push(`admin = $${paramIndex++}`);
      values.push(req.body.admin);
    }

    // If no fields to update, just return the current user
    if (setClauses.length === 0) {
      const fresh = await findUserByTrueId(numericId);
      return res.status(200).json(serializeUser(fresh));
    }

    // Add updatedAt
    setClauses.push(`"updatedAt" = NOW()`);

    // Add userId to values
    values.push(userId);

    // Update user directly via SQL
    await pool.query(
      `UPDATE "user" SET ${setClauses.join(', ')} WHERE id = $${paramIndex}`,
      values
    );

    const fresh = await findUserByTrueId(numericId);
    res.status(200).json(serializeUser(fresh));
  } catch (error) {
    console.error('Update user error:', error);
    res.status(500).json({ message: 'Internal server error' });
  }
};
