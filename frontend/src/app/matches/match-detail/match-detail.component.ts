import { Component, OnInit, ChangeDetectorRef, ElementRef, ViewChild } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { MatchService, MatchResponse, PlayerJoinRequestResponse } from '@app/matches/match.service';
import { AuthService } from '@app/auth/auth.service';
import { MapsLoaderService } from '@shared/services/maps-loader.service';
import { OpenMapService } from '@shared/services/open-map.service';
import { GeolocationService } from '@shared/services/geolocation.service';
import { environment } from '@env/environment';
import { NotificationService } from '@app/notifications/notification.service';
import { JoinRequestService } from '@app/matches/join-request.service';
import { PlayerReportService } from '@app/matches/player-report.service';
import {
  REPORT_DETAILS_MAX_LENGTH,
  REPORT_REASON_OPTIONS,
  ReportReason
} from '@shared/models/player-report.models';

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
  reportReason: ReportReason = 'UNSPORTSMANLIKE_CONDUCT';
  reportDetails = '';
  reportConfirmation = '';
  reportError = '';
  reportSubmitting = false;
  /** Jugadores que el usuario ya reportó en este partido; el backend rechaza duplicados. */
  reportedPlayerIds = new Set<number>();
  readonly reportReasons = REPORT_REASON_OPTIONS;
  readonly reportDetailsMaxLength = REPORT_DETAILS_MAX_LENGTH;

  constructor(
    private matchService: MatchService,
    private joinRequestService: JoinRequestService,
    private playerReportService: PlayerReportService,
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
        this.loadReportedPlayers();
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
    if (!this.match || !this.mapContainer) {
      this.mapLoading = false;
      return;
    }
    const container = this.mapContainer.nativeElement;

    // La ubicación del usuario solo es necesaria para la ruta; la cancha se muestra siempre.
    const [mapsResult, positionResult] = await Promise.allSettled([
      this.mapsLoader.load(),
      this.geolocation.getPosition()
    ]);
    const mapsReady = mapsResult.status === 'fulfilled';
    const position = positionResult.status === 'fulfilled' ? positionResult.value : null;
    const locationHint = 'Activa el permiso de ubicación para ver la ruta.';

    if (mapsReady && position) {
      await this.renderDirections(position);
      return;
    }

    try {
      if (mapsReady) {
        await this.renderGoogleDestination(container);
        this.mapError = locationHint;
      } else {
        this.renderOpenMapFallback(container, position);
        this.mapError = this.mapError || (position ? '' : locationHint);
      }
    } catch {
      this.mapError = 'No se pudo cargar el mapa. Usa “Abrir en Google Maps”.';
    } finally {
      this.mapLoading = false;
      this.cdr.detectChanges();
    }
  }

  private async renderGoogleDestination(container: HTMLElement): Promise<void> {
    const google = window.google;
    const match = this.match!;
    let center: { lat: number; lng: number };

    if (match.latitude !== null && match.longitude !== null) {
      center = { lat: match.latitude, lng: match.longitude };
    } else {
      const { results } = await new google.maps.Geocoder().geocode({ address: match.address });
      if (!results?.length) throw new Error('Dirección no encontrada');
      const location = results[0].geometry.location;
      center = { lat: location.lat(), lng: location.lng() };
    }

    const map = new google.maps.Map(container, {
      center,
      zoom: 15,
      disableDefaultUI: true,
      zoomControl: true,
      zoomControlOptions: { position: google.maps.ControlPosition.RIGHT_TOP },
      styles: DARK_MAP_STYLE
    });
    new google.maps.Marker({ position: center, map, title: match.title });
  }

  private renderOpenMapFallback(container: HTMLElement, position: GeolocationPosition | null): void {
    const match = this.match!;
    if (match.latitude === null || match.longitude === null) {
      this.mapError = 'No hay coordenadas guardadas. Usa “Abrir en Google Maps” para ver la ruta.';
      return;
    }

    if (position) {
      const summary = this.openMap.renderDirectRoute(
        container,
        [position.coords.latitude, position.coords.longitude],
        [match.latitude, match.longitude]
      );
      this.routeDuration = summary.duration;
      this.routeDistance = summary.distance;
    } else {
      this.openMap.renderLocation(container, match.latitude, match.longitude, match.title);
    }
  }

  /** Calcula la ruta en auto con la Routes API (la Directions API legacy no está habilitada). */
  private async renderDirections(position: GeolocationPosition): Promise<void> {
    if (!this.match || !this.mapContainer) {
      this.mapLoading = false;
      return;
    }
    const google = window.google;
    const match = this.match;
    const origin = { lat: position.coords.latitude, lng: position.coords.longitude };
    const destination = match.latitude !== null && match.longitude !== null
      ? { lat: match.latitude, lng: match.longitude }
      : match.address;

    const map = new google.maps.Map(this.mapContainer.nativeElement, {
      center: origin,
      zoom: 13,
      disableDefaultUI: true,
      zoomControl: true,
      zoomControlOptions: { position: google.maps.ControlPosition.RIGHT_TOP },
      styles: DARK_MAP_STYLE
    });

    try {
      const { Route } = await google.maps.importLibrary('routes') as any;
      const { routes } = await Route.computeRoutes({
        origin,
        destination,
        travelMode: 'DRIVING',
        language: 'es',
        region: 'PE',
        fields: ['path', 'distanceMeters', 'durationMillis']
      });
      const route = routes?.[0];
      if (!route?.path?.length) throw new Error('Sin rutas');

      new google.maps.Polyline({ map, path: route.path, strokeColor: '#c6f135', strokeWeight: 5 });
      const end = route.path[route.path.length - 1];
      new google.maps.Marker({ position: origin, map, title: 'Tu ubicación', label: 'A' });
      new google.maps.Marker({ position: end, map, title: match.title, label: 'B' });

      const bounds = new google.maps.LatLngBounds();
      route.path.forEach((point: any) => bounds.extend(point));
      map.fitBounds(bounds, 48);

      this.routeDuration = this.formatDuration(route.durationMillis);
      this.routeDistance = this.formatDistance(route.distanceMeters);
      this.mapError = '';
    } catch (err) {
      console.warn('[Mapa] Routes API no pudo calcular la ruta:', err);
      this.renderStraightLine(map, origin);
    } finally {
      this.mapLoading = false;
      this.cdr.detectChanges();
    }
  }

  private formatDuration(millis: number | undefined): string {
    if (!millis) return '';
    const minutes = Math.max(1, Math.round(millis / 60000));
    if (minutes < 60) return `${minutes} min`;
    const hours = Math.floor(minutes / 60);
    const rest = minutes % 60;
    return rest ? `${hours} h ${rest} min` : `${hours} h`;
  }

  private formatDistance(meters: number | undefined): string {
    if (!meters) return '';
    return meters < 1000 ? `${meters} m` : `${(meters / 1000).toFixed(1)} km`;
  }

  /** Si Google no puede calcular la ruta, muestra ambos puntos y la distancia en línea recta. */
  private renderStraightLine(map: any, origin: { lat: number; lng: number }): void {
    const google = window.google;
    const match = this.match;
    if (!match || match.latitude === null || match.longitude === null) {
      this.mapError = 'No se pudo calcular la ruta a la cancha.';
      return;
    }
    const destination = { lat: match.latitude, lng: match.longitude };

    new google.maps.Marker({ position: origin, map, title: 'Tu ubicación' });
    new google.maps.Marker({ position: destination, map, title: match.title });
    new google.maps.Polyline({
      map,
      path: [origin, destination],
      strokeOpacity: 0,
      icons: [{
        icon: { path: 'M 0,-1 0,1', strokeOpacity: 1, strokeColor: '#c6f135', scale: 3 },
        offset: '0',
        repeat: '14px'
      }]
    });

    const bounds = new google.maps.LatLngBounds();
    bounds.extend(origin);
    bounds.extend(destination);
    map.fitBounds(bounds, 48);

    const km = this.geolocation.distanceKm(origin.lat, origin.lng, destination.lat, destination.lng);
    this.routeDuration = '';
    this.routeDistance = '';
    this.mapError = `No se pudo calcular la ruta en auto · ${km.toFixed(1)} km en línea recta.`;
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
    this.joinRequestService.submitProof(this.match.id, this.currentUserId, this.proofFile).subscribe({
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

  playerName(playerId: number): string {
    return this.match?.playerNames?.[playerId] || `Jugador #${playerId}`;
  }

  initial(playerId: number): string {
    return this.playerName(playerId).trim().charAt(0).toUpperCase() || 'J';
  }

  /** Solo quien participa del partido puede reportar, y nunca a sí mismo. */
  canReport(playerId: number): boolean {
    return this.hasJoined || this.isOrganizer
      ? playerId !== this.currentUserId
      : false;
  }

  isReported(playerId: number): boolean {
    return this.reportedPlayerIds.has(playerId);
  }

  get reportDetailsRequired(): boolean {
    return this.reportReason === 'OTHER';
  }

  get canSubmitReport(): boolean {
    return !this.reportSubmitting
      && !this.reportConfirmation
      && (!this.reportDetailsRequired || this.reportDetails.trim().length > 0);
  }

  openReportModal(playerId: number): void {
    if (!this.canReport(playerId) || this.isReported(playerId)) return;
    this.reportTargetId = playerId;
    this.reportReason = 'UNSPORTSMANLIKE_CONDUCT';
    this.reportDetails = '';
    this.reportConfirmation = '';
    this.reportError = '';
  }

  closeReportModal(): void {
    if (this.reportSubmitting) return;
    this.reportTargetId = null;
  }

  submitReport(): void {
    const matchId = this.match?.id;
    const reportedPlayerId = this.reportTargetId;
    if (!matchId || reportedPlayerId === null || !this.canSubmitReport) return;

    this.reportSubmitting = true;
    this.reportError = '';
    this.playerReportService.submit(matchId, {
      reportedPlayerId,
      reason: this.reportReason,
      details: this.reportDetails.trim() || null
    }).subscribe({
      next: () => {
        this.reportedPlayerIds.add(reportedPlayerId);
        this.reportSubmitting = false;
        this.reportConfirmation = 'Reporte enviado. Nuestro equipo lo revisará.';
        this.cdr.detectChanges();
        setTimeout(() => {
          this.closeReportModal();
          this.cdr.detectChanges();
        }, 1500);
      },
      error: (err) => {
        this.reportSubmitting = false;
        if (err.status === 409) {
          // Ya existía un reporte (por ejemplo, desde otra pestaña): reflejarlo en la UI.
          this.reportedPlayerIds.add(reportedPlayerId);
          this.reportError = 'Ya reportaste a este jugador en este partido.';
        } else {
          this.reportError = err.error?.message || 'No se pudo enviar el reporte. Inténtalo nuevamente.';
        }
        this.cdr.detectChanges();
      }
    });
  }

  private loadReportedPlayers(): void {
    if (!this.match || !(this.hasJoined || this.isOrganizer)) return;
    this.playerReportService.reportedByMe(this.match.id).subscribe({
      next: (ids) => {
        this.reportedPlayerIds = new Set(ids);
        this.cdr.detectChanges();
      },
      // No es crítico: si falla, el backend igual rechaza los duplicados con 409.
      error: () => {}
    });
  }

  approveRequest(request: PlayerJoinRequestResponse): void {
    if (!this.match || !this.currentUserId || !confirm('Aprobar este comprobante?')) return;
    this.joinRequestService.approve(this.match.id, request.id, this.currentUserId).subscribe({
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
    this.joinRequestService.reject(this.match.id, request.id, this.currentUserId, this.rejectReason).subscribe({
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
    this.joinRequestService.getRequests(this.match.id, this.currentUserId).subscribe({
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
