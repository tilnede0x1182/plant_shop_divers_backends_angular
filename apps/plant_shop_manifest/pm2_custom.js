#!/usr/bin/env node

/**
 * Lightweight wrapper around the pm2 CLI to keep process management logic
 * next to the Manifest backend. We rely on the pm2 command being available
 * (globally or via `npx pm2`). The wrapper exposes a tiny API so Makefile
 * targets can start/stop the proxy and the Manifest server individually
 * or together.
 */

const { spawnSync } = require('child_process');
const path = require('path');

class Pm2Controller {
  constructor(rootDir) {
    this.rootDir = rootDir;
    this.pm2Bin = process.env.PM2_BIN || 'pm2';
    this.services = {
      server: {
        name: 'plant-shop-manifest-server',
        script: 'npm',
        args: ['run', 'dev'],
        pm2Args: ['--time'],
      },
      proxy: {
        name: 'plant-shop-manifest-proxy',
        script: path.join(rootDir, 'proxy.js'),
        args: [],
        pm2Args: ['--interpreter', 'node', '--time'],
      },
    };
  }

  /**
   * Exécute une commande pm2.
   * @param {Array} args Arguments de la commande pm2
   * @param {Object} options Options (allowFailure)
   */
  runPm2(args, { allowFailure = false } = {}) {
    const result = spawnSync(this.pm2Bin, args, {
      cwd: this.rootDir,
      stdio: 'inherit',
    });

    if (result.error && result.error.code === 'ENOENT' && this.pm2Bin !== 'npx') {
      // Fallback to npx pm2 if pm2 is not globally available.
      return this.runWithNpx(args, allowFailure);
    }

    if (result.status !== 0 && !allowFailure) {
      throw new Error(`pm2 command failed: ${args.join(' ')}`);
    }
  }

  /**
   * Exécute une commande pm2 via npx.
   * @param {Array} args Arguments de la commande
   * @param {boolean} allowFailure Si true, ne lance pas d'erreur en cas d'échec
   */
  runWithNpx(args, allowFailure) {
    const result = spawnSync('npx', ['pm2', ...args], {
      cwd: this.rootDir,
      stdio: 'inherit',
    });
    if (result.status !== 0 && !allowFailure) {
      throw new Error(`pm2 command failed via npx: ${args.join(' ')}`);
    }
  }

  /**
   * Démarre les services spécifiés.
   * @param {string} target Service à démarrer (server, proxy ou all)
   */
  start(target = 'all') {
    const services = this.resolveTargets(target);
    services.forEach((service) => {
      const cliArgs = [
        'start',
        service.script,
        '--name',
        service.name,
        '--cwd',
        this.rootDir,
      ];

      if (service.pm2Args?.length) {
        cliArgs.push(...service.pm2Args);
      }

      if (service.args?.length) {
        cliArgs.push('--', ...service.args);
      }

      this.runPm2(cliArgs);
    });
  }

  /**
   * Arrête les services spécifiés.
   * @param {string} target Service à arrêter (server, proxy ou all)
   */
  stop(target = 'all') {
    const services = this.resolveTargets(target);
    services.forEach((service) => {
      this.runPm2(['delete', service.name], { allowFailure: true });
    });
  }

  /**
   * Résout les cibles de service.
   * @param {string} target Nom du service ou 'all'
   * @return {Array} Tableau des services correspondants
   */
  resolveTargets(target) {
    if (target === 'all') {
      return Object.values(this.services);
    }

    const service = this.services[target];
    if (!service) {
      const names = Object.keys(this.services).join(', ');
      throw new Error(`Unknown service "${target}". Available: ${names}`);
    }
    return [service];
  }
}

/**
 * Point d'entrée du script pm2.
 * Parse les arguments et exécute l'action demandée.
 */
function main() {
  const [, , action, target = 'all'] = process.argv;
  if (!['start', 'stop'].includes(action)) {
    console.error('Usage: node pm2_custom.js <start|stop> [server|proxy|all]');
    process.exit(1);
  }

  const controller = new Pm2Controller(process.cwd());
  try {
    controller[action](target);
  } catch (err) {
    console.error(`❌ ${err.message}`);
    process.exit(1);
  }
}

main();
