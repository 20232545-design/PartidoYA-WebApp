import {
  ApplicationConfig,
  provideBrowserGlobalErrorListeners,
  provideZoneChangeDetection
} from '@angular/core';
import { provideRouter } from '@angular/router';
import { provideHttpClient } from '@angular/common/http';
import { API_BASE_URL } from '@core/http/api-base-url.token';
import { environment } from '@env/environment';

import { routes } from './app.routes';

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    provideZoneChangeDetection(),
    provideRouter(routes),
    provideHttpClient(),
    { provide: API_BASE_URL, useValue: environment.apiUrl }
  ]
};
