import {strongPassword} from './logic';
export function validatePasswordChange(current, next, confirmation) {
  if (!current) return 'Enter your current password.';
  if (!strongPassword(next)) return 'Use 10–72 characters including uppercase, lowercase and a number.';
  if (new TextEncoder().encode(next).length > 72) return 'Password must be at most 72 UTF-8 bytes.';
  if (current === next) return 'Choose a different password.';
  if (next !== confirmation) return 'Passwords do not match.';
  return '';
}
