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

function generateUserToken(user) {
  if (!user?.email) {
    throw new Error('Cannot generate token without user email');
  }

  if (!TOKEN_SECRET || TOKEN_SECRET === 'REPLACE_ME') {
    console.warn('[auth] TOKEN_SECRET_KEY is missing or default.');
  }

  return jwt.sign(
    {
      email: user.email,
      entitySlug: USER_ENTITY_SLUG,
      id: user.id,
      admin: !!user.admin
    },
    TOKEN_SECRET,
    { expiresIn: '1d' }
  );
}

module.exports = {
  generateUserToken
};
