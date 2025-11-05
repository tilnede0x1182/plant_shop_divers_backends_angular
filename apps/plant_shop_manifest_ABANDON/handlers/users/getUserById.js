module.exports = async (req, res, manifest) => {
  try {
    const { id } = req.params;
    const currentUser = req.authenticable;

    // Check if user is accessing their own profile or is an admin
    if (currentUser.id !== parseInt(id) && !currentUser.admin) {
      return res.status(403).json({ message: 'Forbidden' });
    }

    const user = await manifest.from('users').findOneById(parseInt(id));

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
    console.error('Get user by ID error:', error);
    res.status(500).json({ message: 'Internal server error' });
  }
};
