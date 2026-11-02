import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { API_BASE_URL } from '@shared/services/api-base-url.token';
import { CourtAvailabilityResponse, SaveCourtAvailabilityRequest } from './court.models';

@Injectable({ providedIn: 'root' })
export class CourtAvailabilityService {
  private readonly apiUrl = `${inject(API_BASE_URL)}/courts`;

  constructor(private http: HttpClient) {}

  getManagedAvailability(
    ownerId: number,
    from: string,
    to: string,
    courtId?: number
  ): Observable<CourtAvailabilityResponse[]> {
    let params = new HttpParams().set('from', from).set('to', to);
    if (courtId) params = params.set('courtId', courtId);
    return this.http.get<CourtAvailabilityResponse[]>(`${this.apiUrl}/managed/${ownerId}/availability`, { params });
  }

  createAvailability(
    ownerId: number,
    request: SaveCourtAvailabilityRequest
  ): Observable<CourtAvailabilityResponse> {
    return this.http.post<CourtAvailabilityResponse>(`${this.apiUrl}/managed/${ownerId}/availability`, request);
  }

  updateAvailability(
    ownerId: number,
    availabilityId: number,
    request: SaveCourtAvailabilityRequest
  ): Observable<CourtAvailabilityResponse> {
    return this.http.put<CourtAvailabilityResponse>(
      `${this.apiUrl}/managed/${ownerId}/availability/${availabilityId}`,
      request
    );
  }

  deleteAvailability(ownerId: number, availabilityId: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/managed/${ownerId}/availability/${availabilityId}`);
  }

  getAvailableSchedules(courtId: number, date: string): Observable<string[]> {
    return this.http.get<string[]>(`${this.apiUrl}/${courtId}/available-schedules`, {
      params: new HttpParams().set('date', date)
    });
  }
}
