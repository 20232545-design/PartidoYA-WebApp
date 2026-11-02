export interface AuthResponse {
  token: string;
  userId: number;
  email: string;
  fullName: string;
  role: string;
  plan: string;
}

export interface UserResponse {
  id: number;
  email: string;
  fullName: string;
  emailVerified: boolean;
  role: string;
  plan: string;
  status?: string;
  suspensionReason?: string | null;
  suspendedUntil?: string | null;
  lastAdministrativeActionAt?: string | null;
}
