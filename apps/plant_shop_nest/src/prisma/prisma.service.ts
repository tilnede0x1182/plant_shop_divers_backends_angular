import { Injectable, OnModuleInit } from '@nestjs/common';
import { PrismaClient } from '@prisma/client';

/**
 * Service Prisma pour la connexion à la base de données
 */
@Injectable()
export class PrismaService extends PrismaClient implements OnModuleInit {
  /**
   * Initialise la connexion à la base de données
   */
  async onModuleInit() {
    await this.$connect();
  }

  /**
   * Configure les hooks de fermeture propre
   * @param app Instance de l application NestJS
   */
  async enableShutdownHooks(app: any) {
    this.$on('beforeExit' as any, async () => {
      await app.close();
    });
  }
}
