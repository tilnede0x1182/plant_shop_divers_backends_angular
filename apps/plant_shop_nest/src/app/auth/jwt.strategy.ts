// # Importations
import { Injectable } from '@nestjs/common';
import { PassportStrategy } from '@nestjs/passport';
import { ExtractJwt, Strategy } from 'passport-jwt';
import { Request } from 'express';

/**
 * Stratégie JWT pour Passport
 */
@Injectable()
export class JwtStrategy extends PassportStrategy(Strategy) {
  /**
   * Constructeur configurant l extraction du token depuis les cookies
   */
  constructor() {
    super({
      jwtFromRequest: ExtractJwt.fromExtractors([
        // Lecture d'abord dans le cookie
        (req: Request) => {
          if (req && req.cookies) {
            return req.cookies['jwt'];
          }
          return null;
        },
      ]),
      ignoreExpiration: false,
      secretOrKey: process.env.JWT_SECRET || 'secret_dev',
    });
  }

  /**
   * Valide le payload JWT et retourne les infos utilisateur
   * @param payload Payload décodé du token JWT
   * @return Objet utilisateur pour req.user
   */
  async validate(payload: any) {
    // Retourne les infos utiles du user dans req.user
    return {
      id: payload.sub,
      email: payload.email,
      admin: payload.admin,
      name: payload.name,
    };
  }
}
