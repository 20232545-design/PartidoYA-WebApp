import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, Router, convertToParamMap } from '@angular/router';
import { AuthService } from '@app/auth/auth.service';
import { of } from 'rxjs';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { Team } from '../team.models';
import { TeamService } from '../team.service';
import { TeamListComponent } from './team-list.component';

describe('TeamListComponent', () => {
  let fixture: ComponentFixture<TeamListComponent>;
  let component: TeamListComponent;
  let service: { search: ReturnType<typeof vi.fn>; mine: ReturnType<typeof vi.fn>; apply: ReturnType<typeof vi.fn>; invitation: ReturnType<typeof vi.fn> };
  let router: { navigate: ReturnType<typeof vi.fn> };
  const team: Team = {
    id: 1, name: 'Leones', sport: 'Fútbol', description: 'Equipo competitivo de Miraflores',
    logoUrl: null, district: 'Miraflores', competitiveLevel: 'INTERMEDIATE', openToRequests: true,
    status: 'ACTIVE', captainId: 8, captainName: 'Ana', memberCount: 4, maxMembers: 30,
    createdAt: '2026-11-12T10:00:00Z', member: false, captain: false, pendingRequest: false,
    viewerRelationship: 'EXTERNAL',
    statistics: { available: false, played: 0, wins: 0, draws: 0, losses: 0, winPercentage: 0 }
  };

  beforeEach(() => {
    team.pendingRequest = false;
    team.viewerRelationship = 'EXTERNAL';
    service = {
      search: vi.fn(() => of({ content: [team], page: 0, size: 9, totalElements: 10, totalPages: 2 })),
      mine: vi.fn(() => of({ administered: [], memberships: [], invitations: [], applications: [], challenges: [], pendingCount: 0 })),
      apply: vi.fn(() => of({})),
      invitation: vi.fn(() => of({}))
    };
    router = { navigate: vi.fn(() => Promise.resolve(true)) };
    TestBed.configureTestingModule({
      imports: [TeamListComponent],
      providers: [
        { provide: TeamService, useValue: service },
        { provide: AuthService, useValue: { isPlayerPlan: () => true } },
        { provide: Router, useValue: router },
        { provide: ActivatedRoute, useValue: { queryParamMap: of(convertToParamMap({ tab: 'explore', page: '0' })) } }
      ]
    });
    fixture = TestBed.createComponent(TeamListComponent);
    component = fixture.componentInstance;
  });

  it('requests nine teams from the backend and renders numeric pagination', async () => {
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();

    expect(service.search).toHaveBeenCalledWith(expect.objectContaining({ page: 0, size: 9 }));
    const pageNumbers = fixture.nativeElement.querySelectorAll('.page-number');
    expect(pageNumbers.length).toBe(2);
    expect((fixture.nativeElement as HTMLElement).textContent).toContain('10 equipos encontrados');
  });

  it('prevents duplicate requests and marks a successful application as pending', async () => {
    expect(component.canApply(team)).toBe(true);
    await component.apply(team);
    await component.apply(team);

    expect(service.apply).toHaveBeenCalledTimes(1);
    expect(team.pendingRequest).toBe(true);
    expect(component.canApply(team)).toBe(false);
  });

  it('does not allow an application to a full team', () => {
    const fullTeam = { ...team, pendingRequest: false, memberCount: 30, maxMembers: 30 };
    expect(component.canApply(fullTeam)).toBe(false);
  });
});
