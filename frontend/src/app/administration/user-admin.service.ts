import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { API_BASE_URL } from '@shared/services/api-base-url.token';
import { UserResponse } from '@app/auth/auth.service';
import { AdminUserDetailResponse, AdminUserPageResponse } from './admin-user.models';

export type {
  AdminUserActionResponse,
  AdminUserDetailResponse,
  AdminUserPageResponse,
  AdminUserResponse
} from './admin-user.models';

@Injectable({ providedIn: 'root' })
export class UserAdminService {
  private readonly apiUrl = `${inject(API_BASE_URL)}/users/admin`;

  constructor(private http: HttpClient) {}

  search(adminId: number, search: string, role: string, page: number, size: number): Observable<AdminUserPageResponse> {
    const params = new HttpParams()
      .set('adminId', adminId)
      .set('search', search)
      .set('role', role)
      .set('page', page)
      .set('size', size);
    return this.http.get<AdminUserPageResponse>(this.apiUrl, { params });
  }

  detail(adminId: number, userId: number): Observable<AdminUserDetailResponse> {
    return this.http.get<AdminUserDetailResponse>(`${this.apiUrl}/${userId}`, {
      params: new HttpParams().set('adminId', adminId)
    });
  }

  activate(adminId: number, userId: number, reason: string): Observable<UserResponse> {
    return this.http.patch<UserResponse>(`${this.apiUrl}/${userId}/activate`, { adminId, reason });
  }

  suspend(adminId: number, userId: number, reason: string, suspendedUntil: string | null): Observable<UserResponse> {
    return this.http.patch<UserResponse>(`${this.apiUrl}/${userId}/suspend`, { adminId, reason, suspendedUntil });
  }

  reactivate(adminId: number, userId: number, reason: string): Observable<UserResponse> {
    return this.http.patch<UserResponse>(`${this.apiUrl}/${userId}/reactivate`, { adminId, reason });
  }
}
