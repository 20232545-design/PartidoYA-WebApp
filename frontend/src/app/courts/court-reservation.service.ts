import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { API_BASE_URL } from '@shared/services/api-base-url.token';
import { ManagedReservationResponse, ReservationResponse } from './court.models';

@Injectable({ providedIn: 'root' })
export class CourtReservationService {
  private readonly apiUrl = `${inject(API_BASE_URL)}/courts`;

  constructor(private http: HttpClient) {}

  getManagedReservations(
    ownerId: number,
    from: string,
    to: string,
    courtId?: number
  ): Observable<ManagedReservationResponse[]> {
    let params = new HttpParams().set('from', from).set('to', to);
    if (courtId) params = params.set('courtId', courtId);
    return this.http.get<ManagedReservationResponse[]>(`${this.apiUrl}/managed/${ownerId}/reservations`, { params });
  }

  reserveCourt(
    courtId: number,
    userId: number,
    date: string,
    startTime: string,
    endTime: string,
    paymentMethod: string,
    culqiToken: string,
    payerEmail: string,
    idempotencyKey: string
  ): Observable<ReservationResponse> {
    return this.http.post<ReservationResponse>(`${this.apiUrl}/${courtId}/reservations`, {
      userId,
      date,
      startTime,
      endTime,
      paymentMethod,
      culqiToken,
      payerEmail,
      idempotencyKey
    });
  }
}
