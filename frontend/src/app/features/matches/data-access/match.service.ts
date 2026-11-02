import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { API_BASE_URL } from '@core/http/api-base-url.token';
import { CreateMatchRequest, MatchResponse, PlayerJoinRequestResponse } from '../models/match.models';

export type { CreateMatchRequest, MatchResponse, PlayerJoinRequestResponse } from '../models/match.models';

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

  submitJoinProof(matchId: number, userId: number, proof: File): Observable<PlayerJoinRequestResponse> {
    const formData = new FormData();
    formData.append('proof', proof);
    return this.http.post<PlayerJoinRequestResponse>(`${this.apiUrl}/${matchId}/join-requests?userId=${userId}`, formData);
  }

  getJoinRequests(matchId: number, requesterId: number): Observable<PlayerJoinRequestResponse[]> {
    return this.http.get<PlayerJoinRequestResponse[]>(`${this.apiUrl}/${matchId}/join-requests`, { params: { requesterId } });
  }

  approveJoinRequest(matchId: number, requestId: number, reviewerId: number): Observable<PlayerJoinRequestResponse> {
    return this.http.post<PlayerJoinRequestResponse>(`${this.apiUrl}/${matchId}/join-requests/${requestId}/approve?reviewerId=${reviewerId}`, {});
  }

  rejectJoinRequest(matchId: number, requestId: number, reviewerId: number, rejectionReason: string): Observable<PlayerJoinRequestResponse> {
    return this.http.post<PlayerJoinRequestResponse>(`${this.apiUrl}/${matchId}/join-requests/${requestId}/reject`, {
      reviewerId,
      rejectionReason
    });
  }

  getMatchesByOrganizer(userId: number): Observable<MatchResponse[]> {
    return this.http.get<MatchResponse[]>(`${this.apiUrl}/organized/${userId}`);
  }

  getMatchesByParticipant(userId: number): Observable<MatchResponse[]> {
    return this.http.get<MatchResponse[]>(`${this.apiUrl}/joined/${userId}`);
  }
}
