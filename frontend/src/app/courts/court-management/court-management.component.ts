import { ChangeDetectorRef, Component, ElementRef, OnInit, ViewChild } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { firstValueFrom } from 'rxjs';
import { AuthService } from '@app/auth/auth.service';
import { CourtMediaResponse, CourtResponse, CourtService, SaveCourtRequest } from '@app/courts/court.service';
import { MapsLoaderService } from '@shared/services/maps-loader.service';

interface ScheduleRow { enabled: boolean; start: string; end: string; duration: number; }

@Component({
  selector: 'app-court-management',
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './court-management.component.html',
  styleUrl: './court-management.component.scss'
})
export class CourtManagementComponent implements OnInit {
  @ViewChild('placeAutocompleteHost') placeAutocompleteHost?: ElementRef<HTMLDivElement>;
  @ViewChild('mapHost') mapHost?: ElementRef<HTMLDivElement>;
  courts: CourtResponse[] = [];
  media: CourtMediaResponse[] = [];
  loading = true;
  saving = false;
  uploading = false;
  uploadProgress = 0;
  draggingFiles = false;
  editingCourtId: number | null = null;
  step = 1;
  errorMessage = '';
  successMessage = '';
  mapsMessage = 'Cargando búsqueda de Google Maps...';
  mapsReady = false;
  googlePhotoPreviews: { url: string; attribution: string }[] = [];
  showGooglePhotos = false;
  private geocodeRequestId = 0;
  private placeAutocompleteElement: any = null;
  selectedFiles: File[] = [];
  filePreviews: string[] = [];
  form: SaveCourtRequest = this.emptyForm();

  readonly steps = ['Datos', 'Ubicación', 'Fotografías', 'Horarios y precio', 'Revisión'];
  readonly sports = ['Fútbol', 'Futsal', 'Básquet', 'Voleibol', 'Tenis', 'Pádel'];
  readonly surfaces = ['Grass sintético', 'Césped natural', 'Losa', 'Parquet', 'Arcilla', 'Cemento'];
  readonly serviceOptions = ['Vestidores', 'Duchas', 'Estacionamiento', 'Iluminación', 'Cafetería', 'Alquiler de implementos'];
  readonly dayOptions = [
    { key: 'MONDAY', label: 'Lunes' }, { key: 'TUESDAY', label: 'Martes' },
    { key: 'WEDNESDAY', label: 'Miércoles' }, { key: 'THURSDAY', label: 'Jueves' },
    { key: 'FRIDAY', label: 'Viernes' }, { key: 'SATURDAY', label: 'Sábado' },
    { key: 'SUNDAY', label: 'Domingo' }
  ];
  schedules: Record<string, ScheduleRow> = this.defaultSchedules();

  constructor(
    private readonly courtService: CourtService,
    private readonly authService: AuthService,
    private readonly mapsLoader: MapsLoaderService,
    private readonly cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void { this.loadCourts(); }

  nextStep(): void {
    this.errorMessage = '';
    if (this.isStepValid(this.step)) {
      this.step = Math.min(5, this.step + 1);
      if (this.step === 2) setTimeout(() => void this.initializePlaces());
    }
  }

  previousStep(): void {
    this.errorMessage = '';
    this.step = Math.max(1, this.step - 1);
    if (this.step === 2) setTimeout(() => void this.initializePlaces());
  }
  goToStep(step: number): void {
    if (step < this.step || this.stepsBeforeAreValid(step)) {
      this.step = step;
      if (step === 2) setTimeout(() => void this.initializePlaces());
    }
  }

  toggleListValue(list: string[], value: string): void {
    const index = list.indexOf(value);
    if (index >= 0) list.splice(index, 1); else list.push(value);
  }

  onFilesSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    this.selectFiles(Array.from(input.files || []));
    input.value = '';
  }

  onFileDragOver(event: DragEvent): void {
    event.preventDefault();
    this.draggingFiles = true;
  }

  onFileDragLeave(event: DragEvent): void {
    event.preventDefault();
    this.draggingFiles = false;
  }

