/**
 * test_cors_debug.js
 * ─────────────────────────────────────────────────────────────────────
 * 1. Simule exactement ce que fait le front Angular :
 *      • pré-requête OPTIONS avec Origin : http://localhost:8300
 *      • appel GET /api/auth/me avec Cookie (= JWT) et withCredentials
 * 2. Vérifie puis LOG tout ce qui concerne CORS :
 *      • Access-Control-Allow-Origin
 *      • Access-Control-Allow-Credentials
 *      • Access-Control-Allow-Headers / Methods
 * 3. Montre aussi le comportement quand l’Origin n’est **pas** autorisée
 * --------------------------------------------------------------------
 *  USAGE :  node test_cors_debug.js
 *           (Node ≥18 ou `npm i node-fetch`)
 * --------------------------------------------------------------------
 */

if (typeof fetch === 'undefined')
  global.fetch = (...a) =>
    import('node-fetch').then(({ default: f }) => f(...a));

/* ---------- CONFIG -------------------------------------------------- */
const CFG = {
  api: process.env.API_BASE_URL || 'http://localhost:4100/api',
  originOk: 'http://localhost:8300', // celui de votre Vite/Angular
  originKo: 'http://evil.com',
  jwt: process.env.JWT || '', // facultatif : pour GET /auth/me
};

console.table(CFG);

/* ---------- LOG ----------------------------------------------------- */
function log(title, obj) {
  console.log(`\n═════════════════════════ ${title} ═════════════════════════`);
  if (obj !== undefined) console.dir(obj, { depth: 6, colors: true });
}

/* ---------- OUTILS BAS-NIVEAU --------------------------------------- */
async function rawRequest(method, route, extraHeaders = {}, body) {
  const url = `${CFG.api}${route}`;
  const headers = { ...extraHeaders };
  const opt = { method, headers };
  if (body) opt.body = body;

  log(`➡️  ${method} ${url}`, { headers });
  const res = await fetch(url, opt);
  const txt = await res.clone().text();
  log(`⬅️  ${method} ${url} [${res.status}]`, {
    status: res.status,
    resHeaders: Object.fromEntries(res.headers.entries()),
    body: txt.slice(0, 300),
  });
  return res;
}

/* ---------- TESTS --------------------------------------------------- */
async function preflight(origin, route = '/auth/me') {
  const headers = {
    Origin: origin,
    'Access-Control-Request-Method': 'GET',
    'Access-Control-Request-Headers': 'Content-Type,Authorization,Cookie',
  };
  return rawRequest('OPTIONS', route, headers);
}

async function simpleGet(origin, route = '/auth/me', cookie) {
  const headers = { Origin: origin };
  if (cookie) headers.Cookie = cookie;
  return rawRequest('GET', route, headers);
}

function checkCors(res, expectedOrigin, label) {
  const allowOrigin = res.headers.get('access-control-allow-origin');
  const allowCreds = res.headers.get('access-control-allow-credentials');
  console.log(
    `\n🔎  CORS HEADERS (${label})  allow-origin="${allowOrigin}"  allow-creds="${allowCreds}"`
  );
  if (allowOrigin !== expectedOrigin)
    console.error('❌  Access-Control-Allow-Origin inattendu !');
  else console.log('✅  Access-Control-Allow-Origin correct.');
  if (allowCreds !== 'true')
    console.error('❌  Access-Control-Allow-Credentials manquant / faux !');
  else console.log('✅  Access-Control-Allow-Credentials = true');
}

/* ---------- RUN ----------------------------------------------------- */
(async () => {
  console.log('\n🚀 DÉBUT DES TESTS CORS');

  /* --- Pré-flight ORIGIN OK ---------------------------------------- */
  const preOk = await preflight(CFG.originOk);
  checkCors(preOk, CFG.originOk, 'preflight OK');

  /* --- Pré-flight ORIGIN KO ---------------------------------------- */
  const preKo = await preflight(CFG.originKo);
  checkCors(preKo, null, 'preflight KO'); // on attend header absent

  /* --- GET /auth/me ORIGIN OK (401 attendu mais CORS) -------------- */
  const getOk = await simpleGet(
    CFG.originOk,
    '/auth/me',
    CFG.jwt ? `jwt=${CFG.jwt}` : undefined
  );
  checkCors(getOk, CFG.originOk, 'GET OK');

  /* --- GET ORIGIN KO ---------------------------------------------- */
  const getKo = await simpleGet(CFG.originKo, '/auth/me');
  checkCors(getKo, null, 'GET KO');

  console.log('\n🏁 FIN DES TESTS CORS');
})().catch((e) => {
  console.error('\n💥  ERREUR', e);
  process.exit(1);
});
