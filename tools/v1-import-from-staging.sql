-- MANUAL OPERATOR TOOL. Never run automatically on application startup.
-- Run only against a backed-up, empty/test Business V2 database after loading
-- reviewed V1 exports into the v1_stage_* tables defined below.
START TRANSACTION;

CREATE TABLE IF NOT EXISTS v1_stage_members (
  legacy_id BIGINT PRIMARY KEY, name VARCHAR(160) NOT NULL, phone VARCHAR(30),
  email VARCHAR(160), join_date DATE, membership_type VARCHAR(100),
  membership_start_date DATE, expiry_date DATE, status VARCHAR(20)
);
CREATE TABLE IF NOT EXISTS v1_stage_payments (
  legacy_id BIGINT PRIMARY KEY, legacy_member_id BIGINT NOT NULL,
  amount DECIMAL(12,2) NOT NULL, payment_date TIMESTAMP NOT NULL,
  method VARCHAR(30), receipt_number VARCHAR(40), period_start DATE, period_end DATE
);
CREATE TABLE IF NOT EXISTS v1_stage_attendance (
  legacy_id BIGINT PRIMARY KEY, legacy_member_id BIGINT NOT NULL,
  checked_in_at TIMESTAMP NOT NULL, checked_out_at TIMESTAMP NULL
);

INSERT INTO members(member_code,name,phone,email,join_date,plan_id,membership_start_date,expiry_date,status,notes)
SELECT CONCAT('V1-',s.legacy_id),TRIM(s.name),NULLIF(TRIM(s.phone),''),NULLIF(LOWER(TRIM(s.email)),''),
       COALESCE(s.join_date,CURRENT_DATE),p.id,s.membership_start_date,s.expiry_date,
       CASE WHEN UPPER(s.status)='INACTIVE' THEN 'INACTIVE' WHEN s.expiry_date<CURRENT_DATE THEN 'EXPIRED' ELSE 'ACTIVE' END,
       CONCAT('Imported from Phoenix V1 legacy member ',s.legacy_id)
FROM v1_stage_members s
LEFT JOIN membership_plans p ON LOWER(p.name)=LOWER(s.membership_type)
WHERE NOT EXISTS(SELECT 1 FROM members m WHERE m.member_code=CONCAT('V1-',s.legacy_id));

INSERT INTO payments(receipt_number,member_id,plan_id,plan_name_snapshot,period_start,period_end,payment_date,amount,method,status,received_by,reference_note)
SELECT COALESCE(NULLIF(s.receipt_number,''),CONCAT('V1-',s.legacy_id)),m.id,m.plan_id,
       COALESCE(p.name,'Legacy membership'),COALESCE(s.period_start,DATE(s.payment_date)),
       COALESCE(s.period_end,DATE(s.payment_date)),s.payment_date,s.amount,
       CASE UPPER(REPLACE(COALESCE(s.method,'OTHER'),' ','_')) WHEN 'CASH' THEN 'CASH' WHEN 'UPI' THEN 'UPI' WHEN 'CARD' THEN 'CARD' WHEN 'BANK_TRANSFER' THEN 'BANK_TRANSFER' ELSE 'OTHER' END,
       'PAID',u.id,CONCAT('Imported from Phoenix V1 payment ',s.legacy_id)
FROM v1_stage_payments s
JOIN members m ON m.member_code=CONCAT('V1-',s.legacy_member_id)
LEFT JOIN membership_plans p ON p.id=m.plan_id
JOIN users u ON u.role='OWNER'
WHERE NOT EXISTS(SELECT 1 FROM payments x WHERE x.receipt_number=COALESCE(NULLIF(s.receipt_number,''),CONCAT('V1-',s.legacy_id)));

INSERT INTO attendance(member_id,checked_in_at,checked_out_at,status,source,recorded_by)
SELECT m.id,s.checked_in_at,s.checked_out_at,CASE WHEN s.checked_out_at IS NULL THEN 'CHECKED_IN' ELSE 'CHECKED_OUT' END,'V1_IMPORT',u.id
FROM v1_stage_attendance s JOIN members m ON m.member_code=CONCAT('V1-',s.legacy_member_id)
JOIN users u ON u.role='OWNER'
WHERE NOT EXISTS(SELECT 1 FROM attendance a WHERE a.member_id=m.id AND a.checked_in_at=s.checked_in_at);

-- Review these counts before replacing ROLLBACK with COMMIT.
SELECT 'members' entity,COUNT(*) imported FROM members WHERE member_code LIKE 'V1-%'
UNION ALL SELECT 'payments',COUNT(*) FROM payments WHERE reference_note LIKE 'Imported from Phoenix V1%'
UNION ALL SELECT 'attendance',COUNT(*) FROM attendance WHERE source='V1_IMPORT';
ROLLBACK;
