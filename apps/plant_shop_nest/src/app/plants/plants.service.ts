// # Importations
import { Injectable, NotFoundException } from '@nestjs/common';
import { PrismaService } from '../../prisma/prisma.service';
import { CreatePlantDto, UpdatePlantDto } from './dto/plant.dto';

/**
  Service métier des plantes, accès base via Prisma
  @constructor PrismaService service Prisma injecté
*/
@Injectable()
export class PlantsService {
  constructor(private readonly prisma: PrismaService) {}

  /**
    Retourne toutes les plantes disponibles (stock >= 1)
  */
  async list() {
    return this.prisma.plant.findMany({
      where: { stock: { gte: 1 } },
      orderBy: { name: 'asc' },
    });
  }

  /**
    findAll (alias de list)
  */
  async findAll() {
    return this.list();
  }

  /**
    Retourne une plante par id
    @id identifiant numérique plante
  */
  async one(id: number) {
    const plant = await this.prisma.plant.findUnique({ where: { id } });
    if (!plant) throw new NotFoundException('Plante non trouvée');
    return plant;
  }

  /**
    findOne (alias de one)
  */
  async findOne(id: number) {
    return this.one(id);
  }

  /**
    Création plante
    @dto données plante
  */
  async create(dto: CreatePlantDto) {
    return this.prisma.plant.create({ data: dto });
  }

  /**
    Mise à jour plante
    @id identifiant plante
    @dto données mises à jour
  */
  async update(id: number, dto: UpdatePlantDto) {
    return this.prisma.plant.update({ where: { id }, data: dto });
  }

  /**
    Suppression plante
    @id identifiant plante
  */
  async remove(id: number) {
    return this.prisma.plant.delete({ where: { id } });
  }
}
