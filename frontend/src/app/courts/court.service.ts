import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { API_BASE_URL } from '@shared/services/api-base-url.token';
import {
  CourtResponse,
  SaveCourtRequest
} from './court.models';

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
} from './court.models';

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

  createCourt(request: SaveCourtRequest): Observable<CourtResponse> {
    return this.http.post<CourtResponse>(this.apiUrl, request);
  }

  updateCourt(id: number, request: SaveCourtRequest): Observable<CourtResponse> {
    return this.http.put<CourtResponse>(`${this.apiUrl}/${id}`, request);
  }

  setPublication(id: number, requesterId: number, published: boolean): Observable<CourtResponse> {
    return this.http.post<CourtResponse>(`${this.apiUrl}/${id}/publication`, { requesterId, published });
  }

}
