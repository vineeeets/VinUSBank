# 🏦 VinUSBank — US Banking System with AI Bots
## Low-Level Design (LLD) Document

**Version:** 1.0  
**Date:** April 11, 2026  
**Author:** Vineet  
**Status:** Draft — Awaiting Review

---

## 1. Database Design

### 1.1 Database Strategy
- **One database per service** (logical separation within a single MySQL instance for local dev)
- Each service owns its schema — no cross-service direct DB access
- All tables use `InnoDB` engine with `utf8mb4` charset
- Soft deletes via `is_deleted` flag + `deleted_at` timestamp
- All tables include audit columns: `created_at`, `updated_at`, `created_by`, `updated_by`

### 1.2 MySQL Databases

| Database | Service | Description |
|----------|---------|-------------|
| `vinusbank_auth` | auth-service | Users, roles, permissions, tokens |
| `vinusbank_customer` | customer-service | Customer profiles, KYC documents |
| `vinusbank_account` | account-service | Accounts, balances, statements |
| `vinusbank_transaction` | transaction-service | Transactions, transfers, payments |
| `vinusbank_loan` | loan-service | Loans, EMI, amortization |
| `vinusbank_card` | card-service | Virtual cards, limits |
| `vinusbank_compliance` | compliance-service | KYC reviews, SAR, CTR, OFAC |
| `vinusbank_notification` | notification-service | Notification logs, templates |

---

### 1.3 Entity-Relationship Diagrams & Table Schemas

---

#### 📦 AUTH-SERVICE (`vinusbank_auth`)

```
┌─────────────────────────┐      ┌─────────────────────────┐
│        users             │      │         roles            │
├─────────────────────────┤      ├─────────────────────────┤
│ id (PK, UUID)           │      │ id (PK, BIGINT)         │
│ email (UNIQUE)          │      │ name (UNIQUE)           │
│ password_hash           │      │ description             │
│ phone_number            │      │ created_at              │
│ mfa_enabled             │      │ updated_at              │
│ mfa_secret              │      └────────────┬────────────┘
│ status (ENUM)           │                   │
│ failed_login_attempts   │      ┌────────────┴────────────┐
│ locked_until            │      │      permissions         │
│ last_login_at           │      ├─────────────────────────┤
│ created_at              │      │ id (PK, BIGINT)         │
│ updated_at              │      │ name (UNIQUE)           │
└────────────┬────────────┘      │ resource                │
             │                   │ action                  │
             │                   │ created_at              │
┌────────────┴────────────┐      └─────────────────────────┘
│     user_roles           │
├─────────────────────────┤      ┌─────────────────────────┐
│ user_id (FK)            │      │    role_permissions      │
│ role_id (FK)            │      ├─────────────────────────┤
│ assigned_at             │      │ role_id (FK)            │
└─────────────────────────┘      │ permission_id (FK)      │
                                 └─────────────────────────┘
┌─────────────────────────┐
│    refresh_tokens        │
├─────────────────────────┤
│ id (PK, UUID)           │
│ user_id (FK)            │
│ token_hash              │
│ device_info             │
│ ip_address              │
│ expires_at              │
│ revoked                 │
│ created_at              │
└─────────────────────────┘

┌─────────────────────────┐
│    login_audit_log       │
├─────────────────────────┤
│ id (PK, BIGINT)         │
│ user_id (FK)            │
│ action (ENUM)           │
│ ip_address              │
│ user_agent              │
│ status (SUCCESS/FAIL)   │
│ failure_reason          │
│ created_at              │
└─────────────────────────┘
```

