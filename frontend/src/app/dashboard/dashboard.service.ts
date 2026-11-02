import { Injectable } from '@angular/core';
import { map, Observable } from 'rxjs';
import { MatchResponse, MatchService } from '@app/matches/match.service';
import { GeolocationService } from '@shared/services/geolocation.service';

const NEARBY_RADIUS_KM = 50;

export interface DashboardOverview {
  allMatches: MatchResponse[];
  upcomingMatches: MatchResponse[];
  almostFullMatches: MatchResponse[];
  almostFullCount: number;
}

@Injectable({ providedIn: 'root' })
export class DashboardService {
  constructor(
    private matchService: MatchService,
    private geolocation: GeolocationService
  ) {}

  loadOverview(): Observable<DashboardOverview> {
    return this.matchService.getOpenMatches().pipe(
      map(matches => {
        const sorted = this.sortByDate(matches);
        const almostFull = matches.filter(match => match.availableSlots > 0 && match.availableSlots <= 3);
        return {
          allMatches: matches,
          upcomingMatches: sorted.slice(0, 4),
          almostFullMatches: almostFull.slice(0, 4),
          almostFullCount: almostFull.length
        };
      })
    );
  }

  loadJoinedMatches(userId: number): Observable<MatchResponse[]> {
    return this.matchService.getMatchesByParticipant(userId).pipe(
      map(matches => this.sortByDate(matches).slice(0, 6))
    );
  }

  async loadNearbyMatches(matches: MatchResponse[]): Promise<MatchResponse[]> {
    const position = await this.geolocation.getPosition();
    const { latitude, longitude } = position.coords;
    return matches
      .filter(match => match.latitude !== null && match.longitude !== null)
      .filter(match => this.geolocation.distanceKm(
        latitude,
        longitude,
        match.latitude!,
        match.longitude!
      ) <= NEARBY_RADIUS_KM)
      .slice(0, 6);
  }

  searchByAddress(matches: MatchResponse[], query: string): MatchResponse[] {
    const normalizedQuery = query.trim().toLowerCase();
    if (!normalizedQuery) return [];
    return matches
      .filter(match => match.address.toLowerCase().includes(normalizedQuery))
      .slice(0, 6);
  }

  joinMatch(matchId: number, userId: number): Observable<MatchResponse> {
    return this.matchService.joinMatch(matchId, userId);
  }

  private sortByDate(matches: MatchResponse[]): MatchResponse[] {
    return [...matches].sort((first, second) =>
      new Date(first.matchDate).getTime() - new Date(second.matchDate).getTime()
    );
  }
}
