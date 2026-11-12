import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { API_BASE_URL } from '@shared/services/api-base-url.token';
import { afterEach, beforeEach, describe, expect, it } from 'vitest';
import { SaveTeam } from './team.models';
import { TeamService } from './team.service';

describe('TeamService', () => {
  let service: TeamService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        TeamService,
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: API_BASE_URL, useValue: 'http://localhost:8080/api/v1' }
      ]
    });
    service = TestBed.inject(TeamService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('queries paginated teams with server-side filters', () => {
    service.search({ search: 'Leones', sport: 'Fútbol', open: true, page: 2, size: 12 }).subscribe();

    const request = http.expectOne(req => req.url === 'http://localhost:8080/api/v1/teams');
    expect(request.request.method).toBe('GET');
    expect(request.request.params.get('search')).toBe('Leones');
    expect(request.request.params.get('sport')).toBe('Fútbol');
    expect(request.request.params.get('open')).toBe('true');
    expect(request.request.params.get('page')).toBe('2');
    request.flush({ content: [], page: 2, size: 12, totalElements: 0, totalPages: 0 });
  });

  it('creates a real team resource using the authenticated API', () => {
    const body: SaveTeam = {
      name: 'Leones del Sur',
      sport: 'Fútbol',
      description: 'Equipo para competir cada semana',
      district: 'Surco',
      competitiveLevel: 'INTERMEDIATE',
      openToRequests: true
    };

    service.create(body).subscribe(result => expect(result.name).toBe('Leones del Sur'));

    const request = http.expectOne('http://localhost:8080/api/v1/teams');
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual(body);
    request.flush({ ...body, id: 10, logoUrl: null, status: 'ACTIVE', captainId: 1,
      captainName: 'Ana', memberCount: 1, createdAt: '2026-11-12T10:00:00Z', member: true,
      captain: true, pendingRequest: false,
      statistics: { available: false, played: 0, wins: 0, draws: 0, losses: 0, winPercentage: 0 } });
  });

  it('uses explicit invitation and challenge action endpoints', () => {
    service.invitation(8, 'accept').subscribe();
    http.expectOne('http://localhost:8080/api/v1/teams/invitations/8/accept').flush({});

    service.challengeAction(9, 'rejected').subscribe();
    http.expectOne('http://localhost:8080/api/v1/teams/challenges/9/rejected').flush({});
  });
});
