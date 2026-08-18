PRAGMA foreign_keys = ON;

CREATE TABLE IF NOT EXISTS app_user (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    user_code TEXT NOT NULL UNIQUE,
    name TEXT NOT NULL,
    login_name TEXT NOT NULL UNIQUE,
    password_hash TEXT NOT NULL,
    phone TEXT,
    active INTEGER NOT NULL DEFAULT 1 CHECK (active IN (0, 1)),
    must_change_password INTEGER NOT NULL DEFAULT 1 CHECK (must_change_password IN (0, 1)),
    failed_login_count INTEGER NOT NULL DEFAULT 0,
    locked_until TEXT,
    created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_login_at TEXT
);
CREATE TABLE IF NOT EXISTS role (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    code TEXT NOT NULL UNIQUE,
    name TEXT NOT NULL UNIQUE,
    system_role INTEGER NOT NULL DEFAULT 0 CHECK (system_role IN (0, 1))
);
CREATE TABLE IF NOT EXISTS user_role (
    user_id INTEGER NOT NULL REFERENCES app_user(id),
    role_id INTEGER NOT NULL REFERENCES role(id),
    PRIMARY KEY (user_id, role_id)
);
CREATE TABLE IF NOT EXISTS permission (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    code TEXT NOT NULL UNIQUE,
    name TEXT NOT NULL
);
CREATE TABLE IF NOT EXISTS role_permission (
    role_id INTEGER NOT NULL REFERENCES role(id),
    permission_id INTEGER NOT NULL REFERENCES permission(id),
    PRIMARY KEY (role_id, permission_id)
);

CREATE TABLE IF NOT EXISTS patient (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    patient_code TEXT NOT NULL UNIQUE,
    patient_type TEXT NOT NULL CHECK (patient_type IN ('ADULT', 'CHILD')),
    name TEXT NOT NULL,
    gender TEXT NOT NULL CHECK (gender IN ('MALE', 'FEMALE', 'UNKNOWN')),
    id_type TEXT NOT NULL,
    id_number TEXT NOT NULL,
    birth_date TEXT NOT NULL,
    phone TEXT NOT NULL,
    address TEXT,
    guardian_name TEXT,
    guardian_relationship TEXT,
    guardian_phone TEXT,
    allergies TEXT,
    medical_history TEXT,
    remark TEXT,
    created_by INTEGER NOT NULL REFERENCES app_user(id),
    created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (id_type, id_number),
    CHECK (patient_type = 'ADULT' OR (guardian_name IS NOT NULL AND guardian_relationship IS NOT NULL AND guardian_phone IS NOT NULL))
);

