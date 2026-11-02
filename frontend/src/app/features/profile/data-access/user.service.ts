import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable, of, tap } from 'rxjs';
import { API_BASE_URL } from '@core/http/api-base-url.token';
import { UserResponse } from '@core/auth/auth.service';

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
}
