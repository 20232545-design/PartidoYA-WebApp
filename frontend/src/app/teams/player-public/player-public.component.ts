import { CommonModule } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { firstValueFrom } from 'rxjs';
import { PublicPlayer } from '../team.models';
import { TeamService } from '../team.service';

@Component({
  selector: 'app-player-public',
  imports: [CommonModule, RouterLink],
  templateUrl: './player-public.component.html',
  styleUrl: '../teams.scss'
})
export class PlayerPublicComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly teams = inject(TeamService);
  player: PublicPlayer | null = null;
  loading = true;
  error = '';

  async ngOnInit(): Promise<void> {
    try {
      this.player = await firstValueFrom(this.teams.publicPlayer(Number(this.route.snapshot.paramMap.get('userId'))));
    } catch {
      this.error = 'No se pudo cargar el perfil público del jugador.';
    } finally {
      this.loading = false;
    }
  }

  level(value: string | null): string {
    return ({ BEGINNER: 'Básico', INTERMEDIATE: 'Intermedio', ADVANCED: 'Avanzado' } as Record<string, string>)[value || ''] || 'Sin nivel';
  }
}
