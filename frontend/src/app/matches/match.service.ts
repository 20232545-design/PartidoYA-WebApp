import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { API_BASE_URL } from '@shared/services/api-base-url.token';
import { CreateMatchRequest, MatchResponse } from './match.models';

export type { CreateMatchRequest, MatchResponse, PlayerJoinRequestResponse } from './match.models';

@Injectable({ providedIn: 'root' })
export class MatchService {
  private readonly apiUrl = `${inject(API_BASE_URL)}/matches`;

  constructor(private http: HttpClient) {}

  getOpenMatches(sport?: string): Observable<MatchResponse[]> {
    if (sport) {
      return this.http.get<MatchResponse[]>(this.apiUrl, { params: { sport } });
    }
    return this.http.get<MatchResponse[]>(this.apiUrl);
  }

  getMatchById(id: number): Observable<MatchResponse> {
    return this.http.get<MatchResponse>(`${this.apiUrl}/${id}`);
  }

  createMatch(request: CreateMatchRequest): Observable<MatchResponse> {
    return this.http.post<MatchResponse>(this.apiUrl, request);
  }

  joinMatch(matchId: number, userId: number): Observable<MatchResponse> {
    return this.http.post<MatchResponse>(`${this.apiUrl}/${matchId}/join`, { userId });
  }

  cancelMatch(matchId: number, requesterId: number): Observable<MatchResponse> {
    return this.http.post<MatchResponse>(`${this.apiUrl}/${matchId}/cancel`, { requesterId });
  }

  leaveMatch(matchId: number, userId: number): Observable<MatchResponse> {
    return this.http.post<MatchResponse>(`${this.apiUrl}/${matchId}/leave`, { userId });
  }

  getMatchesByOrganizer(userId: number): Observable<MatchResponse[]> {
    return this.http.get<MatchResponse[]>(`${this.apiUrl}/organized/${userId}`);
  }

  getMatchesByParticipant(userId: number): Observable<MatchResponse[]> {
    return this.http.get<MatchResponse[]>(`${this.apiUrl}/joined/${userId}`);
  }
}
