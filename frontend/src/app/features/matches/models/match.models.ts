export interface MatchResponse {
  id: number;
  organizerId: number;
  sport: string;
  title: string;
  description: string | null;
  address: string;
  matchDate: string;
  totalSlots: number;
  availableSlots: number;
  price: number | null;
  latitude: number | null;
  longitude: number | null;
  status: string;
  participantIds: number[];
  courtReservationId: number | null;
  requiresPlayerPayment: boolean;
  yapePhone: string | null;
  playerPaymentAmount: number | null;
}

export interface CreateMatchRequest {
  organizerId: number;
  courtReservationId?: number | null;
  sport: string;
  title: string;
  description: string | null;
  address: string;
  matchDate: string;
  totalSlots: number;
  price: number | null;
  latitude: number | null;
  longitude: number | null;
  requiresPlayerPayment?: boolean;
  yapePhone?: string | null;
}

export interface PlayerJoinRequestResponse {
  id: number;
  matchId: number;
  playerId: number;
  proofUrl: string;
  originalFileName: string;
  amount: number;
  status: string;
  submittedAt: string;
  reviewedAt: string | null;
  reviewedBy: number | null;
  rejectionReason: string | null;
}
