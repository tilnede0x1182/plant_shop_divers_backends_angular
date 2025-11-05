module.exports = async (req, res, manifest) => {
  try {
    // Get all users (admin only - enforced by policy)
    const users = await manifest.from('users').find();

    // Remove passwords from response
    const sanitizedUsers = users.map(user => ({
      id: user.id,
      email: user.email,
      name: user.name,
      admin: user.admin,
      createdAt: user.createdAt
    }));

    res.status(200).json(sanitizedUsers);
  } catch (error) {
    console.error('Get admin users error:', error);
    res.status(500).json({ message: 'Internal server error' });
  }
};
