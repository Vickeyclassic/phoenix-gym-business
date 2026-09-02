CREATE INDEX ix_payment_member_date ON payments(member_id, payment_date);
CREATE INDEX ix_audit_user_date ON audit_logs(actor_username, created_at);
CREATE INDEX ix_audit_action_date ON audit_logs(action, created_at);
CREATE INDEX ix_audit_entity_date ON audit_logs(entity_type, created_at);
CREATE INDEX ix_revoked_expiry_revoked ON revoked_tokens(expires_at, revoked_at);
