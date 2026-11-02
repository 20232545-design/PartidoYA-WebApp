import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { AuthService } from '@app/auth/auth.service';
import { CourtReportResponse, CourtResponse, CourtService } from '@app/courts/court.service';
import { CourtReportService } from '@app/courts/court-report.service';

type Period = 'this-month' | 'last-month' | 'custom';

@Component({
  selector: 'app-reports',
  imports: [CommonModule, FormsModule],
  templateUrl: './reports.component.html',
  styleUrl: './reports.component.scss'
})
export class ReportsComponent implements OnInit {
  courts: CourtResponse[] = [];
  report: CourtReportResponse | null = null;
  selectedCourtId = 'all';
  period: Period = 'this-month';
  customFrom = '';
  customTo = '';
  loading = true;
  errorMessage = '';

  constructor(
    private authService: AuthService,
    private courtService: CourtService,
    private reportService: CourtReportService
  ) {}

  ngOnInit(): void {
    const range = this.resolveRange();
    this.customFrom = range.from;
    this.customTo = range.to;
    this.loadCourts();
  }

  get chartMax(): number {
    const values = this.report?.dailyIncome.map(item => item.income) || [];
    return Math.max(...values, 1);
  }

  loadCourts(): void {
    const ownerId = this.authService.getUserId();
    if (!ownerId) return;
    this.loading = true;
    this.courtService.getManagedCourts(ownerId).subscribe({
      next: courts => {
        this.courts = courts;
        this.loadReport();
      },
      error: err => {
        this.errorMessage = err.error?.message || 'No se pudieron cargar tus canchas.';
        this.loading = false;
      }
    });
  }

  loadReport(): void {
    const ownerId = this.authService.getUserId();
    if (!ownerId) return;
    const range = this.resolveRange();
    this.loading = true;
    this.errorMessage = '';
    this.reportService.getManagedReport(
      ownerId,
      range.from,
      range.to,
      this.selectedCourtId === 'all' ? undefined : Number(this.selectedCourtId)
    ).subscribe({
      next: report => {
        this.report = report;
        this.loading = false;
      },
      error: err => {
        this.errorMessage = err.error?.message || 'No se pudo cargar el reporte.';
        this.loading = false;
      }
    });
  }

  onPeriodChange(): void {
    const range = this.resolveRange();
    this.customFrom = range.from;
    this.customTo = range.to;
    this.loadReport();
  }

  barHeight(value: number): string {
    return `${Math.max(6, (value / this.chartMax) * 100)}%`;
  }

  formatDay(value: string): string {
    return new Date(`${value}T12:00:00`).toLocaleDateString('es-PE', { day: '2-digit', month: 'short' });
  }

  private resolveRange(): { from: string; to: string } {
    const now = new Date();
    if (this.period === 'last-month') {
      return {
        from: this.toIsoDate(new Date(now.getFullYear(), now.getMonth() - 1, 1)),
        to: this.toIsoDate(new Date(now.getFullYear(), now.getMonth(), 0))
      };
    }
    if (this.period === 'custom') {
      return { from: this.customFrom, to: this.customTo };
    }
    return {
      from: this.toIsoDate(new Date(now.getFullYear(), now.getMonth(), 1)),
      to: this.toIsoDate(new Date(now.getFullYear(), now.getMonth() + 1, 0))
    };
  }

  private toIsoDate(date: Date): string {
    const year = date.getFullYear();
    const month = `${date.getMonth() + 1}`.padStart(2, '0');
    const day = `${date.getDate()}`.padStart(2, '0');
    return `${year}-${month}-${day}`;
  }
}
