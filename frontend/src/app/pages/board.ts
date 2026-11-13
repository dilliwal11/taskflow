import { Component, OnInit, computed, inject, input, signal } from '@angular/core';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { forkJoin, of, switchMap } from 'rxjs';
import { ApiService, errorMessage } from '../core/api.service';
import { Member, Project, Task, TaskPriority, TaskStatus } from '../core/models';

interface Column {
  status: TaskStatus;
  title: string;
  tasks: Task[];
}

const STATUS_ORDER: TaskStatus[] = ['TODO', 'IN_PROGRESS', 'DONE'];
const STATUS_TITLES: Record<TaskStatus, string> = {
  TODO: 'To do',
  IN_PROGRESS: 'In progress',
  DONE: 'Done',
};

@Component({
  selector: 'app-board',
  imports: [ReactiveFormsModule, RouterLink],
  template: `
    @if (project(); as project) {
      <p><a [routerLink]="['/teams', project.teamId]">← Back to team</a></p>
      <h1>{{ project.name }}</h1>

      @if (error()) {
        <p class="error" role="alert">{{ error() }}</p>
      }

      <section class="card">
        <h2>New task</h2>
        <form class="inline" [formGroup]="form" (ngSubmit)="createTask()">
          <input type="text" formControlName="title" placeholder="What needs doing?" aria-label="Task title" />
          <select formControlName="priority" aria-label="Priority">
            <option value="LOW">Low</option>
            <option value="MEDIUM">Medium</option>
            <option value="HIGH">High</option>
          </select>
          <select formControlName="assigneeId" aria-label="Assignee">
            <option value="">Unassigned</option>
            @for (member of members(); track member.userId) {
              <option [value]="member.userId">{{ member.displayName }}</option>
            }
          </select>
          <input type="date" formControlName="dueDate" aria-label="Due date" />
          <button type="submit" [disabled]="form.invalid">Add task</button>
        </form>
      </section>

      <div class="board">
        @for (column of columns(); track column.status) {
          <section class="column">
            <h2>{{ column.title }} <span class="muted">{{ column.tasks.length }}</span></h2>
            @for (task of column.tasks; track task.id) {
              <article class="task">
                <div class="task-title">{{ task.title }}</div>
                <div class="task-meta">
                  <span class="badge priority-{{ task.priority.toLowerCase() }}">{{ task.priority }}</span>
                  @if (task.dueDate) {
                    <span class="muted">Due {{ task.dueDate }}</span>
                  }
                </div>
                <select
                  aria-label="Assignee"
                  (change)="reassign(task, $any($event.target).value)"
                >
                  <option value="" [selected]="task.assigneeId === null">Unassigned</option>
                  @for (member of members(); track member.userId) {
                    <option [value]="member.userId" [selected]="member.userId === task.assigneeId">
                      {{ member.displayName }}
                    </option>
                  }
                </select>
                <div class="task-actions">
                  <button type="button" class="link" [disabled]="column.status === 'TODO'" (click)="move(task, -1)">← Back</button>
                  <button type="button" class="link" [disabled]="column.status === 'DONE'" (click)="move(task, 1)">Forward →</button>
                  <button type="button" class="link danger" (click)="remove(task)">Delete</button>
                </div>
              </article>
            } @empty {
              <p class="muted">Nothing here.</p>
            }
          </section>
        }
      </div>
    } @else if (error()) {
      <p class="error" role="alert">{{ error() }}</p>
    } @else {
      <p class="muted">Loading…</p>
    }
  `,
})
export class BoardPage implements OnInit {
  private readonly api = inject(ApiService);

  /** Filled by the router from ":projectId". */
  readonly projectId = input.required<string>();

  protected readonly project = signal<Project | null>(null);
  protected readonly tasks = signal<Task[]>([]);
  protected readonly members = signal<Member[]>([]);
  protected readonly error = signal('');

  /** The single task list, grouped into columns. Recomputed whenever tasks() changes. */
  protected readonly columns = computed<Column[]>(() =>
    STATUS_ORDER.map((status) => ({
      status,
      title: STATUS_TITLES[status],
      tasks: this.tasks().filter((task) => task.status === status),
    })),
  );

  protected readonly form = inject(NonNullableFormBuilder).group({
    title: ['', [Validators.required, Validators.maxLength(200)]],
    priority: ['MEDIUM' as TaskPriority],
    assigneeId: [''],
    dueDate: [''],
  });

  ngOnInit(): void {
    const id = Number(this.projectId());
    // The members request needs the project's teamId, so it has to wait for the project:
    // switchMap chains "then do this with the result".
    this.api
      .getProject(id)
      .pipe(
        switchMap((project) =>
          forkJoin({
            project: of(project),
            tasks: this.api.listTasks(id),
            members: this.api.listMembers(project.teamId),
          }),
        ),
      )
      .subscribe({
        next: ({ project, tasks, members }) => {
          this.project.set(project);
          this.tasks.set(tasks);
          this.members.set(members);
        },
        error: (err) => this.error.set(errorMessage(err)),
      });
  }

  protected createTask(): void {
    if (this.form.invalid) {
      return;
    }
    const { title, priority, assigneeId, dueDate } = this.form.getRawValue();
    this.error.set('');
    this.api
      .createTask(Number(this.projectId()), {
        title,
        priority,
        assigneeId: assigneeId ? Number(assigneeId) : null,
        dueDate: dueDate || null,
      })
      .subscribe({
        next: (task) => {
          this.tasks.update((tasks) => [task, ...tasks]);
          this.form.reset();
        },
        error: (err) => this.error.set(errorMessage(err)),
      });
  }

  /**
   * Optimistic update: move the card straight away so the UI feels instant, then confirm with
   * the server. If the server says no, put the card back where it was.
   */
  protected move(task: Task, direction: -1 | 1): void {
    const next = STATUS_ORDER[STATUS_ORDER.indexOf(task.status) + direction];
    if (!next) {
      return;
    }
    this.error.set('');
    this.replace({ ...task, status: next });
    this.api.changeTaskStatus(task.id, next).subscribe({
      next: (saved) => this.replace(saved),
      error: (err) => {
        this.replace(task);
        this.error.set(errorMessage(err));
      },
    });
  }

  /**
   * Sends the version we last read. If someone else edited the task since then, the server
   * answers 409 and we reload the board instead of overwriting their change.
   */
  protected reassign(task: Task, value: string): void {
    this.error.set('');
    this.api
      .updateTask(task.id, {
        title: task.title,
        description: task.description,
        status: task.status,
        priority: task.priority,
        assigneeId: value ? Number(value) : null,
        dueDate: task.dueDate,
        version: task.version,
      })
      .subscribe({
        next: (saved) => this.replace(saved),
        error: (err) => {
          this.error.set(errorMessage(err));
          this.reloadTasks();
        },
      });
  }

  protected remove(task: Task): void {
    this.error.set('');
    this.api.deleteTask(task.id).subscribe({
      next: () => this.tasks.update((tasks) => tasks.filter((t) => t.id !== task.id)),
      error: (err) => this.error.set(errorMessage(err)),
    });
  }

  private replace(task: Task): void {
    this.tasks.update((tasks) => tasks.map((t) => (t.id === task.id ? task : t)));
  }

  private reloadTasks(): void {
    this.api.listTasks(Number(this.projectId())).subscribe((tasks) => this.tasks.set(tasks));
  }
}
