#!/usr/bin/env python3
"""SQLite schema and data-compatibility checks; not a substitute for Flyway/JVM tests."""
import sqlite3
from pathlib import Path

root = Path(__file__).resolve().parents[1]
migrations = root / 'src/main/resources/db/migration'
con = sqlite3.connect(':memory:')
for name in ['V1__initial_schema.sql', 'V2__seed_defaults.sql']:
    con.executescript((migrations / name).read_text())
con.execute("INSERT INTO app_user(id,user_code,name,login_name,password_hash) VALUES(1,'U1','admin','admin','test-hash')")
con.execute("INSERT INTO user_role SELECT 1,id FROM role WHERE code='ADMIN'")
con.execute("INSERT INTO audit_log(action,target_type) VALUES('LEGACY','TEST')")
# Existing invalid grants must be removed without dropping users or business data.
con.execute("INSERT INTO role_permission SELECT r.id,p.id FROM role r,permission p WHERE r.code='RECEPTION' AND p.code='SYSTEM_MANAGE'")
con.commit()
con.executescript((migrations / 'V3__security_and_workflows.sql').read_text())
assert con.execute('SELECT COUNT(*) FROM app_user').fetchone()[0] == 1
assert con.execute('SELECT COUNT(*) FROM audit_log').fetchone()[0] == 1
assert con.execute("SELECT COUNT(*) FROM role_permission rp JOIN role r ON r.id=rp.role_id JOIN permission p ON p.id=rp.permission_id WHERE r.code<>'ADMIN' AND p.code IN ('SYSTEM_MANAGE','DISCOUNT_APPLY')").fetchone()[0] == 0
for permission in ['SYSTEM_MANAGE', 'DISCOUNT_APPLY']:
    try:
        con.execute("INSERT INTO role_permission SELECT r.id,p.id FROM role r,permission p WHERE r.code='RECEPTION' AND p.code=?", (permission,))
        raise AssertionError('Reserved grant accepted')
    except sqlite3.IntegrityError:
        pass
for statement in ["UPDATE audit_log SET action='FORGED'", 'DELETE FROM audit_log']:
    try:
        con.execute(statement)
        raise AssertionError('Audit mutation accepted')
    except sqlite3.IntegrityError:
        pass
version = con.execute('SELECT session_version FROM app_user WHERE id=1').fetchone()[0]
con.execute('UPDATE app_user SET active=0 WHERE id=1')
assert con.execute('SELECT session_version FROM app_user WHERE id=1').fetchone()[0] == version + 1
con.execute("UPDATE app_user SET password_hash='different' WHERE id=1")
assert con.execute('SELECT session_version FROM app_user WHERE id=1').fetchone()[0] == version + 2
con.execute('DELETE FROM user_role WHERE user_id=1')
assert con.execute('SELECT session_version FROM app_user WHERE id=1').fetchone()[0] == version + 3
assert not con.execute('PRAGMA foreign_key_check').fetchall()
assert con.execute('PRAGMA integrity_check').fetchone()[0] == 'ok'
print('PASS: V1/V2 -> V3 preserves records, removes forbidden grants, protects audit rows, invalidates sessions, and maintains FK/integrity.')
