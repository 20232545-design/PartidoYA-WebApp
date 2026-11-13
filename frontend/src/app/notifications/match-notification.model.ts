export type MatchNotificationType =
  | 'PLAYER_JOINED'
  | 'PLAYER_LEFT'
  | 'MATCH_FULL'
  | 'MATCH_CANCELLED'
  | 'PAYMENT_APPROVED'
  | 'PAYMENT_REJECTED'
  | 'CHAT_MESSAGES';

/** Aviso del backend sobre un partido en el que el usuario participa u organiza. */
export interface MatchNotification {
  id: number;
  matchId: number;
  type: MatchNotificationType;
  message: string;
  read: boolean;
  occurredAt: string;
  /** En las de chat, cuántos mensajes sin leer agrupa; 1 en las demás. */
  groupedCount: number;
}

export interface MatchNotificationPage {
  content: MatchNotification[];
  unreadCount: number;
  page: number;
  size: number;
}

const TYPE_LABELS: Record<MatchNotificationType, string> = {
  PLAYER_JOINED: 'Nuevo jugador',
  PLAYER_LEFT: 'Cupo liberado',
  MATCH_FULL: 'Partido completo',
  MATCH_CANCELLED: 'Cancelado',
  PAYMENT_APPROVED: 'Pago aprobado',
  PAYMENT_REJECTED: 'Pago rechazado',
  CHAT_MESSAGES: 'Chat'
};

export function matchNotificationLabel(type: string): string {
  return TYPE_LABELS[type as MatchNotificationType] ?? type;
}
