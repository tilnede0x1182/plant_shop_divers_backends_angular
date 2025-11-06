const path = require('path');
const dotenv = require('dotenv');
const jwt = require('jsonwebtoken');

dotenv.config({ path: path.resolve(__dirname, '../../.env') });

const TOKEN_SECRET =
  process.env.TOKEN_SECRET_KEY ||
  process.env.JWT_SECRET ||
  'REPLACE_ME';
const USER_ENTITY_SLUG =
  process.env.USER_ENTITY_SLUG ||
  process.env.MANIFEST_USER_SLUG ||
  'users';
const ADMIN_ENTITY_SLUG =
  process.env.ADMIN_ENTITY_SLUG ||
  process.env.MANIFEST_ADMIN_SLUG ||
  'admins';

function generateUserToken(user, options = {}) {
  if (!user?.email) {
    throw new Error('Cannot generate token without user email');
  }

  if (!TOKEN_SECRET || TOKEN_SECRET === 'REPLACE_ME') {
    console.warn('[auth] TOKEN_SECRET_KEY is missing or default.');
  }

  const entitySlug =
    options.entitySlug || (user.admin ? ADMIN_ENTITY_SLUG : USER_ENTITY_SLUG);

  return jwt.sign(
    {
      email: user.email,
      entitySlug,
      id: user.id,
      admin: !!user.admin
    },
    TOKEN_SECRET,
    { expiresIn: '1d' }
  );
}

module.exports = {
  generateUserToken,
  USER_ENTITY_SLUG,
  ADMIN_ENTITY_SLUG
};
