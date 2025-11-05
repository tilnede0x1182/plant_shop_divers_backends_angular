module.exports = async (req, res, manifest) => {
  try {
    // User is already authenticated by the policy
    // req.authenticable contains the authenticated user
    const userId = req.authenticable?.id;

    if (!userId) {
      return res.status(401).json({ message: 'Not authenticated' });
    }

    // Get user info
    const user = await manifest
      .from('users')
      .findOneById(userId);

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
    console.error('Get current user error:', error);
    res.status(500).json({ message: 'Internal server error' });
  }
};
