// # Importations
import {
  Controller,
  Post,
  Body,
  Res,
  Get,
  Req,
  UseGuards,
} from '@nestjs/common';
import { Response, Request } from 'express';
import { AuthService } from './auth.service';
import { JwtAuthGuard } from './jwt-auth.guard';

@Controller('auth')
export class AuthController {
  constructor(private readonly authService: AuthService) {}

  /**
   * Inscription : crée un compte
   * @param email Email de l'utilisateur
   * @param password Mot de passe
   * @param name Nom de l'utilisateur
   * @return Utilisateur créé
   */
  @Post('register')
  async register(
    @Body('email') email: string,
    @Body('password') password: string,
    @Body('name') name: string
  ) {
    const user = await this.authService.register(email, password, name);
    return { user };
  }

  /**
   * Connexion d'un utilisateur
   * @param email Email de l'utilisateur
   * @param password Mot de passe
   * @param res Réponse Express pour le cookie
   * @return Utilisateur connecté
   */
  @Post('login')
  async login(
    @Body('email') email: string,
    @Body('password') password: string,
    @Res({ passthrough: true }) res: Response
  ) {
    const utilisateur = await this.authService.validateUser(email, password);
    const { access_token, user } = await this.authService.login(utilisateur);

    res.cookie('jwt', access_token, {
      httpOnly: true,
      secure: process.env.NODE_ENV === 'production',
      sameSite: 'strict',
      maxAge: 1000 * 60 * 60 * 24,
    });

    return { user };
  }

  /**
   * Déconnexion de l'utilisateur
   * @param res Réponse Express pour supprimer le cookie
   * @return Message de confirmation
   */
  @Post('logout')
  async logout(@Res({ passthrough: true }) res: Response) {
    res.clearCookie('jwt');
    return { message: 'Déconnecté' };
  }

  /** Récupérer l’utilisateur courant
   * @param req Requête contenant le token JWT
   * @return Données de l'utilisateur connecté
   */
  @UseGuards(JwtAuthGuard)
  @Get('me')
  async me(@Req() req: Request) {
    const user = (req as any).user;
    return {
      id: user.id,
      email: user.email,
      name: user.name,
      admin: user.admin,
    };
  }
}
