// # Importations
import {
  Controller,
  Get,
  Post,
  Patch,
  Delete,
  Param,
  Body,
  UseGuards,
  Req,
  ForbiddenException,
} from '@nestjs/common';
import { UsersService } from './users.service';
import { JwtAuthGuard } from '../auth/guards/jwt-auth.guard';
import { RolesGuard } from '../auth/roles.guard';
import { Roles } from '../auth/roles.decorator';

// # Contrôleur Users
@UseGuards(JwtAuthGuard, RolesGuard)
@Controller('users')
export class UsersController {
  constructor(private readonly usersService: UsersService) {}

  /**
   * Liste tous les utilisateurs (admin)
   * @return Liste des utilisateurs
   */
  @Roles('admin')
  @Get()
  findAll() {
    return this.usersService.findAll();
  }

  /**
   * Récupère un utilisateur par id (admin ou propriétaire)
   * @param id Identifiant de l'utilisateur
   * @param req Requête contenant l'utilisateur courant
   * @return Utilisateur trouvé
   */
  @Get(':id')
  @UseGuards(JwtAuthGuard, RolesGuard)
  async findOne(@Param('id') id: string, @Req() req) {
    const userId = +id;
    const currentUser = req.user;

    // Admin → accès OK
    if (currentUser.admin) {
      return this.usersService.findOne(userId);
    }

    // Utilisateur → accès seulement à son propre profil
    if (currentUser.id === userId) {
      return this.usersService.findOne(userId);
    }

    throw new ForbiddenException('Accès refusé');
  }

  /**
   * Crée un nouvel utilisateur (admin)
   * @param data Données de l'utilisateur
   * @return Utilisateur créé
   */
  @Roles('admin')
  @Post()
  create(@Body() data: any) {
    return this.usersService.create(data);
  }

  /**
   * Met à jour un utilisateur (admin ou propriétaire)
   * @param id Identifiant de l'utilisateur
   * @param data Données à mettre à jour
   * @param req Requête contenant l'utilisateur courant
   * @return Utilisateur mis à jour
   */
  @Patch(':id')
  async update(@Param('id') id: string, @Body() data: any, @Req() req) {
    const userId = +id;
    const currentUser = req.user;

    // Admin → peut modifier n’importe qui
    if (currentUser.admin) {
      return this.usersService.update(userId, data);
    }

    // Utilisateur → peut modifier uniquement son propre profil
    if (currentUser.id === userId) {
      // Sécurité : empêcher qu’il force `admin: true`
      if ('admin' in data) delete data.admin;
      return this.usersService.update(userId, data);
    }

    throw new ForbiddenException('Accès refusé');
  }

  /**
   * Supprime un utilisateur (admin)
   * @param id Identifiant de l'utilisateur
   * @return Utilisateur supprimé
   */
  @Roles('admin')
  @Delete(':id')
  remove(@Param('id') id: string) {
    return this.usersService.remove(+id);
  }
}
