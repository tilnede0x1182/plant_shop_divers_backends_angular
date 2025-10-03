// # Importations
import {
  Controller,
  Get,
  Delete,
  Param,
  UseGuards,
  Post,
  Patch,
  Body,
} from '@nestjs/common';
import { PlantsService } from './plants.service';
import { JwtAuthGuard } from '../auth/guards/jwt-auth.guard';
import { RolesGuard } from '../auth/roles.guard';
import { Roles } from '../auth/roles.decorator';

// # Controller Admin Plants
@Controller('admin/plants')
@UseGuards(JwtAuthGuard, RolesGuard)
export class AdminPlantsController {
  constructor(private readonly plantsService: PlantsService) {}

  @Get()
  @Roles('admin')
  findAll() {
    return this.plantsService.findAll();
  }

  @Post()
  @Roles('admin')
  create(@Body() data: any) {
    return this.plantsService.create(data);
  }

  @Patch(':id')
  @Roles('admin')
  update(@Param('id') id: string, @Body() data: any) {
    return this.plantsService.update(+id, data);
  }

  @Delete(':id')
  @Roles('admin')
  remove(@Param('id') id: string) {
    return this.plantsService.remove(+id);
  }
}
