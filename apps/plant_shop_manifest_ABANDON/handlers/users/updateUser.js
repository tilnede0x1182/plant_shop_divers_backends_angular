module.exports = async (req, res, manifest) => {
  try {
    const { id } = req.params;
    const currentUser = req.authenticable;
    const updates = {};

    // Check if user is updating their own profile or is an admin
    const isOwner = currentUser.id === parseInt(id);
    const isAdmin = currentUser.admin === true;

    if (!isOwner && !isAdmin) {
      return res.status(403).json({ message: 'Forbidden' });
    }

    // Only include fields that are provided
    if (req.body.name !== undefined) updates.name = req.body.name;
    if (req.body.email !== undefined) updates.email = req.body.email;

    // Only admins can change admin status
    if (req.body.admin !== undefined && isAdmin) {
      updates.admin = req.body.admin;
    }

    const user = await manifest.from('users').patch(parseInt(id), updates);

    if (!user) {
      return res.status(404).json({ message: 'User not found' });
    }

    // Return user info (without password)
    res.status(200).json({
      id: user.id,
      email: user.email,
      name: user.name,
      admin: user.admin
    });
  } catch (error) {
    console.error('Update user error:', error);
    res.status(500).json({ message: 'Internal server error' });
  }
};
