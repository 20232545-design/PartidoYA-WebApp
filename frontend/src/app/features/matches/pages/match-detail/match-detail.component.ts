import { Component, OnInit, ChangeDetectorRef, ElementRef, ViewChild } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { MatchService, MatchResponse, PlayerJoinRequestResponse } from '@features/matches/data-access/match.service';
import { AuthService } from '@core/auth/auth.service';
import { MapsLoaderService } from '@core/location/maps-loader.service';
import { OpenMapService } from '@core/location/open-map.service';
import { GeolocationService } from '@core/location/geolocation.service';
import { environment } from '@env/environment';
import { NotificationService } from '@features/notifications/application/notification.service';

@Component({
  selector: 'app-match-detail',
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './match-detail.component.html',
  styleUrl: './match-detail.component.scss'
})
export class MatchDetailComponent implements OnInit {
  @ViewChild('mapContainer') mapContainer?: ElementRef<HTMLDivElement>;
  @ViewChild('proofInput') proofInput?: ElementRef<HTMLInputElement>;

  match: MatchResponse | null = null;
  loading = true;
  errorMessage = '';

  mapLoading = false;
  mapError = '';
  routeDuration = '';
  routeDistance = '';
  joinRequests: PlayerJoinRequestResponse[] = [];
  proofFile: File | null = null;
  proofPreview: string | null = null;
  proofSubmitting = false;
  proofMessage = '';
  rejectReason = '';
  reportTargetId: number | null = null;
  reportReason = 'Comportamiento antideportivo';
  reportDetails = '';
  reportConfirmation = '';
  readonly reportReasons = [
    'Comportamiento antideportivo',
    'Insultos o acoso',
    'Inasistencia',
    'Informacion falsa',
    'Otro motivo'
  ];

  constructor(
    private matchService: MatchService,
    private authService: AuthService,
    private mapsLoader: MapsLoaderService,
    private openMap: OpenMapService,
    private geolocation: GeolocationService,
    private notificationService: NotificationService,
    private route: ActivatedRoute,
    private router: Router,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    if (!id) {
      this.router.navigate(['/deportes']);
      return;
    }
    this.loadMatch(id);
  }

  private loadMatch(id: number): void {
    this.matchService.getMatchById(id).subscribe({
      next: (data) => {
        this.match = data;
        this.loading = false;
        this.mapLoading = true;
        this.cdr.detectChanges();
        setTimeout(() => this.initMap(), 0);
        this.loadJoinRequests();
      },
      error: () => {
        this.errorMessage = 'Partido no encontrado.';
        this.loading = false;
        this.cdr.detectChanges();
      }
    });
  }

  get isOrganizer(): boolean {
    return this.match?.organizerId === this.authService.getUserId();
  }

  get hasJoined(): boolean {
    const userId = this.authService.getUserId();
    return userId !== null && (this.match?.participantIds.includes(userId) ?? false);
  }

  get currentUserJoinRequest(): PlayerJoinRequestResponse | null {
    const userId = this.currentUserId;
    return this.joinRequests.find(request => request.playerId === userId) || null;
  }

  get proofFileName(): string {
    return this.proofFile?.name || '';
  }

  get filledPercent(): number {
    if (!this.match) return 0;
    return ((this.match.totalSlots - this.match.availableSlots) / this.match.totalSlots) * 100;
  }

  get currentUserId(): number | null {
    return this.authService.getUserId();
  }

  get canCancel(): boolean {
    return !!this.match && this.match.status === 'OPEN' && new Date(this.match.matchDate) > new Date();
  }

  get googleMapsUrl(): string {
    if (!this.match) return '#';
    return `https://www.google.com/maps/dir/?api=1&destination=${encodeURIComponent(this.match.address)}`;
  }

  private async initMap(): Promise<void> {
    try {
      await this.mapsLoader.load();
      const position = await this.geolocation.getPosition();
      this.renderDirections(position);
    } catch (err: any) {
      await this.renderOpenMapFallback(err);
    }
  }

