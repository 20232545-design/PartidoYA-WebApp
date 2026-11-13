import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { NgZone } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { API_BASE_URL } from '@shared/services/api-base-url.token';
import { PaymentService } from './payment.service';

const CHECKOUT_SRC = 'https://checkout.culqi.com/js/v4';

/** Doble mínimo del Culqi Checkout global. */
function installFakeCulqi() {
  const fake = {
    publicKey: '',
    token: null as { id: string } | null,
    error: null as { user_message: string } | null,
    opened: false,
    closed: false,
    settings: () => {},
    options: () => {},
    open() { this.opened = true; },
    close() { this.closed = true; }
  };
  window.Culqi = fake;
  return fake;
}

describe('PaymentService.openCulqiCheckout', () => {
  let service: PaymentService;
  let http: HttpTestingController;
  /** Verdadero mientras se ejecuta código dentro de `NgZone.run`. */
  let insideZoneRun: boolean;
  let script: HTMLScriptElement;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: API_BASE_URL, useValue: 'http://api.test/api/v1' }
      ]
    });
    // Los tests corren sin Zone.js: observamos qué código pasa por `NgZone.run`.
    insideZoneRun = false;
    const zone = TestBed.inject(NgZone);
    const originalRun = zone.run.bind(zone);
    vi.spyOn(zone, 'run').mockImplementation(((fn: () => unknown) => {
      insideZoneRun = true;
      try {
        return originalRun(fn);
      } finally {
        insideZoneRun = false;
      }
    }) as NgZone['run']);
    service = TestBed.inject(PaymentService);
    http = TestBed.inject(HttpTestingController);
    // Simula que el script de Culqi ya está cargado.
    script = document.createElement('script');
    script.src = CHECKOUT_SRC;
    document.body.appendChild(script);
  });

  afterEach(() => {
    http.verify();
    script.remove();
    delete window.Culqi;
    delete window.culqi;
  });

  /** Abre el checkout y espera a que el modal de Culqi quede abierto. */
  const openCheckout = async (culqi: ReturnType<typeof installFakeCulqi>) => {
    const result = service.openCulqiCheckout(160, 'Reserva Camp Nou');
    http.expectOne('http://api.test/api/v1/payments/config').flush({ culqiPublicKey: 'pk_test_x' });
    await new Promise(resolve => setTimeout(resolve));
    expect(culqi.opened).toBe(true);
    return { result };
  };

  it('resolves the token Culqi returns', async () => {
    const culqi = installFakeCulqi();
    const { result } = await openCheckout(culqi);

    culqi.token = { id: 'tkn_test_123' };
    window.culqi!();

    expect(await result).toBe('tkn_test_123');
    expect(culqi.closed).toBe(true);
  });

  it('runs the callback inside the Angular zone so the screen is refreshed', async () => {
    const culqi = installFakeCulqi();
    const { result } = await openCheckout(culqi);
    let insideZone = false;
    culqi.close = () => { insideZone = insideZoneRun; };

    culqi.token = { id: 'tkn_test_123' };
    window.culqi!();
    await result;

    expect(insideZone).toBe(true);
  });

  it('rejects with the message Culqi gives the user when the card fails', async () => {
    const culqi = installFakeCulqi();
    const { result } = await openCheckout(culqi);

    culqi.error = { user_message: 'Tarjeta rechazada' };
    window.culqi!();

    await expect(result).rejects.toThrow('Tarjeta rechazada');
  });
});
