
const { execSync } = require('child_process');
const path = require('path');

const appDir = process.cwd();

// Lancer simplement la seed custom qui va créer tout (y compris l'admin pour le panel)
console.log('🌱 Lancement de la seed custom...');
const customSeedPath = path.join(appDir, 'db', 'seed-custom.js');
const envPath = path.join(appDir, '.env');

try {
  execSync(`npx dotenv -e "${envPath}" -- node "${customSeedPath}"`, {
    stdio: 'inherit',
    cwd: appDir
  });
  console.log('✅ Seed complète terminée.');
} catch (error) {
  console.error('❌ Seed échouée!');
  console.error(error);
  process.exit(1);
}
