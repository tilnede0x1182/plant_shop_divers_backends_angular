const bcrypt = require('bcrypt');
const { generateUserToken, ADMIN_ENTITY_SLUG, USER_ENTITY_SLUG } = require('./tokenUtils');
const { findUserByEmail, findAdminByEmail } = require('./db');

async function findAccountByEmail(email) {
  const admin = await findAdminByEmail(email);
  if (admin) {
    return {
      ...admin,
      admin: true,
      entitySlug: ADMIN_ENTITY_SLUG
    };
  }

  const user = await findUserByEmail(email);
  if (user) {
    return {
      ...user,
      admin: !!user.admin,
      entitySlug: USER_ENTITY_SLUG
    };
  }

  return null;
}

module.exports = async (req, res, manifest) => {
  console.log('🔥 authLogin handler called!', req.body);
  try {
    const { email, password } = req.body;
    console.log('📧 Email:', email, 'Password:', password ? '***' : 'missing');

    if (!email || !password) {
      console.log('❌ Email or password missing');
      return res.status(400).json({ message: 'Email and password required' });
    }

    // Find account by email via direct SQL (bypass Manifest SDK filtering)
    console.log('🔍 Searching for account (user/admin) via SQL:', email);
    const account = await findAccountByEmail(email);
    console.log('👤 Account found:', account ? `ID ${account.id} (${account.entitySlug})` : 'NOT FOUND');
    if (!account) {
      return res.status(401).json({ message: 'Invalid credentials' });
    }

    // Verify password
    console.log('🔐 Verifying password...');
    const isValidPassword = await bcrypt.compare(password, account.password);
    console.log('🔐 Password valid:', isValidPassword);
    if (!isValidPassword) {
      return res.status(401).json({ message: 'Invalid credentials' });
    }

    // Generate JWT token
    console.log('🎫 Generating JWT token...');
    const token = generateUserToken(account, { entitySlug: account.entitySlug });
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
        id: account.id,
        email: account.email,
        name: account.name,
        admin: account.admin
      }
    });
  } catch (error) {
    console.error('❌ Login error:', error.message);
    console.error('❌ Stack:', error.stack);
    res.status(500).json({ message: 'Internal server error' });
  }
};
