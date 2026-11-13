import { TestBed } from '@angular/core/testing';
import { ActivatedRoute, Router } from '@angular/router';
import { of, throwError } from 'rxjs';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { Team } from '../team.models';
import { TeamService } from '../team.service';
import { TeamFormComponent } from './team-form.component';

describe('TeamFormComponent', () => {
  const team: Team = {
    id: 1,
    name: 'Leones del Sur',
    sport: 'Fútbol',
    description: 'Equipo para competir cada semana',
    logoUrl: null,
    district: 'Surco',
    competitiveLevel: 'INTERMEDIATE',
    openToRequests: true,
    status: 'ACTIVE',
    captainId: 7,
    captainName: 'Ana',
    memberCount: 1,
    maxMembers: 30,
    createdAt: '2026-11-12T10:00:00Z',
    member: true,
    captain: true,
    pendingRequest: false,
    viewerRelationship: 'CAPTAIN',
    statistics: { available: false, played: 0, wins: 0, draws: 0, losses: 0, winPercentage: 0 }
  };
  let service: {
    create: ReturnType<typeof vi.fn>;
    update: ReturnType<typeof vi.fn>;
    uploadLogo: ReturnType<typeof vi.fn>;
    get: ReturnType<typeof vi.fn>;
  };
  let router: { navigate: ReturnType<typeof vi.fn> };
  let component: TeamFormComponent;

  beforeEach(() => {
    Object.defineProperty(URL, 'createObjectURL', { value: vi.fn(() => 'blob:team-logo'), configurable: true });
    Object.defineProperty(URL, 'revokeObjectURL', { value: vi.fn(), configurable: true });
    service = {
      create: vi.fn(() => of(team)),
      update: vi.fn(() => of(team)),
      uploadLogo: vi.fn(() => throwError(() => new Error('Proveedor de imágenes no disponible'))),
      get: vi.fn(() => of(team))
    };
    router = { navigate: vi.fn(() => Promise.resolve(true)) };
    TestBed.configureTestingModule({
      imports: [TeamFormComponent],
      providers: [
        { provide: TeamService, useValue: service },
        { provide: Router, useValue: router },
        { provide: ActivatedRoute, useValue: { snapshot: { paramMap: { get: () => null } } } }
      ]
    });
    component = TestBed.createComponent(TeamFormComponent).componentInstance;
    component.form.setValue({
      name: team.name,
      sport: team.sport,
      description: team.description,
      district: team.district || '',
      competitiveLevel: team.competitiveLevel,
      openToRequests: true
    });
    component.logo = new File([new Uint8Array([0xff, 0xd8, 0xff])], 'shield.jpg', { type: 'image/jpeg' });
  });

  it('keeps the created team and restores loading when logo upload fails', async () => {
    await component.save();

    expect(service.create).toHaveBeenCalledTimes(1);
    expect(service.uploadLogo).toHaveBeenCalledTimes(1);
    expect(component.id).toBe(1);
    expect(component.logoPending).toBe(true);
    expect(component.loading).toBe(false);
    expect(component.message).toContain('El equipo se creó correctamente');
    expect(router.navigate).not.toHaveBeenCalled();
  });

  it('retries only the pending logo without creating a duplicate team', async () => {
    await component.save();
    service.uploadLogo.mockReturnValue(of({ logoUrl: 'https://cdn.test/shield.jpg' }));

    await component.retryLogo();

    expect(service.create).toHaveBeenCalledTimes(1);
    expect(service.update).not.toHaveBeenCalled();
    expect(service.uploadLogo).toHaveBeenCalledTimes(2);
    expect(component.logoPending).toBe(false);
    expect(component.loading).toBe(false);
    expect(router.navigate).toHaveBeenCalledWith(['/equipos', 1]);
  });

  it('blocks saving and identifies each required field', async () => {
    component.logo = null;
    component.form.patchValue({ name: '', sport: '', description: '', competitiveLevel: '' });

    await component.save();

    expect(service.create).not.toHaveBeenCalled();
    expect(component.formError).toContain('campos obligatorios');
    expect(component.fieldError('name')).toBe('Ingresa el nombre del equipo.');
    expect(component.fieldError('sport')).toBe('Selecciona un deporte.');
    expect(component.fieldError('competitiveLevel')).toBe('Selecciona el nivel competitivo.');
  });

  it('shows a local preview and allows removing the selected image', () => {
    const file = new File([new Uint8Array([0x89, 0x50, 0x4e, 0x47])], 'shield.png', { type: 'image/png' });

    component.selectLogo({ target: { files: [file] } } as unknown as Event);
    expect(component.previewUrl).toBe('blob:team-logo');
    expect(component.logo).toBe(file);

    component.removeSelectedLogo();
    expect(component.logo).toBeNull();
    expect(component.previewUrl).toBeNull();
  });

  it('rejects an invalid image before sending the form', async () => {
    const file = new File(['not an image'], 'shield.txt', { type: 'text/plain' });
    component.selectLogo({ target: { files: [file] } } as unknown as Event);

    await component.save();

    expect(component.logoError).toContain('JPG, PNG o WebP');
    expect(service.create).not.toHaveBeenCalled();
  });

  it('rejects an image larger than five megabytes', () => {
    const file = new File([new Uint8Array(5_000_001)], 'large.jpg', { type: 'image/jpeg' });

    component.selectLogo({ target: { files: [file] } } as unknown as Event);

    expect(component.logo).toBeNull();
    expect(component.logoError).toContain('5 MB');
  });
});
