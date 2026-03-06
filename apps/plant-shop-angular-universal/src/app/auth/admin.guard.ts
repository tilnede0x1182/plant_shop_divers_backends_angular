// # Importations
import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from './auth.service';
import { map, catchError } from 'rxjs/operators';
import { of } from 'rxjs';

// # Guard Admin
/**
 * Guard vérifiant que l utilisateur est admin
 * @return true si admin, sinon redirige vers login
 */
export const AdminGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  const router = inject(Router);

  return auth.getCurrentUser().pipe(
    map((user: any) => {
      if (user && user.admin === true) {
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
