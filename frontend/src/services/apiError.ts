/** The server answered with a non-2xx status. */
export class ApiError extends Error {
  readonly status: number;

  constructor(status: number) {
    super(`Request failed with status ${status}`);
    this.name = 'ApiError';
    this.status = status;
  }
}

/** The request never got an answer (server down, no connection). */
export class NetworkError extends Error {
  constructor() {
    super('Network error');
    this.name = 'NetworkError';
  }
}

export const API_URL = import.meta.env.VITE_API_URL ?? '';

/** Calls the REST API and returns the parsed JSON body; failures become ApiError or NetworkError. */
export async function request<T>(path: string, init?: RequestInit): Promise<T> {
  let response: Response;
  try {
    response = await fetch(`${API_URL}${path}`, init);
  } catch {
    throw new NetworkError();
  }
  if (!response.ok) {
    throw new ApiError(response.status);
  }
  if (response.status === 204) {
    return undefined as T;
  }
  return response.json();
}

/** Sends `body` as JSON with the given method (POST, PUT...) through `request`. */
export function sendJson<T>(method: string, path: string, body: object): Promise<T> {
  return request<T>(path, {
    method,
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(body),
  });
}
