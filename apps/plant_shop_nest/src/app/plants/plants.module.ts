import { Module } from '@nestjs/common';
import { PlantsService } from './plants.service';
import { PlantsController } from './plants.controller';
import { AdminPlantsController } from './admin-plants.controller';
import { PrismaModule } from '../../prisma/prisma.module';

@Module({
  imports: [PrismaModule],
  controllers: [PlantsController, AdminPlantsController],
  providers: [PlantsService],
  exports: [PlantsService],
})
export class PlantsModule {}
