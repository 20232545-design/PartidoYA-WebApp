export interface SportPreferenceResponse {
  sportName: string;
  skillLevel: string;
}

export interface ProfileResponse {
  id: number;
  userId: number;
  phoneNumber: string | null;
  avatarUrl: string | null;
  primarySport: string | null;
  skillLevel: string | null;
  availability: string[];
  sportPreferences: SportPreferenceResponse[];
  district: string | null;
  businessName: string | null;
  businessPhone: string | null;
  operationDistrict: string | null;
}
