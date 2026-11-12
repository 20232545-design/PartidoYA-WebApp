import { Injectable } from '@angular/core';
import { BehaviorSubject, catchError, combineLatest, forkJoin, map, of, switchMap } from 'rxjs';
import { AuthService } from '@app/auth/auth.service';
import { MatchResponse, MatchService, PlayerJoinRequestResponse } from '@app/matches/match.service';
import { UserService } from '@app/profiles/user.service';
import { PaymentNotification } from './payment-notification.model';
import { JoinRequestService } from '@app/matches/join-request.service';
import { TeamService } from '@app/teams/team.service';

export type { PaymentNotification } from './payment-notification.model';

@Injectable({ providedIn: 'root' })
export class NotificationService {
  private readonly notificationsSubject = new BehaviorSubject<PaymentNotification[]>([]);
  readonly notifications$ = this.notificationsSubject.asObservable();
  private readonly teamPendingSubject = new BehaviorSubject<number>(0);
  readonly pendingCount$ = combineLatest([this.notifications$, this.teamPendingSubject]).pipe(
    map(([notifications, teamPending]) => notifications.filter(notification => this.isPending(notification)).length + teamPending)
  );

  constructor(
    private authService: AuthService,
    private matchService: MatchService,
    private joinRequestService: JoinRequestService,
    private userService: UserService,
    private teamService: TeamService
  ) {}

  refresh(): void {
    const userId = this.authService.getUserId();
    if (!userId) {
      this.notificationsSubject.next([]);
      this.teamPendingSubject.next(0);
      return;
    }

    this.matchService.getMatchesByOrganizer(userId).pipe(
      switchMap(matches => this.loadRequestsForMatches(matches, userId)),
      switchMap(notifications => this.attachPlayerNames(notifications)),
      catchError(() => of([]))
    ).subscribe(notifications => {
      this.notificationsSubject.next(notifications);
    });
    if (this.authService.isPlayerPlan()) {
      this.teamService.mine().pipe(catchError(() => of(null))).subscribe(result =>
        this.teamPendingSubject.next(result?.pendingCount || 0));
    } else {
      this.teamPendingSubject.next(0);
    }
  }

  isPending(notification: PaymentNotification): boolean {
    return notification.status === 'PENDING_PAYMENT_VERIFICATION';
  }

  private loadRequestsForMatches(matches: MatchResponse[], userId: number) {
    const paidMatches = matches.filter(match => match.requiresPlayerPayment);
    if (paidMatches.length === 0) return of([]);

    return forkJoin(paidMatches.map(match =>
      this.joinRequestService.getRequests(match.id, userId).pipe(
        map(requests => requests.map(request => this.toNotification(match, request))),
        catchError(() => of([]))
      )
    )).pipe(
      map(groups => groups.flat().sort((a, b) => b.submittedAt.localeCompare(a.submittedAt)))
    );
  }

  private attachPlayerNames(notifications: PaymentNotification[]) {
    const uniquePlayerIds = [...new Set(notifications.map(notification => notification.playerId))];
    if (uniquePlayerIds.length === 0) return of(notifications);

    return forkJoin(uniquePlayerIds.map(id =>
      this.userService.getUserById(id).pipe(catchError(() => of(null)))
    )).pipe(
      map(users => {
        const names = new Map<number, string>();
        users.forEach(user => {
          if (user) names.set(user.id, user.fullName);
        });
        return notifications.map(notification => ({
          ...notification,
          playerName: names.get(notification.playerId) || notification.playerName
        }));
      })
    );
  }

  private toNotification(match: MatchResponse, request: PlayerJoinRequestResponse): PaymentNotification {
    return {
      id: `${match.id}-${request.id}`,
      matchId: match.id,
      requestId: request.id,
      playerId: request.playerId,
      playerName: `Jugador #${request.playerId}`,
      matchTitle: match.title,
      submittedAt: request.submittedAt,
      status: request.status,
      hasProof: !!request.proofUrl,
      proofUrl: request.proofUrl
    };
  }
}
