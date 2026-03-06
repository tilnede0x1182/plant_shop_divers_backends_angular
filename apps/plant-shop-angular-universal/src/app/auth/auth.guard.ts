// # Importations
import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from './auth.service';
import { map, catchError } from 'rxjs/operators';
import { of } from 'rxjs';

// # Guard Auth
/**
 * Guard vérifiant que l utilisateur est connecté
 * @return true si connecté, sinon redirige vers login
 */
export const AuthGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  const router = inject(Router);

  return auth.getCurrentUser().pipe(
    map((user: any) => {
      if (user) {
        return true;
      } else {
        router.navigate(['/login']);
        return false;
      }
    }),
    catchError(() => {
      router.navigate(['/login']);
      return of(false);
    })
  );
};
