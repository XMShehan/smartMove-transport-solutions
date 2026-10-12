const API_BASE = (import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api').replace(/\/$/, '');

export async function apiRequest(path, options = {}) {
  const response = await fetch(`${API_BASE}${path}`, {
    credentials: 'include',
    ...options,
    headers: {
      ...(options.body ? { 'Content-Type': 'application/json' } : {}),
      ...(options.headers || {})
    }
  });

  const text = await response.text();
  let payload = null;
  if (text) {
    try { payload = JSON.parse(text); }
    catch { payload = text; }
  }

  if (!response.ok) {
    const message = payload && typeof payload === 'object'
      ? (payload.error || payload.message || payload.detail || `HTTP ${response.status}`)
      : (payload || `HTTP ${response.status}`);
    throw new Error(message);
  }
  return payload;
}

export const getJson = (path) => apiRequest(path);
export const postJson = (path, body) => apiRequest(path, { method: 'POST', body: JSON.stringify(body) });
export const patchJson = (path, body) => apiRequest(path, { method: 'PATCH', body: JSON.stringify(body) });
