import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { RealtimeService } from '@app/realtime/realtime.service';
import { API_BASE_URL } from '@shared/services/api-base-url.token';

export const CHAT_MESSAGE_MAX_LENGTH = 1000;
export const CHAT_PAGE_SIZE = 50;

export interface ChatMessage {
  id: number;
  matchId: number;
  authorId: number;
  authorName: string;
  content: string;
  sentAt: string;
}

/** Une mensajes sin duplicados (el propio llega por la respuesta HTTP y por el WebSocket) y en orden. */
export function mergeChatMessages(current: readonly ChatMessage[], incoming: readonly ChatMessage[]): ChatMessage[] {
  const byId = new Map<number, ChatMessage>();
  for (const message of [...current, ...incoming]) byId.set(message.id, message);
  return [...byId.values()].sort((a, b) => a.id - b.id);
}

/** Chat del partido: historial y envío por REST, mensajes nuevos en vivo por WebSocket. */
@Injectable({ providedIn: 'root' })
export class MatchChatService {
  private readonly http = inject(HttpClient);
  private readonly realtime = inject(RealtimeService);
  private readonly apiUrl = `${inject(API_BASE_URL)}/matches`;

  /** Mensajes en orden cronológico; con `beforeId` devuelve los anteriores a ese mensaje. */
  history(matchId: number, beforeId?: number): Observable<ChatMessage[]> {
    let params = new HttpParams().set('limit', CHAT_PAGE_SIZE);
    if (beforeId !== undefined) params = params.set('before', beforeId);
    return this.http.get<ChatMessage[]>(`${this.apiUrl}/${matchId}/messages`, { params });
  }

  send(matchId: number, content: string): Observable<ChatMessage> {
    return this.http.post<ChatMessage>(`${this.apiUrl}/${matchId}/messages`, { content });
  }

  liveMessages(matchId: number): Observable<ChatMessage> {
    this.realtime.connect();
    return this.realtime.watch<ChatMessage>(`/topic/matches/${matchId}/chat`);
  }

  get connected$(): Observable<boolean> {
    return this.realtime.connected$;
  }
}
