/* ------- Variables globales -------- */
const cookieJars = {
  admin: '',
  user: '',
};
const maintenant = new Date()
  .toISOString()
  .replace(/[^0-9]/g, '')
  .slice(0, 14);

/* ---------- configuration ---------- */
const config = {
  apiBaseUrl: process.env.API_BASE_URL || 'http://localhost:4100/api',
  logLevel: 'verbose', // 'silent', 'normal', 'verbose'
  adminEmail: process.env.ADMIN_EMAIL || 'admin1@planteshop.com',
  adminPassword: process.env.ADMIN_PASSWORD || 'password',
};

/* ---------- utilitaires HTTP ---------- */
async function hit(method, route, expectedStatus, body, who = 'default') {
  const url = `${config.apiBaseUrl}${route}`;
  const label = `${method} ${route}`;

  try {
    const res = await fetch(url, {
      method,
      headers: {
        'Content-Type': 'application/json',
        ...(cookieJars[who] ? { Cookie: cookieJars[who] } : {}),
      },
      body: body ? JSON.stringify(body) : undefined,
    });

    /* --- Mettre à jour le cookie si Set-Cookie présent --- */
    const setCookie = res.headers.get('set-cookie');
    if (setCookie) {
      cookieJars[who] = setCookie.split(',')[0].split(';')[0];
    }

    const success = res.status === expectedStatus;
    if (config.logLevel !== 'silent') {
      console.log(`${success ? '✅' : '❌'} ${label} [${res.status}]`);
    }
    if (!success) {
      const txt = await res.text();
      throw new Error(
        `API ${label} → ${res.status} (attendu ${expectedStatus})\n${txt}`
      );
    }

    try {
      return await res.json();
    } catch {
      return {};
    }
  } catch (err) {
    if (err.message.includes('fetch failed')) {
      console.error(`❌ Connection error: ${url} - API down ?`);
    }
    throw err;
  }
}

/* ---------- assertions ---------- */
function assertEq(obj, key, expected) {
  if (!obj) throw new Error(`Objet vide – clé ${key} recherchée`);
  const actual = obj[key];
  const ok = actual === expected;
  if (config.logLevel !== 'silent') {
    console.log(
      `${ok ? '✅' : '❌'}   ↳ ${key}=${actual} (attendu ${expected})`
    );
  }
  if (!ok)
    throw new Error(
      `Assertion failed: ${key} = ${actual}, expected ${expected}`
    );
}

/* --- nouvelles assertions génériques --- */
function assertNumericId(id, label) {
  if (!/^\d+$/.test(String(id)))
    throw new Error(`${label} doit être un identifiant numérique, reçu ${id}`);
}

function assertSortedAscByField(arr, field, label) {
  if (arr.length < 2) return;
  for (let i = 1; i < arr.length; i++) {
    if (String(arr[i - 1][field]).localeCompare(String(arr[i][field])) > 0)
      throw new Error(`Liste ${label} non triée croissant par ${field}`);
  }
}

function assertAdminsFirstThenName(arr) {
  if (arr.length < 2) return;
  let foundNonAdmin = false;
  for (let i = 0; i < arr.length; i++) {
    const cur = arr[i];
    if (!cur.hasOwnProperty('admin'))
      throw new Error('Objet user sans champ admin');
    if (foundNonAdmin && cur.admin)
      throw new Error('Admins doivent précéder les non-admins');
    if (!cur.admin) foundNonAdmin = true;

    /* tri alphabétique à l’intérieur de chaque groupe */
    if (i > 0 && cur.admin === arr[i - 1].admin) {
      if (String(arr[i - 1].name).localeCompare(String(cur.name)) > 0)
        throw new Error('Tri alphabétique ascendant incorrect');
    }
  }
}

/* ------------ helpers  ------------ */
async function login(email, password, who = 'default') {
  await hit('POST', '/auth/login', 201, { email, password }, who);
  return true; // cookie stocké dans cookieJars[who]
}

async function registerUser(name, email, password, who = 'default') {
  await hit('POST', '/auth/register', 201, { name, email, password }, who);
  return true;
}

async function findUserIdByEmail(who, email) {
  const users = await hit('GET', '/users', 200, null, who);
  const u = users.find((usr) => usr.email === email);
  if (!u) throw new Error(`User ${email} not found in admin list`);
  return u.id;
}

