import { Injectable } from '@angular/core';
import * as L from 'leaflet';

export interface OpenRouteSummary {
  duration: string;
  distance: string;
}

@Injectable({ providedIn: 'root' })
export class OpenMapService {
  renderLocation(container: HTMLElement, latitude: number, longitude: number, title: string): void {
    const map = this.createMap(container, [latitude, longitude], 15);
    L.circleMarker([latitude, longitude], {
      radius: 9,
      color: '#0a0f1a',
      weight: 3,
      fillColor: '#c6f135',
      fillOpacity: 1
    }).addTo(map).bindPopup(title);
  }

  renderDirectRoute(
    container: HTMLElement,
    origin: L.LatLngExpression,
    destination: L.LatLngExpression
  ): OpenRouteSummary {
    const map = this.createMap(container, origin, 13);
    const originPoint = L.latLng(origin);
    const destinationPoint = L.latLng(destination);
    const route = L.polyline([originPoint, destinationPoint], {
      color: '#c6f135',
      weight: 5,
      opacity: 0.9,
      dashArray: '10 8'
    }).addTo(map);

    L.circleMarker(originPoint, {
      radius: 7,
      color: '#fff',
      weight: 2,
      fillColor: '#4ade80',
      fillOpacity: 1
    }).addTo(map).bindPopup('Tu ubicación');

    L.circleMarker(destinationPoint, {
      radius: 8,
      color: '#fff',
      weight: 2,
      fillColor: '#c6f135',
      fillOpacity: 1
    }).addTo(map).bindPopup('Cancha');

    map.fitBounds(route.getBounds(), { padding: [42, 42] });

    const distanceKm = originPoint.distanceTo(destinationPoint) / 1000;
    const estimatedMinutes = Math.max(1, Math.round((distanceKm / 25) * 60));
    return {
      duration: `≈ ${estimatedMinutes} min`,
      distance: `≈ ${distanceKm.toFixed(1)} km en línea directa`
    };
  }

  private createMap(container: HTMLElement, center: L.LatLngExpression, zoom: number): L.Map {
    container.replaceChildren();
    delete (container as HTMLElement & { _leaflet_id?: number })._leaflet_id;
    const map = L.map(container, { zoomControl: true, attributionControl: true }).setView(center, zoom);
    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
      maxZoom: 19,
      attribution: '&copy; OpenStreetMap contributors'
    }).addTo(map);
    return map;
  }
}
