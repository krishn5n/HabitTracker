export type FieldType = "NUMBER" | "TEXT" | "BOOLEAN" | "DURATION";
export type Gender = "MALE" | "FEMALE" | "OTHER" | "UNSPECIFIED";

export interface Page<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
  first: boolean;
  last: boolean;
}

export interface UserResponse {
  userSeqId: number;
  displayName: string;
  email: string;
  gender: Gender | null;
  dateOfBirth: string | null;
  age: number | null;
  weightKg: number | null;
  heightCm: number | null;
}

export interface HabitResponse {
  habitSeqId: number;
  userSeqId: number;
  name: string;
  description: string | null;
  active: boolean;
}

export interface HabitFieldResponse {
  habitFieldSeqId: number;
  habitSeqId: number;
  fieldName: string;
  fieldType: FieldType;
  unit: string | null;
  goalValue: number | null;
}

export interface EntryResponse {
  entrySeqId: number;
  habitFieldSeqId: number;
  entryValue: string;
  entryDate: string;
  entryTimestamp: string;
  updatedAt: string;
  updatedBy: number;
  completed: boolean;
}

export interface ConsistencyEntry {
  entryDate: string;
  completed: boolean;
}

export interface ConsistencyResponse {
  habitFieldSeqId: number;
  windowStart: string;
  windowEnd: string;
  entries: ConsistencyEntry[];
}

export interface DashboardField {
  habitFieldSeqId: number;
  fieldName: string;
  fieldType: FieldType;
  unit: string | null;
  goalValue: number | null;
  completedToday: boolean;
  todayValue: string | null;
  updatedAt: string | null;
}

export interface DashboardHabit {
  habitSeqId: number;
  name: string;
  fields: DashboardField[];
}

export interface DashboardResponse {
  habits: DashboardHabit[];
}

export interface ProblemDetail {
  type: string;
  title: string;
  status: number;
  detail: string;
  instance: string;
}
