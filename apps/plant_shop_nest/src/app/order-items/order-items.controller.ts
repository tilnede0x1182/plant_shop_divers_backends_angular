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
import { OrderItemsService } from './order-items.service';
import { JwtAuthGuard } from '../auth/guards/jwt-auth.guard';
import { RolesGuard } from '../auth/roles.guard';
import { Roles } from '../auth/roles.decorator';

// # Contrôleur OrderItems
@UseGuards(JwtAuthGuard, RolesGuard)
@Controller('order-items')
export class OrderItemsController {
  constructor(private readonly orderItemsService: OrderItemsService) {}

  /**
   * Liste tous les order-items (admin)
   * @return Liste des order-items
   */
  @Roles('admin')
  @Get()
  findAll() {
    return this.orderItemsService.findAll();
  }

  /**
   * Récupère un order-item par id (propriétaire)
   * @param id Identifiant de l'order-item
   * @param req Requête contenant l'utilisateur courant
   * @return Order-item trouvé
   */
  @Get(':id')
  findOne(@Param('id') id: string, @Req() req: any) {
    const user = req.user;
    return this.orderItemsService.findOneForUser(+id, user);
  }

  //**
   * Crée un nouvel order-item
   * @param data Données de l'order-item
   * @param req Requête contenant l'utilisateur courant
   * @return Order-item créé
   */
  @Post()
  create(@Body() data: any, @Req() req: any) {
    const user = req.user;
    return this.orderItemsService.create(data, user);
  }

  /**
   * Met à jour un order-item (admin)
   * @param id Identifiant de l'order-item
   * @param data Données à mettre à jour
   * @return Order-item mis à jour
   */
  @Roles('admin')
  @Patch(':id')
  update(@Param('id') id: string, @Body() data: any) {
    return this.orderItemsService.update(+id, data);
  }

  /**
   * Supprime un order-item (admin)
   * @param id Identifiant de l'order-item
   * @return Order-item supprimé
   */
  @Roles('admin')
  @Delete(':id')
  remove(@Param('id') id: string) {
    return this.orderItemsService.remove(+id);
  }
}
