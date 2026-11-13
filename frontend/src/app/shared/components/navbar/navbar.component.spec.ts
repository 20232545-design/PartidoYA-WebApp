import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { AuthService } from '@app/auth/auth.service';
import { NotificationService } from '@app/notifications/notification.service';
import { BehaviorSubject } from 'rxjs';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { NavbarComponent } from './navbar.component';

describe('NavbarComponent team navigation', () => {
  let fixture: ComponentFixture<NavbarComponent>;
  let player = false;
  let loggedIn = true;

  beforeEach(() => {
    player = false;
    loggedIn = true;
    TestBed.configureTestingModule({
      imports: [NavbarComponent],
      providers: [
        provideRouter([]),
        {
          provide: AuthService,
          useValue: {
            isLoggedIn: () => loggedIn,
            isPlayerPlan: () => player,
            isCourtPlan: () => loggedIn && !player,
            isCourtBusiness: () => false,
            isPlatformAdmin: () => false,
            getUserId: () => loggedIn ? 1 : null,
            getUserName: () => 'Nombre de usuario muy largo para el header',
            logout: vi.fn()
          }
        },
        {
          provide: NotificationService,
          useValue: { pendingCount$: new BehaviorSubject(0), refresh: vi.fn() }
        }
      ]
    });
    fixture = TestBed.createComponent(NavbarComponent);
  });

  it('shows Equipos to an authenticated player in the responsive navigation', () => {
    player = true;
    fixture.detectChanges();
    const links = fixture.nativeElement.querySelector('.navbar-links') as HTMLElement;
    expect(links.textContent).toContain('Equipos');
    expect(links.querySelector('a[href="/equipos"]')).not.toBeNull();
  });

  it('keeps court administrator navigation without adding Equipos', () => {
    fixture.detectChanges();
    const text = (fixture.nativeElement as HTMLElement).textContent || '';
    expect(text).toContain('Administrar canchas');
    expect(text).not.toContain('Equipos');
  });

  it('does not expose Equipos to unauthenticated users', () => {
    loggedIn = false;
    fixture.detectChanges();
    expect((fixture.nativeElement as HTMLElement).textContent).not.toContain('Equipos');
  });
});
