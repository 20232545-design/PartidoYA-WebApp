export type ReportReason =
  | 'UNSPORTSMANLIKE_CONDUCT'
  | 'HARASSMENT'
  | 'NO_SHOW'
  | 'FALSE_INFORMATION'
  | 'OTHER';

export type PlayerReportStatus = 'PENDING' | 'ACTION_TAKEN' | 'DISMISSED';

/** Opciones en el orden en que se muestran al usuario. */
export const REPORT_REASON_OPTIONS: ReadonlyArray<{ value: ReportReason; label: string }> = [
  { value: 'UNSPORTSMANLIKE_CONDUCT', label: 'Comportamiento antideportivo' },
  { value: 'HARASSMENT', label: 'Insultos o acoso' },
  { value: 'NO_SHOW', label: 'Inasistencia' },
  { value: 'FALSE_INFORMATION', label: 'Información falsa' },
  { value: 'OTHER', label: 'Otro motivo' }
];

export const REPORT_DETAILS_MAX_LENGTH = 1000;
export const RESOLUTION_NOTE_MAX_LENGTH = 500;

export function reportReasonLabel(reason: ReportReason | string): string {
  return REPORT_REASON_OPTIONS.find(option => option.value === reason)?.label ?? reason;
}

export interface SubmitPlayerReportRequest {
  reportedPlayerId: number;
  reason: ReportReason;
  details: string | null;
}

export interface SubmittedPlayerReportResponse {
  id: number;
  matchId: number;
  reportedPlayerId: number;
  status: PlayerReportStatus;
  submittedAt: string;
}

export interface PlayerReportResponse {
  id: number;
  matchId: number;
  matchTitle: string;
  reporterId: number;
  reporterName: string;
  reportedPlayerId: number;
  reportedPlayerName: string;
  reason: ReportReason;
  details: string | null;
  status: PlayerReportStatus;
  submittedAt: string;
  reviewedAt: string | null;
  reviewedBy: number | null;
  resolutionNote: string | null;
}

export interface PlayerReportPageResponse {
  content: PlayerReportResponse[];
  totalElements: number;
  page: number;
  size: number;
}
