import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { API_BASE_URL } from '@shared/services/api-base-url.token';
import { PlayerJoinRequestResponse } from './match.models';

@Injectable({ providedIn: 'root' })
export class JoinRequestService {
  private readonly apiUrl = `${inject(API_BASE_URL)}/matches`;

  constructor(private http: HttpClient) {}

  submitProof(matchId: number, userId: number, proof: File): Observable<PlayerJoinRequestResponse> {
    const formData = new FormData();
    formData.append('proof', proof);
    return this.http.post<PlayerJoinRequestResponse>(`${this.apiUrl}/${matchId}/join-requests?userId=${userId}`, formData);
  }

  getRequests(matchId: number, requesterId: number): Observable<PlayerJoinRequestResponse[]> {
    return this.http.get<PlayerJoinRequestResponse[]>(`${this.apiUrl}/${matchId}/join-requests`, {
      params: { requesterId }
    });
  }

  approve(matchId: number, requestId: number, reviewerId: number): Observable<PlayerJoinRequestResponse> {
    return this.http.post<PlayerJoinRequestResponse>(
      `${this.apiUrl}/${matchId}/join-requests/${requestId}/approve?reviewerId=${reviewerId}`,
      {}
    );
  }

  reject(
    matchId: number,
    requestId: number,
    reviewerId: number,
    rejectionReason: string
  ): Observable<PlayerJoinRequestResponse> {
    return this.http.post<PlayerJoinRequestResponse>(
      `${this.apiUrl}/${matchId}/join-requests/${requestId}/reject`,
      { reviewerId, rejectionReason }
    );
  }
}
