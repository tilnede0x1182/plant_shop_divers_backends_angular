// diagnose-ora.js
const fs = require('fs');
const path = require('path');
const { execSync } = require('child_process');

console.log('== DIAGNOSTIC ORA ==');

// 1. Vérifier toutes les versions d'ora installées
console.log("\n[1] Versions d'ora installées:");
try {
  const result = execSync(
    'find node_modules -name "package.json" | xargs grep -l "ora" | xargs grep -A 3 \'"name": "ora"\''
  ).toString();
  console.log(result);
} catch (e) {
  console.log("Erreur lors de la recherche des versions d'ora:", e.message);
}

// 2. Trouver tous les packages qui dépendent d'ora
console.log("\n[2] Packages qui dépendent d'ora:");
try {
  const result = execSync(
    'find node_modules -name "package.json" | xargs grep -l \'"ora":\' | sort'
  ).toString();
  console.log(result);
} catch (e) {
  console.log("Erreur lors de la recherche des dépendances d'ora:", e.message);
}

// 3. Inspecter le fichier spinner.js problématique
console.log('\n[3] Contenu du fichier spinner.js:');
try {
  const spinnerPath =
    'node_modules/.pnpm/@angular-devkit+build-angular@20.2.2_dd4e8dc09f926a8404895d7b44030157/node_modules/@angular-devkit/build-angular/src/utils/spinner.js';
  if (fs.existsSync(spinnerPath)) {
    const content = fs.readFileSync(spinnerPath, 'utf8');
    console.log(content.substring(0, 500) + '...');

    // Vérifier la ligne problématique
    const requireLine = content.match(/.*require\(['"]ora['"]\).*/);
    if (requireLine) {
      console.log('\nLigne problématique:', requireLine[0]);
    }
  } else {
    console.log("Fichier spinner.js non trouvé à l'emplacement attendu");
  }
} catch (e) {
  console.log("Erreur lors de l'inspection de spinner.js:", e.message);
}

// 4. Vérifier les résolutions de pnpm
console.log('\n[4] Résolutions de pnpm:');
try {
  if (fs.existsSync('pnpm-lock.yaml')) {
    const lockContent = fs.readFileSync('pnpm-lock.yaml', 'utf8');
    const oraResolutions = lockContent.match(/.*ora(@|\/|:).*version:.*/g);
    if (oraResolutions && oraResolutions.length > 0) {
      console.log(oraResolutions.join('\n'));
    } else {
      console.log('Aucune résolution pour ora trouvée dans pnpm-lock.yaml');
    }
  } else {
    console.log('Fichier pnpm-lock.yaml non trouvé');
  }
} catch (e) {
  console.log('Erreur lors de la vérification des résolutions:', e.message);
}
