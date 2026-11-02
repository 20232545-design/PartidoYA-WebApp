import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { AuthService } from '@core/auth/auth.service';
import { CourtResponse, CourtService, SaveCourtRequest } from '@features/courts/data-access/court.service';

@Component({
  selector: 'app-admin-canchas',
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './admin-canchas.component.html',
  styleUrl: './admin-canchas.component.scss'
})
export class AdminCanchasComponent implements OnInit {
  courts: CourtResponse[] = [];
  loading = true;
  saving = false;
  editingCourtId: number | null = null;
  errorMessage = '';
  successMessage = '';

  form: SaveCourtRequest = this.emptyForm();
  sportsInput = 'Fútbol';
  schedulesInput = '18:00-19:00, 19:00-20:00';
  imageUrlsInput = '';
  servicesInput = 'Vestidores, Iluminación';
  featuresInput = 'Grass sintético';

  constructor(
    private courtService: CourtService,
    private authService: AuthService
  ) {}

  ngOnInit(): void {
    this.loadCourts();
  }

  loadCourts(): void {
    const ownerId = this.authService.getUserId();
    if (!ownerId) return;
    this.loading = true;
    this.errorMessage = '';
    this.courtService.getManagedCourts(ownerId).subscribe({
      next: (courts) => {
        this.courts = courts;
        this.loading = false;
      },
      error: (err) => {
        this.errorMessage = err.error?.message || 'No se pudieron cargar tus canchas.';
        this.loading = false;
      }
    });
  }

  saveCourt(): void {
    const userId = this.authService.getUserId();
    if (!userId || this.saving) return;

    this.saving = true;
    this.errorMessage = '';
    this.successMessage = '';

    const request = this.buildRequest(userId);
    const save$ = this.editingCourtId
      ? this.courtService.updateCourt(this.editingCourtId, request)
      : this.courtService.createCourt(request);

    save$.subscribe({
      next: () => {
        this.successMessage = this.editingCourtId ? 'Cancha actualizada.' : 'Cancha creada.';
        this.saving = false;
        this.resetForm();
        this.loadCourts();
      },
      error: (err) => {
        this.errorMessage = err.error?.message || 'No se pudo guardar la cancha.';
        this.saving = false;
      }
    });
  }

  editCourt(court: CourtResponse): void {
    this.editingCourtId = court.id;
    this.form = {
      requesterId: this.authService.getUserId() || undefined,
      name: court.name,
      complexName: court.complexName,
      description: court.description,
      address: court.address,
      district: court.district,
      latitude: court.latitude,
      longitude: court.longitude,
      pricePerHour: court.pricePerHour,
      sports: court.sports,
      schedules: court.schedules,
      mainImageUrl: court.mainImageUrl,
      imageUrls: court.imageUrls,
      services: court.services,
      features: court.features
    };
    this.sportsInput = court.sports.join(', ');
    this.schedulesInput = court.schedules.join(', ');
    this.imageUrlsInput = court.imageUrls.join(', ');
    this.servicesInput = court.services.join(', ');
    this.featuresInput = court.features.join(', ');
    window.scrollTo({ top: 0, behavior: 'smooth' });
  }

  togglePublication(court: CourtResponse): void {
    const requesterId = this.authService.getUserId();
    if (!requesterId) return;
    this.errorMessage = '';
    this.successMessage = '';
    this.courtService.setPublication(court.id, requesterId, !court.published).subscribe({
      next: () => {
        this.successMessage = !court.published ? 'Cancha publicada.' : 'Cancha retirada de publicación.';
        this.loadCourts();
      },
      error: (err) => {
        this.errorMessage = err.error?.message || 'No se pudo actualizar la publicación.';
      }
    });
  }

  resetForm(): void {
    this.editingCourtId = null;
    this.form = this.emptyForm();
    this.sportsInput = 'Fútbol';
    this.schedulesInput = '18:00-19:00, 19:00-20:00';
    this.imageUrlsInput = '';
    this.servicesInput = 'Vestidores, Iluminación';
    this.featuresInput = 'Grass sintético';
  }

  trackCourt(_: number, court: CourtResponse): number {
    return court.id;
  }

  private buildRequest(userId: number): SaveCourtRequest {
    return {
      ...this.form,
      ownerId: this.editingCourtId ? undefined : userId,
      requesterId: userId,
      sports: this.parseList(this.sportsInput),
      schedules: this.parseList(this.schedulesInput),
      imageUrls: this.parseList(this.imageUrlsInput),
      services: this.parseList(this.servicesInput),
      features: this.parseList(this.featuresInput)
    };
  }

  private emptyForm(): SaveCourtRequest {
    return {
      name: '',
      complexName: null,
      description: null,
      address: '',
      district: '',
      latitude: null,
      longitude: null,
      pricePerHour: 0,
      sports: [],
      schedules: [],
      mainImageUrl: null,
      imageUrls: [],
      services: [],
      features: []
    };
  }

  private parseList(value: string): string[] {
    return value
      .split(',')
      .map(item => item.trim())
      .filter(Boolean);
  }
}
