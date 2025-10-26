import { inject } from '@angular/core';
import { CanActivateFn, Router, ActivatedRouteSnapshot } from '@angular/router';
import { AuthService } from './auth.service';
import { map, catchError } from 'rxjs/operators';
import { of } from 'rxjs';

export const SelfProfileGuard: CanActivateFn = (
  route: ActivatedRouteSnapshot
) => {
  const auth = inject(AuthService);
  const router = inject(Router);
  const requestedId = Number(route.paramMap.get('id'));

  return auth.getCurrentUser().pipe(
    map((user) => {
      if (user?.id === requestedId) return true;
      router.navigate(['/login']);
      return false;
    }),
    catchError(() => {
      router.navigate(['/login']);
      return of(false);
    })
  );
};
