import { SetMetadata } from '@nestjs/common';

/**
 * Clé de métadonnée pour les rôles
 */
export const ROLES_KEY = 'roles';
/**
 * Décorateur définissant les rôles requis pour une route
 * @param roles Liste des rôles autorisés
 */
export const Roles = (...roles: string[]) => SetMetadata(ROLES_KEY, roles);
