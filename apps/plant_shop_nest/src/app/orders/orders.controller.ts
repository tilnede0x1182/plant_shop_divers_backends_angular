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

  // ✅ Tout utilisateur authentifié voit uniquement ses propres commandes
  @Get()
  findAll(@Req() req: any) {
    const userId = req.user.id;
    return this.ordersService.findAll(userId);
  }

  // ✅ Tout utilisateur authentifié peut créer une commande
  @Post()
  create(@Body() data: any, @Req() req: any) {
    const user = req.user;
    return this.ordersService.create(data, user);
  }

  // ✅ Un admin peut mettre à jour une commande (ex. statut)
  @Roles('admin')
  @Patch(':id')
  update(@Param('id') id: string, @Body() data: any) {
    return this.ordersService.update(+id, data);
  }

  // ✅ Un admin peut supprimer une commande
  @Roles('admin')
  @Delete(':id')
  remove(@Param('id') id: string) {
    return this.ordersService.remove(+id);
  }
}
