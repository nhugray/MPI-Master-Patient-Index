export interface ApiErrorLike {
  error?: {
    message?: string;
  };
}

export function getApiErrorMessage(error: ApiErrorLike | null | undefined, fallback: string): string {
  return error?.error?.message || fallback;
}