  onFilesDropped(event: DragEvent): void {
    event.preventDefault();
    this.draggingFiles = false;
    this.selectFiles(Array.from(event.dataTransfer?.files || []));
  }

  private selectFiles(files: File[]): void {
    if (files.some(file => !file.type.startsWith('image/') || file.size > 8 * 1024 * 1024)) {
      this.errorMessage = 'Cada archivo debe ser una imagen de máximo 8 MB.';
      return;
    }
    if (files.length > 8) {
      this.errorMessage = 'Puedes seleccionar hasta 8 imágenes por carga.';
      return;
    }
    this.errorMessage = '';
    this.clearFilePreviews();
    this.selectedFiles = files;
    this.filePreviews = this.selectedFiles.map(file => URL.createObjectURL(file));
  }

  async saveCourt(publishAfterSave: boolean): Promise<void> {
    const userId = this.authService.getUserId();
    if (!userId || this.saving || !this.stepsBeforeAreValid(6)) return;
    if (publishAfterSave && !this.canPublish) {
      this.errorMessage = 'Para publicar selecciona una ubicación de Google Maps y agrega al menos una imagen.';
      return;
    }
    this.saving = true;
    this.errorMessage = '';
    this.successMessage = '';
    const wasEditing = this.editingCourtId !== null;
    try {
      const request = this.buildRequest(userId);
      const saved = this.editingCourtId
        ? await firstValueFrom(this.courtService.updateCourt(this.editingCourtId, request))
        : await firstValueFrom(this.courtService.createCourt(request));
      this.editingCourtId = saved.id;
      this.uploading = this.selectedFiles.length > 0;
      this.uploadProgress = 0;
      for (let index = 0; index < this.selectedFiles.length; index += 1) {
        await firstValueFrom(this.courtService.uploadMedia(saved.id, this.selectedFiles[index], this.media.length === 0 && index === 0));
        this.uploadProgress = Math.round(((index + 1) / this.selectedFiles.length) * 100);
      }
      this.uploading = false;
      if (publishAfterSave) await firstValueFrom(this.courtService.setPublication(saved.id, userId, true));
      this.successMessage = publishAfterSave ? 'Cancha publicada correctamente.'
        : wasEditing ? 'Borrador actualizado.' : 'Borrador guardado. Puedes publicarlo cuando esté listo.';
      this.resetForm();
      this.loadCourts();
    } catch (error: any) {
      this.uploading = false;
      this.uploadProgress = 0;
      this.errorMessage = error?.error?.message || error?.message || 'No se pudo guardar la cancha.';
    } finally {
      this.saving = false;
      this.cdr.detectChanges();
    }
  }

  editCourt(court: CourtResponse): void {
    this.editingCourtId = court.id;
    this.form = {
      requesterId: this.authService.getUserId() || undefined,
      name: court.name, complexName: court.complexName, description: court.description,
      address: court.address, district: court.district, latitude: court.latitude, longitude: court.longitude,
      pricePerHour: court.pricePerHour, sports: [...court.sports], schedules: [...court.schedules],
      mainImageUrl: court.mainImageUrl, imageUrls: [...court.imageUrls], services: [...court.services],
      features: [...court.features], googlePlaceId: court.googlePlaceId, googlePlaceName: court.googlePlaceName,
      surfaceType: court.surfaceType, capacity: court.capacity, covered: court.covered
    };
    this.schedules = this.parseSchedules(court.schedules);
    this.step = 1;
    this.googlePhotoPreviews = [];
    this.clearFilePreviews();
    this.uploadProgress = 0;
    this.courtService.listMedia(court.id).subscribe(items => this.media = items);
    window.scrollTo({ top: 0, behavior: 'smooth' });
  }

