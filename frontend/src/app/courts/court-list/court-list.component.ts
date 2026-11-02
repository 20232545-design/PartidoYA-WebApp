import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { CourtResponse, CourtService } from '@app/courts/court.service';

@Component({
  selector: 'app-court-list',
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './court-list.component.html',
  styleUrl: './court-list.component.scss'
})
export class CourtListComponent implements OnInit {
  courts: CourtResponse[] = [];
  sportFilter = '';
  districtFilter = '';
  loading = true;
  errorMessage = '';

  readonly sports = ['Fútbol', 'Básquet', 'Voleibol', 'Tenis'];

  constructor(private courtService: CourtService) {}

  ngOnInit(): void {
    this.loadCourts();
  }

  loadCourts(): void {
    this.loading = true;
    this.errorMessage = '';
    this.courtService.searchCourts(this.sportFilter || undefined, this.districtFilter || undefined).subscribe({
      next: (courts) => {
        this.courts = courts;
        this.loading = false;
      },
      error: (err) => {
        this.errorMessage = err.error?.message || 'No se pudieron cargar las canchas.';
        this.loading = false;
      }
    });
  }

  clearFilters(): void {
    this.sportFilter = '';
    this.districtFilter = '';
    this.loadCourts();
  }

  trackCourt(_: number, court: CourtResponse): number {
    return court.id;
  }
}
