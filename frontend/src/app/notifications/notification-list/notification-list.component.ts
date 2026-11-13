import { CommonModule } from '@angular/common';
import { Component, inject, OnInit } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { NotificationService } from '@app/notifications/notification.service';
import { MatchNotificationService } from '@app/notifications/match-notification.service';
import { MatchNotification, matchNotificationLabel } from '@app/notifications/match-notification.model';

@Component({
  selector: 'app-notification-list',
  imports: [CommonModule, RouterLink],
  templateUrl: './notification-list.component.html',
  styleUrl: './notification-list.component.scss'
})
export class NotificationListComponent implements OnInit {
  readonly notificationService = inject(NotificationService);
  readonly matchNotificationService = inject(MatchNotificationService);
  private readonly router = inject(Router);

  readonly typeLabel = matchNotificationLabel;

  ngOnInit(): void {
    this.notificationService.refresh();
    this.matchNotificationService.reload();
  }

  openMatchNotification(notification: MatchNotification): void {
    this.matchNotificationService.markAsRead(notification);
    this.router.navigate(['/partidos', notification.matchId]);
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

  formatDate(value: string): string {
    return new Date(value).toLocaleString('es-PE', {
      day: '2-digit',
      month: 'short',
      hour: '2-digit',
      minute: '2-digit'
    });
  }
}
