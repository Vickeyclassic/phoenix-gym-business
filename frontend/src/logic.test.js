import{describe,expect,it}from'vitest';import{businessDate,canAccessNavigation,formatIndiaDateTime,membershipLabel,strongPassword,validateMember,validatePlan,queryString}from'./logic';describe('role-aware navigation',()=>{it('allows owner everywhere',()=>expect(canAccessNavigation('OWNER','ADMIN')).toBe(true));it('allows admin pages for admin',()=>expect(canAccessNavigation('ADMIN','ADMIN')).toBe(true));it('blocks staff from admin and owner pages',()=>{expect(canAccessNavigation('STAFF','ADMIN')).toBe(false);expect(canAccessNavigation('STAFF','OWNER')).toBe(false)});it('blocks admin from owner-only pages',()=>expect(canAccessNavigation('ADMIN','OWNER')).toBe(false))});describe('business form validation',()=>{it('requires core member fields',()=>expect(validateMember({})).toContain('required'));it('accepts complete member core fields',()=>expect(validateMember({memberCode:'M1',name:'A',joinDate:'2026-09-02'})).toBe(''));it('validates plan values',()=>{expect(validatePlan({name:'',durationMonths:0,price:-1})).not.toBe('');expect(validatePlan({name:'Monthly',durationMonths:1,price:100})).toBe('')});it('enforces password strength',()=>{expect(strongPassword('StrongPass1')).toBe(true);expect(strongPassword('weak')).toBe(false)});it('omits empty filters',()=>expect(queryString({from:'2026-01-01',to:'',method:null})).toBe('from=2026-01-01'))});describe('India business time',()=>{it('uses the Kolkata calendar date',()=>expect(businessDate(new Date('2026-09-11T19:19:46Z'))).toBe('2026-09-12'));it('formats attendance instants for people',()=>expect(formatIndiaDateTime('2026-09-11T19:19:46Z')).toBe('12 Sep 2026, 12:49 AM'));it('handles missing timestamps',()=>expect(formatIndiaDateTime(null)).toBe('—'))});

describe('time and expiry boundaries',()=>{
 it('does not convert an offset timestamp twice',()=>expect(formatIndiaDateTime('2026-09-12T00:49:46+05:30')).toBe(formatIndiaDateTime('2026-09-11T19:19:46Z')));
 it('distinguishes the reminder window and expiry date',()=>{
  const label=(expiryDate,status='ACTIVE')=>membershipLabel({expiryDate,status},7,'2026-09-16');
  expect(label('2026-09-15')).toBe('Expired');expect(label('2026-09-16')).toBe('Expiring today');
  expect(label('2026-09-17')).toBe('Expiring Soon');expect(label('2026-09-23')).toBe('Expiring Soon');
  expect(label('2026-09-24')).toBe('Active');expect(label(null)).toBe('Active');
  expect(label('2026-09-18','INACTIVE')).toBe('Inactive');
  expect(membershipLabel({status:'ACTIVE',expiryDate:'2026-09-17'},0,'2026-09-16')).toBe('Active');
 });
});
