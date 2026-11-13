import {
  ChangeDetectionStrategy,
  Component,
  computed,
  DestroyRef,
  ElementRef,
  inject,
  input,
  OnInit,
  signal,
  viewChild
} from '@angular/core';
import { takeUntilDestroyed, toSignal } from '@angular/core/rxjs-interop';
import { HttpErrorResponse } from '@angular/common/http';
import { filter } from 'rxjs';
import {
  CHAT_MESSAGE_MAX_LENGTH,
  CHAT_PAGE_SIZE,
  ChatMessage,
  MatchChatService,
  mergeChatMessages
} from '@app/matches/match-chat.service';
import { MatchNotificationService } from '@app/notifications/match-notification.service';

/** Distancia al fondo (px) bajo la cual seguimos al último mensaje automáticamente. */
const STICK_TO_BOTTOM_THRESHOLD = 80;

@Component({
  selector: 'app-match-chat',
  templateUrl: './match-chat.component.html',
  styleUrl: './match-chat.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class MatchChatComponent implements OnInit {
  readonly matchId = input.required<number>();
  readonly currentUserId = input<number | null>(null);
  /** Falso cuando el partido está cancelado: el historial se puede leer pero no escribir. */
  readonly canPost = input(true);

  private readonly chatService = inject(MatchChatService);
  private readonly matchNotificationService = inject(MatchNotificationService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly scroller = viewChild<ElementRef<HTMLElement>>('scroller');

  readonly maxLength = CHAT_MESSAGE_MAX_LENGTH;
  readonly messages = signal<ChatMessage[]>([]);
  readonly draft = signal('');
  readonly loading = signal(true);
  readonly loadingOlder = signal(false);
  readonly hasOlder = signal(false);
  readonly sending = signal(false);
  readonly error = signal('');
  readonly connected = toSignal(this.chatService.connected$, { initialValue: false });
  readonly canSend = computed(() => {
    const length = this.draft().trim().length;
    return this.canPost() && !this.sending() && length > 0 && length <= this.maxLength;
  });

  ngOnInit(): void {
    const matchId = this.matchId();
    this.chatService.liveMessages(matchId)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(message => this.append([message]));
    // Tras una reconexión recargamos lo último para recuperar mensajes enviados durante el corte.
    this.chatService.connected$
      .pipe(filter(Boolean), takeUntilDestroyed(this.destroyRef))
      .subscribe(() => this.loadLatest());
    this.loadLatest();
  }

  isOwn(message: ChatMessage): boolean {
    return message.authorId === this.currentUserId();
  }

  onDraftInput(event: Event): void {
    this.draft.set((event.target as HTMLTextAreaElement).value);
  }

  onKeydown(event: KeyboardEvent): void {
    // Enter envía; Shift+Enter hace salto de línea. Se respeta la composición de acentos (IME).
    if (event.key === 'Enter' && !event.shiftKey && !event.isComposing) {
      event.preventDefault();
      this.send();
    }
  }

  send(): void {
    if (!this.canSend()) return;
    this.sending.set(true);
    this.error.set('');
    this.chatService.send(this.matchId(), this.draft().trim()).subscribe({
      next: message => {
        this.sending.set(false);
        this.draft.set('');
        this.append([message], true);
      },
      error: (err: HttpErrorResponse) => {
        this.sending.set(false);
        this.error.set(this.sendErrorMessage(err));
      }
    });
  }

  loadOlder(): void {
    const oldest = this.messages()[0];
    if (!oldest || this.loadingOlder()) return;
    this.loadingOlder.set(true);
    const element = this.scroller()?.nativeElement;
    const previousHeight = element?.scrollHeight ?? 0;
    this.chatService.history(this.matchId(), oldest.id).subscribe({
      next: older => {
        this.loadingOlder.set(false);
        this.hasOlder.set(older.length === CHAT_PAGE_SIZE);
        this.messages.update(current => mergeChatMessages(current, older));
        // Mantiene a la vista el mensaje que el usuario estaba leyendo.
        setTimeout(() => {
          if (element) element.scrollTop += element.scrollHeight - previousHeight;
        });
      },
      error: () => {
        this.loadingOlder.set(false);
        this.error.set('No se pudieron cargar los mensajes anteriores.');
      }
    });
  }

  formatTime(sentAt: string): string {
    return new Date(sentAt).toLocaleString('es-PE', {
      day: '2-digit',
      month: 'short',
      hour: '2-digit',
      minute: '2-digit'
    });
  }

  private loadLatest(): void {
    this.chatService.history(this.matchId()).subscribe({
      next: latest => {
        const isFirstLoad = this.loading();
        this.loading.set(false);
        if (isFirstLoad) this.hasOlder.set(latest.length === CHAT_PAGE_SIZE);
        this.append(latest, isFirstLoad);
        // El usuario ya está viendo estos mensajes: su aviso agrupado deja de estar pendiente.
        this.matchNotificationService.markChatAsRead(this.matchId());
      },
      error: () => {
        this.loading.set(false);
        this.error.set('No se pudo cargar el chat. Recarga la página para intentarlo de nuevo.');
      }
    });
  }

  private append(incoming: ChatMessage[], forceScroll = false): void {
    const stickToBottom = forceScroll || this.isNearBottom();
    this.messages.update(current => mergeChatMessages(current, incoming));
    if (stickToBottom) setTimeout(() => this.scrollToBottom());
  }

  private isNearBottom(): boolean {
    const element = this.scroller()?.nativeElement;
    if (!element) return true;
    return element.scrollHeight - element.scrollTop - element.clientHeight < STICK_TO_BOTTOM_THRESHOLD;
  }

  private scrollToBottom(): void {
    const element = this.scroller()?.nativeElement;
    if (element) element.scrollTop = element.scrollHeight;
  }

  private sendErrorMessage(err: HttpErrorResponse): string {
    if (err.status === 403) return 'Ya no participas en este partido.';
    if (err.status === 409) return 'El partido fue cancelado: el chat quedó en modo lectura.';
    if (err.status === 400) return 'El mensaje no es válido.';
    return 'No se pudo enviar el mensaje. Inténtalo nuevamente.';
  }
}
