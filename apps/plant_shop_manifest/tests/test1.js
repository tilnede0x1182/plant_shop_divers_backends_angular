const bcrypt = require('bcryptjs');

const API = 'http://localhost:4120/api';
const email = 'admin1@planteshop.com';
const password = 'password';
const hash = bcrypt.hashSync(password, 10);
console.log('Hash à insérer manuellement dans la base :', hash);

async function main() {
  // Test login Manifest standard
  const loginRes = await fetch(`${API}/auth/users/login`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ email, password }),
  });
  console.log('POST /auth/users/login', loginRes.status);
  const loginJson = await loginRes.json();
  console.log(loginJson);

  if (!loginRes.ok) {
    console.error('Échec login. Vérifiez email, mot de passe et accès BDD.');
    process.exit(1);
  }

  const token = loginJson.token;
  if (!token) {
    console.error('Token JWT absent de la réponse.');
    process.exit(1);
  }

  // Test accès /auth/users/me
  const meRes = await fetch(`${API}/auth/users/me`, {
    method: 'GET',
    headers: { Authorization: `Bearer ${token}` },
  });
  console.log('GET /auth/users/me', meRes.status);
  const meJson = await meRes.json();
  console.log(meJson);

  if (!meRes.ok) {
    console.error("Impossible d'accéder à /me avec le token obtenu.");
    process.exit(1);
  }

  console.log('Test réussi : Manifest répond et la BDD est bien connectée.');
}

main();
