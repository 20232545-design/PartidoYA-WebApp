import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { forkJoin } from 'rxjs';
import { AuthService } from '@app/auth/auth.service';
import {
  CourtAvailabilityResponse,
  CourtResponse,
  CourtService,
  ManagedReservationResponse,
  SaveCourtAvailabilityRequest
} from '@app/courts/court.service';
import { CourtAvailabilityService } from '@app/courts/court-availability.service';
import { CourtReservationService } from '@app/courts/court-reservation.service';

type CalendarView = 'month' | 'week' | 'day';

interface CalendarDay {
  date: Date;
  key: string;
  label: string;
  isCurrentMonth: boolean;
  isToday: boolean;
}

@Component({
  selector: 'app-calendar',
  imports: [CommonModule, FormsModule],
  templateUrl: './calendar.component.html',
  styleUrl: './calendar.component.scss'
})
export class CalendarComponent implements OnInit {
  courts: CourtResponse[] = [];
  reservations: ManagedReservationResponse[] = [];
  availability: CourtAvailabilityResponse[] = [];
  selectedReservation: ManagedReservationResponse | null = null;
  selectedAvailability: CourtAvailabilityResponse | null = null;
  loading = true;
  errorMessage = '';
  successMessage = '';

  viewMode: CalendarView = 'week';
  currentDate = new Date();
  selectedCourtId = 'all';
  agendaCourtId = 'all';
  agendaDate = '';
  agendaStatus = 'all';
  agendaPaymentStatus = 'all';

  readonly timeSlots = [
    '07:00-08:00', '08:00-09:00', '09:00-10:00', '10:00-11:00',
    '11:00-12:00', '12:00-13:00', '13:00-14:00', '14:00-15:00',
    '15:00-16:00', '16:00-17:00', '17:00-18:00', '18:00-19:00',
    '19:00-20:00', '20:00-21:00', '21:00-22:00', '22:00-23:00'
  ];
  readonly weekDayNames = ['Lun', 'Mar', 'Mie', 'Jue', 'Vie', 'Sab', 'Dom'];
  readonly availabilityReasons = ['Mantenimiento', 'Evento privado', 'Cierre temporal', 'Otro'];
  private readonly colors = ['#c6f135', '#38bdf8', '#f59e0b', '#a78bfa', '#fb7185', '#34d399'];
  availabilityForm: SaveCourtAvailabilityRequest = {
    courtId: 0,
    date: '',
    allDay: false,
    startTime: '07:00',
    endTime: '08:00',
    type: 'BLOCKED',
    reason: 'Mantenimiento'
  };

  constructor(
    private authService: AuthService,
    private courtService: CourtService,
    private availabilityService: CourtAvailabilityService,
    private reservationService: CourtReservationService
  ) {}

  ngOnInit(): void {
    this.loadData();
  }

  get calendarReservations(): ManagedReservationResponse[] {
    return this.reservations.filter(reservation =>
      this.selectedCourtId === 'all' || reservation.courtId === Number(this.selectedCourtId)
    );
  }

  get calendarAvailability(): CourtAvailabilityResponse[] {
    return this.availability.filter(record =>
      this.selectedCourtId === 'all' || record.courtId === Number(this.selectedCourtId)
    );
  }

  get agendaReservations(): ManagedReservationResponse[] {
    const today = this.toIsoDate(new Date());
    return this.reservations
      .filter(reservation => this.agendaCourtId === 'all' || reservation.courtId === Number(this.agendaCourtId))
      .filter(reservation => !this.agendaDate || reservation.date === this.agendaDate)
      .filter(reservation => this.agendaStatus === 'all' || reservation.status === this.agendaStatus)
      .filter(reservation => this.agendaPaymentStatus === 'all' || reservation.paymentStatus === this.agendaPaymentStatus)
      .filter(reservation => this.agendaDate || reservation.date >= today)
      .sort((a, b) => `${a.date} ${this.shortTime(a.startTime)}`.localeCompare(`${b.date} ${this.shortTime(b.startTime)}`));
  }

  get statusOptions(): string[] {
    return [...new Set(this.reservations.map(reservation => reservation.status))];
  }

  get paymentStatusOptions(): string[] {
    return [...new Set(this.reservations.map(reservation => reservation.paymentStatus))];
  }

  get monthCells(): CalendarDay[] {
    const first = new Date(this.currentDate.getFullYear(), this.currentDate.getMonth(), 1);
    const start = this.startOfWeek(first);
    return Array.from({ length: 42 }, (_, index) => {
      const date = this.addDays(start, index);
      return {
        date,
        key: this.toIsoDate(date),
        label: date.getDate().toString(),
        isCurrentMonth: date.getMonth() === this.currentDate.getMonth(),
        isToday: this.toIsoDate(date) === this.toIsoDate(new Date())
      };
    });
  }

