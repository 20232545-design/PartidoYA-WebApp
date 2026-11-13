import { CommonModule } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { AuthService } from '@app/auth/auth.service';
import { firstValueFrom } from 'rxjs';
import { teamErrorMessage } from '../team-error';
import { MyTeams, Team, TeamMember } from '../team.models';
import { TeamService } from '../team.service';

@Component({
  selector: 'app-team-detail',
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './team-detail.component.html',
  styleUrl: '../teams.scss'
})
export class TeamDetailComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly service = inject(TeamService);
  readonly auth = inject(AuthService);
  team: Team | null = null;
  members: TeamMember[] = [];
  mine: MyTeams | null = null;
  teamLoading = true;
  membersLoading = false;
  relatedLoading = false;
  actionLoading = false;
  teamError = '';
  membersError = '';
  relatedError = '';
  actionError = '';
  message = '';
  challengerTeamId: number | null = null;
  challengeMessage = '';
  private loadVersion = 0;

  ngOnInit(): void { void this.load(); }

  async load(): Promise<void> {
    const version = ++this.loadVersion;
    this.teamLoading = true;
    this.teamError = '';
    try {
      const id = Number(this.route.snapshot.paramMap.get('id'));
      const team = await firstValueFrom(this.service.get(id));
      if (version !== this.loadVersion) return;
      this.team = team;
      this.teamLoading = false;
      await Promise.all([this.loadMembers(id, version), this.loadRelatedTeams(version)]);
    } catch (error: unknown) {
      if (version === this.loadVersion) this.teamError = teamErrorMessage(error, 'No se pudo cargar el equipo.');
    } finally {
      if (version === this.loadVersion) this.teamLoading = false;
    }
  }

  async apply(): Promise<void> {
    if (!this.team || this.actionLoading || !this.canApply) return;
    this.actionLoading = true;
    this.actionError = '';
    try {
      await firstValueFrom(this.service.apply(this.team.id));
      this.message = 'Solicitud enviada al capitán.';
      this.team.pendingRequest = true;
      this.team.viewerRelationship = 'PENDING';
    } catch (error: unknown) {
      this.actionError = teamErrorMessage(error, 'No se pudo enviar la solicitud.');
    } finally {
      this.actionLoading = false;
    }
  }

  get canApply(): boolean {
    return !!this.team && this.team.viewerRelationship === 'EXTERNAL' && this.team.openToRequests &&
      this.team.memberCount < this.team.maxMembers;
  }

  get isCaptain(): boolean { return this.team?.viewerRelationship === 'CAPTAIN'; }
  get isMember(): boolean { return this.team?.viewerRelationship === 'MEMBER'; }
  get isPending(): boolean { return this.team?.viewerRelationship === 'PENDING'; }

  async leave(): Promise<void> {
    if (!this.team || this.actionLoading || !confirm('¿Confirmas que deseas abandonar este equipo?')) return;
    this.actionLoading = true;
    this.actionError = '';
    try {
      await firstValueFrom(this.service.leave(this.team.id));
      this.message = 'Abandonaste el equipo.';
      await this.load();
    } catch (error: unknown) {
      this.actionError = teamErrorMessage(error, 'No se pudo abandonar el equipo.');
    } finally {
      this.actionLoading = false;
    }
  }

  async challenge(): Promise<void> {
    if (!this.team || !this.challengerTeamId || this.actionLoading) return;
    this.actionLoading = true;
    this.actionError = '';
    try {
      await firstValueFrom(this.service.challenge(this.team.id, this.challengerTeamId, this.challengeMessage));
      this.message = 'Desafío enviado al capitán rival.';
    } catch (error: unknown) {
      this.actionError = teamErrorMessage(error, 'No se pudo enviar el desafío.');
    } finally {
      this.actionLoading = false;
    }
  }

  imageFailed(): void { if (this.team) this.team.logoUrl = null; }

  private async loadMembers(teamId: number, version: number): Promise<void> {
    this.membersLoading = true;
    this.membersError = '';
    try {
      const members = await firstValueFrom(this.service.members(teamId));
      if (version === this.loadVersion) this.members = members;
    } catch (error: unknown) {
      if (version === this.loadVersion) this.membersError = teamErrorMessage(error, 'No se pudieron cargar los integrantes.');
    } finally {
      if (version === this.loadVersion) this.membersLoading = false;
    }
  }

  private async loadRelatedTeams(version: number): Promise<void> {
    if (!this.auth.isPlayerPlan()) return;
    this.relatedLoading = true;
    this.relatedError = '';
    try {
      const mine = await firstValueFrom(this.service.mine());
      if (version === this.loadVersion) this.mine = mine;
    } catch (error: unknown) {
      if (version === this.loadVersion) this.relatedError = teamErrorMessage(error, 'No se pudieron cargar tus equipos para enviar desafíos.');
    } finally {
      if (version === this.loadVersion) this.relatedLoading = false;
    }
  }

  level(value: string | null): string {
    return ({ BEGINNER: 'Básico', INTERMEDIATE: 'Intermedio', ADVANCED: 'Avanzado' } as Record<string, string>)[value || ''] || 'Sin nivel';
  }
}
