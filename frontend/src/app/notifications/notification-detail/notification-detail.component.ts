import { CommonModule } from '@angular/common';
import { Component, OnDestroy, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { AuthService } from '@app/auth/auth.service';
import { MatchResponse, MatchService, PlayerJoinRequestResponse } from '@app/matches/match.service';
import { NotificationService } from '@app/notifications/notification.service';
import { JoinRequestService } from '@app/matches/join-request.service';

@Component({
  selector: 'app-notification-detail',
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './notification-detail.component.html',
  styleUrl: './notification-detail.component.scss'
})
export class NotificationDetailComponent implements OnInit, OnDestroy {
  match: MatchResponse | null = null;
  request: PlayerJoinRequestResponse | null = null;
  reviewMessage = '';
  loading = true;
  processing = false;
  errorMessage = '';
  successMessage = '';
  proofImageUrl: string | null = null;
  proofLoading = false;
  proofError = '';

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private authService: AuthService,
    private matchService: MatchService,
    private joinRequestService: JoinRequestService,
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

  ngOnDestroy(): void {
    this.releaseProofImage();
  }

  /** Descarga el comprobante con el token de sesión y lo muestra desde memoria (URL `blob:`). */
  private loadProofImage(proofPath: string): void {
    this.proofLoading = true;
    this.proofError = '';
    this.joinRequestService.getProofImage(proofPath).subscribe({
      next: (image) => {
        this.releaseProofImage();
        this.proofImageUrl = URL.createObjectURL(image);
        this.proofLoading = false;
      },
      error: () => {
        this.proofLoading = false;
        this.proofError = 'No se pudo cargar la imagen del comprobante.';
      }
    });
  }

  /** Libera la memoria de la imagen anterior. */
  private releaseProofImage(): void {
    if (this.proofImageUrl) URL.revokeObjectURL(this.proofImageUrl);
    this.proofImageUrl = null;
  }

  approve(): void {
    if (!this.match || !this.request || !this.authService.getUserId() || !this.canReview) return;
    this.processing = true;
    this.errorMessage = '';
    this.joinRequestService.approve(this.match.id, this.request.id, this.authService.getUserId()!).subscribe({
      next: (request) => this.finishReview(request, 'Solicitud aceptada.'),
      error: (err) => this.handleError(err, 'No se pudo aceptar la solicitud.')
    });
  }

  reject(): void {
    if (!this.match || !this.request || !this.authService.getUserId() || !this.canReview) return;
    this.processing = true;
    this.errorMessage = '';
    this.joinRequestService.reject(this.match.id, this.request.id, this.authService.getUserId()!, this.reviewMessage).subscribe({
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
        this.joinRequestService.getRequests(matchId, userId).subscribe({
          next: (requests) => {
            this.request = requests.find(item => item.id === requestId) || null;
            if (!this.request) {
              this.errorMessage = 'Solicitud no encontrada.';
              this.loading = false;
              return;
            }
            this.loading = false;
            this.loadProofImage(this.request.proofUrl);
          },
          error: (err) => this.handleLoadError(err)
        });
      },
      error: (err) => this.handleLoadError(err)
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