  get dayColumns(): Date[] {
    if (this.viewMode === 'day') return [this.currentDate];
    const start = this.startOfWeek(this.currentDate);
    return Array.from({ length: 7 }, (_, index) => this.addDays(start, index));
  }

  get periodTitle(): string {
    if (this.viewMode === 'month') {
      return this.currentDate.toLocaleDateString('es-PE', { month: 'long', year: 'numeric' });
    }
    if (this.viewMode === 'day') {
      return this.currentDate.toLocaleDateString('es-PE', { weekday: 'long', day: 'numeric', month: 'long' });
    }
    const start = this.dayColumns[0];
    const end = this.dayColumns[this.dayColumns.length - 1];
    return `${start.toLocaleDateString('es-PE', { day: 'numeric', month: 'short' })} - ${end.toLocaleDateString('es-PE', { day: 'numeric', month: 'short', year: 'numeric' })}`;
  }

  get scheduleGridColumns(): string {
    return `82px repeat(${this.dayColumns.length}, minmax(150px, 1fr))`;
  }

  loadData(): void {
    const ownerId = this.authService.getUserId();
    if (!ownerId) {
      this.errorMessage = 'Inicia sesion para ver tu calendario.';
      this.loading = false;
      return;
    }
    this.loading = true;
    this.errorMessage = '';
    this.courtService.getManagedCourts(ownerId).subscribe({
      next: courts => {
        this.courts = courts;
        this.loadReservations();
      },
      error: err => {
        this.errorMessage = err.error?.message || 'No se pudo cargar tus canchas.';
        this.loading = false;
      }
    });
  }

  loadReservations(): void {
    const ownerId = this.authService.getUserId();
    if (!ownerId) {
      this.errorMessage = 'Inicia sesion para ver tu calendario.';
      this.loading = false;
      return;
    }
    const range = this.fetchRange();
    this.loading = true;
    forkJoin({
      reservations: this.reservationService.getManagedReservations(ownerId, range.from, range.to),
      availability: this.availabilityService.getManagedAvailability(ownerId, range.from, range.to)
    }).subscribe({
      next: result => {
        this.reservations = result.reservations;
        this.availability = result.availability;
        this.loading = false;
      },
      error: err => {
        this.errorMessage = err.error?.message || 'No se pudieron cargar las reservas.';
        this.loading = false;
      }
    });
  }

  setView(view: CalendarView): void {
    this.viewMode = view;
    this.loadReservations();
  }

  goToday(): void {
    this.currentDate = new Date();
    this.loadReservations();
  }

  movePeriod(step: number): void {
    const next = new Date(this.currentDate);
    if (this.viewMode === 'month') next.setMonth(next.getMonth() + step);
    if (this.viewMode === 'week') next.setDate(next.getDate() + step * 7);
    if (this.viewMode === 'day') next.setDate(next.getDate() + step);
    this.currentDate = next;
    this.loadReservations();
  }

  onCourtChange(): void {
    this.selectedReservation = null;
    this.selectedAvailability = null;
  }

  reservationsForDate(date: Date): ManagedReservationResponse[] {
    const key = this.toIsoDate(date);
    return this.calendarReservations
      .filter(reservation => reservation.date === key)
      .sort((a, b) => this.shortTime(a.startTime).localeCompare(this.shortTime(b.startTime)));
  }

  reservationsForSlot(date: Date, slot: string): ManagedReservationResponse[] {
    const key = this.toIsoDate(date);
    const [slotStart, slotEnd] = slot.split('-');
    return this.calendarReservations.filter(reservation =>
      reservation.date === key &&
      this.shortTime(reservation.startTime) < slotEnd &&
      this.shortTime(reservation.endTime) > slotStart
    );
  }

  availabilityForDate(date: Date): CourtAvailabilityResponse[] {
    const key = this.toIsoDate(date);
    return this.calendarAvailability
      .filter(record => record.date === key)
      .sort((a, b) => this.shortTime(a.startTime).localeCompare(this.shortTime(b.startTime)));
  }

  availabilityForSlot(date: Date, slot: string): CourtAvailabilityResponse[] {
    const key = this.toIsoDate(date);
    const [slotStart, slotEnd] = slot.split('-');
    return this.calendarAvailability.filter(record =>
      record.date === key &&
      this.shortTime(record.startTime) < slotEnd &&
      this.shortTime(record.endTime) > slotStart
    );
  }

