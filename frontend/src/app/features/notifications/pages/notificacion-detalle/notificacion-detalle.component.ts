import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { environment } from '@env/environment';
import { AuthService, UserResponse } from '@core/auth/auth.service';
import { MatchResponse, MatchService, PlayerJoinRequestResponse } from '@features/matches/data-access/match.service';
import { NotificationService } from '@features/notifications/application/notification.service';
import { UserService } from '@features/profile/data-access/user.service';

@Component({
  selector: 'app-notificacion-detalle',
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './notificacion-detalle.component.html',
  styleUrl: './notificacion-detalle.component.scss'
})
export class NotificacionDetalleComponent implements OnInit {
  match: MatchResponse | null = null;
  request: PlayerJoinRequestResponse | null = null;
  player: UserResponse | null = null;
  reviewMessage = '';
  loading = true;
  processing = false;
  errorMessage = '';
  successMessage = '';

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private authService: AuthService,
    private matchService: MatchService,
    private userService: UserService,
    private notificationService: NotificationService
  ) {}

  ngOnInit(): void {
    const matchId = Number(this.route.snapshot.paramMap.get('matchId'));
    const requestId = Number(this.route.snapshot.paramMap.get('requestId'));
    const userId = this.authService.getUserId();
    if (!matchId || !requestId || !userId) {
      this.router.navigate(['/notificaciones']);
      return;
    }
    this.load(matchId, requestId, userId);
  }

  get canReview(): boolean {
    return !!this.request && this.request.status === 'PENDING_PAYMENT_VERIFICATION' && !this.processing;
  }

  get proofUrl(): string {
    if (!this.request) return '';
    return `${environment.apiUrl.replace('/api/v1', '')}${this.request.proofUrl}`;
  }

  approve(): void {
    if (!this.match || !this.request || !this.authService.getUserId() || !this.canReview) return;
    this.processing = true;
    this.errorMessage = '';
    this.matchService.approveJoinRequest(this.match.id, this.request.id, this.authService.getUserId()!).subscribe({
      next: (request) => this.finishReview(request, 'Solicitud aceptada.'),
      error: (err) => this.handleError(err, 'No se pudo aceptar la solicitud.')
    });
  }

  reject(): void {
    if (!this.match || !this.request || !this.authService.getUserId() || !this.canReview) return;
    this.processing = true;
    this.errorMessage = '';
    this.matchService.rejectJoinRequest(this.match.id, this.request.id, this.authService.getUserId()!, this.reviewMessage).subscribe({
      next: (request) => this.finishReview(request, 'Solicitud rechazada.'),
      error: (err) => this.handleError(err, 'No se pudo rechazar la solicitud.')
    });
  }

  formatDate(value: string): string {
    return new Date(value).toLocaleString('es-PE', {
      weekday: 'long',
      day: '2-digit',
      month: 'long',
      hour: '2-digit',
      minute: '2-digit'
    });
  }

  statusLabel(status: string): string {
    const labels: Record<string, string> = {
      PENDING_PAYMENT_VERIFICATION: 'Pendiente',
      CONFIRMED: 'Aceptada',
      REJECTED: 'Rechazada',
      CANCELLED: 'Cancelada'
    };
    return labels[status] || status;
  }

  private load(matchId: number, requestId: number, userId: number): void {
    this.matchService.getMatchById(matchId).subscribe({
      next: (match) => {
        this.match = match;
        this.matchService.getJoinRequests(matchId, userId).subscribe({
          next: (requests) => {
            this.request = requests.find(item => item.id === requestId) || null;
            if (!this.request) {
              this.errorMessage = 'Solicitud no encontrada.';
              this.loading = false;
              return;
            }
            this.loadPlayer(this.request.playerId);
          },
          error: (err) => this.handleLoadError(err)
        });
      },
      error: (err) => this.handleLoadError(err)
    });
  }

  private loadPlayer(playerId: number): void {
    this.userService.getUserById(playerId).subscribe({
      next: (player) => {
        this.player = player;
        this.loading = false;
      },
      error: () => {
        this.loading = false;
      }
    });
  }

  private finishReview(request: PlayerJoinRequestResponse, message: string): void {
    this.request = request;
    this.processing = false;
    this.successMessage = message;
    this.notificationService.refresh();
  }

  private handleError(err: any, fallback: string): void {
    this.processing = false;
    this.errorMessage = err.error?.message || fallback;
  }

  private handleLoadError(err: any): void {
    this.errorMessage = err.error?.message || 'No se pudo cargar la solicitud.';
    this.loading = false;
  }
}
