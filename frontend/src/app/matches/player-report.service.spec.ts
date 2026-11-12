import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { afterEach, beforeEach, describe, expect, it } from 'vitest';
import { API_BASE_URL } from '@shared/services/api-base-url.token';
import { reportReasonLabel } from '@shared/models/player-report.models';
import { PlayerReportService } from './player-report.service';

describe('PlayerReportService', () => {
  let service: PlayerReportService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: API_BASE_URL, useValue: 'http://api.test/api/v1' }
      ]
    });
    service = TestBed.inject(PlayerReportService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('posts the report without sending the reporter id', () => {
    service.submit(5, { reportedPlayerId: 18, reason: 'HARASSMENT', details: null }).subscribe();

    const request = http.expectOne('http://api.test/api/v1/matches/5/reports');
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual({ reportedPlayerId: 18, reason: 'HARASSMENT', details: null });
    request.flush({ id: 1, matchId: 5, reportedPlayerId: 18, status: 'PENDING', submittedAt: '2026-11-12T18:00' });
  });

  it('loads the players already reported by the current user', () => {
    let reported: number[] = [];
    service.reportedByMe(5).subscribe(ids => (reported = ids));

    http.expectOne('http://api.test/api/v1/matches/5/reports/mine').flush([7, 18]);

    expect(reported).toEqual([7, 18]);
  });
});

describe('reportReasonLabel', () => {
  it('translates known reasons and falls back to the raw value', () => {
    expect(reportReasonLabel('NO_SHOW')).toBe('Inasistencia');
    expect(reportReasonLabel('SOMETHING_NEW')).toBe('SOMETHING_NEW');
  });
});
