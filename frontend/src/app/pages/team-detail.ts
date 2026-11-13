import { Component, OnInit, computed, inject, input, signal } from '@angular/core';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { forkJoin } from 'rxjs';
import { ApiService, errorMessage } from '../core/api.service';
import { AuthService } from '../core/auth.service';
import { Member, Project, Team, TeamRole } from '../core/models';

@Component({
  selector: 'app-team-detail',
  imports: [ReactiveFormsModule, RouterLink],
  template: `
    <p><a routerLink="/teams">← All teams</a></p>

    @if (team(); as team) {
      <h1>{{ team.name }}</h1>

      @if (error()) {
        <p class="error" role="alert">{{ error() }}</p>
      }

      <section class="card">
        <h2>Projects</h2>
        <ul class="list">
          @for (project of projects(); track project.id) {
            <li>
              <div>
                <a [routerLink]="['/projects', project.id]">{{ project.name }}</a>
                @if (project.description) {
                  <div class="muted">{{ project.description }}</div>
                }
              </div>
              @if (isOwner()) {
                <button type="button" class="link danger" (click)="deleteProject(project)">Delete</button>
              }
            </li>
          } @empty {
            <li class="muted">No projects yet.</li>
          }
        </ul>
        <form class="inline" [formGroup]="projectForm" (ngSubmit)="createProject()">
          <input type="text" formControlName="name" placeholder="Project name" aria-label="Project name" />
          <input type="text" formControlName="description" placeholder="Description (optional)" aria-label="Description" />
          <button type="submit" [disabled]="projectForm.invalid">Add project</button>
        </form>
      </section>

      <section class="card">
        <h2>Members</h2>
        <ul class="list">
          @for (member of members(); track member.userId) {
            <li>
              <div>
                {{ member.displayName }}
                <span class="muted">{{ member.email }}</span>
              </div>
              <div>
                <span class="badge">{{ member.role === 'OWNER' ? 'Owner' : 'Member' }}</span>
                @if (isOwner() && member.userId !== currentUserId()) {
                  <button type="button" class="link danger" (click)="removeMember(member)">Remove</button>
                }
              </div>
            </li>
          }
        </ul>
        <!-- Hiding this form is a convenience. The server still checks the role on every call. -->
        @if (isOwner()) {
          <form class="inline" [formGroup]="memberForm" (ngSubmit)="addMember()">
            <input type="email" formControlName="email" placeholder="Email of a registered user" aria-label="Member email" />
            <select formControlName="role" aria-label="Role">
              <option value="MEMBER">Member</option>
              <option value="OWNER">Owner</option>
            </select>
            <button type="submit" [disabled]="memberForm.invalid">Add member</button>
          </form>
        }
      </section>
    } @else if (error()) {
      <p class="error" role="alert">{{ error() }}</p>
    } @else {
      <p class="muted">Loading…</p>
    }
  `,
})
export class TeamDetailPage implements OnInit {
  private readonly api = inject(ApiService);
  private readonly auth = inject(AuthService);
  private readonly forms = inject(NonNullableFormBuilder);

  /** Filled by the router from the ":teamId" part of the URL. Route params are always strings. */
  readonly teamId = input.required<string>();

  protected readonly team = signal<Team | null>(null);
  protected readonly projects = signal<Project[]>([]);
  protected readonly members = signal<Member[]>([]);
  protected readonly error = signal('');

  // computed() re-runs only when a signal it reads changes.
  protected readonly isOwner = computed(() => this.team()?.role === 'OWNER');
  protected readonly currentUserId = computed(() => this.auth.user()?.id);

  protected readonly projectForm = this.forms.group({
    name: ['', [Validators.required, Validators.maxLength(120)]],
    description: ['', Validators.maxLength(2000)],
  });
  protected readonly memberForm = this.forms.group({
    email: ['', [Validators.required, Validators.email]],
    role: ['MEMBER' as TeamRole, Validators.required],
  });

  ngOnInit(): void {
    const id = Number(this.teamId());
    // forkJoin fires the three requests in parallel and emits once when all have answered.
    forkJoin({
      team: this.api.getTeam(id),
      projects: this.api.listProjects(id),
      members: this.api.listMembers(id),
    }).subscribe({
      next: ({ team, projects, members }) => {
        this.team.set(team);
        this.projects.set(projects);
        this.members.set(members);
      },
      error: (err) => this.error.set(errorMessage(err)),
    });
  }

  protected createProject(): void {
    if (this.projectForm.invalid) {
      return;
    }
    const { name, description } = this.projectForm.getRawValue();
    this.error.set('');
    this.api.createProject(Number(this.teamId()), name, description.trim() || null).subscribe({
      next: (project) => {
        this.projects.update((projects) => [project, ...projects]);
        this.projectForm.reset();
      },
      error: (err) => this.error.set(errorMessage(err)),
    });
  }

  protected deleteProject(project: Project): void {
    this.error.set('');
    this.api.deleteProject(project.id).subscribe({
      next: () => this.projects.update((projects) => projects.filter((p) => p.id !== project.id)),
      error: (err) => this.error.set(errorMessage(err)),
    });
  }

  protected addMember(): void {
    if (this.memberForm.invalid) {
      return;
    }
    const { email, role } = this.memberForm.getRawValue();
    this.error.set('');
    this.api.addMember(Number(this.teamId()), email, role).subscribe({
      next: (member) => {
        this.members.update((members) => [...members, member]);
        this.memberForm.reset();
      },
      error: (err) => this.error.set(errorMessage(err)),
    });
  }

  protected removeMember(member: Member): void {
    this.error.set('');
    this.api.removeMember(Number(this.teamId()), member.userId).subscribe({
      next: () => this.members.update((members) => members.filter((m) => m.userId !== member.userId)),
      error: (err) => this.error.set(errorMessage(err)),
    });
  }
}
