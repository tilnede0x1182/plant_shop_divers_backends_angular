import { Injectable } from '@nestjs/common';

/**
 * Service principal de l application
 */
@Injectable()
export class AppService {
  /**
   * Retourne un message de bienvenue
   * @return Message Hello API
   */
  getData(): { message: string } {
    return { message: 'Hello API' };
  }
}
