// # Importations
import {
  Controller,
  Get,
  Delete,
  Param,
  UseGuards,
  Patch,
  Body,
} from '@nestjs/common';
import { UsersService } from './users.service';
import { JwtAuthGuard } from '../auth/guards/jwt-auth.guard';
import { RolesGuard } from '../auth/roles.guard';
import { Roles } from '../auth/roles.decorator';

// # Controller Admin Users
@Controller('admin/users')
@UseGuards(JwtAuthGuard, RolesGuard)
export class AdminUsersController {
  constructor(private readonly usersService: UsersService) {}

  /**
   * Liste tous les utilisateurs (admin)
   * @return Liste des utilisateurs
   */
  @Get()
  @Roles('admin')
  findAll() {
    return this.usersService.findAll();
  }

  /**
   * Met à jour un utilisateur (admin)
   * @param id Identifiant de l'utilisateur
   * @param data Données à mettre à jour
   * @return Utilisateur mis à jour
   */
  @Patch(':id')
  @Roles('admin')
  update(@Param('id') id: string, @Body() data: any) {
    return this.usersService.update(+id, data);
  }

  /**
   * Supprime un utilisateur (admin)
   * @param id Identifiant de l'utilisateur
   * @return Utilisateur supprimé
   */
  @Delete(':id')
  @Roles('admin')
  remove(@Param('id') id: string) {
    return this.usersService.remove(+id);
  }
}
