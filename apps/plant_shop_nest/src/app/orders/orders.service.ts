// # Importations
import {
  Injectable,
  NotFoundException,
  BadRequestException,
} from '@nestjs/common';
import { PrismaService } from '../../prisma/prisma.service';
import { CreateOrderDto, UpdateOrderDto } from './dto/order.dto';
import { User } from '@prisma/client';

/**
  Service métier des commandes, accès base via Prisma
  @constructor PrismaService service Prisma injecté
*/
@Injectable()
export class OrdersService {
  constructor(private readonly prisma: PrismaService) {}

  /**
    Liste toutes les commandes (optionnel : inclure items/plantes)
  */
  async list() {
    return this.prisma.order.findMany({
      include: { orderItems: { include: { plant: true } } },
    });
  }

  /**
   * Liste les commandes de l’utilisateur courant uniquement
   * @userId identifiant de l’utilisateur connecté
   */
  async findAll(userId: number) {
    return this.prisma.order.findMany({
      where: { userId },
      include: { orderItems: { include: { plant: true } } },
      orderBy: { createdAt: 'desc' },
    });
  }

  /**
    Détail commande par id (inclut items/plantes)
    @id identifiant numérique commande
  */
  async one(id: number) {
    const order = await this.prisma.order.findUnique({
      where: { id },
      include: { orderItems: { include: { plant: true } } },
    });
    if (!order) throw new NotFoundException('Commande non trouvée');
    return order;
  }

  /**
    retourne une commande pour un utilisateur donné
    @id identifiant de la commande
    @user utilisateur connecté
  */
  async findOneForUser(id: number, user: User) {
    if (user.admin) {
      return this.one(id); // admin → accès global
    }
    const order = await this.prisma.order.findFirst({
      where: { id, userId: user.id },
      include: { orderItems: { include: { plant: true } } },
    });
    if (!order)
      throw new NotFoundException('Commande non trouvée pour cet utilisateur');
    return order;
  }

  /**
    Création commande (corrigé pour attendre aussi l’utilisateur)
    @dto données commande
    @user utilisateur connecté
  */
  async create(dto: CreateOrderDto, user: User) {
    let total = 0;
    const { items } = dto;

    const order = await this.prisma.order.create({
      data: { userId: user.id, status: 'confirmed', totalPrice: 0 },
    });

    for (const item of items) {
      const plant = await this.prisma.plant.findUnique({
        where: { id: item.plantId },
      });
      if (!plant || plant.stock < item.quantity) {
        throw new BadRequestException(
          `Stock insuffisant pour la plante ${item.plantId}`
        );
      }
      total += plant.price * item.quantity;
      await this.prisma.plant.update({
        where: { id: plant.id },
        data: { stock: plant.stock - item.quantity },
      });
      await this.prisma.orderItem.create({
        data: { orderId: order.id, plantId: plant.id, quantity: item.quantity },
      });
    }

    return this.prisma.order.update({
      where: { id: order.id },
      data: { totalPrice: total },
    });
  }

  /**
    Mise à jour commande (statut, items)
    @id identifiant commande
    @dto données mises à jour
  */
  async update(id: number, dto: UpdateOrderDto) {
    return this.prisma.order.update({ where: { id }, data: dto });
  }

  /**
    Suppression commande (+ suppression items liés)
    @id identifiant commande
  */
  async remove(id: number) {
    await this.prisma.orderItem.deleteMany({ where: { orderId: id } });
    return this.prisma.order.delete({ where: { id } });
  }
}
