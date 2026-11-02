import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { RouterLink } from '@angular/router';
import { NotificationService } from '@features/notifications/application/notification.service';

@Component({
  selector: 'app-notificaciones',
  imports: [CommonModule, RouterLink],
  templateUrl: './notificaciones.component.html',
  styleUrl: './notificaciones.component.scss'
})
export class NotificacionesComponent implements OnInit {
  constructor(public notificationService: NotificationService) {}

  ngOnInit(): void {
    this.notificationService.refresh();
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
