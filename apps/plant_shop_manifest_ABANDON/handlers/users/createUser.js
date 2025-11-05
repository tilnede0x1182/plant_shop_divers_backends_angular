const bcrypt = require('bcrypt');

module.exports = async (req, res, manifest) => {
  try {
    const { email, password, name, admin } = req.body;

    if (!email || !password || !name) {
      return res.status(400).json({ message: 'Email, password, and name required' });
    }

    // Check if user already exists
    const existingUser = await manifest
      .from('users')
      .where([{ email }])
      .findOne();

    if (existingUser) {
      return res.status(400).json({ message: 'User already exists' });
    }

    // Hash password
    const hashedPassword = await bcrypt.hash(password, 10);

    // Create user
    const user = await manifest.from('users').create({
      email,
      name,
      password: hashedPassword,
      admin: admin === true
    });

    // Return user info (without password)
    res.status(201).json({
      id: user.id,
      email: user.email,
      name: user.name,
      admin: user.admin
    });
  } catch (error) {
    console.error('Create user error:', error);
    res.status(500).json({ message: 'Internal server error' });
  }
};
