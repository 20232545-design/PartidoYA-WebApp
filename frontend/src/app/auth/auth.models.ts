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

export interface RegistrationRequest {
  email: string;
  firstName: string;
  lastName: string;
  password: string;
  role: 'JUGADOR' | 'ADMIN_CANCHA';
  planCode: string;
  phone: string;
  district: string;
  primarySport: string | null;
  skillLevel: string | null;
  availability: string[];
  businessName: string | null;
  businessPhone: string | null;
  operationDistrict: string | null;
  termsAccepted: boolean;
  privacyAccepted: boolean;
  marketingConsent: boolean;
}
