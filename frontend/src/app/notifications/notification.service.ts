import { inject, Injectable } from '@angular/core';
import { BehaviorSubject, catchError, combineLatest, forkJoin, map, of, switchMap } from 'rxjs';
import { AuthService } from '@app/auth/auth.service';
import { MatchResponse, MatchService, PlayerJoinRequestResponse } from '@app/matches/match.service';
import { PaymentNotification } from './payment-notification.model';
import { JoinRequestService } from '@app/matches/join-request.service';
import { TeamService } from '@app/teams/team.service';
import { MatchNotificationService } from './match-notification.service';

export type { PaymentNotification } from './payment-notification.model';

@Injectable({ providedIn: 'root' })
export class NotificationService {
  private readonly matchNotificationService = inject(MatchNotificationService);
  private readonly notificationsSubject = new BehaviorSubject<PaymentNotification[]>([]);
  readonly notifications$ = this.notificationsSubject.asObservable();
  private readonly teamPendingSubject = new BehaviorSubject<number>(0);
  readonly pendingCount$ = combineLatest([
    this.notifications$,
    this.teamPendingSubject,
    this.matchNotificationService.unreadCount$
  ]).pipe(
    map(([notifications, teamPending, unreadMatchNotifications]) =>
      notifications.filter(notification => this.isPending(notification)).length + teamPending + unreadMatchNotifications)
  );

  constructor(
    private authService: AuthService,
    private matchService: MatchService,
    private joinRequestService: JoinRequestService,
    private teamService: TeamService
  ) {}

  refresh(): void {
    const userId = this.authService.getUserId();
    if (!userId) {
      this.notificationsSubject.next([]);
      this.teamPendingSubject.next(0);
      this.matchNotificationService.stop();
      return;
    }

    // Solo los jugadores organizan o se inscriben en partidos, que es lo que generan estas notificaciones.
    if (this.authService.isPlayerPlan()) {
      this.matchNotificationService.start(userId);
    } else {
      this.matchNotificationService.stop();
    }

    this.matchService.getMatchesByOrganizer(userId).pipe(
      switchMap(matches => this.loadRequestsForMatches(matches, userId)),
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

  private toNotification(match: MatchResponse, request: PlayerJoinRequestResponse): PaymentNotification {
    return {
      id: `${match.id}-${request.id}`,
      matchId: match.id,
      requestId: request.id,
      playerId: request.playerId,
      playerName: request.playerName,
      matchTitle: match.title,
      submittedAt: request.submittedAt,
      status: request.status,
      hasProof: !!request.proofUrl,
      proofUrl: request.proofUrl
    };
  }
}
