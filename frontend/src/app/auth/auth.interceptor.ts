import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { BrowserSessionStore } from './browser-session.store';

export const authInterceptor: HttpInterceptorFn = (request, next) => {
  const token = inject(BrowserSessionStore).token();
  if (!token) return next(request);
  return next(request.clone({ setHeaders: { Authorization: `Bearer ${token}` } }));
};
