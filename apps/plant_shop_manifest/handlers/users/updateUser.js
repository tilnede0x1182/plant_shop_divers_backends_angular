const { pool, findUserIdByTrueId, findUserByTrueId, findAdminById, serializeUser } = require('../auth/db');
const { getUserFromToken, generateUserToken } = require('../auth/tokenUtils');

module.exports = async (req, res, manifest) => {
  try {
    const { id } = req.params;
    const numericId = Number(id);

    console.log('🔧 updateUser called for user ID:', numericId);
    console.log('🔧 Request headers:', JSON.stringify(req.headers, null, 2));

    const currentUser = getUserFromToken(req);
    console.log('🔧 Current user from token:', currentUser);

    if (!currentUser) {
      console.log('❌ No current user found - returning 401');
      return res.status(401).json({ message: 'Unauthorized' });
    }

    // Trouver l'id INTEGER via true_id
    const userId = await findUserIdByTrueId(numericId);
    if (!userId) {
      return res.status(404).json({ message: 'User not found' });
    }

    // Un utilisateur normal ne peut modifier que son propre profil
    // Un admin peut modifier n'importe quel profil
    if (!currentUser.admin && currentUser.id !== numericId) {
      return res.status(403).json({ message: 'Forbidden: you can only modify your own profile' });
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

    // Fetch updated user data
    let fresh;
    if (currentUser.entitySlug === 'admins') {
      // Admin updated - check if it's updating themselves
      if (currentUser.id === numericId) {
        // Admin can't update users through this endpoint, this shouldn't happen
        // But if it does, fetch admin data
        fresh = await findAdminById(currentUser.id);
      } else {
        // Admin updating a user
        fresh = await findUserByTrueId(numericId);
      }
    } else {
      // Regular user
      fresh = await findUserByTrueId(numericId);
    }

    // If the current user updated their own profile, regenerate JWT with new data
    if (currentUser.id === numericId) {
      // Prepare user object for JWT with true_id as id
      const userForToken = {
        id: numericId, // Use true_id
        email: fresh.email,
        admin: fresh.admin
      };

      const newToken = generateUserToken(userForToken, {
        entitySlug: currentUser.entitySlug
      });

      res.cookie('jwt', newToken, {
        httpOnly: true,
        secure: process.env.NODE_ENV === 'production',
        sameSite: 'strict',
        maxAge: 24 * 60 * 60 * 1000 // 24 hours
      });

      console.log('🔄 User updated their own profile - JWT regenerated with new email:', fresh.email);
    }

    res.status(200).json(serializeUser(fresh));
  } catch (error) {
    console.error('Update user error:', error);
    res.status(500).json({ message: 'Internal server error' });
  }
};
