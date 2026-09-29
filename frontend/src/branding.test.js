import {describe, expect, it} from 'vitest';
import {logoSource,squareCropFrame,validateLogoFile} from './branding';

describe('business logo URLs', () => {
  it('resolves backend assets against the API origin for split deployments', () => {
    expect(logoSource('/api/public/branding-assets/gym.jpg', 'https://api.example.com/api', 'https://gym.example.com')).toBe('https://api.example.com/api/public/branding-assets/gym.jpg');
  });
  it('supports same-origin and hosted logos', () => {
    expect(logoSource('/api/public/branding-assets/gym.jpg', '/api', 'https://gym.example.com')).toBe('https://gym.example.com/api/public/branding-assets/gym.jpg');
    expect(logoSource('https://cdn.example.com/logo.png', '/api', 'https://gym.example.com')).toBe('https://cdn.example.com/logo.png');
  });
  it('rejects executable and non-web schemes', () => {
    for (const value of ['javascript:alert(1)', 'data:image/svg+xml,test', 'file:///etc/passwd', null]) {
      expect(logoSource(value, '/api', 'https://gym.example.com')).toBe('');
    }
  });
});

describe('business logo uploads', () => {
  it('accepts PNG and JPEG files up to 2 MB', () => {
    expect(validateLogoFile({type:'image/png',size:2_000_000})).toBe('');
    expect(validateLogoFile({type:'image/jpeg',size:1000})).toBe('');
  });
  it('rejects missing, unsupported and oversized files', () => {
    expect(validateLogoFile(null)).toContain('Choose');
    expect(validateLogoFile({type:'image/svg+xml',size:1000})).toContain('PNG or JPEG');
    expect(validateLogoFile({type:'image/png',size:2_000_001})).toContain('2 MB');
  });
});

describe('square logo crop geometry', () => {
  it('centres and zooms a square crop', () => {
    expect(squareCropFrame(1200, 800, 1, 0, 0)).toEqual({x:200,y:0,size:800});
    expect(squareCropFrame(1200, 800, 2, 0, 0)).toEqual({x:400,y:200,size:400});
  });
  it('clamps zoom and crop position inside the image', () => {
    const frame=squareCropFrame(600, 900, 99, 5, -5);
    expect(frame).toEqual({x:400,y:0,size:200});
  });
});
