ALTER TABLE members ADD COLUMN archived BOOLEAN NOT NULL DEFAULT FALSE;
CREATE INDEX ix_member_archived_name ON members(archived, name);
