const bcrypt = require('bcrypt');

module.exports = async (req, res, manifest) => {
  console.log('🔥 authLogin handler called!', req.body);
  try {
    const { email, password } = req.body;
    console.log('📧 Email:', email, 'Password:', password ? '***' : 'missing');

    if (!email || !password) {
      console.log('❌ Email or password missing');
      return res.status(400).json({ message: 'Email and password required' });
    }

    // Find user by email
    console.log('🔍 Searching for user with email:', email);
    console.log('🔍 Where clause:', `email = ${email}`);
    const user = await manifest
      .from('User')
      .where(`email = ${email}`)
      .findOne();

    console.log('👤 User found:', user ? `ID ${user.id}` : 'NOT FOUND');
    if (!user) {
      return res.status(401).json({ message: 'Invalid credentials' });
    }

    // Verify password
    console.log('🔐 Verifying password...');
    const isValidPassword = await bcrypt.compare(password, user.password);
    console.log('🔐 Password valid:', isValidPassword);
    if (!isValidPassword) {
      return res.status(401).json({ message: 'Invalid credentials' });
    }

    // Generate JWT token
    console.log('🎫 Generating JWT token...');
    const token = manifest.auth.generateToken({
      id: user.id,
      email: user.email,
      admin: user.admin
    });
    console.log('🎫 Token generated:', token ? 'YES' : 'NO');

    // Set cookie
    res.cookie('jwt', token, {
      httpOnly: true,
      secure: process.env.NODE_ENV === 'production',
      sameSite: 'strict',
      maxAge: 24 * 60 * 60 * 1000 // 24 hours
    });

    console.log('✅ Login successful, sending response');
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
    console.error('❌ Login error:', error.message);
    console.error('❌ Stack:', error.stack);
    res.status(500).json({ message: 'Internal server error' });
  }
};
