import { Controller, Get } from '@nestjs/common';

import { AppService } from './app.service';

/**
 * Contrôleur principal de l application
 */
@Controller('')
export class AppController {
  constructor(private readonly appService: AppService) {}

  /**
   * Route de test retournant un message
   * @return Données de bienvenue
   */
  @Get('hello')
  getData() {
    return this.appService.getData();
  }
}
