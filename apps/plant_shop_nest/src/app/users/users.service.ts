// # Importations
import { Injectable, NotFoundException } from '@nestjs/common';
import { PrismaService } from '../../prisma/prisma.service';
import { CreateUserDto, UpdateUserDto } from './dto/user.dto';
import * as bcrypt from 'bcryptjs';

/**
  Service métier des utilisateurs, accès base via Prisma
  @constructor PrismaService service Prisma injecté
*/
@Injectable()
export class UsersService {
  constructor(private readonly prisma: PrismaService) {}

  /**
    Liste tous les utilisateurs (admins d'abord, tri par nom)
  */
  async list() {
    return this.prisma.user.findMany({
      orderBy: [{ admin: 'desc' }, { name: 'asc' }],
    });
  }

  /**
    findAll (alias de list)
  */
  async findAll() {
    return this.list();
  }

  /**
    Récupère un utilisateur par id
    @id identifiant numérique utilisateur
  */
  async one(id: number) {
    const user = await this.prisma.user.findUnique({ where: { id } });
    if (!user) throw new NotFoundException('Utilisateur non trouvé');
    return user;
  }

  /**
    findOne (alias de one)
  */
  async findOne(id: number) {
    return this.one(id);
  }

  /**
    Création utilisateur
    @dto données utilisateur (CreateUserDto)
    @return utilisateur créé
  */
  async create(dto: CreateUserDto) {
    const user = await this.prisma.user.create({ data: dto });
    return user;
  }

  /**
    Recherche utilisateur par email
    @param email adresse email
    @return utilisateur trouvé ou null
  */
  async findByEmail(email: string) {
    const user = await this.prisma.user.findUnique({ where: { email } });
    return user;
  }

  /**
    Suppression utilisateur
    @id identifiant utilisateur
    @return utilisateur supprimé
  */
  async remove(id: number) {
    return this.prisma.user.delete({ where: { id } });
  }

  /**
    Mise à jour utilisateur
    @id identifiant utilisateur
    @dto données à mettre à jour (UpdateUserDto)
    @return utilisateur mis à jour
  */
  async update(id: number, dto: UpdateUserDto) {
    const user = await this.prisma.user.update({ where: { id }, data: dto });
    return user;
  }
}
