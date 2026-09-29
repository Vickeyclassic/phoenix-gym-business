export const canAccessNavigation=(userRole,minimumRole)=>!minimumRole||userRole==='OWNER'||(minimumRole==='ADMIN'&&userRole==='ADMIN');
export const strongPassword=value=>/^(?=.*[a-z])(?=.*[A-Z])(?=.*\d).{10,72}$/.test(value||'');
export const validateMember=m=>!m.memberCode||!m.name||!m.joinDate?'Member ID, name and join date are required.':'';
export const validatePlan=p=>!p.name||Number(p.durationMonths)<1||Number(p.price)<0?'Enter a name, positive duration and non-negative price.':'';
export function queryString(values){const q=new URLSearchParams();Object.entries(values).forEach(([k,v])=>{if(v!==''&&v!==null&&v!==undefined)q.set(k,String(v))});return q.toString()}
const INDIA_TIME_ZONE='Asia/Kolkata';
export function businessDate(value=new Date()){
  const parts=new Intl.DateTimeFormat('en-CA',{timeZone:INDIA_TIME_ZONE,year:'numeric',month:'2-digit',day:'2-digit'}).formatToParts(value);
  const part=type=>parts.find(item=>item.type===type)?.value;
  return `${part('year')}-${part('month')}-${part('day')}`;
}
export function formatIndiaDateTime(value){
  if(!value)return '—';
  const date=value instanceof Date?value:new Date(value);
  if(Number.isNaN(date.getTime()))return '—';
  const parts=new Intl.DateTimeFormat('en-US',{timeZone:INDIA_TIME_ZONE,day:'2-digit',month:'short',year:'numeric',hour:'numeric',minute:'2-digit',hour12:true}).formatToParts(date);
  const part=type=>parts.find(item=>item.type===type)?.value;
  return `${Number(part('day'))} ${part('month')} ${part('year')}, ${part('hour')}:${part('minute')} ${part('dayPeriod')?.toUpperCase()}`;
}

export function membershipLabel(member,reminderDays=7,date=businessDate()) {
  if(member.archived)return 'Archived';
  if(member.status==='INACTIVE')return 'Inactive';
  if(member.status==='EXPIRED'||(member.expiryDate&&member.expiryDate<date))return 'Expired';
  const limit=new Date(date+'T00:00:00Z');
  limit.setUTCDate(limit.getUTCDate()+Number(reminderDays));
  if(member.expiryDate===date)return 'Expiring today';
  if(reminderDays>0&&member.expiryDate>date&&member.expiryDate<=limit.toISOString().slice(0,10))return 'Expiring Soon';
  return 'Active';
}
