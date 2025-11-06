#!/bin/sh
set -e
SCRIPT_DIR=$(cd "$(dirname "$0")" && pwd)
APP_DIR=$(dirname "$SCRIPT_DIR")
# Resolve path to actual manifest package (handles pnpm symlinks)
MANIFEST_DIR=$(readlink -f "$APP_DIR/node_modules/manifest/dist/manifest/src")

# Étape 1 : Désactiver synchronize dans config.js
CONFIG_FILE="$MANIFEST_DIR/config/config.js"
echo "🔧 Désactivation de synchronize dans la config Manifest..."
sed -i 's/synchronize: true/synchronize: false/g' "$CONFIG_FILE"
echo "✅ Synchronisation automatique désactivée"

# Étape 2 : Overwrite manifest seed script with custom seed
DEST="$MANIFEST_DIR/seed/scripts/seed.js"
cp -f "$SCRIPT_DIR/seed.js" "$DEST"
