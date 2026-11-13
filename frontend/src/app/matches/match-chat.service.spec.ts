import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { afterEach, beforeEach, describe, expect, it } from 'vitest';
import { API_BASE_URL } from '@shared/services/api-base-url.token';
import { ChatMessage, MatchChatService, mergeChatMessages } from './match-chat.service';

const message = (id: number, content = `m${id}`): ChatMessage => ({
  id,
  matchId: 5,
  authorId: 7,
  authorName: 'Ana',
  content,
  sentAt: '2026-11-12T18:00'
});

describe('mergeChatMessages', () => {
  it('keeps chronological order and removes duplicates', () => {
    const merged = mergeChatMessages([message(1), message(3)], [message(3), message(2), message(4)]);

    expect(merged.map(item => item.id)).toEqual([1, 2, 3, 4]);
  });

  it('prefers the incoming copy of a repeated message', () => {
    const merged = mergeChatMessages([message(1, 'old')], [message(1, 'new')]);

    expect(merged).toEqual([message(1, 'new')]);
  });

  it('does not mutate the current list', () => {
    const current = [message(2)];

    mergeChatMessages(current, [message(1)]);

    expect(current.map(item => item.id)).toEqual([2]);
  });
});

describe('MatchChatService', () => {
  let service: MatchChatService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: API_BASE_URL, useValue: 'http://api.test/api/v1' }
      ]
    });
    service = TestBed.inject(MatchChatService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('loads the latest page of messages', () => {
    let result: ChatMessage[] = [];
    service.history(5).subscribe(messages => (result = messages));

    http.expectOne('http://api.test/api/v1/matches/5/messages?limit=50').flush([message(1)]);

    expect(result).toEqual([message(1)]);
  });

  it('pages backwards with the before cursor', () => {
    service.history(5, 40).subscribe();

    http.expectOne('http://api.test/api/v1/matches/5/messages?limit=50&before=40').flush([]);
  });

  it('sends only the content; the author comes from the token', () => {
    service.send(5, 'Hola').subscribe();

    const request = http.expectOne('http://api.test/api/v1/matches/5/messages');
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual({ content: 'Hola' });
    request.flush(message(1, 'Hola'));
  });
});
