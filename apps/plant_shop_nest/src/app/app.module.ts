// # Importations
import { Module } from '@nestjs/common';
import { AppController } from './app.controller';
import { AppService } from './app.service';
import { PrismaModule } from '../prisma/prisma.module';
import { UsersModule } from './users/users.module';
import { PlantsModule } from './plants/plants.module';
import { OrdersModule } from './orders/orders.module';
import { OrderItemsModule } from './order-items/order-items.module';
import { AuthModule } from './auth/auth.module';
// import { AngularModule } from './angular/angular.module';

// # Module principal
@Module({
  imports: [
    PrismaModule,
    UsersModule,
    PlantsModule,
    OrdersModule,
    OrderItemsModule,
    AuthModule,
    // ...(process.env.SERVE_SSR === 'true' ? [AngularModule] : []),
  ],
  controllers: [AppController],
  providers: [AppService],
})
export class AppModule {}
