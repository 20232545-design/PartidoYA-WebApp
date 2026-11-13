import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { BehaviorSubject, filter, Observable, Subject, Subscription } from 'rxjs';
import { RealtimeService } from '@app/realtime/realtime.service';
import { API_BASE_URL } from '@shared/services/api-base-url.token';
import { MatchNotification, MatchNotificationPage } from './match-notification.model';

const NOTIFICATIONS_QUEUE = '/user/queue/notifications';
const PAGE_SIZE = 20;

/**
 * Notificaciones de partidos: carga inicial por REST y novedades en vivo por WebSocket.
 * Al reconectar vuelve a cargar la lista para recuperar lo ocurrido mientras no hubo conexión.
 */
@Injectable({ providedIn: 'root' })
export class MatchNotificationService {
  private readonly http = inject(HttpClient);
  private readonly realtime = inject(RealtimeService);
  private readonly apiUrl = `${inject(API_BASE_URL)}/notifications`;

  private readonly notificationsSubject = new BehaviorSubject<MatchNotification[]>([]);
  private readonly unreadCountSubject = new BehaviorSubject<number>(0);
  private readonly receivedSubject = new Subject<MatchNotification>();
  private feeds = new Subscription();
  private startedForUserId: number | null = null;

  readonly notifications$ = this.notificationsSubject.asObservable();
  readonly unreadCount$ = this.unreadCountSubject.asObservable();
  /** Solo las notificaciones que llegan en vivo, para que una pantalla reaccione al momento. */
  readonly received$: Observable<MatchNotification> = this.receivedSubject.asObservable();

  /** Idempotente por usuario: si cambia la cuenta, reinicia el canal para la nueva. */
  start(userId: number): void {
    if (this.startedForUserId === userId) return;
    this.stop();
    this.startedForUserId = userId;
    this.realtime.connect();
    this.reload();
    this.feeds.add(this.realtime.watch<MatchNotification>(NOTIFICATIONS_QUEUE)
      .subscribe(notification => this.receive(notification)));
    this.feeds.add(this.realtime.connected$.pipe(filter(Boolean))
      .subscribe(() => this.reload()));
  }

  stop(): void {
    if (this.startedForUserId === null) return;
    this.startedForUserId = null;
    this.feeds.unsubscribe();
    this.feeds = new Subscription();
    void this.realtime.disconnect();
    this.notificationsSubject.next([]);
    this.unreadCountSubject.next(0);
  }

  reload(): void {
    this.http.get<MatchNotificationPage>(this.apiUrl, { params: { page: 0, size: PAGE_SIZE } }).subscribe({
      next: page => {
        this.notificationsSubject.next(page.content);
        this.unreadCountSubject.next(page.unreadCount);
      },
      // Sin conexión o sesión vencida: se conserva lo último mostrado.
      error: () => {}
    });
  }

  markAsRead(notification: MatchNotification): void {
    if (notification.read) return;
    this.http.post<MatchNotification>(`${this.apiUrl}/${notification.id}/read`, null).subscribe({
      next: updated => {
        this.notificationsSubject.next(this.notificationsSubject.value
          .map(current => current.id === updated.id ? updated : current));
        this.unreadCountSubject.next(Math.max(0, this.unreadCountSubject.value - 1));
      },
      error: () => this.reload()
    });
  }

  markAllAsRead(): void {
    this.http.post<void>(`${this.apiUrl}/read-all`, null).subscribe({
      next: () => {
        this.notificationsSubject.next(this.notificationsSubject.value.map(current => ({ ...current, read: true })));
        this.unreadCountSubject.next(0);
      },
      error: () => this.reload()
    });
  }

  /** Al abrir el chat de un partido, su aviso de mensajes deja de estar pendiente. */
  markChatAsRead(matchId: number): void {
    const isPendingChat = (notification: MatchNotification) =>
      notification.matchId === matchId && notification.type === 'CHAT_MESSAGES' && !notification.read;
    this.http.post<void>(`${this.apiUrl}/matches/${matchId}/chat/read`, null).subscribe({
      next: () => {
        const current = this.notificationsSubject.value;
        const pending = current.filter(isPendingChat).length;
        if (pending === 0) return;
        this.notificationsSubject.next(current.map(item => isPendingChat(item) ? { ...item, read: true } : item));
        this.unreadCountSubject.next(Math.max(0, this.unreadCountSubject.value - pending));
      },
      // No es crítico: el aviso se podrá marcar como leído desde la lista.
      error: () => {}
    });
  }

  /**
   * Inserta o reemplaza (las de chat se actualizan con el mismo id) y la sube al inicio.
   * Una notificación repetida tras reconectar no altera el contador.
   */
  private receive(notification: MatchNotification): void {
    const current = this.notificationsSubject.value;
    const previous = current.find(existing => existing.id === notification.id);
    const others = current.filter(existing => existing.id !== notification.id);
    this.notificationsSubject.next([notification, ...others].slice(0, PAGE_SIZE));
    const wasUnread = previous !== undefined && !previous.read;
    const unreadDelta = (notification.read ? 0 : 1) - (wasUnread ? 1 : 0);
    if (unreadDelta !== 0) {
      this.unreadCountSubject.next(Math.max(0, this.unreadCountSubject.value + unreadDelta));
    }
    this.receivedSubject.next(notification);
  }
}
