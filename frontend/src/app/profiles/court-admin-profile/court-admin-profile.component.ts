import { ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { forkJoin } from 'rxjs';
import { AuthService } from '@app/auth/auth.service';
import { ProfileService } from '../profile.service';
import { UserService } from '../user.service';
import { SubscriptionService } from '@app/subscriptions/subscription.service';
import { CourtService } from '@app/courts/court.service';

@Component({
  selector: 'app-court-admin-profile',
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './court-admin-profile.component.html',
  styleUrl: '../player-profile/player-profile.component.scss'
})
export class CourtAdminProfileComponent implements OnInit {
  fullName = '';
  email = '';
  plan = '';
  phoneNumber = '';
  district = '';
  businessName = '';
  businessPhone = '';
  operationDistrict = '';
  currentPassword = '';
  newPassword = '';
  confirmPassword = '';
  loading = true;
  saving = false;
  changingPassword = false;
  errorMessage = '';
  successMessage = '';
  passwordMessage = '';
  subscriptionStatus = 'Sin suscripción';
  subscriptionExpiresAt: string | null = null;
  managedCourts = 0;

  readonly districts = ['Barranco', 'Jesús María', 'La Molina', 'Lince', 'Miraflores', 'San Borja', 'San Isidro', 'San Miguel', 'Santiago de Surco'];
  readonly plans = [
    { value: 'CANCHA_SIN_PLAN', label: 'Sin plan comercial' },
    { value: 'CANCHA_EMPRENDEDOR', label: 'Cancha Emprendedor' },
    { value: 'CANCHA_BUSINESS', label: 'Cancha Business' }
  ];

  constructor(
    private readonly profileService: ProfileService,
    private readonly userService: UserService,
    private readonly subscriptionService: SubscriptionService,
    private readonly courtService: CourtService,
    private readonly authService: AuthService,
    private readonly cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.fullName = this.authService.getUserName() || '';
    this.email = this.authService.getUserEmail() || '';
    this.plan = this.authService.getPlan() || '';
    const userId = this.authService.getUserId();
    if (!userId) {
      this.loading = false;
      return;
    }
    if (this.authService.isCourtPlan()) {
      this.courtService.getManagedCourts(userId).subscribe({ next: courts => this.managedCourts = courts.length });
    }
    forkJoin({
      profile: this.profileService.getProfileByUserId(userId),
      subscription: this.subscriptionService.getMine()
    }).subscribe({
      next: ({ profile, subscription }) => {
        this.plan = subscription.currentPlanCode;
        this.phoneNumber = profile.phoneNumber || '';
        this.district = profile.district || '';
        this.businessName = profile.businessName || '';
        this.businessPhone = profile.businessPhone || '';
        this.operationDistrict = profile.operationDistrict || '';
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

  get initials(): string {
    const parts = this.fullName.trim().split(/\s+/).filter(Boolean);
    return parts.length ? parts.slice(0, 2).map(part => part[0]).join('').toUpperCase() : 'PY';
  }

  get planLabel(): string {
    return this.plans.find(plan => plan.value === this.plan)?.label || 'Sin plan seleccionado';
  }

  get subscriptionBenefit(): string {
    if (this.plan === 'CANCHA_BUSINESS') return 'Gestión de múltiples canchas y reportes avanzados durante el periodo vigente.';
    if (this.plan === 'CANCHA_EMPRENDEDOR') return 'Publicación y gestión de canchas durante el periodo vigente.';
    return 'Contrata un plan comercial para publicar canchas y recibir nuevas reservas.';
  }

  get subscriptionActionLabel(): string {
    return this.plan === 'CANCHA_SIN_PLAN' ? 'Contratar un plan' : 'Renovar o cambiar plan';
  }

  save(): void {
    this.errorMessage = '';
    this.successMessage = '';
    if (!this.fullName.trim()) {
      this.errorMessage = 'El nombre completo es obligatorio.';
      return;
    }
    const userId = this.authService.getUserId();
    if (!userId) return;
    this.saving = true;
    forkJoin({
      fullName: this.profileService.updateFullName(userId, this.fullName.trim()),
      profile: this.profileService.updateCourtAdminProfile({
        phoneNumber: this.phoneNumber,
        district: this.district,
        businessName: this.businessName || null,
        businessPhone: this.businessPhone || null,
        operationDistrict: this.operationDistrict || null
      })
    }).subscribe({
      next: result => {
        this.authService.updateUserName(result.fullName.fullName);
        this.saving = false;
        this.successMessage = 'Perfil empresarial actualizado correctamente.';
        this.cdr.detectChanges();
      },
      error: err => {
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
      error: err => {
        this.changingPassword = false;
        this.errorMessage = err.error?.message || 'No se pudo actualizar la contraseña.';
        this.cdr.detectChanges();
      }
    });
  }
}
