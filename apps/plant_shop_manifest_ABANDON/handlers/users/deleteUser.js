module.exports = async (req, res, manifest) => {
  try {
    const { id } = req.params;

    // Admin only - enforced by policy
    await manifest.from('users').destroy(parseInt(id));

    res.status(200).json({ message: 'User deleted successfully' });
  } catch (error) {
    console.error('Delete user error:', error);
    res.status(500).json({ message: 'Internal server error' });
  }
};
