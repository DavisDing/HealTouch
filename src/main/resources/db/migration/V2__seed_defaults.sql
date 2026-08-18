INSERT OR IGNORE INTO role(code, name, system_role) VALUES
  ('ADMIN', '系统管理员', 1), ('RECEPTION', '前台', 1), ('THERAPIST', '治疗师', 1), ('FINANCE', '财务/主管', 1);
INSERT OR IGNORE INTO permission(code, name) VALUES
  ('PATIENT_VIEW', '查看患者'), ('PATIENT_EDIT', '编辑患者'), ('TREATMENT_CREATE', '新增治疗登记'),
  ('BILL_CHARGE', '账单收费'), ('DEPOSIT_RECHARGE', '预存充值'), ('DEPOSIT_REFUND', '预存退款'),
  ('BILL_REFUND', '账单退款'), ('REPORT_VIEW', '查看统计'), ('SYSTEM_MANAGE', '系统管理'),
  ('DISCOUNT_APPLY', '设置折扣');
INSERT OR IGNORE INTO project_category(name, code, parent_id, sort_order, active) VALUES
  ('小儿推拿', 'PEDIATRIC', NULL, 10, 1), ('成人推拿', 'ADULT', NULL, 20, 1),
  ('面针', 'FACIAL_ACUPUNCTURE', NULL, 30, 1), ('其他', 'OTHER', NULL, 40, 1);

INSERT OR IGNORE INTO role_permission(role_id, permission_id)
SELECT r.id, p.id FROM role r CROSS JOIN permission p
WHERE (r.code = 'RECEPTION' AND p.code IN ('PATIENT_VIEW','PATIENT_EDIT','TREATMENT_CREATE','BILL_CHARGE','DEPOSIT_RECHARGE'))
   OR (r.code = 'THERAPIST' AND p.code IN ('PATIENT_VIEW','TREATMENT_CREATE'))
   OR (r.code = 'FINANCE' AND p.code IN ('PATIENT_VIEW','DEPOSIT_REFUND','BILL_REFUND','REPORT_VIEW'));
