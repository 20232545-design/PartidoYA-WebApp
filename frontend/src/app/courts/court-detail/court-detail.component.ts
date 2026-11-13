import { CommonModule } from '@angular/common';
import { AfterViewInit, ChangeDetectorRef, Component, ElementRef, OnInit, ViewChild } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { AuthService } from '@app/auth/auth.service';
import { CourtResponse, CourtService, ReservationResponse } from '@app/courts/court.service';
import { MapsLoaderService } from '@shared/services/maps-loader.service';
import { OpenMapService } from '@shared/services/open-map.service';
import { PaymentService } from '@app/courts/payment.service';
import { CourtAvailabilityService } from '@app/courts/court-availability.service';
import { CourtReservationService } from '@app/courts/court-reservation.service';

@Component({
  selector: 'app-court-detail',
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './court-detail.component.html',
  styleUrl: './court-detail.component.scss'
})
export class CourtDetailComponent implements OnInit, AfterViewInit {
  @ViewChild('mapContainer') mapContainer?: ElementRef<HTMLDivElement>;

  court: CourtResponse | null = null;
  loading = true;
  errorMessage = '';
  successMessage = '';
  mapError = '';
  mapLoading = false;
  private viewReady = false;
  private mapInitialized = false;

  selectedDate = '';
  selectedSchedule = '';
  availableSchedules: string[] = [];
  schedulesLoading = false;
  paymentMethod = 'Culqi Sandbox';
  reserving = false;
  paymentLocked = false;
  reservation: ReservationResponse | null = null;

  constructor(
    private courtService: CourtService,
    private availabilityService: CourtAvailabilityService,
    private reservationService: CourtReservationService,
    private authService: AuthService,
    private paymentService: PaymentService,
    private mapsLoader: MapsLoaderService,
    private openMap: OpenMapService,
    private route: ActivatedRoute,
    private router: Router,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    if (!id) {
      this.router.navigate(['/canchas']);
      return;
    }
    this.loadCourt(id);
  }

  ngAfterViewInit(): void {
    this.viewReady = true;
    this.tryInitMap();
  }

  get canReserve(): boolean {
    return !!this.selectedDate && !!this.selectedSchedule && !this.reserving && !this.paymentLocked && !this.reservation;
  }

  get primaryImage(): string | null {
    return this.court?.mainImageUrl || this.court?.imageUrls?.[0] || null;
  }

  get primarySport(): string {
    return this.court?.sports?.[0] || 'Fútbol';
  }

  get selectedStartTime(): string {
    return this.selectedSchedule.split('-')[0] || '';
  }

  /**
   * Fecha del partido: la de la reserva confirmada. No usa el formulario porque tras pagar se recargan
   * los horarios y el horario seleccionado se limpia.
   */
  get reservationMatchDate(): string {
    if (this.reservation) return `${this.reservation.date}T${this.reservation.startTime}`;
    return this.selectedDate && this.selectedStartTime ? `${this.selectedDate}T${this.selectedStartTime}` : '';
  }

  get reservationTitle(): string {
    return this.court ? `Pichanga en ${this.court.name}` : '';
  }

  get totalToPay(): number {
    return this.court?.pricePerHour || 0;
  }

  private loadCourt(id: number): void {
    this.courtService.getCourtById(id).subscribe({
      next: (court) => {
        this.court = court;
        this.availableSchedules = court.schedules;
        this.loading = false;
        this.cdr.detectChanges();
        this.tryInitMap();
      },
      error: (err) => {
        this.errorMessage = err.error?.message || 'Cancha no encontrada.';
        this.loading = false;
      }
    });
  }

  loadAvailableSchedules(): void {
    this.selectedSchedule = '';
    this.availableSchedules = [];
    if (!this.court || !this.selectedDate) {
      this.availableSchedules = this.court?.schedules || [];
      return;
    }
    this.schedulesLoading = true;
    this.availabilityService.getAvailableSchedules(this.court.id, this.selectedDate).subscribe({
      next: schedules => {
        this.availableSchedules = schedules;
        this.schedulesLoading = false;
      },
      error: err => {
        this.errorMessage = err.error?.message || 'No se pudieron cargar los horarios disponibles.';
        this.schedulesLoading = false;
      }
    });
  }

  async reserve(): Promise<void> {
    if (!this.court || !this.canReserve) return;
    const userId = this.authService.getUserId();
    const payerEmail = this.authService.getUserEmail();
    if (!userId) {
      this.router.navigate(['/login']);
      return;
    }
    if (!payerEmail) {
      this.errorMessage = 'Vuelve a iniciar sesion para procesar el pago.';
      return;
    }
    const [startTime, endTime] = this.selectedSchedule.split('-');
    this.reserving = true;
    this.paymentLocked = true;
    this.errorMessage = '';
    this.successMessage = '';
    const reservedDate = this.selectedDate;
    const reservedSchedule = this.selectedSchedule;
    let culqiToken = '';
    try {
      culqiToken = await this.paymentService.openCulqiCheckout(this.totalToPay, `Reserva ${this.court.name}`);
    } catch (err: any) {
      this.errorMessage = err?.message || 'No se pudo procesar el pago.';
      this.reserving = false;
      this.paymentLocked = false;
      return;
    }
    if (reservedDate !== this.selectedDate || reservedSchedule !== this.selectedSchedule) {
      this.errorMessage = 'La fecha u horario cambiaron durante el pago. Vuelve a iniciar la reserva.';
      this.reserving = false;
      this.paymentLocked = false;
      return;
    }
    const idempotencyKey = `reservation-${this.court.id}-${userId}-${this.selectedDate}-${startTime}-${Date.now()}`;
    this.reservationService.reserveCourt(this.court.id, userId, this.selectedDate, startTime, endTime, this.paymentMethod,
      culqiToken, payerEmail, idempotencyKey).subscribe({
      next: (reservation) => {
        this.reservation = reservation;
        this.successMessage = 'Pago realizado correctamente. Reserva confirmada.';
        this.reserving = false;
        this.loadAvailableSchedules();
      },
      error: (err) => {
        this.errorMessage = err.error?.message || 'No se pudo procesar el pago.';
        this.reserving = false;
        this.paymentLocked = false;
      }
    });
  }

  private tryInitMap(): void {
    if (!this.viewReady || !this.court || !this.mapContainer || this.mapInitialized) return;
    this.mapInitialized = true;
    requestAnimationFrame(() => this.initMap());
  }

  private async initMap(): Promise<void> {
    if (!this.court || !this.mapContainer) return;
    if (this.court.latitude === null || this.court.longitude === null) {
      this.mapError = 'Esta cancha aún no tiene coordenadas válidas.';
      return;
    }
    try {
      this.mapLoading = true;
      await this.mapsLoader.load();
      const google = window.google;
      const center = { lat: this.court.latitude, lng: this.court.longitude };
      const map = new google.maps.Map(this.mapContainer.nativeElement, {
        center,
        zoom: 15,
        disableDefaultUI: true,
        zoomControl: true
      });
      new google.maps.Marker({ position: center, map, title: this.court.name });
      google.maps.event.trigger(map, 'resize');
      map.setCenter(center);
      this.mapLoading = false;
    } catch {
      this.openMap.renderLocation(
        this.mapContainer.nativeElement,
        this.court.latitude,
        this.court.longitude,
        this.court.name
      );
      this.mapLoading = false;
      this.mapError = '';
    }
    this.cdr.detectChanges();
  }
}