  async deleteMedia(item: CourtMediaResponse): Promise<void> {
    if (!this.editingCourtId || !window.confirm('¿Eliminar esta imagen de la cancha?')) return;
    try {
      await firstValueFrom(this.courtService.deleteMedia(this.editingCourtId, item.id));
      this.media = this.media.filter(media => media.id !== item.id);
      this.form.imageUrls = this.form.imageUrls.filter(url => url !== item.secureUrl);
      if (this.form.mainImageUrl === item.secureUrl) this.form.mainImageUrl = this.media[0]?.secureUrl || null;
    } catch (error: any) { this.errorMessage = error?.error?.message || 'No se pudo eliminar la imagen.'; }
  }

  async moveMedia(index: number, direction: number): Promise<void> {
    if (!this.editingCourtId) return;
    const destination = index + direction;
    if (destination < 0 || destination >= this.media.length) return;
    const reordered = [...this.media];
    [reordered[index], reordered[destination]] = [reordered[destination], reordered[index]];
    try {
      this.media = await firstValueFrom(this.courtService.reorderMedia(this.editingCourtId, reordered.map(item => item.id)));
    } catch (error: any) { this.errorMessage = error?.error?.message || 'No se pudo reordenar la galería.'; }
  }

  async setPrimaryMedia(item: CourtMediaResponse): Promise<void> {
    if (!this.editingCourtId || item.primaryAsset) return;
    try {
      await firstValueFrom(this.courtService.setPrimaryMedia(this.editingCourtId, item.id));
      this.media = this.media.map(media => ({ ...media, primaryAsset: media.id === item.id }));
      this.form.mainImageUrl = item.secureUrl;
    } catch (error: any) { this.errorMessage = error?.error?.message || 'No se pudo cambiar la imagen principal.'; }
  }

  togglePublication(court: CourtResponse): void {
    const requesterId = this.authService.getUserId();
    if (!requesterId) return;
    this.courtService.setPublication(court.id, requesterId, !court.published).subscribe({
      next: () => { this.successMessage = !court.published ? 'Cancha publicada.' : 'Cancha retirada de publicación.'; this.loadCourts(); },
      error: err => this.errorMessage = err.error?.message || 'No se pudo actualizar la publicación.'
    });
  }

  resetForm(): void {
    this.editingCourtId = null;
    this.form = this.emptyForm();
    this.schedules = this.defaultSchedules();
    this.media = [];
    this.googlePhotoPreviews = [];
    this.clearFilePreviews();
    this.step = 1;
  }

  trackCourt(_: number, court: CourtResponse): number { return court.id; }

  get enabledSchedules(): { label: string; value: string }[] {
    return this.dayOptions.filter(day => this.schedules[day.key].enabled)
      .map(day => ({ label: day.label, value: this.scheduleValue(day.key) }));
  }

  get canPublish(): boolean {
    return this.form.latitude !== null && this.form.longitude !== null
      && (this.selectedFiles.length > 0 || this.media.length > 0 || this.form.imageUrls.length > 0);
  }

  private loadCourts(): void {
    const ownerId = this.authService.getUserId();
    if (!ownerId) return;
    this.loading = true;
    this.courtService.getManagedCourts(ownerId).subscribe({
      next: courts => { this.courts = courts; this.loading = false; this.cdr.detectChanges(); },
      error: err => { this.errorMessage = err.error?.message || 'No se pudieron cargar tus canchas.'; this.loading = false; this.cdr.detectChanges(); }
    });
  }

  private async initializePlaces(): Promise<void> {
    try {
      await this.mapsLoader.load();
      const { PlaceAutocompleteElement } = await window.google.maps.importLibrary('places') as any;
      this.placeAutocompleteElement = PlaceAutocompleteElement;
      this.mountPlaceAutocomplete();
      this.mapsReady = true;
      this.mapsMessage = '';
      setTimeout(() => this.renderMap());
    } catch {
      this.mapsMessage = 'Google Maps no está configurado todavía. Puedes guardar un borrador con la dirección manual.';
      this.mapsReady = false;
    }
    this.cdr.detectChanges();
  }