/* ---------- modules de test ---------- */
async function testPlants(who = 'admin') {
  console.log('\n📌 TEST MODULE: PLANTS (admin)');
  const plantData = { name: 'Test Plant', price: 10, stock: 5 };

  /* --- Création --- */
  const { id: plantId } = await hit(
    'POST',
    '/admin/plants',
    201,
    plantData,
    who
  );
  assertNumericId(plantId, 'plantId');

  /* --- Lecture publique --- */
  assertEq(
    await hit('GET', `/plants/${plantId}`, 200, null, who),
    'name',
    plantData.name
  );

  /* --- Mise à jour --- */
  await hit('PATCH', `/admin/plants/${plantId}`, 200, { price: 15 }, who);
  assertEq(await hit('GET', `/plants/${plantId}`, 200, null, who), 'price', 15);

  /* --- Suppression --- */
  await hit('DELETE', `/admin/plants/${plantId}`, 200, null, who);
  return { success: true };
}

async function testUsers(who = 'admin') {
  console.log('\n📌 TEST MODULE: USERS (admin)');
  const userData = {
    email: `utilisateur_test_${maintenant}@example.com`,
    name: `Utilisateur de test ${maintenant}`,
    password: 'pass123',
  };
  const { id: userId } = await hit('POST', '/users', 201, userData, who);

  await hit('PATCH', `/users/${userId}`, 200, { name: 'Tester Update' }, who);
  assertEq(
    await hit('GET', `/users/${userId}`, 200, null, who),
    'name',
    'Tester Update'
  );

  await hit('DELETE', `/users/${userId}`, 200, null, who);
  return { success: true };
}

async function testOrders(adminWho = 'admin', userWho = 'user') {
  console.log('\n📌 TEST MODULE: ORDERS & ORDER ITEMS');

  /* --- Création plante --- */
  const plantData = {
    name: `Plante_de_test_${maintenant}`,
    price: 10,
    stock: 5,
  };
  const { id: plantId } = await hit(
    'POST',
    '/admin/plants',
    201,
    plantData,
    adminWho
  );
  assertNumericId(plantId, 'plantId');

  /* --- Commande user --- */
  const orderPayload = { items: [{ plantId, quantity: 2 }] };
  const { id: orderId } = await hit(
    'POST',
    '/orders',
    201,
    orderPayload,
    userWho
  );

  /* --- Admin change statut --- */
  await hit(
    'PATCH',
    `/orders/${orderId}`,
    200,
    { status: 'shipped' },
    adminWho
  );

  /* --- Vérification user --- */
  const commandes = await hit('GET', '/orders', 200, null, userWho);
  const commande = commandes.find((o) => o.id === orderId);
  if (!commande) throw new Error(`Commande ${orderId} introuvable`);
  assertEq(commande, 'status', 'shipped');
  if (!commande.orderItems || !commande.orderItems.length)
    throw new Error('Items absents dans la commande');
  assertEq(commande.orderItems[0].plant, 'name', plantData.name);

  /* --- Nettoyage --- */
  await hit('DELETE', `/orders/${orderId}`, 200, null, adminWho);
  await hit('DELETE', `/admin/plants/${plantId}`, 200, null, adminWho);

  return { success: true };
}

async function testUserProfile(
  adminWho = 'admin',
  userWho = 'user',
  userEmail
) {
  console.log('\n📌 TEST MODULE: USER PROFILE (user)');
  const userId = await findUserIdByEmail(adminWho, userEmail);

  /* --- Lecture --- */
  assertEq(
    await hit('GET', `/users/${userId}`, 200, null, userWho),
    'id',
    userId
  );

  /* --- MAJ nom --- */
  const nouveauNom = `Utilisateur_de_test_${maintenant}`;
  await hit('PATCH', `/users/${userId}`, 200, { name: nouveauNom }, userWho);
  assertEq(
    await hit('GET', `/users/${userId}`, 200, null, userWho),
    'name',
    nouveauNom
  );

  /* --- Tentative élévation --- */
  await hit('PATCH', `/users/${userId}`, 200, { admin: true }, userWho);
  const profil = await hit('GET', `/users/${userId}`, 200, null, adminWho);
  assertEq(profil, 'admin', false);
}

async function testAuthRoles(adminWho = 'admin', userWho = 'user') {
  console.log('\n📌 TEST MODULE: ROLES');

  /* --- User essaye POST plante --- */
  await hit(
    'POST',
    '/admin/plants',
    403,
    { name: 'Bad', price: 1, stock: 1 },
    userWho
  );

  /* --- Admin OK puis suppr --- */
  const { id: pid } = await hit(
    'POST',
    '/admin/plants',
    201,
    { name: 'Good', price: 1, stock: 1 },
    adminWho
  );
  await hit('DELETE', `/admin/plants/${pid}`, 200, null, adminWho);

  /* --- User GET /users interdit --- */
  await hit('GET', '/users', 403, null, userWho);

  return { success: true };
}

