#!/bin/sh
set -e

SCRIPT_DIR=$(cd "$(dirname "$0")" && pwd)
APP_DIR=$(dirname "$SCRIPT_DIR")
CONFIG_FILE="$APP_DIR/node_modules/manifest/dist/manifest/src/config/config.js"

echo "🔧 Désactivation de synchronize dans la config Manifest..."

# Remplacer tous les "synchronize: true" par "synchronize: false"
sed -i 's/synchronize: true/synchronize: false/g' "$CONFIG_FILE"

echo "✅ Synchronisation automatique désactivée"
