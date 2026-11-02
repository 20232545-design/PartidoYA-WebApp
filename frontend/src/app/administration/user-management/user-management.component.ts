import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { AuthService } from '@app/auth/auth.service';
import {
  AdminUserDetailResponse,
  AdminUserResponse,
  UserAdminService
} from '@app/administration/user-admin.service';

@Component({
  selector: 'app-user-management',
  imports: [CommonModule, FormsModule],
  templateUrl: './user-management.component.html',
  styleUrl: './user-management.component.scss'
})
export class UserManagementComponent implements OnInit {
  users: AdminUserResponse[] = [];
  detail: AdminUserDetailResponse | null = null;
  selectedUser: AdminUserResponse | null = null;
  loading = true;
  detailLoading = false;
  actionLoading = false;
  errorMessage = '';
  successMessage = '';

  searchTerm = '';
  roleFilter = 'ALL';
  page = 0;
  size = 10;
  totalElements = 0;
  actionReason = '';
  suspendedUntil = '';

  constructor(
    private authService: AuthService,
    private userAdminService: UserAdminService
  ) {}

  ngOnInit(): void {
    this.loadUsers();
  }

  get totalPages(): number {
    return Math.max(1, Math.ceil(this.totalElements / this.size));
  }

  loadUsers(): void {
    const adminId = this.authService.getUserId();
    if (!adminId) return;
    this.loading = true;
    this.errorMessage = '';
    this.userAdminService.search(adminId, this.searchTerm, this.roleFilter, this.page, this.size).subscribe({
      next: page => {
        this.users = page.content;
        this.totalElements = page.totalElements;
        this.loading = false;
        if (this.selectedUser) {
          const refreshed = this.users.find(user => user.id === this.selectedUser?.id);
          if (refreshed) this.selectedUser = refreshed;
        }
      },
      error: err => {
        this.errorMessage = err.error?.message || 'No se pudieron cargar los usuarios.';
        this.loading = false;
      }
    });
  }

  selectUser(user: AdminUserResponse): void {
    const adminId = this.authService.getUserId();
    if (!adminId) return;
    this.selectedUser = user;
    this.detailLoading = true;
    this.actionReason = '';
    this.suspendedUntil = '';
    this.userAdminService.detail(adminId, user.id).subscribe({
      next: detail => {
        this.detail = detail;
        this.selectedUser = detail.user;
        this.detailLoading = false;
      },
      error: err => {
        this.errorMessage = err.error?.message || 'No se pudo cargar el detalle del usuario.';
        this.detailLoading = false;
      }
    });
  }

  applyAction(action: 'activate' | 'suspend' | 'reactivate'): void {
    const adminId = this.authService.getUserId();
    const userId = this.selectedUser?.id;
    if (!adminId || !userId) return;
    if (action === 'suspend' && !this.actionReason.trim()) {
      this.errorMessage = 'Indica el motivo de la suspension.';
      return;
    }
    this.actionLoading = true;
    this.errorMessage = '';
    this.successMessage = '';
    const request = action === 'activate'
      ? this.userAdminService.activate(adminId, userId, this.actionReason)
      : action === 'suspend'
        ? this.userAdminService.suspend(adminId, userId, this.actionReason, this.suspendedUntil || null)
        : this.userAdminService.reactivate(adminId, userId, this.actionReason);
    request.subscribe({
      next: () => {
        this.successMessage = 'Accion registrada correctamente.';
        this.actionLoading = false;
        this.loadUsers();
        if (this.selectedUser) this.selectUser(this.selectedUser);
      },
      error: err => {
        this.errorMessage = err.error?.message || 'No se pudo registrar la accion.';
        this.actionLoading = false;
      }
    });
  }

  changePage(step: number): void {
    const next = this.page + step;
    if (next < 0 || next >= this.totalPages) return;
    this.page = next;
    this.loadUsers();
  }

  search(): void {
    this.page = 0;
    this.loadUsers();
  }

  initials(name: string): string {
    return name.split(' ').filter(Boolean).slice(0, 2).map(part => part[0]).join('').toUpperCase();
  }

  roleLabel(role: string): string {
    const labels: Record<string, string> = {
      JUGADOR: 'Jugador',
      ADMIN_CANCHA: 'Admin cancha',
      ADMIN_GENERAL: 'Admin general'
    };
    return labels[role] || role;
  }

  statusLabel(status: string): string {
    return status === 'SUSPENDED' ? 'Suspendido' : 'Activo';
  }
}
