import { Component, inject, signal } from '@angular/core';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { errorMessage } from '../core/api.service';
import { AuthService } from '../core/auth.service';

@Component({
  selector: 'app-register',
  imports: [ReactiveFormsModule, RouterLink],
  template: `
    <section class="card narrow">
      <h1>Create your account</h1>
      <form [formGroup]="form" (ngSubmit)="submit()">
        <label>
          Name
          <input type="text" formControlName="displayName" autocomplete="name" />
        </label>
        <label>
          Email
          <input type="email" formControlName="email" autocomplete="email" />
        </label>
        <label>
          Password
          <input type="password" formControlName="password" autocomplete="new-password" />
          <small class="muted">At least 8 characters</small>
        </label>
        @if (error()) {
          <p class="error" role="alert">{{ error() }}</p>
        }
        <button type="submit" [disabled]="form.invalid || busy()">
          {{ busy() ? 'Creating…' : 'Create account' }}
        </button>
      </form>
      <p class="muted">Already registered? <a routerLink="/login">Sign in</a></p>
    </section>
  `,
})
export class RegisterPage {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  // The same rules are enforced again on the server (RegisterRequest). Client-side
  // validation is for fast feedback only: never trust it for correctness.
  protected readonly form = inject(NonNullableFormBuilder).group({
    displayName: ['', [Validators.required, Validators.maxLength(100)]],
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required, Validators.minLength(8), Validators.maxLength(72)]],
  });
  protected readonly busy = signal(false);
  protected readonly error = signal('');

  protected submit(): void {
    if (this.form.invalid) {
      return;
    }
    const { email, password, displayName } = this.form.getRawValue();
    this.busy.set(true);
    this.error.set('');
    this.auth.register(email, password, displayName).subscribe({
      next: () => this.router.navigateByUrl('/teams'),
      error: (err) => {
        this.error.set(errorMessage(err));
        this.busy.set(false);
      },
    });
  }
}
