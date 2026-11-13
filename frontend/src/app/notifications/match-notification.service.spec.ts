import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { BehaviorSubject, firstValueFrom, Observable, Subject } from 'rxjs';
import { afterEach, beforeEach, describe, expect, it } from 'vitest';
import { RealtimeService } from '@app/realtime/realtime.service';
import { API_BASE_URL } from '@shared/services/api-base-url.token';
import { MatchNotification } from './match-notification.model';
import { MatchNotificationService } from './match-notification.service';

const LIST_URL = 'http://api.test/api/v1/notifications?page=0&size=20';

const notification = (id: number, read = false): MatchNotification => ({
  id,
  matchId: 3,
  type: 'PLAYER_JOINED',
  message: `Aviso ${id}`,
  read,
  occurredAt: '2026-11-12T18:00',
  groupedCount: 1
});

const chatNotification = (id: number, groupedCount: number, read = false): MatchNotification => ({
  ...notification(id, read),
  type: 'CHAT_MESSAGES',
  message: `${groupedCount} mensajes nuevos`,
  groupedCount
});

/** Doble de RealtimeService: permite simular conexiones y mensajes empujados por el servidor. */
class FakeRealtimeService {
  readonly connected$ = new BehaviorSubject<boolean>(false);
  readonly pushed = new Subject<unknown>();
  connectCalls = 0;
  disconnectCalls = 0;

  connect(): void {
    this.connectCalls++;
  }

  async disconnect(): Promise<void> {
    this.disconnectCalls++;
  }

  watch<T>(): Observable<T> {
    return this.pushed as Observable<T>;
  }
}

describe('MatchNotificationService', () => {
  let service: MatchNotificationService;
  let realtime: FakeRealtimeService;
  let http: HttpTestingController;

  beforeEach(() => {
    realtime = new FakeRealtimeService();
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: API_BASE_URL, useValue: 'http://api.test/api/v1' },
        { provide: RealtimeService, useValue: realtime }
      ]
    });
    service = TestBed.inject(MatchNotificationService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  const start = (initial: MatchNotification[] = [], unreadCount = 0) => {
    service.start(7);
    http.expectOne(LIST_URL).flush({ content: initial, unreadCount, page: 0, size: 20 });
  };

  it('connects and loads the first page on start', async () => {
    start([notification(1)], 1);

    expect(realtime.connectCalls).toBe(1);
    expect(await firstValueFrom(service.notifications$)).toEqual([notification(1)]);
    expect(await firstValueFrom(service.unreadCount$)).toBe(1);
  });

  it('is idempotent for the same user', () => {
    start();

    service.start(7);

    expect(realtime.connectCalls).toBe(1);
  });

  it('prepends live notifications and announces them', async () => {
    start([notification(1)], 1);
    const received: MatchNotification[] = [];
    service.received$.subscribe(item => received.push(item));

    realtime.pushed.next(notification(2));

    expect((await firstValueFrom(service.notifications$)).map(item => item.id)).toEqual([2, 1]);
    expect(await firstValueFrom(service.unreadCount$)).toBe(2);
    expect(received.map(item => item.id)).toEqual([2]);
  });

  it('does not count twice a notification repeated after a reconnection', async () => {
    start([notification(1)], 1);

    realtime.pushed.next(notification(1));

    expect((await firstValueFrom(service.notifications$)).map(item => item.id)).toEqual([1]);
    expect(await firstValueFrom(service.unreadCount$)).toBe(1);
  });

  it('replaces a grouped chat notification in place and moves it to the top', async () => {
    start([notification(1), chatNotification(2, 1)], 2);

    realtime.pushed.next(chatNotification(2, 3));

    const list = await firstValueFrom(service.notifications$);
    expect(list.map(item => item.id)).toEqual([2, 1]);
    expect(list[0].groupedCount).toBe(3);
    expect(await firstValueFrom(service.unreadCount$)).toBe(2);
  });

  it('counts a read chat notification again when new messages reopen it', async () => {
    start([chatNotification(2, 4, true)], 0);

    realtime.pushed.next(chatNotification(2, 1));

    expect(await firstValueFrom(service.unreadCount$)).toBe(1);
  });

  it('marks only the chat notification of that match as read', async () => {
    const otherMatchChat = { ...chatNotification(3, 2), matchId: 99 };
    start([chatNotification(2, 2), otherMatchChat, notification(1)], 3);

    service.markChatAsRead(3);
    const request = http.expectOne('http://api.test/api/v1/notifications/matches/3/chat/read');
    expect(request.request.method).toBe('POST');
    request.flush(null);

    const list = await firstValueFrom(service.notifications$);
    expect(list.filter(item => !item.read).map(item => item.id)).toEqual([3, 1]);
    expect(await firstValueFrom(service.unreadCount$)).toBe(2);
  });

  it('reloads after every (re)connection to recover what was missed', () => {
    start();

    realtime.connected$.next(true);

    http.expectOne(LIST_URL).flush({ content: [notification(5)], unreadCount: 1, page: 0, size: 20 });
  });

  it('marks a notification as read and lowers the unread count', async () => {
    start([notification(1), notification(2)], 2);

    service.markAsRead(notification(1));
    const request = http.expectOne('http://api.test/api/v1/notifications/1/read');
    expect(request.request.method).toBe('POST');
    request.flush(notification(1, true));

    expect((await firstValueFrom(service.notifications$)).find(item => item.id === 1)?.read).toBe(true);
    expect(await firstValueFrom(service.unreadCount$)).toBe(1);
  });

  it('does not call the API for notifications that are already read', () => {
    start();

    service.markAsRead(notification(1, true));

    http.expectNone('http://api.test/api/v1/notifications/1/read');
  });

  it('marks everything as read', async () => {
    start([notification(1), notification(2)], 2);

    service.markAllAsRead();
    http.expectOne('http://api.test/api/v1/notifications/read-all').flush(null);

    expect((await firstValueFrom(service.notifications$)).every(item => item.read)).toBe(true);
    expect(await firstValueFrom(service.unreadCount$)).toBe(0);
  });

  it('clears state and disconnects on stop', async () => {
    start([notification(1)], 1);

    service.stop();
    realtime.pushed.next(notification(9));

    expect(realtime.disconnectCalls).toBe(1);
    expect(await firstValueFrom(service.notifications$)).toEqual([]);
    expect(await firstValueFrom(service.unreadCount$)).toBe(0);
  });
});
