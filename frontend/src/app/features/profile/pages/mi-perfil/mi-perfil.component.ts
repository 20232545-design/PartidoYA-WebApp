import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { forkJoin } from 'rxjs';
import { ProfileService } from '@features/profile/data-access/profile.service';
import { AuthService } from '@core/auth/auth.service';

@Component({
  selector: 'app-mi-perfil',
  imports: [CommonModule, FormsModule],
  templateUrl: './mi-perfil.component.html',
  styleUrl: './mi-perfil.component.scss'
})
export class MiPerfilComponent implements OnInit {
  fullName = '';
  primarySport = '';
  skillLevel = '';
  availability: string[] = [];
  role = '';
  plan = '';
  loading = true;
  saving = false;
  nameInvalid = false;
  errorMessage = '';
  successMessage = '';

  readonly sports = ['Fútbol', 'Básquet', 'Voleibol', 'Tenis'];

  readonly skillLevels = [
    { value: 'BEGINNER', label: 'Básico' },
    { value: 'INTERMEDIATE', label: 'Medio' },
    { value: 'ADVANCED', label: 'Avanzado' }
  ];

  readonly timeSlots = [
    { value: 'MORNING', label: 'Mañana' },
    { value: 'AFTERNOON', label: 'Tarde' },
    { value: 'EVENING', label: 'Noche' }
  ];

  readonly plansByRole: Record<string, { value: string; label: string }[]> = {
    JUGADOR: [
      { value: 'JUGADOR_BASICO', label: 'Básico (gratis)' },
      { value: 'JUGADOR_PLUS', label: 'Plus' }
    ],
    ADMIN_CANCHA: [
      { value: 'CANCHA_EMPRENDEDOR', label: 'Cancha Emprendedor' },
      { value: 'CANCHA_BUSINESS', label: 'Cancha Business' }
    ]
  };

  constructor(
    private profileService: ProfileService,
    private authService: AuthService,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.fullName = this.authService.getUserName() || '';
    this.role = this.authService.getRole() || '';
    this.plan = this.authService.getPlan() || '';
    const userId = this.authService.getUserId();
    if (!userId) {
      this.loading = false;
      return;
    }
    this.profileService.getProfileByUserId(userId).subscribe({
      next: (profile) => {
        this.primarySport = profile.primarySport || '';
        this.skillLevel = profile.skillLevel || '';
        this.availability = profile.availability || [];
        this.loading = false;
        this.cdr.detectChanges();
      },
      error: () => {
        this.loading = false;
        this.cdr.detectChanges();
      }
    });
  }

  get roleLabel(): string {
    return this.role === 'ADMIN_CANCHA' ? 'Administrador de cancha' : 'Jugador';
  }

  get initials(): string {
    const parts = this.fullName.trim().split(/\s+/).filter(Boolean);
    if (parts.length === 0) return 'PY';
    return parts.slice(0, 2).map(part => part[0]).join('').toUpperCase();
  }

  get planLabel(): string {
    return this.availablePlans.find(p => p.value === this.plan)?.label || 'Sin plan seleccionado';
  }

  get displaySport(): string {
    return this.primarySport || 'Sin definir';
  }

  get displaySkillLevel(): string {
    return this.skillLevels.find(level => level.value === this.skillLevel)?.label || 'Sin definir';
  }

  get availablePlans(): { value: string; label: string }[] {
    return this.plansByRole[this.role] || [];
  }

  save(): void {
    this.errorMessage = '';
    this.successMessage = '';
    this.nameInvalid = !this.fullName.trim();
    if (this.nameInvalid) {
      return;
    }

    const userId = this.authService.getUserId();
    if (!userId) return;

    this.saving = true;
    const requests = {
      fullName: this.profileService.updateFullName(userId, this.fullName.trim()),
      plan: this.profileService.updatePlan(userId, this.plan),
      ...(this.primarySport
        ? { primarySport: this.profileService.updatePrimarySport(userId, this.primarySport) }
        : {}),
      ...(this.skillLevel
        ? { gamePreferences: this.profileService.updateGamePreferences(userId, this.skillLevel, this.availability) }
        : {})
    };

    forkJoin(requests).subscribe({
      next: (result) => {
        localStorage.setItem('userName', result.fullName.fullName);
        this.authService.setPlan(result.plan.plan);
        this.saving = false;
        this.successMessage = 'Perfil actualizado correctamente.';
        this.cdr.detectChanges();
      },
      error: (err) => {
        this.saving = false;
        this.errorMessage = err.error?.message || 'No se pudo actualizar el perfil.';
        this.cdr.detectChanges();
      }
    });
  }

  onNameInput(): void {
    if (this.nameInvalid && this.fullName.trim()) {
      this.nameInvalid = false;
    }
  }

  toggleAvailability(slot: string): void {
    const index = this.availability.indexOf(slot);
    if (index === -1) {
      this.availability.push(slot);
    } else {
      this.availability.splice(index, 1);
    }
  }
}
