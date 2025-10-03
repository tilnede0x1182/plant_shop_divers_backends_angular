// # Importations
import {
  HttpInterceptorFn,
  HttpRequest,
  HttpHandlerFn,
} from '@angular/common/http';

/**
 * Intercepteur Auth
 * - On ne gère plus de JWT en localStorage
 * - On force uniquement withCredentials pour inclure le cookie httpOnly
 */
export const authInterceptor: HttpInterceptorFn = (
  req: HttpRequest<unknown>,
  next: HttpHandlerFn
) => {
  const requeteAvecCreds = req.clone({ withCredentials: true });
  return next(requeteAvecCreds);
};
