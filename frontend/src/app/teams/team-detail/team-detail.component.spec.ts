import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, provideRouter } from '@angular/router';
import { AuthService } from '@app/auth/auth.service';
import { of, throwError } from 'rxjs';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { Team } from '../team.models';
import { TeamService } from '../team.service';
import { TeamDetailComponent } from './team-detail.component';

describe('TeamDetailComponent', () => {
  let fixture: ComponentFixture<TeamDetailComponent>;
  let component: TeamDetailComponent;
  const team: Team = {
    id: 15, name: 'Oxapampinos FC', sport: 'Fútbol', description: 'Equipo competitivo de Lince',
    logoUrl: null, district: 'Lince', competitiveLevel: 'ADVANCED', openToRequests: true,
    status: 'ACTIVE', captainId: 7, captainName: 'Sebastián Rodriguez', memberCount: 1, maxMembers: 30,
    createdAt: '2026-11-12T10:00:00Z', member: true, captain: true, pendingRequest: false,
    viewerRelationship: 'CAPTAIN',
    statistics: { available: false, played: 0, wins: 0, draws: 0, losses: 0, winPercentage: 0 }
  };

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [TeamDetailComponent],
      providers: [
        provideRouter([]),
        { provide: AuthService, useValue: { isPlayerPlan: () => true } },
        { provide: ActivatedRoute, useValue: { snapshot: { paramMap: { get: () => '15' } } } },
        {
          provide: TeamService,
          useValue: {
            get: vi.fn(() => of({ ...team })),
            members: vi.fn(() => throwError(() => new Error('members unavailable'))),
            mine: vi.fn(() => throwError(() => new Error('session unavailable'))),
            apply: vi.fn(() => of({})), leave: vi.fn(() => of(null)), challenge: vi.fn(() => of({}))
          }
        }
      ]
    });
    fixture = TestBed.createComponent(TeamDetailComponent);
    component = fixture.componentInstance;
  });

  async function loadAndRender(): Promise<void> {
    await component.load();
  }

  it('keeps the loaded team and scopes secondary failures to their sections', async () => {
    await loadAndRender();

    expect(component.team?.name).toBe('Oxapampinos FC');
    expect(component.teamError).toBe('');
    expect(component.membersError).toContain('members unavailable');
    expect(component.relatedError).toContain('session unavailable');
  });

  it('shows administration to the authoritative captain relationship', async () => {
    await loadAndRender();

    expect(component.isCaptain).toBe(true);
    expect(component.canApply).toBe(false);
  });

  it('never offers another request to a member or pending player', async () => {
    await loadAndRender();
    component.team = { ...team, captain: false, viewerRelationship: 'MEMBER' };
    expect(component.isMember).toBe(true);
    expect(component.canApply).toBe(false);

    component.team = { ...team, member: false, captain: false, pendingRequest: true, viewerRelationship: 'PENDING' };
    expect(component.isPending).toBe(true);
    expect(component.canApply).toBe(false);
  });

  it('offers a request only to an authenticated external player', async () => {
    await loadAndRender();
    component.team = { ...team, member: false, captain: false, viewerRelationship: 'EXTERNAL' };
    expect(component.canApply).toBe(true);
  });

  it('moves an accepted application to pending without offering a duplicate request', async () => {
    await loadAndRender();
    component.team = { ...team, member: false, captain: false, viewerRelationship: 'EXTERNAL' };

    await component.apply();

    expect(component.team.viewerRelationship).toBe('PENDING');
    expect(component.team.pendingRequest).toBe(true);
    expect(component.canApply).toBe(false);
  });
});
