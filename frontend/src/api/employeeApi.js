// Thin wrapper around fetch. Every component talks to the backend through this file only.
// VITE_API_BASE_URL is empty by default: in dev Vite proxies /api, in Docker nginx proxies /api.
const BASE = import.meta.env.VITE_API_BASE_URL ?? '';
const API = `${BASE}/api/employees`;

export class ApiError extends Error {
  constructor(message, status, fieldErrors) {
    super(message);
    this.name = 'ApiError';
    this.status = status;
    this.fieldErrors = fieldErrors || {};
  }
}

export function buildQuery(params = {}) {
  const query = new URLSearchParams();
  Object.entries(params).forEach(([key, value]) => {
    if (value !== undefined && value !== null && String(value).trim() !== '') {
      query.set(key, String(value).trim());
    }
  });
  return query.toString();
}

async function request(path, options = {}) {
  let response;
  try {
    response = await fetch(`${API}${path}`, {
      headers: { 'Content-Type': 'application/json' },
      ...options,
    });
  } catch {
    throw new ApiError('Cannot reach the server. Check that the backend is running.', 0);
  }

  if (response.status === 204) {
    return null;
  }

  const data = await response.json().catch(() => null);
  if (!response.ok) {
    throw new ApiError(
      data?.message || `Request failed with status ${response.status}`,
      response.status,
      data?.fieldErrors,
    );
  }
  return data;
}

export const listEmployees = (filters) => request(`?${buildQuery(filters)}`);
export const getEmployee = (id) => request(`/${id}`);
export const createEmployee = (employee) =>
  request('', { method: 'POST', body: JSON.stringify(employee) });
export const updateEmployee = (id, employee) =>
  request(`/${id}`, { method: 'PUT', body: JSON.stringify(employee) });
export const deleteEmployee = (id) => request(`/${id}`, { method: 'DELETE' });
export const getDepartments = () => request('/departments');

/** URL the browser can navigate to in order to download the CSV for the current filters. */
export const exportUrl = ({ search, department, status } = {}) =>
  `${API}/export?${buildQuery({ search, department, status })}`;
