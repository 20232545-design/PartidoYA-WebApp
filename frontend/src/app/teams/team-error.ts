import { HttpErrorResponse } from '@angular/common/http';

export function teamErrorMessage(error: unknown, fallback: string): string {
  if (error instanceof HttpErrorResponse && error.error && typeof error.error === 'object') {
    const message = (error.error as { message?: unknown }).message;
    if (typeof message === 'string' && message.trim()) return message;
  }
  if (error instanceof Error && error.message.trim()) return error.message;
  return fallback;
}
