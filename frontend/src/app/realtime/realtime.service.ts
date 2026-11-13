import { inject, Injectable } from '@angular/core';
import { Client, IMessage, ReconnectionTimeMode, StompSubscription } from '@stomp/stompjs';
import { BehaviorSubject, distinctUntilChanged, Observable } from 'rxjs';
import { BrowserSessionStore } from '@app/auth/browser-session.store';
import { API_BASE_URL } from '@shared/services/api-base-url.token';

const INITIAL_RECONNECT_DELAY_MS = 1_000;
const MAX_RECONNECT_DELAY_MS = 30_000;
const MAX_CONSECUTIVE_FAILURES = 5;
const HEARTBEAT_MS = 10_000;

/** Presupuesto reiniciable para que una caída prolongada no produzca reconexiones infinitas. */
export class ReconnectBudget {
  private failures = 0;

  constructor(private readonly maximum = MAX_CONSECUTIVE_FAILURES) {}

  reset(): void { this.failures = 0; }

  shouldRetryAfterFailure(): boolean {
    this.failures += 1;
    return this.failures < this.maximum;
  }
}

/** Convierte la URL del API (`https://host/api/v1`) en la del endpoint STOMP (`wss://host/ws`). */
export function websocketUrlFrom(apiUrl: string, origin = globalThis.location?.origin): string {
  const url = new URL(apiUrl, origin);
  url.protocol = url.protocol === 'https:' ? 'wss:' : 'ws:';
  url.pathname = '/ws';
  url.search = '';
  url.hash = '';
  return url.toString();
}

/**
 * Única conexión STOMP de la app. El servidor solo empuja datos: los envíos se hacen por REST.
 * Reconecta sola y vuelve a suscribir cada `watch` activo tras una reconexión.
 */
@Injectable({ providedIn: 'root' })
export class RealtimeService {
  private readonly sessionStore = inject(BrowserSessionStore);
  private readonly brokerUrl = websocketUrlFrom(inject(API_BASE_URL));
  private readonly connectedSubject = new BehaviorSubject<boolean>(false);
  private readonly reconnectBudget = new ReconnectBudget();
  private client: Client | null = null;

  /** Emite `true` en cada conexión (incluidas las reconexiones) y `false` al perderla. */
  readonly connected$ = this.connectedSubject.pipe(distinctUntilChanged());

  /** Idempotente: si ya hay una conexión activa o en curso no hace nada. */
  connect(): void {
    if (this.client?.active) return;
    this.reconnectBudget.reset();
    const client = new Client({
      brokerURL: this.brokerUrl,
      reconnectDelay: INITIAL_RECONNECT_DELAY_MS,
      reconnectTimeMode: ReconnectionTimeMode.EXPONENTIAL,
      maxReconnectDelay: MAX_RECONNECT_DELAY_MS,
      heartbeatIncoming: HEARTBEAT_MS,
      heartbeatOutgoing: HEARTBEAT_MS,
      discardWebsocketOnCommFailure: true,
      // Se evalúa en cada intento, así una reconexión usa siempre el token vigente.
      beforeConnect: async stompClient => {
        const token = this.sessionStore.token();
        if (!token) {
          await stompClient.deactivate();
          return;
        }
        stompClient.connectHeaders = { Authorization: `Bearer ${token}` };
      },
      onConnect: () => {
        this.reconnectBudget.reset();
        this.connectedSubject.next(true);
      },
      onWebSocketClose: () => {
        this.connectedSubject.next(false);
        if (this.client !== client || !client.active) return;
        if (!this.reconnectBudget.shouldRetryAfterFailure()) void this.disconnectClient(client);
      },
      onStompError: () => {
        // Rechazo en el CONNECT (token vencido o inválido): reintentar no lo arreglaría.
        if (this.client === client && !this.connectedSubject.value) void this.disconnectClient(client);
      }
    });
    this.client = client;
    client.activate();
  }

  async disconnect(): Promise<void> {
    const client = this.client;
    if (client) await this.disconnectClient(client);
    else this.connectedSubject.next(false);
  }

  /**
   * Observable de los mensajes JSON de un destino. Se suscribe al conectar, se re-suscribe
   * tras cada reconexión y cancela la suscripción STOMP al desuscribirse.
   */
  watch<T>(destination: string): Observable<T> {
    return new Observable<T>(subscriber => {
      let stompSubscription: StompSubscription | null = null;
      const connection = this.connected$.subscribe(connected => {
        // Al perder la conexión el servidor descarta las suscripciones; no hay que cancelarlas.
        stompSubscription = null;
        if (!connected || !this.client) return;
        stompSubscription = this.client.subscribe(destination, (message: IMessage) => {
          try {
            subscriber.next(JSON.parse(message.body) as T);
          } catch {
            console.warn(`[Realtime] Mensaje inválido en ${destination}`);
          }
        });
      });
      return () => {
        connection.unsubscribe();
        if (stompSubscription && this.client?.connected) stompSubscription.unsubscribe();
      };
    });
  }

  private async disconnectClient(client: Client): Promise<void> {
    if (this.client === client) this.client = null;
    this.connectedSubject.next(false);
    await client.deactivate();
  }
}