async function testAdminPlants(who = 'admin') {
  console.log('\n📌 TEST MODULE: ADMIN PLANTS');
  const plantes = await hit('GET', '/admin/plants', 200, null, who);
  console.log(`   ↳ ${plantes.length} plantes récupérées`);
  assertSortedAscByField(plantes, 'name', 'plantes');

  /* --- CRUD rapide --- */
  const d = {
    name: `Plante_admin_de_test_${maintenant}`,
    price: 99,
    stock: 12,
  };
  const { id } = await hit('POST', '/admin/plants', 201, d, who);
  await hit('PATCH', `/admin/plants/${id}`, 200, { price: 123 }, who);
  await hit('DELETE', `/admin/plants/${id}`, 200, null, who);

  return { success: true };
}

async function testAdminUsers(who = 'admin') {
  console.log('\n📌 TEST MODULE: ADMIN USERS');

  // Étape 1 — Création d’un admin temporaire
  const adminTemp = {
    email: `admin_temp_${maintenant}@example.com`,
    name: `Admin Temporaire ${maintenant}`,
    password: 'password',
    admin: true,
  };
  const { id: adminId } = await hit('POST', '/users', 201, adminTemp, who);
  console.log(`   ↳ Admin temporaire créé: ${adminTemp.email}`);

  // Étape 2 — Vérification qu’on cible bien le bon admin
  const adminList = await hit('GET', '/admin/users', 200, null, who);
  const cible = adminList.find((a) => a.email === adminTemp.email);
  if (!cible)
    throw new Error('L’admin temporaire n’a pas été trouvé dans la liste !');
  console.log(`   ↳ Cible confirmée (${cible.email}, id=${cible.id})`);

  // Étape 3 — Mise à jour du nom de ce seul admin
  const nouveauNom = `Admin_temp_modifié_${maintenant}`;
  await hit('PATCH', `/users/${cible.id}`, 200, { name: nouveauNom }, who);
  assertEq(
    await hit('GET', `/users/${cible.id}`, 200, null, who),
    'name',
    nouveauNom
  );

  // Étape 3b — l'admin modifie le rôle d'un autre utilisateur
  await hit('PATCH', `/users/${cible.id}`, 200, { admin: false }, who);
  assertEq(
    await hit('GET', `/users/${cible.id}`, 200, null, who),
    'admin',
    false
  );

  // Étape 3c — l'admin restaure le rôle admin
  await hit('PATCH', `/users/${cible.id}`, 200, { admin: true }, who);
  assertEq(
    await hit('GET', `/users/${cible.id}`, 200, null, who),
    'admin',
    true
  );

  // Étape 4 — Suppression du compte temporaire
  await hit('DELETE', `/users/${cible.id}`, 200, null, who);
  console.log(`   ↳ Admin temporaire supprimé (${cible.email})`);

  console.log('✅ Test ADMIN USERS terminé sans modification d’admin seedé.');
  return { success: true };
}

async function testAuthMe(who = 'user') {
  console.log('\n📌 TEST MODULE: AUTH /me');
  const me = await hit('GET', '/auth/me', 200, null, who);
  if (!me || !me.email) throw new Error('Réponse invalide pour /auth/me');
  console.log(`   ↳ Utilisateur connecté: ${me.email} (${me.name || '??'})`);
}

/* ---------- exécution des tests ---------- */
async function main() {
  console.log(`🧪 Démarrage des tests: ${config.apiBaseUrl}\n`);

  try {
    /* --- Auth --- */
    await login(config.adminEmail, config.adminPassword, 'admin');
    const userEmail = `utilisateur_de_test_${maintenant}@example.com`;
    await registerUser('User', userEmail, 'pass123', 'user');
    await login(userEmail, 'pass123', 'user');

    /* --- Modules --- */
    await testPlants('admin');
    await testUsers('admin');
    await testOrders('admin', 'user');
    await testUserProfile('admin', 'user', userEmail);
    await testAuthRoles('admin', 'user');
    await testAdminPlants('admin');
    await testAdminUsers('admin');
    await testAuthMe('user');

    console.log('\n🎉 Tous les tests ont réussi!');
    return 0;
  } catch (err) {
    console.error(`\n❌ Tests interrompus: ${err.message}`);
    return 1;
  }
}

if (require.main === module) {
  main().then((code) => process.exit(code));
} else {
  module.exports = {
    runTests: main,
    utils: { hit, assertEq, login },
  };
}
