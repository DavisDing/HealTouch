-- Additive migration: keep V1/V2 checksums and preserve existing business records.
ALTER TABLE app_user ADD COLUMN session_version INTEGER NOT NULL DEFAULT 0;
ALTER TABLE refund_request ADD COLUMN approval_remark TEXT;
ALTER TABLE refund_request ADD COLUMN revoked_at TEXT;
ALTER TABLE refund_request ADD COLUMN revoke_reason TEXT;
CREATE TRIGGER user_credentials_changed AFTER UPDATE OF active,password_hash ON app_user
WHEN NEW.active <> OLD.active OR NEW.password_hash <> OLD.password_hash
BEGIN UPDATE app_user SET session_version=session_version+1 WHERE id=NEW.id; END;
CREATE TRIGGER user_role_added AFTER INSERT ON user_role
BEGIN UPDATE app_user SET session_version=session_version+1 WHERE id=NEW.user_id; END;
CREATE TRIGGER user_role_removed AFTER DELETE ON user_role
BEGIN UPDATE app_user SET session_version=session_version+1 WHERE id=OLD.user_id; END;
CREATE TRIGGER user_role_changed AFTER UPDATE ON user_role
BEGIN
 UPDATE app_user SET session_version=session_version+1 WHERE id=OLD.user_id;
 UPDATE app_user SET session_version=session_version+1 WHERE id=NEW.user_id AND NEW.user_id<>OLD.user_id;
END;
DELETE FROM role_permission WHERE role_id IN (SELECT id FROM role WHERE code<>'ADMIN')
 AND permission_id IN (SELECT id FROM permission WHERE code IN ('SYSTEM_MANAGE','DISCOUNT_APPLY'));
CREATE TRIGGER reserved_permission_insert BEFORE INSERT ON role_permission
WHEN (SELECT code FROM role WHERE id=NEW.role_id)<>'ADMIN'
 AND (SELECT code FROM permission WHERE id=NEW.permission_id) IN ('SYSTEM_MANAGE','DISCOUNT_APPLY')
BEGIN SELECT RAISE(ABORT,'仅管理员可持有系统管理和折扣授权权限'); END;
CREATE TRIGGER reserved_permission_update BEFORE UPDATE ON role_permission
WHEN (SELECT code FROM role WHERE id=NEW.role_id)<>'ADMIN'
 AND (SELECT code FROM permission WHERE id=NEW.permission_id) IN ('SYSTEM_MANAGE','DISCOUNT_APPLY')
BEGIN SELECT RAISE(ABORT,'仅管理员可持有系统管理和折扣授权权限'); END;
CREATE TRIGGER audit_log_no_update BEFORE UPDATE ON audit_log
BEGIN SELECT RAISE(ABORT,'审计日志不允许修改'); END;
CREATE TRIGGER audit_log_no_delete BEFORE DELETE ON audit_log
BEGIN SELECT RAISE(ABORT,'审计日志不允许删除'); END;
CREATE TABLE discount_request (
 id INTEGER PRIMARY KEY AUTOINCREMENT,
 bill_id INTEGER NOT NULL REFERENCES bill(id),
 discount_cents INTEGER NOT NULL CHECK(discount_cents>0),
 reason TEXT NOT NULL,
 applicant_id INTEGER NOT NULL REFERENCES app_user(id),
 status TEXT NOT NULL DEFAULT 'PENDING' CHECK(status IN ('PENDING','APPROVED','REJECTED','CANCELLED')),
 approver_id INTEGER REFERENCES app_user(id),
 created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
 decided_at TEXT
);
CREATE UNIQUE INDEX one_pending_discount ON discount_request(bill_id) WHERE status='PENDING';
CREATE INDEX idx_patient_created_id ON patient(created_at DESC,id DESC);
