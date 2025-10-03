// # Importations
import { Injectable, NotFoundException } from '@nestjs/common';
import { PrismaService } from '../../prisma/prisma.service';
import { CreateOrderItemDto, UpdateOrderItemDto } from './dto/order-item.dto';
import { User } from '@prisma/client';

/**
  Service métier OrderItems, accès base via Prisma
  @constructor PrismaService service Prisma injecté
*/
@Injectable()
export class OrderItemsService {
  constructor(private readonly prisma: PrismaService) {}

  /**
    Liste tous les orderItems (inclut plante et commande)
  */
  async list() {
    return this.prisma.orderItem.findMany({
      include: { plant: true, order: true },
    });
  }

  /**
    Récupère un orderItem par id
    @id identifiant numérique orderItem
  */
  async one(id: number) {
    const orderItem = await this.prisma.orderItem.findUnique({
      where: { id },
      include: { plant: true, order: true },
    });
    if (!orderItem) throw new NotFoundException('OrderItem non trouvé');
    return orderItem;
  }

  /**
    findAll (alias de list pour cohérence)
  */
  async findAll() {
    return this.list();
  }

  /**
    recherche un item spécifique appartenant à un utilisateur
    @id identifiant de l’item
    @user utilisateur connecté
  */
  async findOneForUser(id: number, user: User) {
    const orderItem = await this.prisma.orderItem.findFirst({
      where: { id, order: { userId: user.id } },
      include: { plant: true, order: true },
    });
    if (!orderItem)
      throw new NotFoundException('OrderItem non trouvé pour cet utilisateur');
    return orderItem;
  }

  /**
    Création orderItem (corrigé pour prendre user et dto)
    @dto données orderItem
    @user utilisateur connecté
  */
  async create(dto: CreateOrderItemDto, user: User) {
    // Approche 2 : Utiliser uniquement l'approche relationnelle
    return this.prisma.orderItem.create({
      data: {
        quantity: dto.quantity,
        plant: {
          connect: { id: dto.plantId },
        },
        order: {
          connect: { id: dto.orderId },
        },
      },
    });
  }

  /**
    Mise à jour orderItem (quantité)
    @id identifiant orderItem
    @dto données mises à jour
  */
  async update(id: number, dto: UpdateOrderItemDto) {
    return this.prisma.orderItem.update({ where: { id }, data: dto });
  }

  /**
    Suppression orderItem
    @id identifiant orderItem
  */
  async remove(id: number) {
    return this.prisma.orderItem.delete({ where: { id } });
  }
}
