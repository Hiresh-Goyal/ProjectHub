const configuredApiBase = import.meta.env.VITE_API_BASE_URL?.trim();
const API_BASE = (configuredApiBase || '/api/v1').replace(/\/$/, '');
const SESSION_KEY = 'projecthub.session';

export class ApiError extends Error {
  constructor(message, status = 0, details = null) {
    super(message);
    this.name = 'ApiError';
    this.status = status;
    this.details = details;
  }
}

export function getCompanyId(user) {
  const value = user?.companyId ?? user?.companyID ?? null;
  if (value === null || value === undefined || value === '') return null;
  const numeric = Number(value);
  return Number.isFinite(numeric) ? numeric : null;
}

export function getProjectCompanyId(project) {
  const value = project?.companyId ?? project?.companyID ?? project?.company?.id ?? null;
  if (value === null || value === undefined || value === '') return null;
  const numeric = Number(value);
  return Number.isFinite(numeric) ? numeric : null;
}

export function getProjectOwnerId(project) {
  const value = project?.ownerId ?? project?.ownerID ?? project?.owner?.id ?? null;
  if (value === null || value === undefined || value === '') return null;
  const numeric = Number(value);
  return Number.isFinite(numeric) ? numeric : null;
}

export function hasCompanyFields(user) {
  return Boolean(user) && ['companyId', 'companyID', 'companyName', 'companyRole']
    .some((key) => Object.prototype.hasOwnProperty.call(user, key));
}

export function createBasicAuth(email, password) {
  // TextEncoder supports names/passwords containing non-ASCII characters.
  const bytes = new TextEncoder().encode(`${email.trim().toLowerCase()}:${password}`);
  let binary = '';
  bytes.forEach((byte) => { binary += String.fromCharCode(byte); });
  return `Basic ${btoa(binary)}`;
}

function readSession() {
  try {
    return JSON.parse(sessionStorage.getItem(SESSION_KEY) || 'null');
  } catch {
    return null;
  }
}

export async function apiRequest(path, options = {}) {
  const { method = 'GET', body, authHeader, signal, headers: extraHeaders = {} } = options;
  if (import.meta.env.PROD && !/^https?:\/\//i.test(API_BASE)) {
    throw new ApiError(
      'The backend URL is not configured. Set VITE_API_BASE_URL in Vercel to your Render URL ending in /api/v1.',
      0,
    );
  }

  const session = readSession();
  const effectiveAuth = authHeader === undefined ? session?.authHeader : authHeader;
  const headers = { Accept: 'application/json', ...extraHeaders };
  if (body !== undefined) headers['Content-Type'] = 'application/json';
  if (effectiveAuth) headers.Authorization = effectiveAuth;

  let response;
  try {
    response = await fetch(`${API_BASE}${path}`, {
      method,
      headers,
      body: body === undefined ? undefined : JSON.stringify(body),
      signal,
    });
  } catch (error) {
    if (error.name === 'AbortError') throw error;
    throw new ApiError('Could not reach the backend. Check your connection or try again later.', 0);
  }

  if (response.status === 204) return null;
  const raw = await response.text();
  let data = null;
  if (raw) {
    try { data = JSON.parse(raw); } catch { data = raw; }
  }

  if (!response.ok) {
    let message = 'The request failed.';
    if (typeof data === 'string' && data.trim()) message = data;
    else if (data && typeof data === 'object') {
      message = data.message || data.detail || data.error || message;
      // Spring ResponseStatusException messages may include a status prefix, e.g.
      // `409 CONFLICT "You already belong to a company"`. Show only the useful reason.
      const springReason = String(message).match(/^\d{3}\s+[A-Z_]+\s+"([\s\S]*)"$/);
      if (springReason) message = springReason[1];
    }
    if (response.status === 401) message = 'Email or password is incorrect, or your session has expired.';
    if (response.status === 400 && !data?.message && typeof data !== 'string') message = 'Some information is missing or invalid. Check the form and try again.';
    if (response.status === 403) message = data?.message || 'You do not have permission to perform this action.';
    throw new ApiError(message, response.status, data);
  }
  return data;
}

export const api = {
  get: (path, options = {}) => apiRequest(path, { ...options, method: 'GET' }),
  post: (path, body, options = {}) => apiRequest(path, { ...options, method: 'POST', body }),
  put: (path, body, options = {}) => apiRequest(path, { ...options, method: 'PUT', body }),
  patch: (path, body, options = {}) => apiRequest(path, { ...options, method: 'PATCH', body }),
  delete: (path, options = {}) => apiRequest(path, { ...options, method: 'DELETE' }),
};

export function dedupeById(items = []) {
  const seen = new Set();
  return items.filter((item) => {
    if (item?.id == null || seen.has(item.id)) return false;
    seen.add(item.id);
    return true;
  });
}

export { SESSION_KEY };
