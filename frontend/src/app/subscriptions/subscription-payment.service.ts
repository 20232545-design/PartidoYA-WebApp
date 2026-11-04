import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { API_BASE_URL } from '@shared/services/api-base-url.token';

declare global {
  interface Window {
    Culqi?: any;
    culqi?: () => void;
  }
}

interface PaymentConfigResponse {
  culqiPublicKey: string;
}

@Injectable({ providedIn: 'root' })
export class SubscriptionPaymentService {
  private readonly apiUrl = `${inject(API_BASE_URL)}/payments`;
  private readonly http = inject(HttpClient);
  private scriptPromise: Promise<void> | null = null;
  private publicKey: string | null = null;

  async capturePaymentToken(description: string, amountCents: number): Promise<string> {
    await this.ensureCheckoutLoaded();
    if (!window.Culqi || !this.publicKey) throw new Error('Culqi no está disponible para pagos.');
    window.Culqi.publicKey = this.publicKey;
    window.Culqi.settings({
      title: 'PartidoYA',
      currency: 'PEN',
      description,
      amount: amountCents
    });
    window.Culqi.options({
      lang: 'es',
      installments: false,
      paymentMethods: {
        tarjeta: true,
        yape: true,
        bancaMovil: false,
        agente: false,
        billetera: false,
        cuotealo: false
      },
      style: {
        buttonBackground: '#c6f135',
        buttonText: 'Pagar ahora',
        buttonTextColor: '#0a0f1a'
      }
    });
    return new Promise<string>((resolve, reject) => {
      window.culqi = () => {
        if (window.Culqi?.token?.id) {
          const token = window.Culqi.token.id;
          window.Culqi.close();
          resolve(token);
          return;
        }
        reject(new Error(window.Culqi?.error?.user_message || 'No se pudo validar el medio de pago.'));
      };
      window.Culqi.open();
    });
  }

  private async ensureCheckoutLoaded(): Promise<void> {
    if (!this.publicKey) {
      const config = await firstValueFrom(this.http.get<PaymentConfigResponse>(`${this.apiUrl}/config`));
      this.publicKey = config.culqiPublicKey;
    }
    if (this.scriptPromise) return this.scriptPromise;
    this.scriptPromise = new Promise<void>((resolve, reject) => {
      const existing = document.querySelector<HTMLScriptElement>('script[src="https://checkout.culqi.com/js/v4"]');
      if (existing) {
        resolve();
        return;
      }
      const script = document.createElement('script');
      script.src = 'https://checkout.culqi.com/js/v4';
      script.async = true;
      script.onload = () => resolve();
      script.onerror = () => reject(new Error('No se pudo cargar Culqi Checkout.'));
      document.body.appendChild(script);
    });
    return this.scriptPromise;
  }
}
