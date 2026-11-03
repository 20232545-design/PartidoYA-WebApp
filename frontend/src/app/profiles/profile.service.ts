import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { API_BASE_URL } from '@shared/services/api-base-url.token';
import { ProfileResponse } from './profile.models';

export type { ProfileResponse, SportPreferenceResponse } from './profile.models';

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

  updateProfile(userId: number, profile: {
    phoneNumber: string;
    district: string;
    primarySport: string | null;
    skillLevel: string | null;
    availability: string[];
    businessName: string | null;
    businessPhone: string | null;
    operationDistrict: string | null;
    favoriteSports?: string[];
  }): Observable<ProfileResponse> {
    return this.http.patch<ProfileResponse>(`${this.apiUrl}/profiles/user/${userId}`, profile);
  }

  updatePlayerProfile(profile: {
    phoneNumber: string;
    district: string;
    primarySport: string | null;
    skillLevel: string | null;
    availability: string[];
    favoriteSports: string[];
  }): Observable<ProfileResponse> {
    return this.http.patch<ProfileResponse>(`${this.apiUrl}/profiles/me/player`, {
      ...profile,
      businessName: null,
      businessPhone: null,
      operationDistrict: null
    });
  }

  updateCourtAdminProfile(profile: {
    phoneNumber: string;
    district: string;
    businessName: string | null;
    businessPhone: string | null;
    operationDistrict: string | null;
  }): Observable<ProfileResponse> {
    return this.http.patch<ProfileResponse>(`${this.apiUrl}/profiles/me/court-admin`, {
      ...profile,
      primarySport: null,
      skillLevel: null,
      availability: [],
      favoriteSports: []
    });
  }
}
