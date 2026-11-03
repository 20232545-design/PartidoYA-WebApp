import { Component } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from '@app/auth/auth.service';
import { PlayerProfileComponent } from '../player-profile/player-profile.component';
import { CourtAdminProfileComponent } from '../court-admin-profile/court-admin-profile.component';

@Component({
  selector: 'app-profile-page',
  imports: [PlayerProfileComponent, CourtAdminProfileComponent],
  template: `
    @if (role === 'JUGADOR') {
      <app-player-profile />
    } @else if (role === 'ADMIN_CANCHA') {
      <app-court-admin-profile />
    }
  `
})
export class ProfilePageComponent {
  readonly role: string;

  constructor(authService: AuthService, router: Router) {
    this.role = authService.getRole() || '';
    if (this.role !== 'JUGADOR' && this.role !== 'ADMIN_CANCHA') {
      void router.navigate(['/home']);
    }
  }
}
