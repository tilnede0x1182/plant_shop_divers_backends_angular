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

  // ✅ Un admin peut voir tous les order-items
  @Roles('admin')
  @Get()
  findAll() {
    return this.orderItemsService.findAll();
  }

  // ✅ Un utilisateur peut consulter un order-item uniquement si lié à sa commande
  @Get(':id')
  findOne(@Param('id') id: string, @Req() req: any) {
    const user = req.user;
    return this.orderItemsService.findOneForUser(+id, user);
  }

  // ✅ Tout utilisateur connecté peut ajouter un item à sa commande
  @Post()
  create(@Body() data: any, @Req() req: any) {
    const user = req.user;
    return this.orderItemsService.create(data, user);
  }

  // ✅ Admin peut modifier un item (ex. quantité)
  @Roles('admin')
  @Patch(':id')
  update(@Param('id') id: string, @Body() data: any) {
    return this.orderItemsService.update(+id, data);
  }

  // ✅ Admin peut supprimer un item
  @Roles('admin')
  @Delete(':id')
  remove(@Param('id') id: string) {
    return this.orderItemsService.remove(+id);
  }
}
