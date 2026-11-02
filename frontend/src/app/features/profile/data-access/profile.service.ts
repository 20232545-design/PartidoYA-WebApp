import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { API_BASE_URL } from '@core/http/api-base-url.token';
import { ProfileResponse } from '../models/profile.models';

export type { ProfileResponse, SportPreferenceResponse } from '../models/profile.models';

@Injectable({ providedIn: 'root' })
export class ProfileService {
  private readonly apiUrl = inject(API_BASE_URL);

  constructor(private http: HttpClient) {}

  getProfileByUserId(userId: number): Observable<ProfileResponse> {
    return this.http.get<ProfileResponse>(`${this.apiUrl}/profiles/user/${userId}`);
  }

  updatePrimarySport(userId: number, primarySport: string): Observable<ProfileResponse> {
    return this.http.patch<ProfileResponse>(`${this.apiUrl}/profiles/user/${userId}/primary-sport`, { primarySport });
  }

  updateGamePreferences(userId: number, skillLevel: string, availability: string[]): Observable<ProfileResponse> {
    return this.http.patch<ProfileResponse>(`${this.apiUrl}/profiles/user/${userId}/game-preferences`, { skillLevel, availability });
  }

  updateFullName(userId: number, fullName: string): Observable<{ id: number; email: string; fullName: string; emailVerified: boolean; role: string; plan: string }> {
    return this.http.patch<{ id: number; email: string; fullName: string; emailVerified: boolean; role: string; plan: string }>(`${this.apiUrl}/users/${userId}`, { fullName });
  }

  updatePlan(userId: number, plan: string): Observable<{ id: number; email: string; fullName: string; emailVerified: boolean; role: string; plan: string }> {
    return this.http.patch<{ id: number; email: string; fullName: string; emailVerified: boolean; role: string; plan: string }>(`${this.apiUrl}/users/${userId}/plan`, { plan });
  }
}
