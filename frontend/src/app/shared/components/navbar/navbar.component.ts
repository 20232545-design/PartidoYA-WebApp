import { CommonModule } from '@angular/common';
import { Component, DoCheck, ElementRef, HostListener, OnInit } from '@angular/core';
import { Router, RouterLink, RouterLinkActive } from '@angular/router';
import { AuthService } from '@app/auth/auth.service';
import { NotificationService } from '@app/notifications/notification.service';

@Component({
  selector: 'app-navbar',
  imports: [CommonModule, RouterLink, RouterLinkActive],
  templateUrl: './navbar.component.html',
  styleUrl: './navbar.component.scss'
})
export class NavbarComponent implements OnInit, DoCheck {
  userMenuOpen = false;
  private loadedForUserId: number | null = null;

  constructor(
    private authService: AuthService,
    public notificationService: NotificationService,
    private router: Router,
    private elementRef: ElementRef<HTMLElement>
  ) {}

  ngOnInit(): void {
    if (this.isLoggedIn) {
      this.notificationService.refresh();
      this.loadedForUserId = this.authService.getUserId();
    }
  }

  ngDoCheck(): void {
    const userId = this.authService.getUserId();
    if (userId !== this.loadedForUserId) {
      this.loadedForUserId = userId;
      this.notificationService.refresh();
    }
  }

  @HostListener('document:click', ['$event.target'])
  onDocumentClick(target: EventTarget | null): void {
    if (target instanceof Node && !this.elementRef.nativeElement.contains(target)) {
      this.userMenuOpen = false;
    }
  }

  get isLoggedIn(): boolean {
    return this.authService.isLoggedIn();
  }

  get userName(): string | null {
    return this.authService.getUserName();
  }

  get canManageCourts(): boolean {
    return this.authService.isCourtPlan();
  }

  get canUsePlayerFeatures(): boolean {
    return this.authService.isPlayerPlan();
  }

  get canViewCourtReports(): boolean {
    return this.authService.isCourtBusiness();
  }

  get canManagePlatform(): boolean {
    return this.authService.isPlatformAdmin();
  }

  toggleUserMenu(): void {
    this.userMenuOpen = !this.userMenuOpen;
  }

  closeUserMenu(): void {
    this.userMenuOpen = false;
  }

  logout(): void {
    this.closeUserMenu();
    this.authService.logout();
    this.notificationService.refresh();
    this.router.navigate(['/']);
  }
}
