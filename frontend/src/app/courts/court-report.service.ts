import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { API_BASE_URL } from '@shared/services/api-base-url.token';
import { CourtReportResponse } from './court.models';

@Injectable({ providedIn: 'root' })
export class CourtReportService {
  private readonly apiUrl = `${inject(API_BASE_URL)}/courts`;

  constructor(private http: HttpClient) {}

  getManagedReport(ownerId: number, from: string, to: string, courtId?: number): Observable<CourtReportResponse> {
    let params = new HttpParams().set('from', from).set('to', to);
    if (courtId) params = params.set('courtId', courtId);
    return this.http.get<CourtReportResponse>(`${this.apiUrl}/managed/${ownerId}/reports`, { params });
  }
}
