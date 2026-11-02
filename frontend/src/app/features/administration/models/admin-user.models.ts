export interface AdminUserResponse {
  id: number;
  email: string;
  fullName: string;
  role: string;
  plan: string;
  status: string;
  suspensionReason: string | null;
  suspendedUntil: string | null;
  lastAdministrativeActionAt: string | null;
  reservationCount: number;
  matchCount: number;
  averageRating: number | null;
}

export interface AdminUserActionResponse {
  id: number;
  adminId: number;
  action: string;
  reason: string | null;
  createdAt: string;
}

export interface AdminUserDetailResponse {
  user: AdminUserResponse;
  actions: AdminUserActionResponse[];
}

export interface AdminUserPageResponse {
  content: AdminUserResponse[];
  totalElements: number;
  page: number;
  size: number;
}
