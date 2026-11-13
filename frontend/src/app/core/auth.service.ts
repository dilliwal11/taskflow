import { HttpClient } from '@angular/common/http';
import { Injectable, computed, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { Observable, tap } from 'rxjs';
import { AuthResponse, User } from './models';

const TOKEN_KEY = 'taskflow.token';
const USER_KEY = 'taskflow.user';

/**
 * Holds the session (JWT + user) in signals, and mirrors it to localStorage so a page reload
 * keeps you signed in.
 *
 * Trade-off to know for interviews: localStorage is readable by any script on the page, so an
 * XSS bug can steal the token. The stricter alternative is an HttpOnly cookie set by the server,
 * which then needs CSRF protection.
 */
@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly router = inject(Router);

  private readonly tokenState = signal<string | null>(read(TOKEN_KEY));
  private readonly userState = signal<User | null>(readUser());

  readonly token = this.tokenState.asReadonly();
  readonly user = this.userState.asReadonly();
  readonly isLoggedIn = computed(() => this.tokenState() !== null);

  login(email: string, password: string): Observable<AuthResponse> {
    return this.http
      .post<AuthResponse>('/api/auth/login', { email, password })
      .pipe(tap((response) => this.startSession(response)));
  }

  register(email: string, password: string, displayName: string): Observable<AuthResponse> {
    return this.http
      .post<AuthResponse>('/api/auth/register', { email, password, displayName })
      .pipe(tap((response) => this.startSession(response)));
  }

  logout(): void {
    this.tokenState.set(null);
    this.userState.set(null);
    remove(TOKEN_KEY);
    remove(USER_KEY);
    this.router.navigateByUrl('/login');
  }

  private startSession(response: AuthResponse): void {
    this.tokenState.set(response.token);
    this.userState.set(response.user);
    write(TOKEN_KEY, response.token);
    write(USER_KEY, JSON.stringify(response.user));
  }
}

// localStorage can throw (private browsing, blocked storage), so every access is guarded.
function read(key: string): string | null {
  try {
    return localStorage.getItem(key);
  } catch {
    return null;
  }
}

function write(key: string, value: string): void {
  try {
    localStorage.setItem(key, value);
  } catch {
    // The session still works in memory for this tab.
  }
}

function remove(key: string): void {
  try {
    localStorage.removeItem(key);
  } catch {
    // Nothing to clean up.
  }
}

function readUser(): User | null {
  const raw = read(USER_KEY);
  if (!raw) {
    return null;
  }
  try {
    return JSON.parse(raw) as User;
  } catch {
    return null;
  }
}