  /** Crea (o recrea vacío) el buscador de Google Places. */
  private mountPlaceAutocomplete(): void {
    if (!this.placeAutocompleteElement) return;
    const autocomplete = new this.placeAutocompleteElement({ includedRegionCodes: ['pe'] });
    autocomplete.placeholder = 'Busca el complejo, club o dirección';
    autocomplete.addEventListener('gmp-select', async (event: any) => {
      const place = event.placePrediction.toPlace();
      await place.fetchFields({ fields: ['id', 'displayName', 'formattedAddress', 'location', 'addressComponents', 'photos'] });
      this.form.googlePlaceId = place.id || null;
      this.form.googlePlaceName = place.displayName || null;
      this.form.address = place.formattedAddress || '';
      this.form.latitude = place.location?.lat() ?? null;
      this.form.longitude = place.location?.lng() ?? null;
      if (this.form.latitude !== null && this.form.longitude !== null
        && !this.isSupportedLocation(this.form.latitude, this.form.longitude)) {
        this.form.latitude = null;
        this.form.longitude = null;
        this.errorMessage = 'Selecciona una ubicación dentro del Perú.';
        this.cdr.detectChanges();
        return;
      }
      this.form.district = this.findDistrict(place.addressComponents) || this.form.district;
      this.googlePhotoPreviews = (place.photos || []).slice(0, 6)
        .map((photo: any) => ({
          url: photo.getURI({ maxWidth: 720, maxHeight: 480 }),
          attribution: (photo.authorAttributions || []).map((author: any) => author.displayName).filter(Boolean).join(', ')
        }));
      this.showGooglePhotos = false;
      this.cdr.detectChanges();
      setTimeout(() => this.renderMap());
    });
    this.placeAutocompleteHost?.nativeElement.replaceChildren(autocomplete);
  }

  /** Al marcar un punto nuevo en el mapa, el lugar elegido en el buscador deja de aplicar. */
  private clearSelectedPlace(): void {
    if (!this.form.googlePlaceId && !this.form.googlePlaceName) return;
    this.form.googlePlaceId = null;
    this.form.googlePlaceName = null;
    this.googlePhotoPreviews = [];
    this.showGooglePhotos = false;
    this.mountPlaceAutocomplete();
  }

  private renderMap(): void {
    if (!this.mapHost || !window.google?.maps) return;
    const hasLocation = this.form.latitude !== null && this.form.longitude !== null;
    const position = hasLocation ? { lat: this.form.latitude, lng: this.form.longitude } : null;
    // Sin ubicación todavía se muestra Lima como punto de partida.
    const map = new window.google.maps.Map(this.mapHost.nativeElement, {
      center: position ?? { lat: -12.0464, lng: -77.0428 },
      zoom: position ? 16 : 12,
      mapTypeControl: false
    });
    const marker = new window.google.maps.Marker({ map: position ? map : null, position, draggable: true });
    // isNewPoint: un clic en otro punto es una ubicación nueva; arrastrar el marcador solo la ajusta.
    const updatePosition = (location: any, isNewPoint: boolean) => {
      const latitude = location.lat();
      const longitude = location.lng();
      if (!this.isSupportedLocation(latitude, longitude)) {
        this.errorMessage = 'La ubicación debe encontrarse dentro del Perú.';
        if (this.form.latitude !== null && this.form.longitude !== null) {
          marker.setPosition({ lat: this.form.latitude, lng: this.form.longitude });
        }
        this.cdr.detectChanges();
        return;
      }
      this.errorMessage = '';
      if (isNewPoint) this.clearSelectedPlace();
      this.form.latitude = latitude;
      this.form.longitude = longitude;
      marker.setPosition(location);
      marker.setMap(map);
      this.cdr.detectChanges();
      void this.fillAddressFromCoordinates(location);
    };
    marker.addListener('dragend', (event: any) => updatePosition(event.latLng, false));
    map.addListener('click', (event: any) => updatePosition(event.latLng, true));
  }

