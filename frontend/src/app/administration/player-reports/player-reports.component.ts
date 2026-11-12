import { CommonModule } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import {
  PlayerReportAdminService,
  PlayerReportFilter,
  PlayerReportOutcome
} from '@app/administration/player-report-admin.service';
import {
  PlayerReportResponse,
  PlayerReportStatus,
  RESOLUTION_NOTE_MAX_LENGTH,
  reportReasonLabel
} from '@shared/models/player-report.models';

@Component({
  selector: 'app-player-reports',
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './player-reports.component.html',
  styleUrl: './player-reports.component.scss'
})
export class PlayerReportsComponent implements OnInit {
  private readonly reportAdminService = inject(PlayerReportAdminService);

  readonly filters: ReadonlyArray<{ value: PlayerReportFilter; label: string }> = [
    { value: 'PENDING', label: 'Pendientes' },
    { value: 'ACTION_TAKEN', label: 'Con medidas' },
    { value: 'DISMISSED', label: 'Descartados' },
    { value: 'ALL', label: 'Todos' }
  ];
  readonly noteMaxLength = RESOLUTION_NOTE_MAX_LENGTH;
  readonly reasonLabel = reportReasonLabel;

  reports: PlayerReportResponse[] = [];
  selected: PlayerReportResponse | null = null;
  statusFilter: PlayerReportFilter = 'PENDING';
  page = 0;
  readonly size = 10;
  totalElements = 0;
  loading = true;
  resolving = false;
  resolutionNote = '';
  errorMessage = '';
  successMessage = '';

  ngOnInit(): void {
    this.loadReports();
  }

  get totalPages(): number {
    return Math.max(1, Math.ceil(this.totalElements / this.size));
  }

  loadReports(): void {
    this.loading = true;
    this.errorMessage = '';
    this.reportAdminService.search(this.statusFilter, this.page, this.size).subscribe({
      next: page => {
        this.reports = page.content;
        this.totalElements = page.totalElements;
        this.loading = false;
        this.selected = this.reports.find(report => report.id === this.selected?.id) ?? null;
      },
      error: err => {
        this.errorMessage = err.error?.message || 'No se pudieron cargar los reportes.';
        this.loading = false;
      }
    });
  }

  changeFilter(filter: PlayerReportFilter): void {
    if (filter === this.statusFilter) return;
    this.statusFilter = filter;
    this.page = 0;
    this.selected = null;
    this.loadReports();
  }

  changePage(step: number): void {
    const next = this.page + step;
    if (next < 0 || next >= this.totalPages) return;
    this.page = next;
    this.loadReports();
  }

  select(report: PlayerReportResponse): void {
    this.selected = report;
    this.resolutionNote = '';
    this.successMessage = '';
    this.errorMessage = '';
  }

  resolve(outcome: PlayerReportOutcome): void {
    if (!this.selected || this.selected.status !== 'PENDING') return;
    this.resolving = true;
    this.errorMessage = '';
    this.successMessage = '';
    this.reportAdminService.resolve(this.selected.id, outcome, this.resolutionNote.trim() || null).subscribe({
      next: resolved => {
        this.resolving = false;
        this.successMessage = outcome === 'ACTION_TAKEN'
          ? 'Reporte marcado con medidas tomadas.'
          : 'Reporte descartado.';
        this.selected = resolved;
        this.loadReports();
      },
      error: err => {
        this.resolving = false;
        this.errorMessage = err.error?.message || 'No se pudo resolver el reporte.';
      }
    });
  }

  statusLabel(status: PlayerReportStatus): string {
    const labels: Record<PlayerReportStatus, string> = {
      PENDING: 'Pendiente',
      ACTION_TAKEN: 'Con medidas',
      DISMISSED: 'Descartado'
    };
    return labels[status];
  }
}
