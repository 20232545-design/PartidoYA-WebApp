export interface PaymentNotification {
  id: string;
  matchId: number;
  requestId: number;
  playerId: number;
  playerName: string;
  matchTitle: string;
  submittedAt: string;
  status: string;
  hasProof: boolean;
  proofUrl: string;
}
