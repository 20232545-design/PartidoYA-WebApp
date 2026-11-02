import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { AuthService } from '@app/auth/auth.service';
import {
  CourtAvailabilityResponse,
  CourtResponse,
  CourtService,
  SaveCourtAvailabilityRequest
} from '@app/courts/court.service';
import { CourtAvailabilityService } from '@app/courts/court-availability.service';

@Component({
  selector: 'app-availability',
  imports: [CommonModule, FormsModule],
  templateUrl: './availability.component.html',
  styleUrl: './availability.component.scss'
})
export class AvailabilityComponent implements OnInit {
  courts: CourtResponse[] = [];
  records: CourtAvailabilityResponse[] = [];
  loading = true;
  saving = false;
  errorMessage = '';
  successMessage = '';
  editingId: number | null = null;
  selectedFilterCourt = 'all';

  form: SaveCourtAvailabilityRequest = this.emptyForm();

  readonly timeOptions = [
    '07:00', '08:00', '09:00', '10:00', '11:00', '12:00', '13:00', '14:00',
    '15:00', '16:00', '17:00', '18:00', '19:00', '20:00', '21:00', '22:00', '23:00'
  ];
  readonly reasons = ['Mantenimiento', 'Evento privado', 'Cierre temporal', 'Otro'];

  constructor(
    private authService: AuthService,
    private courtService: CourtService,
    private availabilityService: CourtAvailabilityService
  ) {}

  ngOnInit(): void {
    this.loadCourts();
  }

  get filteredRecords(): CourtAvailabilityResponse[] {
    return this.records
      .filter(record => this.selectedFilterCourt === 'all' || record.courtId === Number(this.selectedFilterCourt))
      .sort((a, b) => `${a.date} ${a.startTime}`.localeCompare(`${b.date} ${b.startTime}`));
  }

  loadCourts(): void {
    const ownerId = this.authService.getUserId();
    if (!ownerId) return;
    this.loading = true;
    this.courtService.getManagedCourts(ownerId).subscribe({
      next: courts => {
        this.courts = courts;
        if (courts.length > 0) this.form.courtId = courts[0].id;
        this.loadAvailability();
      },
      error: err => {
        this.errorMessage = err.error?.message || 'No se pudieron cargar tus canchas.';
        this.loading = false;
      }
    });
  }

  loadAvailability(): void {
    const ownerId = this.authService.getUserId();
    if (!ownerId) return;
    const range = this.defaultRange();
    this.availabilityService.getManagedAvailability(ownerId, range.from, range.to).subscribe({
      next: records => {
        this.records = records;
        this.loading = false;
      },
      error: err => {
        this.errorMessage = err.error?.message || 'No se pudo cargar la disponibilidad.';
        this.loading = false;
      }
    });
  }

  save(): void {
    const ownerId = this.authService.getUserId();
    if (!ownerId || !this.validate()) return;
    this.saving = true;
    this.errorMessage = '';
    this.successMessage = '';
    const payload = this.normalizedForm();
    const request = this.editingId
      ? this.availabilityService.updateAvailability(ownerId, this.editingId, payload)
      : this.availabilityService.createAvailability(ownerId, payload);
    request.subscribe({
      next: () => {
        this.successMessage = this.editingId ? 'Horario actualizado.' : 'Horario guardado.';
        this.resetForm();
        this.loadAvailability();
        this.saving = false;
      },
      error: err => {
        this.errorMessage = err.error?.message || 'No se pudo guardar el horario.';
        this.saving = false;
      }
    });
  }

  edit(record: CourtAvailabilityResponse): void {
    this.editingId = record.id;
    this.form = {
      courtId: record.courtId,
      date: record.date,
      allDay: record.allDay,
      startTime: this.shortTime(record.startTime),
      endTime: this.shortTime(record.endTime),
      type: record.type,
      reason: record.reason
    };
    this.successMessage = '';
    this.errorMessage = '';
  }

  remove(record: CourtAvailabilityResponse): void {
    const ownerId = this.authService.getUserId();
    if (!ownerId || !confirm('Deseas eliminar este registro de disponibilidad?')) return;
    this.availabilityService.deleteAvailability(ownerId, record.id).subscribe({
      next: () => {
        this.successMessage = 'Registro eliminado.';
        this.loadAvailability();
        if (this.editingId === record.id) this.resetForm();
      },
      error: err => this.errorMessage = err.error?.message || 'No se pudo eliminar el registro.'
    });
  }

  resetForm(): void {
    this.editingId = null;
    this.form = this.emptyForm();
    if (this.courts.length > 0) this.form.courtId = this.courts[0].id;
  }

  onAllDayChange(): void {
    if (this.form.allDay) {
      this.form.startTime = '07:00';
      this.form.endTime = '23:00';
    }
  }

  typeLabel(type: string): string {
    return type === 'BLOCKED' ? 'Bloqueado' : 'Disponible';
  }

  private validate(): boolean {
    if (!this.form.courtId || !this.form.date) {
      this.errorMessage = 'Selecciona una cancha y una fecha.';
      return false;
    }
    const payload = this.normalizedForm();
    if (payload.endTime <= payload.startTime) {
      this.errorMessage = 'La hora final debe ser posterior a la inicial.';
      return false;
    }
    if (payload.type === 'BLOCKED' && !payload.reason) {
      this.errorMessage = 'Indica un motivo para bloquear el horario.';
      return false;
    }
    return true;
  }

  private normalizedForm(): SaveCourtAvailabilityRequest {
    return {
      ...this.form,
      startTime: this.form.allDay ? '07:00' : this.form.startTime,
      endTime: this.form.allDay ? '23:00' : this.form.endTime,
      reason: this.form.type === 'BLOCKED' ? this.form.reason : null
    };
  }

  private emptyForm(): SaveCourtAvailabilityRequest {
    return {
      courtId: 0,
      date: this.toIsoDate(new Date()),
      allDay: false,
      startTime: '07:00',
      endTime: '08:00',
      type: 'BLOCKED',
      reason: 'Mantenimiento'
    };
  }

  private defaultRange(): { from: string; to: string } {
    const from = new Date();
    from.setMonth(from.getMonth() - 1, 1);
    const to = new Date();
    to.setMonth(to.getMonth() + 3, 0);
    return { from: this.toIsoDate(from), to: this.toIsoDate(to) };
  }

  shortTime(value: string): string {
    return value.slice(0, 5);
  }

  private toIsoDate(date: Date): string {
    const year = date.getFullYear();
    const month = `${date.getMonth() + 1}`.padStart(2, '0');
    const day = `${date.getDate()}`.padStart(2, '0');
    return `${year}-${month}-${day}`;
  }
}
