import { Injectable } from '@angular/core';

@Injectable({ providedIn: 'root' })
export class GeolocationService {
  getPosition(timeout = 8000): Promise<GeolocationPosition> {
    return new Promise((resolve, reject) => {
      if (!navigator.geolocation) {
        reject(new Error('Tu navegador no soporta geolocalización.'));
        return;
      }
      navigator.geolocation.getCurrentPosition(
        resolve,
        () => reject(new Error('Activa el permiso de ubicación.')),
        { timeout }
      );
    });
  }

  distanceKm(lat1: number, lng1: number, lat2: number, lng2: number): number {
    const R = 6371;
    const dLat = this.toRad(lat2 - lat1);
    const dLng = this.toRad(lng2 - lng1);
    const a =
      Math.sin(dLat / 2) ** 2 +
      Math.cos(this.toRad(lat1)) * Math.cos(this.toRad(lat2)) * Math.sin(dLng / 2) ** 2;
    return 2 * R * Math.asin(Math.sqrt(a));
  }

  private toRad(deg: number): number {
    return (deg * Math.PI) / 180;
  }
}
