export const canAccessNavigation=(userRole,minimumRole)=>!minimumRole||userRole==='OWNER'||(minimumRole==='ADMIN'&&userRole==='ADMIN');
export const strongPassword=value=>/^(?=.*[a-z])(?=.*[A-Z])(?=.*\d).{10,72}$/.test(value||'');
export const validateMember=m=>!m.memberCode||!m.name||!m.joinDate?'Member ID, name and join date are required.':'';
export const validatePlan=p=>!p.name||Number(p.durationMonths)<1||Number(p.price)<0?'Enter a name, positive duration and non-negative price.':'';
export function queryString(values){const q=new URLSearchParams();Object.entries(values).forEach(([k,v])=>{if(v!==''&&v!==null&&v!==undefined)q.set(k,String(v))});return q.toString()}
