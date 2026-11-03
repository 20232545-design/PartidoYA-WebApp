import { CommonModule } from '@angular/common';
import { ChangeDetectorRef, Component, OnInit, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { firstValueFrom } from 'rxjs';
import { AuthService } from '@app/auth/auth.service';
import { RegistrationRequest } from '@app/auth/auth.models';
import { SubscriptionPaymentService } from '@app/subscriptions/subscription-payment.service';
import { SubscriptionService } from '@app/subscriptions/subscription.service';
import { SubscriptionPlan } from '@app/subscriptions/subscription.models';

type PublicRole = 'JUGADOR' | 'ADMIN_CANCHA';

@Component({
  selector: 'app-register',
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  templateUrl: './register.component.html',
  styleUrl: './register.component.scss'
})
export class RegisterComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly authService = inject(AuthService);
  private readonly subscriptionService = inject(SubscriptionService);
  private readonly paymentService = inject(SubscriptionPaymentService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly cdr = inject(ChangeDetectorRef);

  step = 1;
  role: PublicRole = 'JUGADOR';
  selectedPlanCode = '';
  plans: SubscriptionPlan[] = [];
  loadingPlans = true;
  submitting = false;
  showPassword = false;
  accountCreated = false;
  errorMessage = '';
  completionTitle = '';
  completionMessage = '';
  readonly availability = new Set<string>();

  readonly sports = ['Fútbol', 'Básquet', 'Voleibol', 'Tenis'];
  readonly districts = ['Barranco', 'Jesús María', 'La Molina', 'Lince', 'Miraflores', 'San Borja', 'San Isidro', 'San Miguel', 'Santiago de Surco'];
  readonly skillLevels = [
    { value: 'BEGINNER', label: 'Básico' },
    { value: 'INTERMEDIATE', label: 'Intermedio' },
    { value: 'ADVANCED', label: 'Avanzado' }
  ];
  readonly timeSlots = [
    { value: 'MORNING', label: 'Mañana' },
    { value: 'AFTERNOON', label: 'Tarde' },
    { value: 'EVENING', label: 'Noche' }
  ];

  readonly form = this.fb.nonNullable.group({
    firstName: ['', [Validators.required, Validators.maxLength(60)]],
    lastName: ['', [Validators.required, Validators.maxLength(60)]],
    email: ['', [Validators.required, Validators.email]],
    phone: ['+51', [Validators.required, Validators.pattern(/^\+51\d{9}$/)]],
    district: ['', Validators.required],
    password: ['', [Validators.required, Validators.minLength(10), Validators.pattern(/^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[^A-Za-z0-9]).{10,}$/)]],
    confirmPassword: ['', Validators.required],
    primarySport: [''],
    skillLevel: [''],
    businessName: [''],
    businessPhone: ['+51'],
    operationDistrict: [''],
    termsAccepted: [false, Validators.requiredTrue],
    privacyAccepted: [false, Validators.requiredTrue],
    marketingConsent: [false]
  });

  ngOnInit(): void {
    const queryRole = this.route.snapshot.queryParamMap.get('role');
    if (queryRole === 'JUGADOR' || queryRole === 'ADMIN_CANCHA') this.role = queryRole;
    this.subscriptionService.getPlans().subscribe({
      next: plans => {
        this.plans = plans;
        const requestedPlan = this.route.snapshot.queryParamMap.get('plan');
        const compatible = this.rolePlans.find(plan => plan.code === requestedPlan);
        this.selectedPlanCode = compatible?.code || this.rolePlans[0]?.code || '';
        this.loadingPlans = false;
        this.cdr.detectChanges();
      },
      error: () => {
        this.loadingPlans = false;
        this.errorMessage = 'No pudimos cargar los planes. Inténtalo nuevamente.';
        this.cdr.detectChanges();
      }
    });
  }

  get rolePlans(): SubscriptionPlan[] {
    return this.plans.filter(plan => plan.role === this.role);
  }

  get selectedPlan(): SubscriptionPlan | undefined {
    return this.plans.find(plan => plan.code === this.selectedPlanCode);
  }

  get passwordsMatch(): boolean {
    return this.form.controls.password.value === this.form.controls.confirmPassword.value;
  }

  get passwordStrength(): number {
    const value = this.form.controls.password.value;
    return [value.length >= 10, /[a-z]/.test(value), /[A-Z]/.test(value), /\d/.test(value), /[^A-Za-z0-9]/.test(value)]
      .filter(Boolean).length;
  }

  get priceLabel(): string {
    if (!this.selectedPlan) return '';
    if (this.selectedPlan.amountCents === 0) return 'Gratis';
    return `S/ ${(this.selectedPlan.amountCents / 100).toFixed(2)} por 30 días`;
  }

  selectRole(role: PublicRole): void {
    this.role = role;
    this.selectedPlanCode = this.rolePlans[0]?.code || '';
    this.errorMessage = '';
  }

  selectPlan(code: string): void {
    this.selectedPlanCode = code;
    this.errorMessage = '';
  }

  next(): void {
    this.errorMessage = '';
    if (this.step === 2 && !this.selectedPlan) {
      this.errorMessage = 'Selecciona un plan para continuar.';
      return;
    }
    if (this.step === 3 && !this.validDataStep()) return;
    if (this.step < 4) this.step += 1;
  }

  previous(): void {
    this.errorMessage = '';
    if (this.step > 1) this.step -= 1;
  }

  toggleAvailability(slot: string): void {
    if (this.availability.has(slot)) this.availability.delete(slot);
    else this.availability.add(slot);
  }

  async submit(): Promise<void> {
    if (!this.validDataStep() || !this.selectedPlan) return;
    this.submitting = true;
    this.errorMessage = '';
    const request = this.registrationRequest();
    try {
      await firstValueFrom(this.authService.register(request));
      await firstValueFrom(this.authService.login(request.email, request.password));
      this.accountCreated = true;
      if (!this.selectedPlan.paid) {
        this.completionTitle = 'Tu cuenta está lista';
        this.completionMessage = 'Tu plan Básico ya está activo y no tiene costo.';
      } else if (!this.selectedPlan.subscriptionAvailable) {
        this.completionTitle = 'Cuenta creada sin cobros';
        this.completionMessage = 'Los pagos con Culqi aún no están habilitados. Tu cuenta permanece sin plan pagado.';
      } else {
        const token = await this.paymentService.captureCardToken(
          `Acceso por 30 días - ${this.selectedPlan.displayName}`,
          this.selectedPlan.amountCents);
        const subscription = await firstValueFrom(this.subscriptionService.create(this.selectedPlan.code, token));
        this.completionTitle = subscription.status === 'FAILED' ? 'Cuenta creada sin cobros' : 'Plan activado';
        this.completionMessage = subscription.status === 'FAILED'
          ? 'Culqi no pudo completar el pago. Tu cuenta permanece en el plan gratuito o sin plan.'
          : 'El pago fue confirmado y tienes 30 días de acceso, sin renovación automática.';
      }
    } catch (error: any) {
      if (this.accountCreated) {
        this.completionTitle = 'Cuenta creada sin cobros';
        this.completionMessage = error?.message || 'No se completó el pago. Tu cuenta permanece sin un plan pagado.';
      } else {
        this.errorMessage = error?.error?.message || 'No pudimos crear la cuenta. Revisa los datos e inténtalo nuevamente.';
      }
    } finally {
      this.submitting = false;
      this.cdr.detectChanges();
    }
  }

  goToAccount(): void {
    this.router.navigate([this.selectedPlan?.paid ? '/mi-suscripcion' : '/home']);
  }

  private validDataStep(): boolean {
    this.form.markAllAsTouched();
    if (this.form.invalid || !this.passwordsMatch) {
      this.errorMessage = 'Completa los campos obligatorios y revisa la contraseña.';
      return false;
    }
    if (this.role === 'JUGADOR' && (!this.form.controls.primarySport.value || !this.form.controls.skillLevel.value)) {
      this.errorMessage = 'Selecciona tu deporte principal y nivel de juego.';
      return false;
    }
    if (this.role === 'ADMIN_CANCHA'
      && (!this.form.controls.businessName.value || !this.form.controls.operationDistrict.value
        || !/^\+51\d{9}$/.test(this.form.controls.businessPhone.value))) {
      this.errorMessage = 'Completa el nombre, teléfono y distrito de operación de la cancha.';
      return false;
    }
    return true;
  }

  private registrationRequest(): RegistrationRequest {
    const value = this.form.getRawValue();
    return {
      email: value.email.trim(),
      firstName: value.firstName.trim(),
      lastName: value.lastName.trim(),
      password: value.password,
      role: this.role,
      planCode: this.selectedPlanCode,
      phone: value.phone,
      district: value.district,
      primarySport: this.role === 'JUGADOR' ? value.primarySport : null,
      skillLevel: this.role === 'JUGADOR' ? value.skillLevel : null,
      availability: this.role === 'JUGADOR' ? [...this.availability] : [],
      businessName: this.role === 'ADMIN_CANCHA' ? value.businessName.trim() : null,
      businessPhone: this.role === 'ADMIN_CANCHA' ? value.businessPhone : null,
      operationDistrict: this.role === 'ADMIN_CANCHA' ? value.operationDistrict : null,
      termsAccepted: value.termsAccepted,
      privacyAccepted: value.privacyAccepted,
      marketingConsent: value.marketingConsent
    };
  }
}
