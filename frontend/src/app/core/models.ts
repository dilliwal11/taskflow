// TypeScript mirrors of the backend's DTO records. Keep these in step with the Java side.

export interface User {
  id: number;
  email: string;
  displayName: string;
}

export interface AuthResponse {
  token: string;
  user: User;
}

export type TeamRole = 'OWNER' | 'MEMBER';

export interface Team {
  id: number;
  name: string;
  role: TeamRole;
}

export interface Member {
  userId: number;
  email: string;
  displayName: string;
  role: TeamRole;
}

export interface Project {
  id: number;
  teamId: number;
  name: string;
  description: string | null;
  createdAt: string;
}

export type TaskStatus = 'TODO' | 'IN_PROGRESS' | 'DONE';
export type TaskPriority = 'LOW' | 'MEDIUM' | 'HIGH';

export interface Task {
  id: number;
  projectId: number;
  title: string;
  description: string | null;
  status: TaskStatus;
  priority: TaskPriority;
  assigneeId: number | null;
  assigneeName: string | null;
  createdById: number;
  dueDate: string | null;
  createdAt: string;
  updatedAt: string;
  version: number;
}

export interface CreateTaskRequest {
  title: string;
  description?: string | null;
  priority?: TaskPriority;
  assigneeId?: number | null;
  dueDate?: string | null;
}

export interface UpdateTaskRequest {
  title: string;
  description: string | null;
  status: TaskStatus;
  priority: TaskPriority;
  assigneeId: number | null;
  dueDate: string | null;
  version: number;
}
