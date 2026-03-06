import { Injectable } from '@nestjs/common';
import { AuthGuard } from '@nestjs/passport';

/**
 * Guard JWT simple héritant de AuthGuard
 */
@Injectable()
export class JwtAuthGuard extends AuthGuard('jwt') {}
