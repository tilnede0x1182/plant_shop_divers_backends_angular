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
} from '@nestjs/common';
import { OrdersService } from './orders.service';
import { JwtAuthGuard } from '../auth/guards/jwt-auth.guard';
import { RolesGuard } from '../auth/roles.guard';
import { Roles } from '../auth/roles.decorator';
import { Request } from 'express';

// # Contrôleur Orders
@UseGuards(JwtAuthGuard, RolesGuard)
@Controller('orders')
export class OrdersController {
  constructor(private readonly ordersService: OrdersService) {}

  //**
   * Liste les commandes de l'utilisateur courant
   * @param req Requête contenant l'utilisateur courant
   * @return Liste des commandes
   */
  @Get()
  findAll(@Req() req: any) {
    const userId = req.user.id;
    return this.ordersService.findAll(userId);
  }

  //**
   * Crée une nouvelle commande pour l'utilisateur courant
   * @param data Données de la commande (items)
   * @param req Requête contenant l'utilisateur courant
   * @return Commande créée
   */
  @Post()
  create(@Body() data: any, @Req() req: any) {
    const user = req.user;
    return this.ordersService.create(data, user);
  }

  /**
   * Met à jour une commande (admin)
   * @param id Identifiant de la commande
   * @param data Données à mettre à jour
   * @return Commande mise à jour
   */
  @Roles('admin')
  @Patch(':id')
  update(@Param('id') id: string, @Body() data: any) {
    return this.ordersService.update(+id, data);
  }

  /**
   * Supprime une commande (admin)
   * @param id Identifiant de la commande
   * @return Commande supprimée
   */
  @Roles('admin')
  @Delete(':id')
  remove(@Param('id') id: string) {
    return this.ordersService.remove(+id);
  }
}
