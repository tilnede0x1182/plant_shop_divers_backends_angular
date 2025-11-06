const bcrypt = require('bcrypt');

module.exports = async (req, res, manifest) => {
  console.log('🔥 authLogin handler called!', req.body);
  try {
    const { email, password } = req.body;

    if (!email || !password) {
      return res.status(400).json({ message: 'Email and password required' });
    }

    // Find user by email
    const user = await manifest
      .from('User')
      .where([{ email: { '=': email } }])
      .findOne();

    if (!user) {
      return res.status(401).json({ message: 'Invalid credentials' });
    }

    // Verify password
    const isValidPassword = await bcrypt.compare(password, user.password);
    if (!isValidPassword) {
      return res.status(401).json({ message: 'Invalid credentials' });
    }

    // Generate JWT token
    const token = manifest.auth.generateToken({
      id: user.id,
      email: user.email,
      admin: user.admin
    });

    // Set cookie
    res.cookie('jwt', token, {
      httpOnly: true,
      secure: process.env.NODE_ENV === 'production',
      sameSite: 'strict',
      maxAge: 24 * 60 * 60 * 1000 // 24 hours
    });

    // Return user info (without password)
    res.status(201).json({
      user: {
        id: user.id,
        email: user.email,
        name: user.name,
        admin: user.admin
      }
    });
  } catch (error) {
    console.error('Login error:', error);
    res.status(500).json({ message: 'Internal server error' });
  }
};
