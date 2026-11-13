import { HttpErrorResponse } from '@angular/common/http';
import { errorMessage } from './core/api.service';

// A plain unit test of a pure function: no TestBed, no DOM.
describe('errorMessage', () => {
  it('uses the problem detail sent by the backend', () => {
    const error = new HttpErrorResponse({ status: 403, error: { detail: 'You are not a member of this team' } });
    expect(errorMessage(error)).toBe('You are not a member of this team');
  });

  it('lists field errors from a validation failure', () => {
    const error = new HttpErrorResponse({
      status: 400,
      error: { detail: 'Validation failed', errors: { password: 'must be 8 to 72 characters' } },
    });
    expect(errorMessage(error)).toBe('password must be 8 to 72 characters');
  });

  it('explains when the server is unreachable', () => {
    expect(errorMessage(new HttpErrorResponse({ status: 0 }))).toContain('Cannot reach the server');
  });

  it('falls back to a generic message', () => {
    expect(errorMessage(new Error('boom'))).toContain('Something went wrong');
  });
});
