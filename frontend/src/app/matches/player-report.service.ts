import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { API_BASE_URL } from '@shared/services/api-base-url.token';
import {
  SubmitPlayerReportRequest,
  SubmittedPlayerReportResponse
} from '@shared/models/player-report.models';

/** Reportes enviados por jugadores. Quien reporta se toma del token en el backend. */
@Injectable({ providedIn: 'root' })
export class PlayerReportService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = `${inject(API_BASE_URL)}/matches`;

  submit(matchId: number, request: SubmitPlayerReportRequest): Observable<SubmittedPlayerReportResponse> {
    return this.http.post<SubmittedPlayerReportResponse>(`${this.apiUrl}/${matchId}/reports`, request);
  }

  /** Ids de los jugadores que el usuario actual ya reportó en el partido. */
  reportedByMe(matchId: number): Observable<number[]> {
    return this.http.get<number[]>(`${this.apiUrl}/${matchId}/reports/mine`);
  }
}
