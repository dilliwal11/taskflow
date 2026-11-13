import { HttpClient, HttpErrorResponse, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import {
  CreateTaskRequest,
  Member,
  Project,
  Task,
  TaskStatus,
  Team,
  TeamRole,
  UpdateTaskRequest,
} from './models';

/** One typed method per backend endpoint. Components never build URLs themselves. */
@Injectable({ providedIn: 'root' })
export class ApiService {
  private readonly http = inject(HttpClient);

  // Teams
  listTeams(): Observable<Team[]> {
    return this.http.get<Team[]>('/api/teams');
  }

  createTeam(name: string): Observable<Team> {
    return this.http.post<Team>('/api/teams', { name });
  }

  getTeam(teamId: number): Observable<Team> {
    return this.http.get<Team>(`/api/teams/${teamId}`);
  }

  listMembers(teamId: number): Observable<Member[]> {
    return this.http.get<Member[]>(`/api/teams/${teamId}/members`);
  }

  addMember(teamId: number, email: string, role: TeamRole): Observable<Member> {
    return this.http.post<Member>(`/api/teams/${teamId}/members`, { email, role });
  }

  removeMember(teamId: number, userId: number): Observable<void> {
    return this.http.delete<void>(`/api/teams/${teamId}/members/${userId}`);
  }

  // Projects
  listProjects(teamId: number): Observable<Project[]> {
    return this.http.get<Project[]>(`/api/teams/${teamId}/projects`);
  }

  createProject(teamId: number, name: string, description: string | null): Observable<Project> {
    return this.http.post<Project>(`/api/teams/${teamId}/projects`, { name, description });
  }

  getProject(projectId: number): Observable<Project> {
    return this.http.get<Project>(`/api/projects/${projectId}`);
  }

  deleteProject(projectId: number): Observable<void> {
    return this.http.delete<void>(`/api/projects/${projectId}`);
  }

  // Tasks
  listTasks(projectId: number, filter: { status?: TaskStatus; assigneeId?: number } = {}): Observable<Task[]> {
    let params = new HttpParams();
    if (filter.status) {
      params = params.set('status', filter.status);
    }
    if (filter.assigneeId) {
      params = params.set('assigneeId', filter.assigneeId);
    }
    return this.http.get<Task[]>(`/api/projects/${projectId}/tasks`, { params });
  }

  createTask(projectId: number, request: CreateTaskRequest): Observable<Task> {
    return this.http.post<Task>(`/api/projects/${projectId}/tasks`, request);
  }

  updateTask(taskId: number, request: UpdateTaskRequest): Observable<Task> {
    return this.http.put<Task>(`/api/tasks/${taskId}`, request);
  }

  changeTaskStatus(taskId: number, status: TaskStatus): Observable<Task> {
    return this.http.patch<Task>(`/api/tasks/${taskId}/status`, { status });
  }

  deleteTask(taskId: number): Observable<void> {
    return this.http.delete<void>(`/api/tasks/${taskId}`);
  }
}

/**
 * Turns a failed request into one line of text for the user.
 * The backend sends RFC 9457 problem details: { detail: "...", errors?: { field: "message" } }.
 */
export function errorMessage(error: unknown): string {
  if (error instanceof HttpErrorResponse) {
    if (error.status === 0) {
      return 'Cannot reach the server. Is the backend running?';
    }
    const problem = error.error as { detail?: string; errors?: Record<string, string> } | null;
    if (problem?.errors) {
      return Object.entries(problem.errors)
        .map(([field, message]) => `${field} ${message}`)
        .join('. ');
    }
    if (problem?.detail) {
      return problem.detail;
    }
  }
  return 'Something went wrong. Please try again.';
}
