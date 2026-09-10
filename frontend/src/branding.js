export function logoSource(value, apiBase = '/api', origin = window.location.origin) {
  if (!value) return '';
  try {
    const api = new URL(apiBase, origin);
    const url = new URL(value, api.origin);
    return ['http:', 'https:'].includes(url.protocol) ? url.href : '';
  } catch { return ''; }
}
