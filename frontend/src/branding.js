export function logoSource(value, apiBase = '/api', origin = window.location.origin) {
  if (!value) return '';
  try {
    const api = new URL(apiBase, origin);
    const url = new URL(value, api.origin);
    return ['http:', 'https:'].includes(url.protocol) ? url.href : '';
  } catch { return ''; }
}

export const MAX_LOGO_BYTES = 2_000_000;
export function validateLogoFile(file) {
  if (!file) return 'Choose a PNG or JPEG logo.';
  if (!['image/png', 'image/jpeg'].includes(file.type)) return 'Logo must be a PNG or JPEG image.';
  if (file.size > MAX_LOGO_BYTES) return 'Logo must be 2 MB or smaller.';
  return '';
}

export function squareCropFrame(width, height, zoom = 1, offsetX = 0, offsetY = 0) {
  const safeZoom = Math.min(3, Math.max(1, Number(zoom) || 1));
  const size = Math.min(width, height) / safeZoom;
  const maxX = Math.max(0, (width - size) / 2);
  const maxY = Math.max(0, (height - size) / 2);
  const x = Math.min(width - size, Math.max(0, (width - size) / 2 + Math.max(-1, Math.min(1, offsetX)) * maxX));
  const y = Math.min(height - size, Math.max(0, (height - size) / 2 + Math.max(-1, Math.min(1, offsetY)) * maxY));
  return {x, y, size};
}
