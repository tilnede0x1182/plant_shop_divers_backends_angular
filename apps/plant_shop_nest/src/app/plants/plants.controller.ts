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
} from '@nestjs/common';
import { PlantsService } from './plants.service';
import { JwtAuthGuard } from '../auth/guards/jwt-auth.guard';
import { RolesGuard } from '../auth/roles.guard';
import { Roles } from '../auth/roles.decorator';

// # Contrôleur Plants
@Controller('plants')
export class PlantsController {
  constructor(private readonly plantsService: PlantsService) {}

  //**
   * Retourne toutes les plantes disponibles
   * @return Liste des plantes
   */
  @Get()
  findAll() {
    return this.plantsService.findAll();
  }

  /**
   * Retourne une plante par son identifiant
   * @param id Identifiant de la plante
   * @return Plante trouvée
   */
  @Get(':id')
  findOne(@Param('id') id: string) {
    return this.plantsService.findOne(+id);
  }

  /**
   * Crée une nouvelle plante (admin)
   * @param data Données de la plante
   * @return Plante créée
   */
  @UseGuards(JwtAuthGuard, RolesGuard)
  @Roles('admin')
  @Post()
  create(@Body() data: any) {
    return this.plantsService.create(data);
  }

  /**
   * Met à jour une plante (admin)
   * @param id Identifiant de la plante
   * @param data Données à mettre à jour
   * @return Plante mise à jour
   */
  @UseGuards(JwtAuthGuard, RolesGuard)
  @Roles('admin')
  @Patch(':id')
  update(@Param('id') id: string, @Body() data: any) {
    return this.plantsService.update(+id, data);
  }

  /**
   * Supprime une plante (admin)
   * @param id Identifiant de la plante
   * @return Plante supprimée
   */
  @UseGuards(JwtAuthGuard, RolesGuard)
  @Roles('admin')
  @Delete(':id')
  remove(@Param('id') id: string) {
    return this.plantsService.remove(+id);
  }
}
