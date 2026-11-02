import { Injectable } from '@angular/core';
import { environment } from '@env/environment';

declare global {
  interface Window {
    google: any;
  }
}

@Injectable({ providedIn: 'root' })
export class MapsLoaderService {
  private loadPromise: Promise<void> | null = null;

  load(): Promise<void> {
    if (this.loadPromise) {
      return this.loadPromise;
    }

    this.loadPromise = new Promise((resolve, reject) => {
      if (!environment.googleMapsApiKey) {
        reject(new Error('Google Maps no está configurado.'));
        return;
      }

      if (window.google?.maps) {
        resolve();
        return;
      }

      const existing = document.querySelector<HTMLScriptElement>('script[src^="https://maps.googleapis.com/maps/api/js"]');
      if (existing) {
        existing.addEventListener('load', () => resolve(), { once: true });
        existing.addEventListener('error', () => reject(new Error('No se pudo cargar Google Maps.')), { once: true });
        return;
      }

      const script = document.createElement('script');
      script.src = `https://maps.googleapis.com/maps/api/js?key=${environment.googleMapsApiKey}&libraries=places&language=es&region=PE`;
      script.async = true;
      script.defer = true;
      script.onload = () => {
        if (window.google?.maps) {
          resolve();
          return;
        }
        reject(new Error('Google Maps respondió sin inicializar la API.'));
      };
      script.onerror = () => reject(new Error('No se pudo cargar Google Maps.'));
      document.head.appendChild(script);
    });

    this.loadPromise.catch(() => {
      this.loadPromise = null;
    });

    return this.loadPromise;
  }
}
