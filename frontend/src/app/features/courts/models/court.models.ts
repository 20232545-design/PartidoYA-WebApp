export interface CourtResponse {
  id: number;
  ownerId: number;
  name: string;
  complexName: string | null;
  description: string | null;
  address: string;
  district: string;
  latitude: number | null;
  longitude: number | null;
  pricePerHour: number;
  active: boolean;
  published: boolean;
  availableForReservations: boolean;
  sports: string[];
  schedules: string[];
  mainImageUrl: string | null;
  imageUrls: string[];
  services: string[];
  features: string[];
}

export interface SaveCourtRequest {
  ownerId?: number;
  requesterId?: number;
  name: string;
  complexName: string | null;
  description: string | null;
  address: string;
  district: string;
  latitude: number | null;
  longitude: number | null;
  pricePerHour: number;
  sports: string[];
  schedules: string[];
  mainImageUrl: string | null;
  imageUrls: string[];
  services: string[];
  features: string[];
}

export interface ReservationResponse {
  id: number;
  userId: number;
  courtId: number;
  date: string;
  startTime: string;
  endTime: string;
  price: number;
  currency: string;
  providerReference: string | null;
  status: string;
  paymentStatus: string;
  createdAt: string;
}

export interface ManagedReservationResponse {
  id: number;
  userId: number;
  customerName: string;
  courtId: number;
  courtName: string;
  date: string;
  startTime: string;
  endTime: string;
  price: number;
  currency: string;
  providerReference: string | null;
  status: string;
  paymentStatus: string;
  createdAt: string;
}

export interface CourtAvailabilityResponse {
  id: number;
  courtId: number;
  courtName: string;
  date: string;
  allDay: boolean;
  startTime: string;
  endTime: string;
  type: 'AVAILABLE' | 'BLOCKED';
  reason: string | null;
  createdAt: string;
}

export interface SaveCourtAvailabilityRequest {
  courtId: number;
  date: string;
  allDay: boolean;
  startTime: string;
  endTime: string;
  type: 'AVAILABLE' | 'BLOCKED';
  reason: string | null;
}

export interface DailyIncomeResponse {
  date: string;
  income: number;
  reservations: number;
}

export interface CourtRankingResponse {
  courtId: number;
  courtName: string;
  reservations: number;
  percentage: number;
}

export interface CourtReportResponse {
  from: string;
  to: string;
  totalIncome: number;
  confirmedReservations: number;
  canceledReservations: number;
  occupancyPercentage: number;
  averageTicket: number;
  dailyIncome: DailyIncomeResponse[];
  topCourt: CourtRankingResponse | null;
  ranking: CourtRankingResponse[];
}
