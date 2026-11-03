import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { API_BASE_URL } from '@shared/services/api-base-url.token';
import {
  CourtResponse,
  SaveCourtRequest,
  CourtMediaResponse
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
  SaveCourtRequest,
  CourtMediaResponse
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

  listMedia(courtId: number): Observable<CourtMediaResponse[]> {
    return this.http.get<CourtMediaResponse[]>(`${this.apiUrl}/${courtId}/media`);
  }

  uploadMedia(courtId: number, file: File, primary = false): Observable<CourtMediaResponse> {
    const body = new FormData();
    body.append('file', file);
    return this.http.post<CourtMediaResponse>(`${this.apiUrl}/${courtId}/media`, body, {
      params: new HttpParams().set('primary', primary)
    });
  }

  deleteMedia(courtId: number, mediaId: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${courtId}/media/${mediaId}`);
  }

  reorderMedia(courtId: number, mediaIds: number[]): Observable<CourtMediaResponse[]> {
    return this.http.put<CourtMediaResponse[]>(`${this.apiUrl}/${courtId}/media/order`, { mediaIds });
  }

  setPrimaryMedia(courtId: number, mediaId: number): Observable<CourtMediaResponse> {
    return this.http.put<CourtMediaResponse>(`${this.apiUrl}/${courtId}/media/${mediaId}/primary`, {});
  }

}
