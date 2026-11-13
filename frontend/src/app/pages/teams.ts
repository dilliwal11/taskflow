import { Component, OnInit, inject, signal } from '@angular/core';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { ApiService, errorMessage } from '../core/api.service';
import { Team } from '../core/models';

@Component({
  selector: 'app-teams',
  imports: [ReactiveFormsModule, RouterLink],
  template: `
    <h1>Your teams</h1>

    @if (loading()) {
      <p class="muted">Loading…</p>
    } @else {
      <ul class="list">
        @for (team of teams(); track team.id) {
          <li>
            <a [routerLink]="['/teams', team.id]">{{ team.name }}</a>
            <span class="badge">{{ team.role === 'OWNER' ? 'Owner' : 'Member' }}</span>
          </li>
        } @empty {
          <li class="muted">You are not in any team yet. Create your first one below.</li>
        }
      </ul>
    }

    <section class="card">
      <h2>New team</h2>
      <form class="inline" [formGroup]="form" (ngSubmit)="create()">
        <input type="text" formControlName="name" placeholder="Team name" aria-label="Team name" />
        <button type="submit" [disabled]="form.invalid || saving()">Create team</button>
      </form>
      @if (error()) {
        <p class="error" role="alert">{{ error() }}</p>
      }
    </section>
  `,
})
export class TeamsPage implements OnInit {
  private readonly api = inject(ApiService);

  protected readonly teams = signal<Team[]>([]);
  protected readonly loading = signal(true);
  protected readonly saving = signal(false);
  protected readonly error = signal('');
  protected readonly form = inject(NonNullableFormBuilder).group({
    name: ['', [Validators.required, Validators.maxLength(100)]],
  });

  ngOnInit(): void {
    this.api.listTeams().subscribe({
      next: (teams) => {
        this.teams.set(teams);
        this.loading.set(false);
      },
      error: (err) => {
        this.error.set(errorMessage(err));
        this.loading.set(false);
      },
    });
  }

  protected create(): void {
    if (this.form.invalid) {
      return;
    }
    this.saving.set(true);
    this.error.set('');
    this.api.createTeam(this.form.getRawValue().name).subscribe({
      next: (team) => {
        // Signals hold immutable values: build a new array instead of pushing into the old one.
        this.teams.update((teams) => [...teams, team]);
        this.form.reset();
        this.saving.set(false);
      },
      error: (err) => {
        this.error.set(errorMessage(err));
        this.saving.set(false);
      },
    });
  }
}
