import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { forkJoin } from 'rxjs';
import { ProfileService } from '@app/profiles/profile.service';
import { AuthService } from '@app/auth/auth.service';
import { UserService } from '@app/profiles/user.service';
import { SubscriptionService } from '@app/subscriptions/subscription.service';

@Component({
  selector: 'app-player-profile',
  imports: [CommonModule, FormsModule],
  templateUrl: './player-profile.component.html',
  styleUrl: './player-profile.component.scss'
})
export class PlayerProfileComponent implements OnInit {
  fullName = '';
  primarySport = '';
  skillLevel = '';
  availability: string[] = [];
  favoriteSports: string[] = [];
  role = '';
  plan = '';
  email = '';
  phoneNumber = '';
  district = '';
  currentPassword = '';
  newPassword = '';
  confirmPassword = '';
  changingPassword = false;
  passwordMessage = '';
  subscriptionStatus = 'Sin suscripción';
  subscriptionExpiresAt: string | null = null;
  loading = true;
  saving = false;
  nameInvalid = false;
  errorMessage = '';
  successMessage = '';

  readonly sports = ['Fútbol', 'Básquet', 'Voleibol', 'Tenis'];
  readonly districts = ['Barranco', 'Jesús María', 'La Molina', 'Lince', 'Miraflores', 'San Borja', 'San Isidro', 'San Miguel', 'Santiago de Surco'];

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

  readonly plans = [
    { value: 'JUGADOR_BASICO', label: 'Básico (gratis)' },
    { value: 'JUGADOR_PLUS', label: 'Plus' }
  ];

  constructor(
    private profileService: ProfileService,
    private userService: UserService,
    private subscriptionService: SubscriptionService,
    private authService: AuthService,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.fullName = this.authService.getUserName() || '';
    this.role = this.authService.getRole() || '';
    this.plan = this.authService.getPlan() || '';
    this.email = this.authService.getUserEmail() || '';
    const userId = this.authService.getUserId();
    if (!userId) {
      this.loading = false;
      return;
    }
    forkJoin({
      profile: this.profileService.getProfileByUserId(userId),
      subscription: this.subscriptionService.getMine()
    }).subscribe({
      next: ({ profile, subscription }) => {
        this.plan = subscription.currentPlanCode;
        this.primarySport = profile.primarySport || '';
        this.skillLevel = profile.skillLevel || '';
        this.availability = profile.availability || [];
        this.favoriteSports = (profile.sportPreferences || []).map(preference => preference.sportName);
        this.phoneNumber = profile.phoneNumber || '';
        this.district = profile.district || '';
        this.subscriptionStatus = subscription.subscription?.status || (subscription.legacyAccess ? 'Acceso histórico' : 'Sin suscripción');
        this.subscriptionExpiresAt = subscription.subscription?.currentPeriodEnd || null;
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
    return 'Jugador';
  }

  get initials(): string {
    const parts = this.fullName.trim().split(/\s+/).filter(Boolean);
    if (parts.length === 0) return 'PY';
    return parts.slice(0, 2).map(part => part[0]).join('').toUpperCase();
  }

  get planLabel(): string {
    return this.plans.find(p => p.value === this.plan)?.label || 'Sin plan seleccionado';
  }

  get subscriptionBenefit(): string {
    return this.plan === 'JUGADOR_PLUS'
      ? 'Insignia Plus vigente hasta el fin de tu periodo pagado. Después pasarás al plan gratuito.'
      : 'Los jugadores usan PartidoYA gratis: organiza y participa en partidos sin costo.';
  }

  get displaySport(): string {
    return this.primarySport || 'Sin definir';
  }

  get displaySkillLevel(): string {
    return this.skillLevels.find(level => level.value === this.skillLevel)?.label || 'Sin definir';
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
      profile: this.profileService.updatePlayerProfile({
        phoneNumber: this.phoneNumber,
        district: this.district,
        primarySport: this.primarySport || null,
        skillLevel: this.skillLevel || null,
        availability: this.availability,
        favoriteSports: this.favoriteSports
      })
    };

    forkJoin(requests).subscribe({
      next: (result) => {
        this.authService.updateUserName(result.fullName.fullName);
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

  changePassword(): void {
    this.errorMessage = '';
    this.passwordMessage = '';
    if (!this.currentPassword || !this.newPassword || this.newPassword !== this.confirmPassword) {
      this.errorMessage = 'Completa las contraseñas y confirma correctamente la nueva.';
      return;
    }
    this.changingPassword = true;
    this.userService.changePassword(this.currentPassword, this.newPassword).subscribe({
      next: () => {
        this.changingPassword = false;
        this.currentPassword = '';
        this.newPassword = '';
        this.confirmPassword = '';
        this.passwordMessage = 'Contraseña actualizada correctamente.';
        this.cdr.detectChanges();
      },
      error: (err) => {
        this.changingPassword = false;
        this.errorMessage = err.error?.message || 'No se pudo actualizar la contraseña.';
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

  toggleFavoriteSport(sport: string): void {
    const index = this.favoriteSports.indexOf(sport);
    if (index >= 0) this.favoriteSports.splice(index, 1);
    else this.favoriteSports.push(sport);
  }
}