  courtColor(courtId: number): string {
    const index = this.courts.findIndex(court => court.id === courtId);
    return this.colors[(index >= 0 ? index : courtId) % this.colors.length];
  }

  openReservation(reservation: ManagedReservationResponse): void {
    this.selectedReservation = reservation;
  }

  closeReservation(): void {
    this.selectedReservation = null;
  }

  openAvailability(record: CourtAvailabilityResponse): void {
    this.selectedAvailability = record;
    this.availabilityForm = {
      courtId: record.courtId,
      date: record.date,
      allDay: record.allDay,
      startTime: this.shortTime(record.startTime),
      endTime: this.shortTime(record.endTime),
      type: record.type,
      reason: record.reason || 'Mantenimiento'
    };
  }

  closeAvailability(): void {
    this.selectedAvailability = null;
  }

  saveAvailability(): void {
    const ownerId = this.authService.getUserId();
    if (!ownerId || !this.selectedAvailability) return;
    this.errorMessage = '';
    this.successMessage = '';
    const payload = this.normalizedAvailabilityForm();
    this.availabilityService.updateAvailability(ownerId, this.selectedAvailability.id, payload).subscribe({
      next: () => {
        this.successMessage = 'Disponibilidad actualizada.';
        this.closeAvailability();
        this.loadReservations();
      },
      error: err => this.errorMessage = err.error?.message || 'No se pudo actualizar la disponibilidad.'
    });
  }

  deleteAvailability(): void {
    const ownerId = this.authService.getUserId();
    if (!ownerId || !this.selectedAvailability || !confirm('Deseas eliminar este registro?')) return;
    this.availabilityService.deleteAvailability(ownerId, this.selectedAvailability.id).subscribe({
      next: () => {
        this.successMessage = 'Disponibilidad eliminada.';
        this.closeAvailability();
        this.loadReservations();
      },
      error: err => this.errorMessage = err.error?.message || 'No se pudo eliminar la disponibilidad.'
    });
  }

  formatDayHeader(date: Date): string {
    return date.toLocaleDateString('es-PE', { weekday: 'short', day: 'numeric' });
  }

  formatFullDate(value: string): string {
    return new Date(`${value}T12:00:00`).toLocaleDateString('es-PE', {
      weekday: 'long',
      day: 'numeric',
      month: 'long',
      year: 'numeric'
    });
  }

  shortTime(value: string): string {
    return value.slice(0, 5);
  }

  statusLabel(value: string): string {
    const labels: Record<string, string> = {
      PENDING: 'Pendiente',
      CONFIRMED: 'Confirmada',
      CANCELLED: 'Cancelada',
      COMPLETED: 'Completada',
      APPROVED: 'Aprobado',
      REJECTED: 'Rechazado',
      REFUNDED: 'Reembolsado'
    };
    return labels[value] || value;
  }

  availabilityLabel(value: string): string {
    return value === 'BLOCKED' ? 'Bloqueado' : 'Disponible';
  }

  trackReservation(_: number, reservation: ManagedReservationResponse): number {
    return reservation.id;
  }

  trackAvailability(_: number, record: CourtAvailabilityResponse): number {
    return record.id;
  }

  trackDay(_: number, day: CalendarDay | Date): string {
    return day instanceof Date ? this.toIsoDate(day) : day.key;
  }

  private fetchRange(): { from: string; to: string } {
    const from = new Date(this.currentDate);
    from.setMonth(from.getMonth() - 2, 1);
    const to = new Date(this.currentDate);
    to.setMonth(to.getMonth() + 4, 0);
    return { from: this.toIsoDate(from), to: this.toIsoDate(to) };
  }

  private startOfWeek(date: Date): Date {
    const result = new Date(date);
    const day = result.getDay();
    const diff = day === 0 ? -6 : 1 - day;
    result.setDate(result.getDate() + diff);
    result.setHours(0, 0, 0, 0);
    return result;
  }

  private addDays(date: Date, days: number): Date {
    const result = new Date(date);
    result.setDate(result.getDate() + days);
    return result;
  }

  toIsoDate(date: Date): string {
    const year = date.getFullYear();
    const month = `${date.getMonth() + 1}`.padStart(2, '0');
    const day = `${date.getDate()}`.padStart(2, '0');
    return `${year}-${month}-${day}`;
  }

  private normalizedAvailabilityForm(): SaveCourtAvailabilityRequest {
    return {
      ...this.availabilityForm,
      startTime: this.availabilityForm.allDay ? '07:00' : this.availabilityForm.startTime,
      endTime: this.availabilityForm.allDay ? '23:00' : this.availabilityForm.endTime,
      reason: this.availabilityForm.type === 'BLOCKED' ? this.availabilityForm.reason : null
    };
  }
}
