import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { API_BASE_URL } from '@shared/services/api-base-url.token';
import { PlayerJoinRequestResponse } from './match.models';

@Injectable({ providedIn: 'root' })
export class JoinRequestService {
  private readonly baseUrl = inject(API_BASE_URL);
  private readonly apiUrl = `${this.baseUrl}/matches`;

  constructor(private http: HttpClient) {}

  /**
   * Descarga la imagen del comprobante. Va por HttpClient para que el interceptor agregue el token:
   * un `<img src>` o un enlace directo los pide el navegador sin `Authorization` y el backend responde 401.
   *
   * @param proofPath ruta que devuelve el backend en `proofUrl`, por ejemplo `/api/v1/matches/3/join-requests/1/proof?...`
   */
  getProofImage(proofPath: string): Observable<Blob> {
    const origin = this.baseUrl.replace(/\/api\/v1\/?$/, '');
    return this.http.get(`${origin}${proofPath}`, { responseType: 'blob' });
  }

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
