import { CommonModule } from '@angular/common';
import { Component, OnDestroy, OnInit, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { AuthService } from '@app/auth/auth.service';
import { Subject, debounceTime, distinctUntilChanged, firstValueFrom, takeUntil } from 'rxjs';
import { teamErrorMessage } from '../team-error';
import { MyTeams, Team } from '../team.models';
import { TeamService } from '../team.service';

type TeamTab = 'explore' | 'mine' | 'requests';

@Component({
  selector: 'app-team-list',
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './team-list.component.html',
  styleUrl: '../teams.scss'
})
export class TeamListComponent implements OnInit, OnDestroy {
  private readonly service = inject(TeamService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly destroy$ = new Subject<void>();
  private readonly searchChanges$ = new Subject<string>();
  private loadVersion = 0;

  readonly auth = inject(AuthService);
  activeTab: TeamTab = 'explore';
  teams: Team[] = [];
  myTeams: MyTeams | null = null;
  loading = true;
  error = '';
  actionTeamId: number | null = null;
  page = 0;
  totalPages = 0;
  totalElements = 0;
  search = '';
  sport = '';
  district = '';
  level = '';
  recruitment = '';

  ngOnInit(): void {
    this.searchChanges$.pipe(
      debounceTime(400),
      distinctUntilChanged(),
      takeUntil(this.destroy$)
    ).subscribe(() => this.applyFilters());

    this.route.queryParamMap.pipe(takeUntil(this.destroy$)).subscribe(params => {
      const requestedTab = params.get('tab') as TeamTab | null;
      this.activeTab = requestedTab === 'mine' || requestedTab === 'requests' ? requestedTab : 'explore';
      if (this.activeTab !== 'explore' && !this.auth.isPlayerPlan()) {
        void this.router.navigate([], { relativeTo: this.route, queryParams: { tab: 'explore' }, replaceUrl: true });
        return;
      }
      this.page = Math.max(0, Number(params.get('page') || 0) || 0);
      this.search = params.get('search') || '';
      this.sport = params.get('sport') || '';
      this.district = params.get('district') || '';
      this.level = params.get('level') || '';
      this.recruitment = params.get('recruitment') || '';
      void this.loadActive();
    });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  selectTab(tab: TeamTab): void {
    if (tab !== 'explore' && !this.auth.isPlayerPlan()) return;
    void this.router.navigate([], {
      relativeTo: this.route,
      queryParams: { tab, page: tab === 'explore' ? this.page : null },
      queryParamsHandling: 'merge'
    });
  }

  searchChanged(): void {
    this.searchChanges$.next(this.search.trim());
  }

  applyFilters(): void {
    void this.router.navigate([], {
      relativeTo: this.route,
      queryParams: {
        tab: 'explore', page: 0,
        search: this.search.trim() || null, sport: this.sport || null,
        district: this.district.trim() || null, level: this.level || null,
        recruitment: this.recruitment || null
      }
    });
  }

  clearFilters(): void {
    this.search = ''; this.sport = ''; this.district = ''; this.level = ''; this.recruitment = '';
    this.applyFilters();
  }

  goToPage(page: number): void {
    if (page < 0 || page >= this.totalPages || page === this.page) return;
    void this.router.navigate([], { relativeTo: this.route, queryParams: { page }, queryParamsHandling: 'merge' });
  }

  async apply(team: Team): Promise<void> {
    if (!this.auth.isPlayerPlan() || this.actionTeamId !== null || !this.canApply(team)) return;
    this.actionTeamId = team.id;
    this.error = '';
    try {
      await firstValueFrom(this.service.apply(team.id));
      team.pendingRequest = true;
      team.viewerRelationship = 'PENDING';
    } catch (error: unknown) {
      this.error = teamErrorMessage(error, 'No se pudo enviar la solicitud de ingreso.');
    } finally {
      this.actionTeamId = null;
    }
  }

  async respondInvitation(id: number, action: 'accept' | 'reject'): Promise<void> {
    if (this.actionTeamId !== null) return;
    this.actionTeamId = -id;
    this.error = '';
    try {
      await firstValueFrom(this.service.invitation(id, action));
      await this.loadMine();
    } catch (error: unknown) {
      this.error = teamErrorMessage(error, 'No se pudo responder la invitación.');
    } finally {
      this.actionTeamId = null;
    }
  }

  canApply(team: Team): boolean {
    return team.viewerRelationship === 'EXTERNAL' && team.openToRequests && team.memberCount < team.maxMembers;
  }

  isCaptain(team: Team): boolean { return team.viewerRelationship === 'CAPTAIN'; }
  isMember(team: Team): boolean { return team.viewerRelationship === 'MEMBER'; }
  isPending(team: Team): boolean { return team.viewerRelationship === 'PENDING'; }

  get visiblePages(): number[] {
    if (this.totalPages <= 7) return Array.from({ length: this.totalPages }, (_, index) => index);
    const start = Math.min(Math.max(0, this.page - 2), this.totalPages - 5);
    return Array.from({ length: 5 }, (_, index) => start + index);
  }

  recruitmentLabel(team: Team): string {
    if (team.memberCount >= team.maxMembers) return 'Equipo completo';
    return team.openToRequests ? 'Acepta solicitudes' : 'No acepta solicitudes';
  }

  levelLabel(level: string): string {
    return ({ BEGINNER: 'Básico', INTERMEDIATE: 'Intermedio', ADVANCED: 'Avanzado' } as Record<string, string>)[level] || level;
  }

  requestStatus(status: string): string {
    return ({ PENDING: 'Pendiente', APPROVED: 'Aprobada', ACCEPTED: 'Aceptada', REJECTED: 'Rechazada', CANCELLED: 'Cancelada', EXPIRED: 'Vencida' } as Record<string, string>)[status] || status;
  }

  initial(team: Team): string { return team.name.charAt(0).toUpperCase(); }
  imageFailed(team: Team): void { team.logoUrl = null; }

  private async loadActive(): Promise<void> {
    if (this.activeTab === 'explore') await this.loadExplore();
    else await this.loadMine();
  }

  private async loadExplore(): Promise<void> {
    const version = ++this.loadVersion;
    this.loading = true;
    this.error = '';
    try {
      const result = await firstValueFrom(this.service.search({
        search: this.search, sport: this.sport, district: this.district, level: this.level,
        open: this.recruitment === '' ? undefined : this.recruitment === 'open', page: this.page, size: 9
      }));
      if (version !== this.loadVersion) return;
      if (result.totalPages > 0 && this.page >= result.totalPages) {
        void this.router.navigate([], {
          relativeTo: this.route,
          queryParams: { page: result.totalPages - 1 },
          queryParamsHandling: 'merge',
          replaceUrl: true
        });
        return;
      }
      this.teams = result.content;
      this.page = result.page;
      this.totalPages = result.totalPages;
      this.totalElements = result.totalElements;
    } catch (error: unknown) {
      if (version === this.loadVersion) this.error = teamErrorMessage(error, 'No pudimos cargar los equipos.');
    } finally {
      if (version === this.loadVersion) this.loading = false;
    }
  }

  private async loadMine(): Promise<void> {
    const version = ++this.loadVersion;
    this.loading = true;
    this.error = '';
    try {
      const result = await firstValueFrom(this.service.mine());
      if (version === this.loadVersion) this.myTeams = result;
    } catch (error: unknown) {
      if (version === this.loadVersion) this.error = teamErrorMessage(error, 'No pudimos cargar tus equipos y solicitudes.');
    } finally {
      if (version === this.loadVersion) this.loading = false;
    }
  }
}