  private async renderOpenMapFallback(originalError: unknown): Promise<void> {
    if (!this.match || !this.mapContainer) {
      this.mapLoading = false;
      return;
    }
    if (this.match.latitude === null || this.match.longitude === null) {
      this.mapLoading = false;
      this.mapError = 'No hay coordenadas guardadas. Usa “Abrir en Google Maps” para ver la ruta.';
      this.cdr.detectChanges();
      return;
    }

    try {
      const position = await this.geolocation.getPosition();
      const summary = this.openMap.renderDirectRoute(
        this.mapContainer.nativeElement,
        [position.coords.latitude, position.coords.longitude],
        [this.match.latitude, this.match.longitude]
      );
      this.routeDuration = summary.duration;
      this.routeDistance = summary.distance;
      this.mapError = '';
    } catch {
      this.mapError = originalError instanceof Error
        ? originalError.message
        : 'No se pudo cargar el mapa.';
    } finally {
      this.mapLoading = false;
      this.cdr.detectChanges();
    }
  }

  private renderDirections(position: GeolocationPosition): void {
    if (!this.match || !this.mapContainer) {
      this.mapLoading = false;
      return;
    }
    const google = window.google;
    const origin = { lat: position.coords.latitude, lng: position.coords.longitude };

    const map = new google.maps.Map(this.mapContainer.nativeElement, {
      center: origin,
      zoom: 13,
      disableDefaultUI: true,
      zoomControl: true,
      zoomControlOptions: { position: google.maps.ControlPosition.RIGHT_TOP },
      styles: DARK_MAP_STYLE
    });

    const directionsService = new google.maps.DirectionsService();
    const directionsRenderer = new google.maps.DirectionsRenderer({
      map,
      suppressMarkers: false,
      polylineOptions: { strokeColor: '#c6f135', strokeWeight: 5 }
    });

    directionsService.route(
      {
        origin,
        destination: this.match.address,
        travelMode: google.maps.TravelMode.DRIVING
      },
      (result: any, status: string) => {
        this.mapLoading = false;
        if (status === 'OK') {
          directionsRenderer.setDirections(result);
          const leg = result.routes[0].legs[0];
          this.routeDuration = leg.duration.text;
          this.routeDistance = leg.distance.text;
        } else {
          this.mapError = 'No se pudo calcular la ruta a la cancha.';
        }
        this.cdr.detectChanges();
      }
    );
  }

  joinMatch(): void {
    if (!this.match || !this.currentUserId) return;
    this.matchService.joinMatch(this.match.id, this.currentUserId).subscribe({
      next: (data) => {
        this.match = data;
        this.cdr.detectChanges();
      },
      error: (err) => {
        this.errorMessage = err.error?.message || 'No se pudo unir al partido.';
        this.cdr.detectChanges();
      }
    });
  }

  proofUrl(request: PlayerJoinRequestResponse): string {
    return `${environment.apiUrl.replace('/api/v1', '')}${request.proofUrl}`;
  }

  onProofSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0] || null;
    this.proofMessage = '';
    this.proofFile = null;
    this.proofPreview = null;
    if (!file) return;
    const allowed = ['image/jpeg', 'image/jpg', 'image/png', 'image/webp'];
    if (!allowed.includes(file.type) || file.size === 0 || file.size > 5 * 1024 * 1024) {
      this.proofMessage = 'Adjunta una imagen JPG, PNG o WEBP de maximo 5 MB.';
      input.value = '';
      return;
    }
    this.proofFile = file;
    const reader = new FileReader();
    reader.onload = () => {
      this.proofPreview = reader.result as string;
      this.cdr.detectChanges();
    };
    reader.readAsDataURL(file);
  }

  clearProof(): void {
    this.proofFile = null;
    this.proofPreview = null;
    this.proofMessage = '';
    if (this.proofInput) {
      this.proofInput.nativeElement.value = '';
    }
  }

  submitProof(): void {
    if (this.proofSubmitting) return;
    if (!this.match || !this.currentUserId) return;
    if (!this.proofFile) {
      this.proofMessage = 'Adjunta el comprobante antes de enviar la solicitud.';
      return;
    }
    this.proofSubmitting = true;
    this.proofMessage = '';
    this.matchService.submitJoinProof(this.match.id, this.currentUserId, this.proofFile).subscribe({
      next: () => {
        this.proofSubmitting = false;
        this.clearProof();
        this.proofMessage = 'El pago esta siendo verificado.';
        this.loadJoinRequests();
        this.notificationService.refresh();
      },
      error: (err) => {
        this.proofSubmitting = false;
        this.proofMessage = err.error?.message || 'No se pudo enviar el comprobante.';
        this.cdr.detectChanges();
      }
    });
  }

  joinRequestStatusLabel(status: string): string {
    const labels: Record<string, string> = {
      PENDING_PAYMENT_VERIFICATION: 'Comprobante pendiente de revision.',
      CONFIRMED: 'Comprobante aprobado.',
      REJECTED: 'Comprobante rechazado.',
      CANCELLED: 'Solicitud cancelada.'
    };
    return labels[status] || status;
  }

  openReportModal(playerId: number): void {
    this.reportTargetId = playerId;
    this.reportReason = 'Comportamiento antideportivo';
    this.reportDetails = '';
    this.reportConfirmation = '';
  }

  closeReportModal(): void {
    this.reportTargetId = null;
  }

  submitReport(): void {
    this.reportConfirmation = 'Reporte enviado.';
    setTimeout(() => this.closeReportModal(), 700);
  }

  approveRequest(request: PlayerJoinRequestResponse): void {
    if (!this.match || !this.currentUserId || !confirm('Aprobar este comprobante?')) return;
    this.matchService.approveJoinRequest(this.match.id, request.id, this.currentUserId).subscribe({
      next: () => {
        this.notificationService.refresh();
        this.loadMatch(this.match!.id);
      },
      error: (err) => {
        this.errorMessage = err.error?.message || 'No se pudo aprobar el comprobante.';
        this.cdr.detectChanges();
      }
    });
  }

  rejectRequest(request: PlayerJoinRequestResponse): void {
    if (!this.match || !this.currentUserId || !confirm('Rechazar este comprobante?')) return;
    this.matchService.rejectJoinRequest(this.match.id, request.id, this.currentUserId, this.rejectReason).subscribe({
      next: () => {
        this.rejectReason = '';
        this.notificationService.refresh();
        this.loadJoinRequests();
      },
      error: (err) => {
        this.errorMessage = err.error?.message || 'No se pudo rechazar el comprobante.';
        this.cdr.detectChanges();
      }
    });
  }

  private loadJoinRequests(): void {
    if (!this.match || !this.currentUserId || !this.match.requiresPlayerPayment) return;
    this.matchService.getJoinRequests(this.match.id, this.currentUserId).subscribe({
      next: (requests) => {
        this.joinRequests = requests;
        this.cdr.detectChanges();
      }
    });
  }

  leaveMatch(): void {
    if (!this.match || !this.currentUserId) return;
    this.matchService.leaveMatch(this.match.id, this.currentUserId).subscribe({
      next: (data) => {
        this.match = data;
        this.cdr.detectChanges();
      },
      error: (err) => {
        this.errorMessage = err.error?.message || 'No se pudo abandonar el partido.';
        this.cdr.detectChanges();
      }
    });
  }

  cancelMatch(): void {
    if (!this.match || !this.currentUserId) return;
    this.matchService.cancelMatch(this.match.id, this.currentUserId).subscribe({
      next: (data) => {
        this.match = data;
        this.cdr.detectChanges();
      },
      error: (err) => {
        this.errorMessage = err.error?.message || 'No se pudo cancelar el partido.';
        this.cdr.detectChanges();
      }
    });
  }

  formatDate(dateStr: string): string {
    const date = new Date(dateStr);
    return date.toLocaleDateString('es-PE', {
      weekday: 'long',
      day: 'numeric',
      month: 'long',
      year: 'numeric',
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
}

const DARK_MAP_STYLE = [
  { elementType: 'geometry', stylers: [{ color: '#0c1120' }] },
  { elementType: 'labels.text.fill', stylers: [{ color: 'rgba(255,255,255,0.55)' }] },
  { elementType: 'labels.text.stroke', stylers: [{ color: '#0c1120' }] },
  { featureType: 'road', elementType: 'geometry', stylers: [{ color: '#2a3350' }] },
  { featureType: 'road.arterial', elementType: 'geometry', stylers: [{ color: '#2a3350' }] },
  { featureType: 'road.highway', elementType: 'geometry', stylers: [{ color: '#39456b' }] },
  { featureType: 'poi', elementType: 'geometry', stylers: [{ color: '#141a2c' }] },
  { featureType: 'poi', elementType: 'labels', stylers: [{ visibility: 'off' }] },
  { featureType: 'water', elementType: 'geometry', stylers: [{ color: '#0a0f1a' }] },
  { featureType: 'transit', stylers: [{ visibility: 'off' }] },
  { featureType: 'administrative', elementType: 'geometry', stylers: [{ color: 'rgba(255,255,255,0.08)' }] }
];