CREATE TABLE IF NOT EXISTS project_category (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT NOT NULL,
    code TEXT NOT NULL UNIQUE,
    parent_id INTEGER REFERENCES project_category(id),
    sort_order INTEGER NOT NULL DEFAULT 0,
    active INTEGER NOT NULL DEFAULT 1 CHECK (active IN (0, 1)),
    UNIQUE(name, parent_id)
);
CREATE TABLE IF NOT EXISTS treatment_project (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT NOT NULL,
    code TEXT UNIQUE,
    category_id INTEGER NOT NULL REFERENCES project_category(id),
    price_cents INTEGER NOT NULL CHECK (price_cents >= 0),
    duration_minutes INTEGER,
    description TEXT,
    applicable_population TEXT,
    precautions TEXT,
    active INTEGER NOT NULL DEFAULT 1 CHECK (active IN (0, 1)),
    created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS deposit_account (
    patient_id INTEGER PRIMARY KEY REFERENCES patient(id),
    balance_cents INTEGER NOT NULL DEFAULT 0 CHECK (balance_cents >= 0),
    updated_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE TABLE IF NOT EXISTS bill (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    bill_code TEXT NOT NULL UNIQUE,
    patient_id INTEGER NOT NULL REFERENCES patient(id),
    treatment_date TEXT NOT NULL,
    therapist_id INTEGER NOT NULL REFERENCES app_user(id),
    therapist_name_snapshot TEXT NOT NULL,
    note TEXT,
    status TEXT NOT NULL CHECK (status IN ('PENDING_PAYMENT', 'PAID', 'PARTIALLY_REFUNDED', 'REFUNDED', 'VOIDED')),
    gross_cents INTEGER NOT NULL CHECK (gross_cents >= 0),
    discount_cents INTEGER NOT NULL DEFAULT 0 CHECK (discount_cents >= 0),
    discount_reason TEXT,
    discount_authorizer_id INTEGER REFERENCES app_user(id),
    discount_authorized_at TEXT,
    receivable_cents INTEGER NOT NULL CHECK (receivable_cents >= 0),
    paid_cents INTEGER NOT NULL DEFAULT 0 CHECK (paid_cents >= 0),
    refunded_cents INTEGER NOT NULL DEFAULT 0 CHECK (refunded_cents >= 0),
    created_by INTEGER NOT NULL REFERENCES app_user(id),
    created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
    paid_at TEXT,
    voided_at TEXT,
    void_reason TEXT,
    CHECK (discount_cents <= gross_cents),
    CHECK (receivable_cents = gross_cents - discount_cents),
    CHECK ((status <> 'PAID' AND status <> 'PARTIALLY_REFUNDED' AND status <> 'REFUNDED') OR paid_cents = receivable_cents)
);
CREATE TABLE IF NOT EXISTS bill_item (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    bill_id INTEGER NOT NULL REFERENCES bill(id),
    project_id INTEGER NOT NULL REFERENCES treatment_project(id),
    project_name_snapshot TEXT NOT NULL,
    unit_price_cents INTEGER NOT NULL CHECK (unit_price_cents >= 0),
    quantity INTEGER NOT NULL CHECK (quantity > 0),
    subtotal_cents INTEGER NOT NULL CHECK (subtotal_cents = unit_price_cents * quantity)
);
CREATE TABLE IF NOT EXISTS payment (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    bill_id INTEGER NOT NULL REFERENCES bill(id),
    method TEXT NOT NULL CHECK (method IN ('DEPOSIT', 'CASH', 'WECHAT', 'ALIPAY', 'BANK_CARD', 'OTHER')),
    amount_cents INTEGER NOT NULL CHECK (amount_cents > 0),
    created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE TABLE IF NOT EXISTS treatment_record (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    bill_id INTEGER NOT NULL UNIQUE REFERENCES bill(id),
    patient_id INTEGER NOT NULL REFERENCES patient(id),
    therapist_id INTEGER NOT NULL REFERENCES app_user(id),
    therapist_name_snapshot TEXT NOT NULL,
    treatment_date TEXT NOT NULL,
    note TEXT,
    status TEXT NOT NULL CHECK (status IN ('COMPLETED', 'CORRECTED')),
    created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE TABLE IF NOT EXISTS deposit_transaction (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    transaction_code TEXT NOT NULL UNIQUE,
    patient_id INTEGER NOT NULL REFERENCES patient(id),
    transaction_type TEXT NOT NULL CHECK (transaction_type IN ('RECHARGE', 'CONSUMPTION', 'REFUND')),
    amount_cents INTEGER NOT NULL CHECK (amount_cents <> 0),
    balance_after_cents INTEGER NOT NULL CHECK (balance_after_cents >= 0),
    payment_method TEXT,
    bill_id INTEGER REFERENCES bill(id),
    remark TEXT,
    operator_id INTEGER NOT NULL REFERENCES app_user(id),
    created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE TABLE IF NOT EXISTS refund_request (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    refund_code TEXT NOT NULL UNIQUE,
    bill_id INTEGER NOT NULL REFERENCES bill(id),
    requested_cents INTEGER NOT NULL CHECK (requested_cents > 0),
    reason TEXT NOT NULL,
    status TEXT NOT NULL CHECK (status IN ('PENDING_APPROVAL', 'APPROVED', 'REJECTED', 'EXECUTED', 'REVOKED')),
    applicant_id INTEGER NOT NULL REFERENCES app_user(id),
    approver_id INTEGER REFERENCES app_user(id),
    executor_id INTEGER REFERENCES app_user(id),
    approved_at TEXT,
    executed_at TEXT,
    remark TEXT,
    created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE TABLE IF NOT EXISTS refund_payment (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    refund_request_id INTEGER NOT NULL REFERENCES refund_request(id),
    payment_method TEXT NOT NULL,
    amount_cents INTEGER NOT NULL CHECK (amount_cents > 0)
);
CREATE TABLE IF NOT EXISTS correction_order (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    correction_code TEXT NOT NULL UNIQUE,
    bill_id INTEGER NOT NULL REFERENCES bill(id),
    old_value TEXT NOT NULL,
    new_value TEXT NOT NULL,
    reason TEXT NOT NULL,
    applicant_id INTEGER NOT NULL REFERENCES app_user(id),
    approver_id INTEGER REFERENCES app_user(id),
    status TEXT NOT NULL CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED', 'APPLIED')),
    created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
    approved_at TEXT,
    applied_at TEXT
);
CREATE TABLE IF NOT EXISTS audit_log (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
    operator_id INTEGER REFERENCES app_user(id),
    action TEXT NOT NULL,
    target_type TEXT NOT NULL,
    target_id TEXT,
    before_value TEXT,
    after_value TEXT,
    ip_address TEXT
);

CREATE INDEX IF NOT EXISTS idx_patient_name ON patient(name);
CREATE INDEX IF NOT EXISTS idx_patient_phone ON patient(phone);
CREATE INDEX IF NOT EXISTS idx_bill_status_date ON bill(status, treatment_date);
CREATE INDEX IF NOT EXISTS idx_bill_patient ON bill(patient_id);
CREATE INDEX IF NOT EXISTS idx_deposit_transaction_patient ON deposit_transaction(patient_id, created_at);
