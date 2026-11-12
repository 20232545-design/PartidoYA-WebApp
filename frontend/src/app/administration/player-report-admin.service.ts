import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { API_BASE_URL } from '@shared/services/api-base-url.token';
import {
  PlayerReportPageResponse,
  PlayerReportResponse,
  PlayerReportStatus
} from '@shared/models/player-report.models';

export type PlayerReportFilter = PlayerReportStatus | 'ALL';
export type PlayerReportOutcome = Exclude<PlayerReportStatus, 'PENDING'>;

@Injectable({ providedIn: 'root' })
export class PlayerReportAdminService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = `${inject(API_BASE_URL)}/admin/player-reports`;

  search(status: PlayerReportFilter, page: number, size: number): Observable<PlayerReportPageResponse> {
    const params = new HttpParams().set('status', status).set('page', page).set('size', size);
    return this.http.get<PlayerReportPageResponse>(this.apiUrl, { params });
  }

  resolve(reportId: number, outcome: PlayerReportOutcome, note: string | null): Observable<PlayerReportResponse> {
    return this.http.patch<PlayerReportResponse>(`${this.apiUrl}/${reportId}/resolve`, { outcome, note });
  }
}
