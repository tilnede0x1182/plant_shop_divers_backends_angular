// # Importations
import { Injectable, UnauthorizedException } from '@nestjs/common';
import { UsersService } from '../users/users.service';
import * as bcrypt from 'bcryptjs';
import { JwtService } from '@nestjs/jwt';

// # Service d'authentification
@Injectable()
export class AuthService {
  constructor(
    private readonly usersService: UsersService,
    private readonly jwtService: JwtService
  ) {}

  /**
   * Inscription d'un nouvel utilisateur
   * @param email Email de l'utilisateur
   * @param password Mot de passe en clair
   * @param name Nom de l'utilisateur (optionnel)
   * @return Utilisateur créé (sans mot de passe)
   */
  async register(email: string, password: string, name?: string) {
    const hashed = await bcrypt.hash(password, 10);
    const utilisateur = await this.usersService.create({
      email,
      password: hashed,
      name,
    });

    return {
      id: utilisateur.id,
      email: utilisateur.email,
      name: utilisateur.name,
      admin: utilisateur.admin,
    };
  }

  /**
   * Valide les credentials d'un utilisateur
   * @param email Email de l'utilisateur
   * @param password Mot de passe en clair
   * @return Utilisateur validé
   */
  async validateUser(email: string, password: string) {
    const utilisateur = await this.usersService.findByEmail(email);
    if (!utilisateur) throw new UnauthorizedException('Utilisateur inexistant');
    const valide = await bcrypt.compare(password, utilisateur.password);
    if (!valide) throw new UnauthorizedException('Mot de passe invalide');

    return utilisateur;
  }

  /**
   * Générer un JWT pour un utilisateur
   * @param utilisateur Utilisateur à authentifier
   * @return Token JWT et données utilisateur
   */
  async login(utilisateur: any) {
    const payload = {
      sub: utilisateur.id,
      email: utilisateur.email,
      admin: utilisateur.admin,
      name: utilisateur.name,
    };
    return {
      access_token: this.jwtService.sign(payload),
      user: utilisateur,
    };
  }
}
