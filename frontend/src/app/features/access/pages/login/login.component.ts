import { Component, ChangeDetectorRef } from '@angular/core';
import { Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { AuthService } from '@core/auth/auth.service';

@Component({
  selector: 'app-login',
  imports: [FormsModule],
  templateUrl: './login.component.html',
  styleUrl: './login.component.scss'
})
export class LoginComponent {
  email = '';
  password = '';
  errorMessage = '';

  showRegister = false;
  fullName = '';
  registerEmail = '';
  registerPassword = '';
  role = 'JUGADOR';
  registerErrorMessage = '';
  registerSuccessMessage = '';

  constructor(
    private authService: AuthService,
    private router: Router,
    private cdr: ChangeDetectorRef
  ) {
    this.showRegister = this.router.url.includes('register');
  }

  showRegisterForm(): void {
    this.showRegister = true;
    this.registerErrorMessage = '';
    this.registerSuccessMessage = '';
  }

  showLoginForm(): void {
    this.showRegister = false;
    this.errorMessage = '';
  }

  onSubmit(event: Event): void {
    event.preventDefault();
    this.errorMessage = '';
    this.authService.login(this.email, this.password).subscribe({
      next: () => {
        this.router.navigate(['/home']);
      },
      error: (err) => {
        this.errorMessage = err.error?.message || 'Credenciales incorrectas.';
        this.cdr.detectChanges();
      }
    });
  }

  onRegisterSubmit(event: Event): void {
    event.preventDefault();
    this.registerErrorMessage = '';
    this.registerSuccessMessage = '';
    this.authService.register(this.fullName, this.registerEmail, this.registerPassword, this.role).subscribe({
      next: () => {
        this.registerSuccessMessage = 'Cuenta creada correctamente. Ahora inicia sesión.';
        this.email = this.registerEmail;
        this.password = '';
        this.showRegister = false;
        this.cdr.detectChanges();
      },
      error: (err) => {
        this.registerErrorMessage = err.error?.message || 'Error al registrar. Intenta de nuevo.';
        this.cdr.detectChanges();
      }
    });
  }
}
