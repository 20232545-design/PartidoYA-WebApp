import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable, of, tap } from 'rxjs';
import { API_BASE_URL } from '@shared/services/api-base-url.token';
import { UserResponse } from '@app/auth/auth.service';

@Injectable({ providedIn: 'root' })
export class UserService {
  private readonly apiUrl = `${inject(API_BASE_URL)}/users`;
  private readonly cache = new Map<number, UserResponse>();

  constructor(private http: HttpClient) {}

  getUserById(id: number): Observable<UserResponse> {
    const cached = this.cache.get(id);
    if (cached) return of(cached);
    return this.http.get<UserResponse>(`${this.apiUrl}/${id}`).pipe(
      tap(user => this.cache.set(id, user))
    );
  }

  changePassword(currentPassword: string, newPassword: string): Observable<void> {
    return this.http.put<void>(`${this.apiUrl}/me/password`, { currentPassword, newPassword });
  }
}
