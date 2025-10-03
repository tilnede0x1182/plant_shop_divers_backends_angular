// # Importations
import { Injectable } from '@nestjs/common';
import { PassportStrategy } from '@nestjs/passport';
import { ExtractJwt, Strategy } from 'passport-jwt';
import { Request } from 'express';

@Injectable()
export class JwtStrategy extends PassportStrategy(Strategy) {
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
