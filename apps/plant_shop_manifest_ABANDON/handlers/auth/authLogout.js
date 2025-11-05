module.exports = async (req, res, manifest) => {
  try {
    // Clear the JWT cookie
    res.clearCookie('jwt');

    res.status(200).json({ message: 'Logged out successfully' });
  } catch (error) {
    console.error('Logout error:', error);
    res.status(500).json({ message: 'Internal server error' });
  }
};
