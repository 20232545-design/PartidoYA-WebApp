import { inject, Injectable } from '@angular/core';
import { Client, IMessage, StompSubscription } from '@stomp/stompjs';
import { BehaviorSubject, distinctUntilChanged, Observable } from 'rxjs';
import { BrowserSessionStore } from '@app/auth/browser-session.store';
import { API_BASE_URL } from '@shared/services/api-base-url.token';

const RECONNECT_DELAY_MS = 5_000;
const HEARTBEAT_MS = 10_000;

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
  private client: Client | null = null;

  /** Emite `true` en cada conexión (incluidas las reconexiones) y `false` al perderla. */
  readonly connected$ = this.connectedSubject.pipe(distinctUntilChanged());

  /** Idempotente: si ya hay una conexión activa o en curso no hace nada. */
  connect(): void {
    if (this.client?.active) return;
    const client = new Client({
      brokerURL: this.brokerUrl,
      reconnectDelay: RECONNECT_DELAY_MS,
      heartbeatIncoming: HEARTBEAT_MS,
      heartbeatOutgoing: HEARTBEAT_MS,
      // Se evalúa en cada intento, así una reconexión usa siempre el token vigente.
      beforeConnect: async stompClient => {
        const token = this.sessionStore.token();
        if (!token) {
          await stompClient.deactivate();
          return;
        }
        stompClient.connectHeaders = { Authorization: `Bearer ${token}` };
      },
      onConnect: () => this.connectedSubject.next(true),
      onWebSocketClose: () => this.connectedSubject.next(false),
      onStompError: () => {
        // Rechazo en el CONNECT (token vencido o inválido): reintentar no lo arreglaría.
        if (!this.connectedSubject.value) void this.disconnect();
      }
    });
    this.client = client;
    client.activate();
  }

  async disconnect(): Promise<void> {
    const client = this.client;
    this.client = null;
    this.connectedSubject.next(false);
    await client?.deactivate();
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
}
