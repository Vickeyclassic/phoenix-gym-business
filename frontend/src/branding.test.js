import {describe, expect, it} from 'vitest';
import {logoSource} from './branding';

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
