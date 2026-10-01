export interface ApiResponse<T> {
  statusCode: number;
  message: string;
  data: T;
  error: string | null;
  details: string[] | null;
}

export function isSuccess<T>(response: Pick<ApiResponse<T>, 'statusCode'> | null | undefined): boolean {
  return !!response && response.statusCode >= 200 && response.statusCode < 300;
}
