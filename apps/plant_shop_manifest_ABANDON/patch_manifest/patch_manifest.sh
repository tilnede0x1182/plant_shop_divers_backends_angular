# Patch seed
cp patch_manifest/seed.js node_modules/manifest/dist/manifest/src/seed/scripts/seed.js
# Patch synchronize
sed -i 's/synchronize: true/synchronize: false/g' node_modules/manifest/dist/manifest/src/config/config.js
