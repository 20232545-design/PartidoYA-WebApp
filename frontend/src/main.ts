// zone.js se carga desde "polyfills" en angular.json: así el compilador convierte async/await a una forma
// que Zone.js puede seguir. Importado aquí, el código después de un `await` no actualizaba la pantalla.
import { bootstrapApplication } from '@angular/platform-browser';
import { appConfig } from './app/app.config';
import { App } from './app/app';

bootstrapApplication(App, appConfig)
  .catch((err) => console.error(err));
