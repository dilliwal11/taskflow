import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, throwError } from 'rxjs';
import { AuthService } from './auth.service';

/**
 * Runs for every HttpClient request.
 * Outgoing: attaches the JWT. Incoming: a 401 means the token expired, so sign the user out.
 */
export const authInterceptor: HttpInterceptorFn = (request, next) => {
  const auth = inject(AuthService);
  const token = auth.token();
  const isApiCall = request.url.startsWith('/api/');
  const isAuthCall = request.url.startsWith('/api/auth/');

  const outgoing =
    token && isApiCall && !isAuthCall
      ? request.clone({ setHeaders: { Authorization: `Bearer ${token}` } })
      : request;

  return next(outgoing).pipe(
    catchError((error: HttpErrorResponse) => {
      if (error.status === 401 && isApiCall && !isAuthCall) {
        auth.logout();
      }
      return throwError(() => error);
    }),
  );
};