  /**
   * Completa dirección y distrito a partir del punto marcado en el mapa (geocodificación inversa).
   * Si el lugar viene del buscador de Google, se conserva su dirección y solo se ajusta el punto.
   */
  private async fillAddressFromCoordinates(location: any): Promise<void> {
    if (this.form.googlePlaceId) return;
    const requestId = ++this.geocodeRequestId;
    try {
      const { results } = await new window.google.maps.Geocoder().geocode({ location });
      // Ignora respuestas de clics anteriores si el usuario ya marcó otro punto.
      if (requestId !== this.geocodeRequestId || !results?.length) return;
      this.form.address = results[0].formatted_address || this.form.address;
      this.form.district = this.findDistrict(results[0].address_components) || this.form.district;
      this.cdr.detectChanges();
    } catch {
      // Sin Geocoding API o sin resultados: la dirección se completa a mano.
    }
  }

  private findDistrict(components: any[]): string {
    const district = (components || []).find(component => component.types?.includes('locality')
      || component.types?.includes('sublocality_level_1') || component.types?.includes('administrative_area_level_3'));
    // Places (New) usa longText; Geocoder usa long_name.
    return district?.longText || district?.long_name || '';
  }

  private isSupportedLocation(latitude: number, longitude: number): boolean {
    return latitude >= -18.5 && latitude <= 0.5 && longitude >= -81.5 && longitude <= -68.5;
  }

  private isStepValid(step: number): boolean {
    const messages: Record<number, string> = {
      1: 'Completa nombre, deporte, superficie y capacidad.',
      2: 'Selecciona una ubicación o completa dirección y distrito.',
      3: '',
      4: 'Activa al menos un día, verifica sus horas y define el precio.'
    };
    let valid = true;
    if (step === 1) valid = !!this.form.name.trim() && this.form.sports.length > 0 && !!this.form.surfaceType
      && !!this.form.capacity && this.form.capacity > 0;
    if (step === 2) valid = !!this.form.address.trim() && !!this.form.district.trim();
    if (step === 4) valid = this.form.pricePerHour >= 0 && this.enabledSchedules.length > 0 && this.enabledSchedules.every(item => {
      const parts = item.value.split('|'); return parts[1] < parts[2];
    });
    if (!valid) this.errorMessage = messages[step];
    return valid;
  }

  private stepsBeforeAreValid(target: number): boolean {
    for (let step = 1; step < Math.min(target, 5); step += 1) if (!this.isStepValid(step)) return false;
    return true;
  }

  private buildRequest(userId: number): SaveCourtRequest {
    return {
      ...this.form, ownerId: this.editingCourtId ? undefined : userId, requesterId: userId,
      schedules: this.enabledSchedules.map(item => item.value),
      features: [this.form.surfaceType || '', this.form.covered ? 'Techada' : 'Descubierta'].filter(Boolean)
    };
  }

  private scheduleValue(day: string): string {
    const row = this.schedules[day]; return `${day}|${row.start}|${row.end}|${row.duration}`;
  }

  private parseSchedules(values: string[]): Record<string, ScheduleRow> {
    const result = this.defaultSchedules();
    values.forEach(value => {
      const [day, start, end, duration] = value.split('|');
      if (result[day] && start && end) result[day] = { enabled: true, start, end, duration: Number(duration) || 60 };
    });
    return result;
  }

  private defaultSchedules(): Record<string, ScheduleRow> {
    return Object.fromEntries(this.dayOptions.map(day => [day.key,
      { enabled: day.key !== 'SUNDAY', start: '08:00', end: '22:00', duration: 60 }]));
  }

  private emptyForm(): SaveCourtRequest {
    return {
      name: '', complexName: null, description: null, address: '', district: '', latitude: null, longitude: null,
      pricePerHour: 0, sports: [], schedules: [], mainImageUrl: null, imageUrls: [], services: [], features: [],
      googlePlaceId: null, googlePlaceName: null, surfaceType: null, capacity: null, covered: false
    };
  }

  private clearFilePreviews(): void {
    this.filePreviews.forEach(url => URL.revokeObjectURL(url));
    this.filePreviews = [];
    this.selectedFiles = [];
  }
}
