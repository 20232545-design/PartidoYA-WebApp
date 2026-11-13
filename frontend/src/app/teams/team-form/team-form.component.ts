import { CommonModule } from '@angular/common';
import { Component, ElementRef, OnDestroy, OnInit, ViewChild, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { Subscription, firstValueFrom } from 'rxjs';
import { teamErrorMessage } from '../team-error';
import { TeamService } from '../team.service';

@Component({
  selector: 'app-team-form',
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  templateUrl: './team-form.component.html',
  styleUrl: '../teams.scss'
})
export class TeamFormComponent implements OnInit, OnDestroy {
  private readonly fb = inject(FormBuilder);
  private readonly service = inject(TeamService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);

  id: number | null = null;
  loading = false;
  error = '';
  message = '';
  logo: File | null = null;
  logoPending = false;
  logoError = '';
  formError = '';
  previewUrl: string | null = null;
  existingLogoUrl: string | null = null;
  private ownedPreviewUrl: string | null = null;
  private formChanges?: Subscription;
  @ViewChild('logoInput') private logoInput?: ElementRef<HTMLInputElement>;
  readonly form = this.fb.nonNullable.group({
    name: ['', [Validators.required, Validators.minLength(3), Validators.maxLength(80)]],
    sport: ['', Validators.required],
    description: ['', [Validators.required, Validators.minLength(10), Validators.maxLength(500)]],
    district: [''],
    competitiveLevel: ['', Validators.required],
    openToRequests: [true]
  });

  ngOnInit(): void {
    this.formChanges = this.form.valueChanges.subscribe(() => {
      if (this.form.valid && !this.logoError) this.formError = '';
    });
    const rawId = this.route.snapshot.paramMap.get('id');
    if (!rawId) return;
    this.id = Number(rawId);
    this.service.get(this.id).subscribe({
      next: team => {
        this.form.patchValue({
          name: team.name,
          sport: team.sport,
          description: team.description,
          district: team.district || '',
          competitiveLevel: team.competitiveLevel,
          openToRequests: team.openToRequests
        });
        this.existingLogoUrl = team.logoUrl;
      },
      error: (error: unknown) => this.error = teamErrorMessage(error, 'No se pudo cargar el equipo.')
    });
  }

  ngOnDestroy(): void {
    this.formChanges?.unsubscribe();
    this.revokePreview();
  }

  selectLogo(event: Event): void {
    const file = (event.target as HTMLInputElement).files?.[0] || null;
    this.logoError = '';
    if (this.form.valid) this.formError = '';
    if (!file) {
      if (!this.logoPending) this.removeSelectedLogo();
      return;
    }
    if (!['image/jpeg', 'image/png', 'image/webp'].includes(file.type)) {
      this.logo = null;
      this.revokePreview();
      this.logoError = 'Selecciona un archivo JPG, PNG o WebP.';
      return;
    }
    if (file.size > 5_000_000) {
      this.logo = null;
      this.revokePreview();
      this.logoError = 'La imagen supera el tamaño permitido de 5 MB.';
      return;
    }
    this.revokePreview();
    this.logo = file;
    this.ownedPreviewUrl = URL.createObjectURL(file);
    this.previewUrl = this.ownedPreviewUrl;
  }

  removeSelectedLogo(): void {
    this.logo = null;
    this.logoError = '';
    this.revokePreview();
    if (this.logoInput) this.logoInput.nativeElement.value = '';
  }

  async save(): Promise<void> {
    if (this.loading) return;
    if (this.logoPending) {
      await this.retryLogo();
      return;
    }
    this.form.markAllAsTouched();
    this.formError = '';
    if (this.form.invalid || this.logoError) {
      this.formError = 'Completa los campos obligatorios antes de continuar.';
      setTimeout(() => this.focusFirstInvalid());
      return;
    }
    this.loading = true;
    this.error = '';
    this.message = '';
    const creating = this.id === null;
    try {
      const body = { ...this.form.getRawValue(), district: this.form.value.district?.trim() || null };
      const team = creating
        ? await firstValueFrom(this.service.create(body))
        : await firstValueFrom(this.service.update(this.id!, body));
      this.id = team.id;
      if (this.logo) {
        try {
          await firstValueFrom(this.service.uploadLogo(team.id, this.logo));
          this.removeSelectedLogo();
        } catch (error: unknown) {
          this.logoPending = true;
          this.message = creating
            ? 'El equipo se creó correctamente. Solo falta guardar el escudo.'
            : 'Los datos del equipo se guardaron. Solo falta actualizar el escudo.';
          this.error = teamErrorMessage(error, 'No se pudo guardar el escudo. Puedes reintentarlo sin crear otro equipo.');
          return;
        }
      }
      await this.router.navigate(['/equipos', team.id]);
    } catch (error: unknown) {
      this.error = teamErrorMessage(error, 'No se pudo guardar el equipo.');
    } finally {
      this.loading = false;
    }
  }

  async retryLogo(): Promise<void> {
    if (this.loading || !this.id) return;
    if (!this.logo) {
      this.error = 'Selecciona el escudo que deseas guardar.';
      return;
    }
    this.loading = true;
    this.error = '';
    try {
      await firstValueFrom(this.service.uploadLogo(this.id, this.logo));
      this.removeSelectedLogo();
      this.logoPending = false;
      await this.router.navigate(['/equipos', this.id]);
    } catch (error: unknown) {
      this.error = teamErrorMessage(error, 'No se pudo guardar el escudo. Puedes volver a intentarlo.');
    } finally {
      this.loading = false;
    }
  }

  async continueWithoutLogo(): Promise<void> {
    if (this.id) await this.router.navigate(['/equipos', this.id]);
  }

  fieldError(field: 'name' | 'sport' | 'description' | 'competitiveLevel'): string {
    const control = this.form.controls[field];
    if (!control.touched || !control.errors) return '';
    if (field === 'name') {
      if (control.errors['required']) return 'Ingresa el nombre del equipo.';
      if (control.errors['maxlength']) return 'El nombre del equipo supera los 80 caracteres.';
      return 'El nombre debe tener al menos 3 caracteres.';
    }
    if (field === 'sport') return 'Selecciona un deporte.';
    if (field === 'competitiveLevel') return 'Selecciona el nivel competitivo.';
    if (control.errors['required']) return 'Ingresa una descripción del equipo.';
    if (control.errors['maxlength']) return 'La descripción supera los 500 caracteres.';
    return 'La descripción debe tener al menos 10 caracteres.';
  }

  private focusFirstInvalid(): void {
    const form = document.querySelector<HTMLFormElement>('.team-form');
    form?.querySelector<HTMLElement>('[aria-invalid="true"]')?.focus();
  }

  private revokePreview(): void {
    if (this.ownedPreviewUrl) URL.revokeObjectURL(this.ownedPreviewUrl);
    this.ownedPreviewUrl = null;
    this.previewUrl = null;
  }
}
