import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { MatchService, MatchResponse } from '@features/matches/data-access/match.service';
import { AuthService } from '@core/auth/auth.service';

@Component({
  selector: 'app-mis-partidos',
  imports: [CommonModule, RouterLink],
  templateUrl: './mis-partidos.component.html',
  styleUrl: './mis-partidos.component.scss'
})
export class MisPartidosComponent implements OnInit {
  activeTab: 'organized' | 'joined' = 'organized';
  organizedMatches: MatchResponse[] = [];
  joinedMatches: MatchResponse[] = [];
  loading = true;

  constructor(
    private matchService: MatchService,
    private authService: AuthService,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    const userId = this.authService.getUserId();
    if (!userId) return;

    this.matchService.getMatchesByOrganizer(userId).subscribe({
      next: (data) => {
        this.organizedMatches = data;
        this.loading = false;
        this.cdr.detectChanges();
      }
    });

    this.matchService.getMatchesByParticipant(userId).subscribe({
      next: (data) => {
        this.joinedMatches = data;
        this.cdr.detectChanges();
      }
    });
  }

  switchTab(tab: 'organized' | 'joined'): void {
    this.activeTab = tab;
    this.cdr.detectChanges();
  }

  get currentMatches(): MatchResponse[] {
    return this.activeTab === 'organized' ? this.organizedMatches : this.joinedMatches;
  }

  cancelMatch(matchId: number): void {
    const userId = this.authService.getUserId();
    if (!userId) return;
    this.matchService.cancelMatch(matchId, userId).subscribe({
      next: () => this.ngOnInit()
    });
  }

  leaveMatch(matchId: number): void {
    const userId = this.authService.getUserId();
    if (!userId) return;
    this.matchService.leaveMatch(matchId, userId).subscribe({
      next: () => this.ngOnInit()
    });
  }

  isOrganizer(match: MatchResponse): boolean {
    return match.organizerId === this.authService.getUserId();
  }

  formatDate(dateStr: string): string {
    const date = new Date(dateStr);
    return date.toLocaleDateString('es-PE', {
      weekday: 'long',
      day: 'numeric',
      month: 'short',
      hour: '2-digit',
      minute: '2-digit'
    });
  }

  getStatusLabel(status: string): string {
    const labels: Record<string, string> = {
      OPEN: 'Abierto',
      FULL: 'Completo',
      CANCELLED: 'Cancelado',
      FINISHED: 'Finalizado'
    };
    return labels[status] || status;
  }

  getFilledPercent(match: MatchResponse): number {
    return ((match.totalSlots - match.availableSlots) / match.totalSlots) * 100;
  }
}
