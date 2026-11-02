import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { API_BASE_URL } from '@core/http/api-base-url.token';
import {
  CourtAvailabilityResponse,
  CourtReportResponse,
  CourtResponse,
  ManagedReservationResponse,
  ReservationResponse,
  SaveCourtAvailabilityRequest,
  SaveCourtRequest
} from '../models/court.models';

export type {
  CourtAvailabilityResponse,
  CourtRankingResponse,
  CourtReportResponse,
  CourtResponse,
  DailyIncomeResponse,
  ManagedReservationResponse,
  ReservationResponse,
  SaveCourtAvailabilityRequest,
  SaveCourtRequest
} from '../models/court.models';

@Injectable({ providedIn: 'root' })
export class CourtService {
  private readonly apiUrl = `${inject(API_BASE_URL)}/courts`;

  constructor(private http: HttpClient) {}

  searchCourts(sport?: string, district?: string): Observable<CourtResponse[]> {
    let params = new HttpParams();
    if (sport) params = params.set('sport', sport);
    if (district) params = params.set('district', district);
    return this.http.get<CourtResponse[]>(this.apiUrl, { params });
  }

  getCourtById(id: number): Observable<CourtResponse> {
    return this.http.get<CourtResponse>(`${this.apiUrl}/${id}`);
  }

  getManagedCourts(ownerId: number): Observable<CourtResponse[]> {
    return this.http.get<CourtResponse[]>(`${this.apiUrl}/managed/${ownerId}`);
  }

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

  getManagedAvailability(ownerId: number, from: string, to: string, courtId?: number): Observable<CourtAvailabilityResponse[]> {
    let params = new HttpParams().set('from', from).set('to', to);
    if (courtId) params = params.set('courtId', courtId);
    return this.http.get<CourtAvailabilityResponse[]>(`${this.apiUrl}/managed/${ownerId}/availability`, { params });
  }

  createAvailability(ownerId: number, request: SaveCourtAvailabilityRequest): Observable<CourtAvailabilityResponse> {
    return this.http.post<CourtAvailabilityResponse>(`${this.apiUrl}/managed/${ownerId}/availability`, request);
  }

  updateAvailability(
    ownerId: number,
    availabilityId: number,
    request: SaveCourtAvailabilityRequest
  ): Observable<CourtAvailabilityResponse> {
    return this.http.put<CourtAvailabilityResponse>(`${this.apiUrl}/managed/${ownerId}/availability/${availabilityId}`, request);
  }

  deleteAvailability(ownerId: number, availabilityId: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/managed/${ownerId}/availability/${availabilityId}`);
  }

  getAvailableSchedules(id: number, date: string): Observable<string[]> {
    return this.http.get<string[]>(`${this.apiUrl}/${id}/available-schedules`, {
      params: new HttpParams().set('date', date)
    });
  }

  getManagedReport(ownerId: number, from: string, to: string, courtId?: number): Observable<CourtReportResponse> {
    let params = new HttpParams().set('from', from).set('to', to);
    if (courtId) params = params.set('courtId', courtId);
    return this.http.get<CourtReportResponse>(`${this.apiUrl}/managed/${ownerId}/reports`, { params });
  }

  createCourt(request: SaveCourtRequest): Observable<CourtResponse> {
    return this.http.post<CourtResponse>(this.apiUrl, request);
  }

  updateCourt(id: number, request: SaveCourtRequest): Observable<CourtResponse> {
    return this.http.put<CourtResponse>(`${this.apiUrl}/${id}`, request);
  }

  setPublication(id: number, requesterId: number, published: boolean): Observable<CourtResponse> {
    return this.http.post<CourtResponse>(`${this.apiUrl}/${id}/publication`, { requesterId, published });
  }

  reserveCourt(
    id: number,
    userId: number,
    date: string,
    startTime: string,
    endTime: string,
    paymentMethod: string,
    culqiToken: string,
    payerEmail: string,
    idempotencyKey: string
  ): Observable<ReservationResponse> {
    return this.http.post<ReservationResponse>(`${this.apiUrl}/${id}/reservations`, {
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