**DDL — users table:**
```sql
CREATE TABLE users (
    id              VARCHAR(36) PRIMARY KEY,
    email           VARCHAR(255) NOT NULL UNIQUE,
    password_hash   VARCHAR(255) NOT NULL,
    phone_number    VARCHAR(20),
    mfa_enabled     BOOLEAN DEFAULT FALSE,
    mfa_secret      VARCHAR(64),
    status          ENUM('PENDING', 'ACTIVE', 'SUSPENDED', 'LOCKED', 'CLOSED') DEFAULT 'PENDING',
    failed_login_attempts INT DEFAULT 0,
    locked_until    TIMESTAMP NULL,
    last_login_at   TIMESTAMP NULL,
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_users_email (email),
    INDEX idx_users_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

**DDL — roles table:**
```sql
CREATE TABLE roles (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    name        VARCHAR(50) NOT NULL UNIQUE,
    description VARCHAR(255),
    created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Seed data
INSERT INTO roles (name, description) VALUES
('ROLE_CUSTOMER', 'Banking customer'),
('ROLE_STAFF', 'Bank staff member'),
('ROLE_COMPLIANCE', 'Compliance officer'),
('ROLE_ADMIN', 'System administrator');
```

**DDL — permissions table:**
```sql
CREATE TABLE permissions (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    name        VARCHAR(100) NOT NULL UNIQUE,
    resource    VARCHAR(100) NOT NULL,
    action      VARCHAR(50) NOT NULL,
    created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_resource_action (resource, action)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

---

#### 📦 CUSTOMER-SERVICE (`vinusbank_customer`)

```
┌─────────────────────────┐      ┌─────────────────────────┐
│       customers          │      │    customer_addresses    │
├─────────────────────────┤      ├─────────────────────────┤
│ id (PK, UUID)           │──┐   │ id (PK, BIGINT)         │
│ user_id (UNIQUE, FK→auth)│  │   │ customer_id (FK)        │
│ first_name              │  ├──►│ address_type (ENUM)     │
│ middle_name             │  │   │ street_line_1           │
│ last_name               │  │   │ street_line_2           │
│ date_of_birth           │  │   │ city                    │
│ ssn_encrypted           │  │   │ state                   │
│ ssn_last_four           │  │   │ zip_code                │
│ gender                  │  │   │ country                 │
│ nationality             │  │   │ is_primary              │
│ occupation              │  │   │ created_at              │
│ annual_income_range     │  │   │ updated_at              │
│ employment_status       │  │   └─────────────────────────┘
│ employer_name           │  │
│ risk_rating (ENUM)      │  │   ┌─────────────────────────┐
│ kyc_status (ENUM)       │  │   │    kyc_documents         │
│ kyc_verified_at         │  │   ├─────────────────────────┤
│ profile_image_url       │  └──►│ id (PK, BIGINT)         │
│ created_at              │      │ customer_id (FK)        │
│ updated_at              │      │ document_type (ENUM)    │
│ is_deleted              │      │ document_number_enc     │
│ deleted_at              │      │ file_path               │
└─────────────────────────┘      │ file_name               │
                                 │ mime_type               │
                                 │ verification_status     │
                                 │ verified_by             │
                                 │ verified_at             │
                                 │ rejection_reason        │
                                 │ expiry_date             │
                                 │ created_at              │
                                 │ updated_at              │
                                 └─────────────────────────┘
```

**DDL — customers table:**
```sql
CREATE TABLE customers (
    id                  VARCHAR(36) PRIMARY KEY,
    user_id             VARCHAR(36) NOT NULL UNIQUE,
    first_name          VARCHAR(100) NOT NULL,
    middle_name         VARCHAR(100),
    last_name           VARCHAR(100) NOT NULL,
    date_of_birth       DATE NOT NULL,
    ssn_encrypted       VARBINARY(512) NOT NULL,
    ssn_last_four       VARCHAR(4) NOT NULL,
    gender              ENUM('MALE', 'FEMALE', 'OTHER', 'PREFER_NOT_TO_SAY'),
    nationality         VARCHAR(50) DEFAULT 'US',
    occupation          VARCHAR(100),
    annual_income_range ENUM('UNDER_25K', '25K_50K', '50K_100K', '100K_250K', 'ABOVE_250K'),
    employment_status   ENUM('EMPLOYED', 'SELF_EMPLOYED', 'UNEMPLOYED', 'RETIRED', 'STUDENT'),
    employer_name       VARCHAR(255),
    risk_rating         ENUM('LOW', 'MEDIUM', 'HIGH', 'CRITICAL') DEFAULT 'LOW',
    kyc_status          ENUM('PENDING', 'IN_REVIEW', 'VERIFIED', 'REJECTED', 'EXPIRED') DEFAULT 'PENDING',
    kyc_verified_at     TIMESTAMP NULL,
    profile_image_url   VARCHAR(500),
    created_at          TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_deleted          BOOLEAN DEFAULT FALSE,
    deleted_at          TIMESTAMP NULL,
    INDEX idx_customer_user (user_id),
    INDEX idx_customer_ssn4 (ssn_last_four),
    INDEX idx_customer_kyc (kyc_status),
    INDEX idx_customer_risk (risk_rating),
    FULLTEXT INDEX idx_customer_name (first_name, last_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

---

#### 📦 ACCOUNT-SERVICE (`vinusbank_account`)

```
┌─────────────────────────┐      ┌─────────────────────────┐
│       accounts           │      │   account_statements     │
├─────────────────────────┤      ├─────────────────────────┤
│ id (PK, UUID)           │──┐   │ id (PK, BIGINT)         │
│ customer_id             │  │   │ account_id (FK)         │
│ account_number (UNIQUE) │  ├──►│ statement_date          │
│ account_type (ENUM)     │  │   │ opening_balance         │
│ currency (USD)          │  │   │ closing_balance         │
│ available_balance       │  │   │ total_credits           │
│ current_balance         │  │   │ total_debits            │
│ hold_amount             │  │   │ file_path               │
│ interest_rate           │  │   │ generated_at            │
│ overdraft_limit         │  │   └─────────────────────────┘
│ daily_transfer_limit    │  │
│ status (ENUM)           │  │   ┌─────────────────────────┐
│ opened_at               │  │   │   account_interest       │
│ closed_at               │  └──►├─────────────────────────┤
│ last_activity_at        │      │ id (PK, BIGINT)         │
│ dormant_since           │      │ account_id (FK)         │
│ created_at              │      │ interest_rate           │
│ updated_at              │      │ accrued_amount          │
└─────────────────────────┘      │ period_start            │
                                 │ period_end              │
┌─────────────────────────┐      │ credited                │
│    balance_snapshots     │      │ credited_at             │
├─────────────────────────┤      │ created_at              │
│ id (PK, BIGINT)         │      └─────────────────────────┘
│ account_id (FK)         │
│ balance                 │
│ snapshot_date           │
│ snapshot_type           │
│ created_at              │
└─────────────────────────┘
```

**DDL — accounts table:**
```sql
CREATE TABLE accounts (
    id                  VARCHAR(36) PRIMARY KEY,
    customer_id         VARCHAR(36) NOT NULL,
    account_number      VARCHAR(20) NOT NULL UNIQUE,
    account_type        ENUM('CHECKING', 'SAVINGS', 'MONEY_MARKET', 'CD') NOT NULL,
    currency            VARCHAR(3) DEFAULT 'USD',
    available_balance   DECIMAL(15,2) DEFAULT 0.00,
    current_balance     DECIMAL(15,2) DEFAULT 0.00,
    hold_amount         DECIMAL(15,2) DEFAULT 0.00,
    interest_rate       DECIMAL(5,4) DEFAULT 0.0000,
    overdraft_limit     DECIMAL(15,2) DEFAULT 0.00,
    daily_transfer_limit DECIMAL(15,2) DEFAULT 10000.00,
    status              ENUM('PENDING_APPROVAL', 'ACTIVE', 'FROZEN', 'DORMANT', 'CLOSED') DEFAULT 'PENDING_APPROVAL',
    opened_at           TIMESTAMP NULL,
    closed_at           TIMESTAMP NULL,
    last_activity_at    TIMESTAMP NULL,
    dormant_since       TIMESTAMP NULL,
    created_at          TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_account_customer (customer_id),
    INDEX idx_account_number (account_number),
    INDEX idx_account_status (status),
    INDEX idx_account_type (account_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

---

#### 📦 TRANSACTION-SERVICE (`vinusbank_transaction`)

```
┌───────────────────────────────┐
│         transactions           │
├───────────────────────────────┤
│ id (PK, UUID)                 │
│ reference_number (UNIQUE)     │
│ transaction_type (ENUM)       │
│ category (ENUM)               │
│ source_account_id             │
│ destination_account_id        │
│ source_account_number         │
│ destination_account_number    │
│ amount                        │
│ currency                      │
│ fee_amount                    │
│ description                   │
│ memo                          │
│ status (ENUM)                 │
│ failure_reason                │
│ fraud_score                   │
│ fraud_checked                 │
│ fraud_flagged                 │
│ ip_address                    │
│ device_fingerprint            │
│ geo_location                  │
│ initiated_at                  │
│ completed_at                  │
│ created_at                    │
│ updated_at                    │
└───────────────────────────────┘

┌───────────────────────────────┐      ┌───────────────────────────────┐
│     scheduled_transfers        │      │       bill_payments            │
├───────────────────────────────┤      ├───────────────────────────────┤
│ id (PK, UUID)                 │      │ id (PK, UUID)                 │
│ source_account_id             │      │ customer_id                   │
│ destination_account_id        │      │ account_id                    │
│ amount                        │      │ biller_name                   │
│ frequency (ENUM)              │      │ biller_account_number         │
│ next_execution_date           │      │ amount                        │
│ end_date                      │      │ category                      │
│ status                        │      │ due_date                      │
│ last_executed_at              │      │ status                        │
│ execution_count               │      │ transaction_id (FK)           │
│ created_at                    │      │ is_recurring                  │
│ updated_at                    │      │ created_at                    │
└───────────────────────────────┘      │ updated_at                    │
                                       └───────────────────────────────┘
```

**DDL — transactions table:**
```sql
CREATE TABLE transactions (
    id                       VARCHAR(36) PRIMARY KEY,
    reference_number         VARCHAR(30) NOT NULL UNIQUE,
    transaction_type         ENUM('INTERNAL_TRANSFER', 'ACH_TRANSFER', 'WIRE_TRANSFER', 
                                  'BILL_PAYMENT', 'DEPOSIT', 'WITHDRAWAL', 'FEE', 
                                  'INTEREST_CREDIT', 'REFUND') NOT NULL,
    category                 ENUM('DEBIT', 'CREDIT') NOT NULL,
    source_account_id        VARCHAR(36),
    destination_account_id   VARCHAR(36),
    source_account_number    VARCHAR(20),
    destination_account_number VARCHAR(20),
    amount                   DECIMAL(15,2) NOT NULL,
    currency                 VARCHAR(3) DEFAULT 'USD',
    fee_amount               DECIMAL(15,2) DEFAULT 0.00,
    description              VARCHAR(500),
    memo                     VARCHAR(255),
    status                   ENUM('INITIATED', 'PENDING', 'PROCESSING', 'COMPLETED', 
                                  'FAILED', 'REVERSED', 'CANCELLED', 'ON_HOLD') DEFAULT 'INITIATED',
    failure_reason           VARCHAR(500),
    fraud_score              DECIMAL(5,4),
    fraud_checked            BOOLEAN DEFAULT FALSE,
    fraud_flagged            BOOLEAN DEFAULT FALSE,
    ip_address               VARCHAR(45),
    device_fingerprint       VARCHAR(255),
    geo_location             VARCHAR(100),
    initiated_at             TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    completed_at             TIMESTAMP NULL,
    created_at               TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at               TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_txn_ref (reference_number),
    INDEX idx_txn_source (source_account_id),
    INDEX idx_txn_dest (destination_account_id),
    INDEX idx_txn_status (status),
    INDEX idx_txn_type (transaction_type),
    INDEX idx_txn_date (initiated_at),
    INDEX idx_txn_fraud (fraud_flagged)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

---

#### 📦 LOAN-SERVICE (`vinusbank_loan`)

```
┌───────────────────────────────┐      ┌───────────────────────────────┐
│           loans                │      │      loan_repayments          │
├───────────────────────────────┤      ├───────────────────────────────┤
│ id (PK, UUID)                 │──┐   │ id (PK, BIGINT)              │
│ customer_id                   │  │   │ loan_id (FK)                 │
│ account_id                    │  ├──►│ installment_number           │
│ loan_number (UNIQUE)          │  │   │ due_date                     │
│ loan_type (ENUM)              │  │   │ principal_amount             │
│ principal_amount              │  │   │ interest_amount              │
│ interest_rate                 │  │   │ total_amount                 │
│ tenure_months                 │  │   │ paid_amount                  │
│ emi_amount                    │  │   │ paid_date                    │
│ total_interest                │  │   │ status (ENUM)                │
│ total_payable                 │  │   │ penalty_amount               │
│ outstanding_balance           │  │   │ transaction_id               │
│ disbursed_amount              │  │   │ created_at                   │
│ disbursed_at                  │  │   │ updated_at                   │
│ next_emi_date                 │  │   └───────────────────────────────┘
│ status (ENUM)                 │  │
│ purpose                       │  │   ┌───────────────────────────────┐
│ credit_score                  │  │   │   loan_documents              │
│ applied_at                    │  └──►├───────────────────────────────┤
│ approved_at                   │      │ id (PK, BIGINT)              │
│ approved_by                   │      │ loan_id (FK)                 │
│ rejected_reason               │      │ document_type                │
│ maturity_date                 │      │ file_path                    │
│ created_at                    │      │ uploaded_at                  │
│ updated_at                    │      │ created_at                   │
└───────────────────────────────┘      └───────────────────────────────┘
```

**DDL — loans table:**
```sql
CREATE TABLE loans (
    id                  VARCHAR(36) PRIMARY KEY,
    customer_id         VARCHAR(36) NOT NULL,
    account_id          VARCHAR(36) NOT NULL,
    loan_number         VARCHAR(20) NOT NULL UNIQUE,
    loan_type           ENUM('PERSONAL', 'HOME', 'AUTO', 'EDUCATION', 'BUSINESS') NOT NULL,
    principal_amount    DECIMAL(15,2) NOT NULL,
    interest_rate       DECIMAL(5,4) NOT NULL,
    tenure_months       INT NOT NULL,
    emi_amount          DECIMAL(15,2) NOT NULL,
    total_interest      DECIMAL(15,2) NOT NULL,
    total_payable       DECIMAL(15,2) NOT NULL,
    outstanding_balance DECIMAL(15,2) NOT NULL,
    disbursed_amount    DECIMAL(15,2) DEFAULT 0.00,
    disbursed_at        TIMESTAMP NULL,
    next_emi_date       DATE,
    status              ENUM('DRAFT', 'APPLIED', 'UNDER_REVIEW', 'APPROVED', 'REJECTED',
                             'DISBURSED', 'ACTIVE', 'OVERDUE', 'DEFAULTED', 'CLOSED', 'CANCELLED') DEFAULT 'DRAFT',
    purpose             VARCHAR(500),
    credit_score        INT,
    applied_at          TIMESTAMP NULL,
    approved_at         TIMESTAMP NULL,
    approved_by         VARCHAR(36),
    rejected_reason     VARCHAR(500),
    maturity_date       DATE,
    created_at          TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_loan_customer (customer_id),
    INDEX idx_loan_number (loan_number),
    INDEX idx_loan_status (status),
    INDEX idx_loan_type (loan_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

---

#### 📦 CARD-SERVICE (`vinusbank_card`)

```sql
CREATE TABLE cards (
    id                  VARCHAR(36) PRIMARY KEY,
    customer_id         VARCHAR(36) NOT NULL,
    account_id          VARCHAR(36) NOT NULL,
    card_number_encrypted VARBINARY(512) NOT NULL,
    card_number_last_four VARCHAR(4) NOT NULL,
    card_type           ENUM('DEBIT', 'VIRTUAL_DEBIT') NOT NULL,
    card_network        ENUM('VISA', 'MASTERCARD') DEFAULT 'VISA',
    cardholder_name     VARCHAR(100) NOT NULL,
    expiry_month        TINYINT NOT NULL,
    expiry_year         SMALLINT NOT NULL,
    cvv_hash            VARCHAR(255) NOT NULL,
    pin_hash            VARCHAR(255),
    daily_spend_limit   DECIMAL(15,2) DEFAULT 5000.00,
    monthly_spend_limit DECIMAL(15,2) DEFAULT 25000.00,
    daily_atm_limit     DECIMAL(15,2) DEFAULT 1000.00,
    international_enabled BOOLEAN DEFAULT FALSE,
    online_enabled      BOOLEAN DEFAULT TRUE,
    contactless_enabled BOOLEAN DEFAULT TRUE,
    status              ENUM('PENDING', 'ACTIVE', 'BLOCKED', 'EXPIRED', 'CANCELLED') DEFAULT 'PENDING',
    activated_at        TIMESTAMP NULL,
    blocked_at          TIMESTAMP NULL,
    block_reason        VARCHAR(255),
    created_at          TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_card_customer (customer_id),
    INDEX idx_card_account (account_id),
    INDEX idx_card_last4 (card_number_last_four),
    INDEX idx_card_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

---

#### 📦 COMPLIANCE-SERVICE (`vinusbank_compliance`)

```sql
CREATE TABLE kyc_reviews (
    id                  VARCHAR(36) PRIMARY KEY,
    customer_id         VARCHAR(36) NOT NULL,
    review_type         ENUM('INITIAL', 'PERIODIC', 'ENHANCED', 'TRIGGERED') NOT NULL,
    risk_level          ENUM('LOW', 'MEDIUM', 'HIGH', 'CRITICAL') NOT NULL,
    status              ENUM('PENDING', 'IN_PROGRESS', 'APPROVED', 'REJECTED', 'ESCALATED') DEFAULT 'PENDING',
    reviewer_id         VARCHAR(36),
    reviewer_notes      TEXT,
    due_date            DATE,
    completed_at        TIMESTAMP NULL,
    next_review_date    DATE,
    created_at          TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_kyc_customer (customer_id),
    INDEX idx_kyc_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE suspicious_activity_reports (
    id                  VARCHAR(36) PRIMARY KEY,
    sar_number          VARCHAR(30) NOT NULL UNIQUE,
    customer_id         VARCHAR(36) NOT NULL,
    transaction_ids     JSON,
    activity_type       ENUM('STRUCTURING', 'UNUSUAL_PATTERN', 'LARGE_CASH', 
                             'TERRORIST_FINANCING', 'IDENTITY_FRAUD', 'OTHER') NOT NULL,
    description         TEXT NOT NULL,
    amount_involved     DECIMAL(15,2),
    date_range_start    DATE,
    date_range_end      DATE,
    status              ENUM('DRAFT', 'SUBMITTED', 'UNDER_REVIEW', 'FILED', 'CLOSED') DEFAULT 'DRAFT',
    filed_by            VARCHAR(36),
    filed_at            TIMESTAMP NULL,
    fincen_reference    VARCHAR(50),
    created_at          TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_sar_customer (customer_id),
    INDEX idx_sar_status (status),
    INDEX idx_sar_number (sar_number)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE currency_transaction_reports (
    id                  VARCHAR(36) PRIMARY KEY,
    ctr_number          VARCHAR(30) NOT NULL UNIQUE,
    customer_id         VARCHAR(36) NOT NULL,
    transaction_id      VARCHAR(36) NOT NULL,
    amount              DECIMAL(15,2) NOT NULL,
    transaction_date    DATE NOT NULL,
    status              ENUM('AUTO_GENERATED', 'REVIEWED', 'FILED') DEFAULT 'AUTO_GENERATED',
    filed_at            TIMESTAMP NULL,
    created_at          TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_ctr_customer (customer_id),
    INDEX idx_ctr_transaction (transaction_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE ofac_screening_results (
    id                  VARCHAR(36) PRIMARY KEY,
    customer_id         VARCHAR(36) NOT NULL,
    screening_type      ENUM('ONBOARDING', 'PERIODIC', 'TRANSACTION') NOT NULL,
    match_found         BOOLEAN DEFAULT FALSE,
    match_score         DECIMAL(5,4),
    matched_entity      VARCHAR(255),
    matched_list        VARCHAR(100),
    status              ENUM('CLEAR', 'POTENTIAL_MATCH', 'CONFIRMED_MATCH', 'FALSE_POSITIVE') DEFAULT 'CLEAR',
    reviewed_by         VARCHAR(36),
    reviewed_at         TIMESTAMP NULL,
    created_at          TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_ofac_customer (customer_id),
    INDEX idx_ofac_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE audit_trail (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id             VARCHAR(36),
    action              VARCHAR(100) NOT NULL,
    resource_type       VARCHAR(100) NOT NULL,
    resource_id         VARCHAR(36),
    old_value           JSON,
    new_value           JSON,
    ip_address          VARCHAR(45),
    user_agent          VARCHAR(500),
    service_name        VARCHAR(100),
    created_at          TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_audit_user (user_id),
    INDEX idx_audit_resource (resource_type, resource_id),
    INDEX idx_audit_action (action),
    INDEX idx_audit_date (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

---

#### 📦 NOTIFICATION-SERVICE (`vinusbank_notification`)

```sql
CREATE TABLE notifications (
    id                  VARCHAR(36) PRIMARY KEY,
    user_id             VARCHAR(36) NOT NULL,
    type                ENUM('EMAIL', 'SMS', 'IN_APP', 'PUSH') NOT NULL,
    category            ENUM('TRANSACTION', 'SECURITY', 'ACCOUNT', 'LOAN', 'COMPLIANCE', 
                             'MARKETING', 'SYSTEM') NOT NULL,
    title               VARCHAR(255) NOT NULL,
    message             TEXT NOT NULL,
    data_payload        JSON,
    is_read             BOOLEAN DEFAULT FALSE,
    read_at             TIMESTAMP NULL,
    sent_at             TIMESTAMP NULL,
    delivery_status     ENUM('PENDING', 'SENT', 'DELIVERED', 'FAILED', 'BOUNCED') DEFAULT 'PENDING',
    failure_reason      VARCHAR(500),
    retry_count         INT DEFAULT 0,
    created_at          TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_notif_user (user_id),
    INDEX idx_notif_type (type),
    INDEX idx_notif_read (user_id, is_read),
    INDEX idx_notif_date (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE notification_preferences (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id             VARCHAR(36) NOT NULL,
    category            VARCHAR(50) NOT NULL,
    email_enabled       BOOLEAN DEFAULT TRUE,
    sms_enabled         BOOLEAN DEFAULT FALSE,
    in_app_enabled      BOOLEAN DEFAULT TRUE,
    push_enabled        BOOLEAN DEFAULT TRUE,
    created_at          TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_user_category (user_id, category)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

---

## 2. API Design

### 2.1 API Conventions
- **Base URL:** `http://localhost:8080/api/v1`
- **Auth Header:** `Authorization: Bearer <JWT_TOKEN>`
- **Content-Type:** `application/json`
- **Date Format:** ISO 8601 (`2026-04-11T13:00:00Z`)
- **Pagination:** `?page=0&size=20&sort=createdAt,desc`

### 2.2 Standard Response Envelope

```json
{
  "success": true,
  "status": 200,
  "message": "Operation completed successfully",
  "data": { },
  "timestamp": "2026-04-11T13:00:00Z",
  "path": "/api/v1/accounts"
}
```

**Error Response:**
```json
{
  "success": false,
  "status": 400,
  "message": "Validation failed",
  "errors": [
    {
      "field": "email",
      "message": "Email is required"
    }
  ],
  "timestamp": "2026-04-11T13:00:00Z",
  "path": "/api/v1/auth/register",
  "traceId": "abc-123-def"
}
```

---

### 2.3 API Endpoint Catalog

#### 🔐 Auth Service APIs

| Method | Endpoint | Description | Auth |
|--------|----------|-------------|------|
| POST | `/api/v1/auth/register` | Register new user | ❌ |
| POST | `/api/v1/auth/login` | Login with credentials | ❌ |
| POST | `/api/v1/auth/refresh-token` | Refresh JWT token | ❌ |
| POST | `/api/v1/auth/logout` | Logout & revoke token | ✅ |
| POST | `/api/v1/auth/forgot-password` | Request password reset | ❌ |
| POST | `/api/v1/auth/reset-password` | Reset password with token | ❌ |
| POST | `/api/v1/auth/verify-email` | Verify email address | ❌ |
| POST | `/api/v1/auth/mfa/enable` | Enable MFA | ✅ |
| POST | `/api/v1/auth/mfa/verify` | Verify MFA code | ✅ |
| GET | `/api/v1/auth/me` | Get current user info | ✅ |

**Register Request:**
```json
POST /api/v1/auth/register
{
  "email": "john.doe@email.com",
  "password": "SecureP@ss123!",
  "confirmPassword": "SecureP@ss123!",
  "phoneNumber": "+12025551234",
  "firstName": "John",
  "lastName": "Doe",
  "dateOfBirth": "1990-05-15",
  "ssn": "123-45-6789",
  "acceptTerms": true
}
```

**Login Response:**
```json
{
  "success": true,
  "data": {
    "accessToken": "eyJhbGci...",
    "refreshToken": "dGhpcyBpcyBh...",
    "tokenType": "Bearer",
    "expiresIn": 900,
    "user": {
      "id": "uuid-here",
      "email": "john.doe@email.com",
      "roles": ["ROLE_CUSTOMER"],
      "mfaEnabled": false,
      "kycStatus": "PENDING"
    }
  }
}
```

---

#### 👤 Customer Service APIs

| Method | Endpoint | Description | Auth | Roles |
|--------|----------|-------------|------|-------|
| GET | `/api/v1/customers/me` | Get my profile | ✅ | CUSTOMER |
| PUT | `/api/v1/customers/me` | Update my profile | ✅ | CUSTOMER |
| POST | `/api/v1/customers/me/address` | Add address | ✅ | CUSTOMER |
| POST | `/api/v1/customers/me/kyc/documents` | Upload KYC document | ✅ | CUSTOMER |
| GET | `/api/v1/customers/me/kyc/status` | Get KYC status | ✅ | CUSTOMER |
| GET | `/api/v1/customers/{id}` | Get customer by ID | ✅ | STAFF, ADMIN |
| GET | `/api/v1/customers` | Search customers | ✅ | STAFF, ADMIN |
| PUT | `/api/v1/customers/{id}/risk-rating` | Update risk rating | ✅ | COMPLIANCE |

---

#### 🏦 Account Service APIs

| Method | Endpoint | Description | Auth | Roles |
|--------|----------|-------------|------|-------|
| POST | `/api/v1/accounts` | Open new account | ✅ | CUSTOMER |
| GET | `/api/v1/accounts` | Get my accounts | ✅ | CUSTOMER |
| GET | `/api/v1/accounts/{id}` | Get account details | ✅ | CUSTOMER |
| GET | `/api/v1/accounts/{id}/balance` | Get account balance | ✅ | CUSTOMER |
| GET | `/api/v1/accounts/{id}/statements` | Get account statements | ✅ | CUSTOMER |
| POST | `/api/v1/accounts/{id}/freeze` | Freeze account | ✅ | STAFF, ADMIN |
| POST | `/api/v1/accounts/{id}/unfreeze` | Unfreeze account | ✅ | STAFF, ADMIN |
| POST | `/api/v1/accounts/{id}/close` | Request account closure | ✅ | CUSTOMER |

**Open Account Request:**
```json
POST /api/v1/accounts
{
  "accountType": "CHECKING",
  "initialDeposit": 500.00,
  "currency": "USD"
}
```

**Account Response:**
```json
{
  "success": true,
  "data": {
    "id": "acc-uuid",
    "accountNumber": "1001234567",
    "accountType": "CHECKING",
    "availableBalance": 500.00,
    "currentBalance": 500.00,
    "holdAmount": 0.00,
    "interestRate": 0.0050,
    "status": "ACTIVE",
    "currency": "USD",
    "openedAt": "2026-04-11T13:00:00Z",
    "dailyTransferLimit": 10000.00
  }
}
```

---

#### 💸 Transaction Service APIs

| Method | Endpoint | Description | Auth | Roles |
|--------|----------|-------------|------|-------|
| POST | `/api/v1/transactions/transfer` | Make a transfer | ✅ | CUSTOMER |
| POST | `/api/v1/transactions/bill-payment` | Pay a bill | ✅ | CUSTOMER |
| GET | `/api/v1/transactions` | Get transaction history | ✅ | CUSTOMER |
| GET | `/api/v1/transactions/{id}` | Get transaction details | ✅ | CUSTOMER |
| GET | `/api/v1/transactions/{id}/receipt` | Download receipt | ✅ | CUSTOMER |
| POST | `/api/v1/transactions/scheduled` | Schedule a transfer | ✅ | CUSTOMER |
| GET | `/api/v1/transactions/scheduled` | Get scheduled transfers | ✅ | CUSTOMER |
| DELETE | `/api/v1/transactions/scheduled/{id}` | Cancel scheduled transfer | ✅ | CUSTOMER |

**Transfer Request:**
```json
POST /api/v1/transactions/transfer
{
  "sourceAccountId": "acc-uuid-1",
  "destinationAccountNumber": "1001234568",
  "amount": 250.00,
  "description": "Rent payment",
  "memo": "April 2026 rent",
  "transferType": "INTERNAL_TRANSFER"
}
```

**Transfer Response:**
```json
{
  "success": true,
  "data": {
    "transactionId": "txn-uuid",
    "referenceNumber": "VUB2026041100001",
    "status": "COMPLETED",
    "amount": 250.00,
    "fee": 0.00,
    "sourceAccountNumber": "10012****23",
    "destinationAccountNumber": "10012****68",
    "completedAt": "2026-04-11T13:01:00Z"
  }
}
```

---

#### 🏠 Loan Service APIs

| Method | Endpoint | Description | Auth | Roles |
|--------|----------|-------------|------|-------|
| POST | `/api/v1/loans/calculate` | Calculate EMI | ✅ | CUSTOMER |
| POST | `/api/v1/loans/apply` | Apply for a loan | ✅ | CUSTOMER |
| GET | `/api/v1/loans` | Get my loans | ✅ | CUSTOMER |
| GET | `/api/v1/loans/{id}` | Get loan details | ✅ | CUSTOMER |
| GET | `/api/v1/loans/{id}/schedule` | Get amortization schedule | ✅ | CUSTOMER |
| POST | `/api/v1/loans/{id}/repay` | Make EMI payment | ✅ | CUSTOMER |
| PUT | `/api/v1/loans/{id}/approve` | Approve loan | ✅ | STAFF, ADMIN |
| PUT | `/api/v1/loans/{id}/reject` | Reject loan | ✅ | STAFF, ADMIN |

---

#### 💳 Card Service APIs

| Method | Endpoint | Description | Auth | Roles |
|--------|----------|-------------|------|-------|
| POST | `/api/v1/cards` | Request new card | ✅ | CUSTOMER |
| GET | `/api/v1/cards` | Get my cards | ✅ | CUSTOMER |
| GET | `/api/v1/cards/{id}` | Get card details | ✅ | CUSTOMER |
| POST | `/api/v1/cards/{id}/activate` | Activate card | ✅ | CUSTOMER |
| POST | `/api/v1/cards/{id}/block` | Block card | ✅ | CUSTOMER |
| POST | `/api/v1/cards/{id}/unblock` | Unblock card | ✅ | CUSTOMER |
| PUT | `/api/v1/cards/{id}/limits` | Update card limits | ✅ | CUSTOMER |
| PUT | `/api/v1/cards/{id}/settings` | Update card settings | ✅ | CUSTOMER |

---

#### 📋 Compliance Service APIs

| Method | Endpoint | Description | Auth | Roles |
|--------|----------|-------------|------|-------|
| GET | `/api/v1/compliance/kyc/pending` | Get pending KYC reviews | ✅ | COMPLIANCE |
| PUT | `/api/v1/compliance/kyc/{id}/approve` | Approve KYC | ✅ | COMPLIANCE |
| PUT | `/api/v1/compliance/kyc/{id}/reject` | Reject KYC | ✅ | COMPLIANCE |
| GET | `/api/v1/compliance/sar` | List SARs | ✅ | COMPLIANCE |
| POST | `/api/v1/compliance/sar` | Create SAR | ✅ | COMPLIANCE |
| PUT | `/api/v1/compliance/sar/{id}/file` | File SAR with FinCEN | ✅ | COMPLIANCE |
| GET | `/api/v1/compliance/ctr` | List CTRs | ✅ | COMPLIANCE |
| GET | `/api/v1/compliance/audit-trail` | Search audit trail | ✅ | ADMIN |
| POST | `/api/v1/compliance/ofac/screen` | Run OFAC screening | ✅ | COMPLIANCE |

---

#### 🔔 Notification Service APIs

| Method | Endpoint | Description | Auth | Roles |
|--------|----------|-------------|------|-------|
| GET | `/api/v1/notifications` | Get my notifications | ✅ | ALL |
| GET | `/api/v1/notifications/unread-count` | Get unread count | ✅ | ALL |
| PUT | `/api/v1/notifications/{id}/read` | Mark as read | ✅ | ALL |
| PUT | `/api/v1/notifications/read-all` | Mark all as read | ✅ | ALL |
| GET | `/api/v1/notifications/preferences` | Get notification prefs | ✅ | ALL |
| PUT | `/api/v1/notifications/preferences` | Update notification prefs | ✅ | ALL |

---

#### 🤖 AI Service APIs

| Method | Endpoint | Description | Auth | Roles |
|--------|----------|-------------|------|-------|
| POST | `/api/v1/ai/chat` | Send message to chatbot | ✅ | CUSTOMER |
| GET | `/api/v1/ai/chat/history` | Get chat history | ✅ | CUSTOMER |
| POST | `/api/v1/ai/chat/feedback` | Rate chat response | ✅ | CUSTOMER |
| GET | `/api/v1/ai/insights/spending` | Get spending insights | ✅ | CUSTOMER |
| GET | `/api/v1/ai/insights/savings` | Get savings recommendations | ✅ | CUSTOMER |
| GET | `/api/v1/ai/fraud/alerts` | Get fraud alerts | ✅ | CUSTOMER, STAFF |
| POST | `/api/v1/ai/fraud/check` | Manual fraud check | ✅ | STAFF |

**Chat Request:**
```json
POST /api/v1/ai/chat
{
  "message": "What is my checking account balance?",
  "sessionId": "session-uuid",
  "context": {
    "currentPage": "dashboard"
  }
}
```

**Chat Response:**
```json
{
  "success": true,
  "data": {
    "response": "Your checking account (****4567) has an available balance of $2,450.00. Your current balance is $2,500.00 with $50.00 on hold.",
    "sessionId": "session-uuid",
    "intent": "BALANCE_INQUIRY",
    "confidence": 0.95,
    "suggestedActions": [
      {"label": "View Statement", "action": "NAVIGATE", "target": "/accounts/acc-uuid/statements"},
      {"label": "Make Transfer", "action": "NAVIGATE", "target": "/transfers/new"}
    ],
    "timestamp": "2026-04-11T13:00:00Z"
  }
}
```

---

## 3. Class Diagrams (Key Service Internals)

### 3.1 Layered Architecture per Microservice

```
┌──────────────────────────────────────────────────────────┐
│                    Each Microservice                      │
├──────────────────────────────────────────────────────────┤
│                                                           │
│  ┌─────────────────────────────────────────────────────┐ │
│  │                 Controller Layer                     │ │
│  │  @RestController — Request handling, validation      │ │
│  │  AccountController, TransactionController, etc.      │ │
│  └───────────────────────┬─────────────────────────────┘ │
│                          │                                │
│  ┌───────────────────────▼─────────────────────────────┐ │
│  │                  Service Layer                       │ │
│  │  @Service — Business logic, orchestration            │ │
│  │  AccountService, TransactionService, etc.            │ │
│  └───────────────────────┬─────────────────────────────┘ │
│                          │                                │
│  ┌───────────────────────▼─────────────────────────────┐ │
│  │               Repository Layer                       │ │
│  │  @Repository — Data access, JPA queries              │ │
│  │  AccountRepository, TransactionRepository, etc.      │ │
│  └───────────────────────┬─────────────────────────────┘ │
│                          │                                │
│  ┌───────────────────────▼─────────────────────────────┐ │
│  │                Entity/Model Layer                    │ │
│  │  @Entity — JPA entities, domain objects              │ │
│  │  Account, Transaction, Loan, etc.                    │ │
│  └─────────────────────────────────────────────────────┘ │
│                                                           │
│  ┌─────────────────────────────────────────────────────┐ │
│  │              Cross-Cutting Concerns                  │ │
│  │  DTOs, Mappers, Exceptions, Config, Security,       │ │
│  │  Kafka Producers/Consumers, AuditAspect              │ │
│  └─────────────────────────────────────────────────────┘ │
│                                                           │
└──────────────────────────────────────────────────────────┘
```

### 3.2 Package Structure per Microservice

```
com.vinusbank.{service-name}
├── VinUSBankXxxApplication.java          // Spring Boot main
├── config/
│   ├── SecurityConfig.java              // Security configuration
│   ├── KafkaConfig.java                 // Kafka producer/consumer config
│   ├── RedisConfig.java                 // Redis cache config
│   └── SwaggerConfig.java              // OpenAPI documentation
├── controller/
│   └── XxxController.java              // REST controllers
├── service/
│   ├── XxxService.java                  // Service interface
│   └── impl/
│       └── XxxServiceImpl.java          // Service implementation
├── repository/
│   └── XxxRepository.java              // JPA repositories
├── entity/
│   └── Xxx.java                         // JPA entities
├── dto/
│   ├── request/
│   │   └── XxxRequest.java             // Request DTOs
│   └── response/
│       └── XxxResponse.java             // Response DTOs
├── mapper/
│   └── XxxMapper.java                   // Entity ↔ DTO mappers
├── exception/
│   ├── XxxException.java               // Custom exceptions
│   └── GlobalExceptionHandler.java     // @ControllerAdvice
├── event/
│   ├── producer/
│   │   └── XxxEventProducer.java       // Kafka producers
│   └── consumer/
│       └── XxxEventConsumer.java       // Kafka consumers
├── audit/
│   └── AuditAspect.java                // AOP audit logging
├── util/
│   └── XxxUtil.java                     // Utility classes
└── validation/
    └── XxxValidator.java               // Custom validators
```

### 3.3 Transaction Service — Detailed Class Design

```
┌──────────────────────────────────────────────┐
│            TransactionController              │
├──────────────────────────────────────────────┤
│ + makeTransfer(TransferRequest): ApiResponse │
│ + payBill(BillPaymentRequest): ApiResponse   │
│ + getHistory(filters): ApiResponse<Page>     │
│ + getById(id): ApiResponse                   │
│ + getReceipt(id): ResponseEntity<byte[]>     │
└─────────────────────┬────────────────────────┘
                      │ calls
┌─────────────────────▼────────────────────────┐
│            TransactionService (Interface)     │
├──────────────────────────────────────────────┤
│ + transfer(TransferRequest): TransferResponse│
│ + payBill(BillPayReq): BillPayResponse       │
│ + getHistory(userId, filters): Page<TxnDTO>  │
│ + getById(id): TransactionDetailDTO          │
│ + generateReceipt(id): byte[]                │
└─────────────────────┬────────────────────────┘
                      │ implements
┌─────────────────────▼────────────────────────┐
│         TransactionServiceImpl                │
├──────────────────────────────────────────────┤
│ - transactionRepo: TransactionRepository     │
│ - accountClient: AccountServiceClient        │
│ - fraudClient: FraudDetectionClient          │
│ - kafkaProducer: TransactionEventProducer    │
│ - idGenerator: ReferenceNumberGenerator      │
├──────────────────────────────────────────────┤
│ + transfer(req):                              │
│   1. Validate source account ownership        │
│   2. Check daily limit                        │
│   3. Verify sufficient balance                │
│   4. Call fraud detection service              │
│   5. If fraud_score > threshold → HOLD        │
│   6. Debit source account                     │
│   7. Credit destination account               │
│   8. Save transaction record                  │
│   9. Publish txn.created event                │
│   10. Return response                         │
└──────────────────────────────────────────────┘
```

---

## 4. Inter-Service Communication Flows

### 4.1 Fund Transfer Flow (Sequence)

```
Customer     Angular      API Gateway    Transaction    Account      Fraud        Kafka       Notification
   │           │              │           Service       Service    Detection       │            Service
   │──────────►│              │              │             │           │            │              │
   │  Click    │──────────────►              │             │           │            │              │
   │  Transfer │   POST /transfer            │             │           │            │              │
   │           │              │──────────────►             │           │            │              │
   │           │              │  Route req   │             │           │            │              │
   │           │              │              │──────────────────────────►            │              │
   │           │              │              │  POST /fraud/check      │            │              │
   │           │              │              │◄─────────────────────────            │              │
   │           │              │              │  {score: 0.12}          │            │              │
   │           │              │              │             │           │            │              │
   │           │              │              │──────────────►          │            │              │
   │           │              │              │  POST /debit │          │            │              │
   │           │              │              │◄─────────────           │            │              │
   │           │              │              │  OK          │          │            │              │
   │           │              │              │──────────────►          │            │              │
   │           │              │              │  POST /credit│          │            │              │
   │           │              │              │◄─────────────           │            │              │
   │           │              │              │  OK          │          │            │              │
   │           │              │              │             │           │            │              │
   │           │              │              │─────────────────────────────────────►│              │
   │           │              │              │  Publish: txn.completed │            │              │
   │           │              │              │             │           │            │──────────────►│
   │           │              │              │             │           │            │  Consume      │
   │           │              │◄─────────────              │           │            │              │
   │           │◄─────────────               │             │           │            │     Send     │
   │◄──────────│  Transfer Complete          │             │           │            │     Email    │
   │           │              │              │             │           │            │              │
```

### 4.2 User Registration & KYC Flow

```
1. Customer submits registration form
2. auth-service creates user with PENDING status
3. customer-service creates customer profile
4. Customer uploads KYC documents
5. compliance-service creates kyc_review (PENDING)
6. Compliance officer reviews documents
7. compliance-service publishes kyc.updated event
8. customer-service updates kyc_status → VERIFIED
9. account-service activates pending accounts
10. notification-service sends welcome email
```

### 4.3 Fraud Detection Flow

```
1. Transaction initiated
2. transaction-service calls fraud-detection-service (sync REST)
3. fraud-detection-service:
   a. Extracts features (amount, time, location, frequency, device)
   b. Runs ML model inference
   c. Returns fraud_score (0.0 - 1.0)
4. If score < 0.3 → APPROVE transaction
5. If score 0.3 - 0.7 → FLAG for manual review, process with hold
6. If score > 0.7 → BLOCK transaction
7. Publish fraud.alert event to Kafka
8. compliance-service picks up for investigation
9. notification-service alerts customer
```

---

## 5. AI Bot Detailed Design

### 5.1 Chatbot Architecture

```
┌────────────────────────────────────────────────────────────┐
│                   CHATBOT SERVICE (Python)                   │
├────────────────────────────────────────────────────────────┤
│                                                             │
│  ┌──────────────┐    ┌──────────────────────────────────┐  │
│  │  FastAPI      │    │     LangChain Agent               │  │
│  │  Router       │───►│                                    │  │
│  │  /api/v1/     │    │  ┌─────────────────────────────┐  │  │
│  │  ai/chat      │    │  │   Intent Classifier          │  │  │
│  └──────────────┘    │  │   (Pre-LLM fast routing)     │  │  │
│                      │  └──────────┬──────────────────┘  │  │
│                      │             │                      │  │
│                      │  ┌──────────▼──────────────────┐  │  │
│                      │  │   LLM (OpenAI / Ollama)      │  │  │
│                      │  │   with Banking System Prompt  │  │  │
│                      │  └──────────┬──────────────────┘  │  │
│                      │             │                      │  │
│                      │  ┌──────────▼──────────────────┐  │  │
│                      │  │   Tool Functions              │  │  │
│                      │  │   - check_balance()           │  │  │
│                      │  │   - get_transactions()        │  │  │
│                      │  │   - transfer_funds()          │  │  │
│                      │  │   - get_loan_info()           │  │  │
│                      │  │   - get_card_info()           │  │  │
│                      │  │   - faq_lookup()              │  │  │
│                      │  └──────────┬──────────────────┘  │  │
│                      │             │                      │  │
│                      │  ┌──────────▼──────────────────┐  │  │
│                      │  │   Response Guardrails         │  │  │
│                      │  │   - PII masking               │  │  │
│                      │  │   - Financial advice filter   │  │  │
│                      │  │   - Hallucination check       │  │  │
│                      │  └─────────────────────────────┘  │  │
│                      └──────────────────────────────────┘  │
│                                                             │
│  ┌──────────────────────────────────────────────────────┐  │
│  │                 Conversation Memory                    │  │
│  │   Redis-backed session storage                        │  │
│  │   Maintains last N messages per session               │  │
│  └──────────────────────────────────────────────────────┘  │
│                                                             │
└────────────────────────────────────────────────────────────┘
```

**Chatbot Tool Functions (LangChain):**

| Tool Name | Description | Calls Service |
|-----------|-------------|---------------|
| `check_balance` | Get account balance for user | account-service |
| `get_recent_transactions` | Get last N transactions | transaction-service |
| `get_account_details` | Get account info | account-service |
| `get_loan_status` | Get loan details | loan-service |
| `get_card_info` | Get card details | card-service |
| `calculate_emi` | Calculate EMI for loan | loan-service |
| `get_notification_count` | Get unread notifications | notification-service |
| `faq_lookup` | Search banking FAQs | local vector store |
| `escalate_to_human` | Escalate to human agent | notification-service |

### 5.2 Fraud Detection ML Service

```
┌────────────────────────────────────────────────────────────┐
│              FRAUD DETECTION SERVICE (Python)                │
├────────────────────────────────────────────────────────────┤
│                                                             │
│  ┌──────────────┐    ┌──────────────────────────────────┐  │
│  │  FastAPI      │    │     Feature Engineering           │  │
│  │  POST /check  │───►│                                    │  │
│  └──────────────┘    │  Input Features:                   │  │
│                      │  - Transaction amount               │  │
│                      │  - Time of day / day of week        │  │
│                      │  - Transaction frequency (last 1h)  │  │
│                      │  - Average transaction amount       │  │
│                      │  - Distance from usual location     │  │
│                      │  - New device flag                   │  │
│                      │  - Merchant category                 │  │
│                      │  - Account age                      │  │
│                      │  - Previous fraud flags             │  │
│                      └──────────────┬───────────────────┘  │
│                                     │                       │
│                      ┌──────────────▼───────────────────┐  │
│                      │     ML Model (Random Forest /     │  │
│                      │     Gradient Boosting)             │  │
│                      │     Pre-trained on synthetic data  │  │
│                      └──────────────┬───────────────────┘  │
│                                     │                       │
│                      ┌──────────────▼───────────────────┐  │
│                      │     Risk Score Output              │  │
│                      │     0.0 (safe) → 1.0 (fraud)      │  │
│                      │     + explanation factors           │  │
│                      └──────────────────────────────────┘  │
│                                                             │
│  ┌──────────────────────────────────────────────────────┐  │
│  │              Model Retraining Pipeline                 │  │
│  │  - Kafka consumer for confirmed fraud data            │  │
│  │  - Periodic batch retraining                          │  │
│  │  - A/B testing of model versions                      │  │
│  └──────────────────────────────────────────────────────┘  │
│                                                             │
└────────────────────────────────────────────────────────────┘
```

### 5.3 Financial Advisor Bot

```
┌────────────────────────────────────────────────────────────┐
│              FINANCIAL ADVISOR SERVICE (Python)              │
├────────────────────────────────────────────────────────────┤
│                                                             │
│  Analysis Modules:                                          │
│                                                             │
│  ┌─────────────────────────────────────────────────────┐   │
│  │  1. SPENDING ANALYSIS                                │   │
│  │     - Category-wise spending breakdown               │   │
│  │     - Month-over-month comparison                    │   │
│  │     - Unusual spending detection                     │   │
│  │     - Top merchants / categories                     │   │
│  └─────────────────────────────────────────────────────┘   │
│                                                             │
│  ┌─────────────────────────────────────────────────────┐   │
│  │  2. SAVINGS RECOMMENDATIONS                          │   │
│  │     - Recurring subscription detection               │   │
│  │     - Spending optimization tips                     │   │
│  │     - Savings goal tracking                          │   │
│  │     - Budget suggestions based on income             │   │
│  └─────────────────────────────────────────────────────┘   │
│                                                             │
│  ┌─────────────────────────────────────────────────────┐   │
│  │  3. FINANCIAL HEALTH SCORE                           │   │
│  │     - Income vs expense ratio                        │   │
│  │     - Savings rate                                    │   │
│  │     - Loan-to-income ratio                           │   │
│  │     - Account diversity score                         │   │
│  │     - Combined health score (0-100)                   │   │
│  └─────────────────────────────────────────────────────┘   │
│                                                             │
└────────────────────────────────────────────────────────────┘
```

---

## 6. Angular Frontend Design

### 6.1 Module Structure

```
vinusbank-frontend/
├── src/
│   ├── app/
│   │   ├── core/                          // Singleton services
│   │   │   ├── guards/
│   │   │   │   ├── auth.guard.ts
│   │   │   │   └── role.guard.ts
│   │   │   ├── interceptors/
│   │   │   │   ├── auth.interceptor.ts   // Attach JWT
│   │   │   │   ├── error.interceptor.ts  // Global error handling
│   │   │   │   └── loading.interceptor.ts
│   │   │   ├── services/
│   │   │   │   ├── auth.service.ts
│   │   │   │   ├── api.service.ts
│   │   │   │   ├── websocket.service.ts
│   │   │   │   └── storage.service.ts
│   │   │   └── models/
│   │   │       ├── user.model.ts
│   │   │       ├── account.model.ts
│   │   │       └── api-response.model.ts
│   │   │
│   │   ├── shared/                        // Shared components
│   │   │   ├── components/
│   │   │   │   ├── navbar/
│   │   │   │   ├── sidebar/
│   │   │   │   ├── footer/
│   │   │   │   ├── loading-spinner/
│   │   │   │   ├── data-table/
│   │   │   │   ├── confirmation-dialog/
│   │   │   │   └── toast-notification/
│   │   │   ├── pipes/
│   │   │   │   ├── currency-format.pipe.ts
│   │   │   │   ├── account-mask.pipe.ts
│   │   │   │   └── date-ago.pipe.ts
│   │   │   └── directives/
│   │   │       └── has-role.directive.ts
│   │   │
│   │   ├── features/                      // Feature modules
│   │   │   ├── auth/
│   │   │   │   ├── login/
│   │   │   │   ├── register/
│   │   │   │   ├── forgot-password/
│   │   │   │   └── mfa-setup/
│   │   │   │
│   │   │   ├── dashboard/
│   │   │   │   ├── dashboard.component.ts
│   │   │   │   ├── widgets/
│   │   │   │   │   ├── balance-overview/
│   │   │   │   │   ├── recent-transactions/
│   │   │   │   │   ├── spending-chart/
│   │   │   │   │   ├── quick-actions/
│   │   │   │   │   └── ai-insights/
│   │   │   │   └── dashboard.module.ts
│   │   │   │
│   │   │   ├── accounts/
│   │   │   │   ├── account-list/
│   │   │   │   ├── account-detail/
│   │   │   │   ├── open-account/
│   │   │   │   └── account-statements/
│   │   │   │
│   │   │   ├── transactions/
│   │   │   │   ├── transaction-history/
│   │   │   │   ├── make-transfer/
│   │   │   │   ├── bill-payment/
│   │   │   │   ├── scheduled-transfers/
│   │   │   │   └── transaction-detail/
│   │   │   │
│   │   │   ├── loans/
│   │   │   │   ├── loan-list/
│   │   │   │   ├── loan-apply/
│   │   │   │   ├── loan-detail/
│   │   │   │   ├── emi-calculator/
│   │   │   │   └── amortization-schedule/
│   │   │   │
│   │   │   ├── cards/
│   │   │   │   ├── card-list/
│   │   │   │   ├── card-detail/
│   │   │   │   └── card-settings/
│   │   │   │
│   │   │   ├── chatbot/
│   │   │   │   ├── chat-widget/          // Floating chat button
│   │   │   │   ├── chat-window/          // Full chat interface
│   │   │   │   └── chat.service.ts
│   │   │   │
│   │   │   ├── notifications/
│   │   │   │   ├── notification-center/
│   │   │   │   └── notification-preferences/
│   │   │   │
│   │   │   ├── profile/
│   │   │   │   ├── view-profile/
│   │   │   │   ├── edit-profile/
│   │   │   │   ├── kyc-upload/
│   │   │   │   └── security-settings/
│   │   │   │
│   │   │   └── admin/                    // Admin-only module
│   │   │       ├── user-management/
│   │   │       ├── compliance-dashboard/
│   │   │       ├── kyc-review/
│   │   │       ├── sar-management/
│   │   │       └── audit-logs/
│   │   │
│   │   ├── app-routing.module.ts
│   │   ├── app.component.ts
│   │   └── app.module.ts
│   │
│   ├── assets/
│   ├── environments/
│   └── styles/
│       ├── _variables.scss
│       ├── _mixins.scss
│       ├── _typography.scss
│       └── styles.scss
```

### 6.2 Key Angular Pages

| Page | Route | Description |
|------|-------|-------------|
| Login | `/auth/login` | Email/password login with MFA |
| Register | `/auth/register` | Multi-step registration |
| Dashboard | `/dashboard` | Financial overview with widgets |
| Accounts | `/accounts` | Account list with balances |
| Account Detail | `/accounts/:id` | Account details & mini-statement |
| Transfer | `/transactions/transfer` | Fund transfer form |
| Bill Payment | `/transactions/bill-payment` | Bill payment wizard |
| Transaction History | `/transactions/history` | Filterable transaction list |
| Loan Apply | `/loans/apply` | Loan application wizard |
| Loan Detail | `/loans/:id` | Loan details + amortization |
| Cards | `/cards` | Card management |
| Chatbot | `/chat` | Full-page AI chatbot |
| Profile | `/profile` | User profile & KYC |
| Admin Dashboard | `/admin` | Compliance & admin tools |

---

## 7. Caching Strategy

| Data | Cache Location | TTL | Invalidation |
|------|---------------|-----|--------------|
| User session | Redis | 15 min (access token) | On logout |
| Account balance | Redis | 30 seconds | On transaction |
| Customer profile | Redis | 5 minutes | On profile update |
| Transaction history | Redis | 1 minute | On new transaction |
| Notification count | Redis | 30 seconds | On new notification |
| OFAC list | Redis | 24 hours | Manual refresh |
| Chatbot session | Redis | 30 minutes | On session end |

---

## 8. Error Handling Strategy

### 8.1 Exception Hierarchy

```
BaseException (abstract)
├── AuthException
│   ├── InvalidCredentialsException
│   ├── TokenExpiredException
│   ├── AccountLockedException
│   └── MfaRequiredException
├── BusinessException
│   ├── InsufficientFundsException
│   ├── DailyLimitExceededException
│   ├── AccountNotActiveException
│   ├── DuplicateTransactionException
│   └── KycNotVerifiedException
├── ResourceNotFoundException
├── ValidationException
└── ExternalServiceException
    ├── FraudServiceUnavailableException
    └── NotificationDeliveryException
```

### 8.2 HTTP Status Code Mapping

| Exception | HTTP Status | Error Code |
|-----------|------------|------------|
| ValidationException | 400 | `VALIDATION_ERROR` |
| InvalidCredentialsException | 401 | `INVALID_CREDENTIALS` |
| TokenExpiredException | 401 | `TOKEN_EXPIRED` |
| ForbiddenException | 403 | `ACCESS_DENIED` |
| ResourceNotFoundException | 404 | `NOT_FOUND` |
| DuplicateTransactionException | 409 | `DUPLICATE_TRANSACTION` |
| InsufficientFundsException | 422 | `INSUFFICIENT_FUNDS` |
| DailyLimitExceededException | 422 | `DAILY_LIMIT_EXCEEDED` |
| ExternalServiceException | 503 | `SERVICE_UNAVAILABLE` |

---

*This LLD is designed to be the complete technical blueprint for building the VinUSBank system. Each service follows consistent patterns, making development predictable and maintainable.*
