import {expect,it} from 'vitest';
import {validatePasswordChange as validate} from './password';
it('requires current password and a strong different matching replacement',()=>{
  expect(validate('', 'NewPassword123', 'NewPassword123')).not.toBe('');
  expect(validate('OldPassword123', 'weak', 'weak')).not.toBe('');
  expect(validate('OldPassword123', 'OldPassword123', 'OldPassword123')).not.toBe('');
  expect(validate('OldPassword123', 'NewPassword123', 'OtherPassword123')).not.toBe('');
  expect(validate('OldPassword123', 'NewPassword123', 'NewPassword123')).toBe('');
});
it('enforces the BCrypt byte limit for Unicode passwords',()=>{
  const value='Aa1'+ '界'.repeat(24);
  expect(validate('OldPassword123',value,value)).not.toBe('');
});
