# 🏦 Phase 2: Core Banking — Testing Guide

Welcome to the VinUSBank Phase 2 testing guide! We've built the four core banking services: **account-service**, **transaction-service**, **loan-service**, and **card-service**. This document covers every test case you need to validate via Postman and Swagger.

---

## 📁 What Code Was Written & Why?

### 1. `account-service` (Port 8083)
- **Why?** Every financial operation starts with a bank account. This service owns the account lifecycle — opening, viewing balance, closing. It also exposes **internal** `/internal/accounts/` endpoints (not gateway-routed) that other services (transaction, loan) call directly via Feign to debit/credit balances.

### 2. `transaction-service` (Port 8084)
- **Why?** Moves money between accounts. Uses **Spring Cloud Feign** to call `account-service` internally. Implements a safe debit → credit → save pattern with rollback on failure. Generates globally-unique reference numbers (`VUByyyyMMddXXXXXX`).

### 3. `loan-service` (Port 8085)
- **Why?** Handles loan applications and EMI calculations using the standard compound interest formula. Loans under **$10,000 auto-approve and disburse** immediately (credits the linked account). Larger loans go to `UNDER_REVIEW`.

### 4. `card-service` (Port 8086)
- **Why?** Issues virtual debit cards. Generates masked card numbers (`****-****-****-XXXX`), manages a full card lifecycle: `PENDING → ACTIVE → BLOCKED`.

---

## 🚀 Startup Order (CRITICAL)

Services **must** be started in this exact order because `transaction-service` and `loan-service` use Feign to call `account-service`:

| # | Service | Port | Start When |
|---|---------|------|------------|
| 1 | Docker infra | — | Already running |
| 2 | `discovery-service` | 8761 | First |
| 3 | `api-gateway` | 8080 | After Eureka |
| 4 | `auth-service` | 8081 | After Gateway |
| 5 | `customer-service` | 8082 | After Auth |
| 6 | **`account-service`** | 8083 | **Before transactions/loans** |
| 7 | `transaction-service` | 8084 | After accounts |
| 8 | `loan-service` | 8085 | After accounts |
| 9 | `card-service` | 8086 | Independent |

> [!IMPORTANT]
> If `account-service` is not running, `transaction-service` and `loan-service` will start but all transfer/loan calls will fail with a Feign `Connection Refused` error.

---

## 🔐 Global Prerequisite: Get Your JWT Token

Every Phase 2 endpoint is **protected behind the Gateway's AuthenticationFilter**. You need a live JWT token before running any test.

**Run Phase 1 Test Case 5 first:**
- **Method:** POST  
- **URL:** `http://localhost:8080/auth-service/api/auth/login`
- **Body:**
  ```json
  {
    "email": "customer@vinusbank.com",
    "password": "SecurePassword123!"
  }
  ```
- **Copy the `token` value** from the response and save it. All tests below require:
  - **Header Key:** `Authorization`
  - **Header Value:** `Bearer <YOUR_TOKEN_HERE>`

---

## 🏦 SECTION 1: Account Service Tests

---

### Test Case 1: Open a Checking Account

**What this proves:** The `account-service` correctly receives the call routed by the Gateway, extracts the user's email from the `X-User-Email` header injected by the JWT filter, and creates a new account record in `vinusbank_account`.

- **Tool:** Postman
- **Method:** POST
- **URL:** `http://localhost:8080/account-service/api/accounts`
- **Headers:**
  - `Authorization: Bearer <YOUR_TOKEN>`
  - `Content-Type: application/json`
- **Body / Raw JSON:**
  ```json
  {
    "accountType": "CHECKING",
    "initialDeposit": 5000.00,
    "currency": "USD"
  }
  ```
- **Expected Output (200 OK):**
  ```json
  {
    "message": "Account opened successfully",
    "account": {
      "id": "uuid-here",
      "accountNumber": "VUS20260421XXXX",
      "accountType": "CHECKING",
      "currency": "USD",
      "availableBalance": 5000.00,
      "currentBalance": 5000.00,
      "status": "ACTIVE",
      "openedAt": "2026-04-21T..."
    }
  }
  ```
- **📋 Action:** Copy the `account.id` and `account.accountNumber` — you'll need these for all subsequent tests.

---

### Test Case 2: Open a Savings Account

Open a second account so we have two accounts available for a transfer in Section 2.

- **Method:** POST
- **URL:** `http://localhost:8080/account-service/api/accounts`
- **Headers:** `Authorization: Bearer <YOUR_TOKEN>`, `Content-Type: application/json`
- **Body:**
  ```json
  {
    "accountType": "SAVINGS",
    "initialDeposit": 1000.00,
    "currency": "USD"
  }
  ```
- **Expected:** 200 OK with a second account. Copy this account's `accountNumber` as the **destination** for transfers.

---

### Test Case 3: List My Accounts

**What this proves:** The service correctly filters by the user's email so you only see your own accounts — never another user's.

- **Method:** GET
- **URL:** `http://localhost:8080/account-service/api/accounts`
- **Headers:** `Authorization: Bearer <YOUR_TOKEN>`
- **Expected Output:** A JSON array with 2 accounts (the checking and savings you just opened).
  ```json
  [
    { "accountNumber": "VUS...", "accountType": "CHECKING", "availableBalance": 5000.00 },
    { "accountNumber": "VUS...", "accountType": "SAVINGS",  "availableBalance": 1000.00 }
  ]
  ```

---

### Test Case 4: Get Account Balance

- **Method:** GET
- **URL:** `http://localhost:8080/account-service/api/accounts/<CHECKING_ACCOUNT_ID>/balance`
- **Headers:** `Authorization: Bearer <YOUR_TOKEN>`
- **Expected Output:**
  ```json
  {
    "accountNumber": "VUS20260421XXXX",
    "availableBalance": 5000.00,
    "currentBalance": 5000.00,
    "currency": "USD"
  }
  ```

---

### Test Case 5: 401 Security Check — Access Without Token

**What this proves:** The Gateway's `AuthenticationFilter` blocks the call before it reaches the service.

- **Method:** GET
- **URL:** `http://localhost:8080/account-service/api/accounts`
- **Headers:** None (no Authorization header)
- **Expected:** `401 Unauthorized: Missing authorization header`

---

## 💸 SECTION 2: Transaction Service Tests

---

### Test Case 6: Transfer Money Between Accounts

**What this proves:** The full Feign-based fund transfer chain —
1. `transaction-service` verifies source account ownership via `account-service`
2. Debits source via `/internal/accounts/{id}/debit`
3. Credits destination via `/internal/accounts/{id}/credit`
4. Saves a `Transaction` record with a unique reference number

- **Method:** POST
- **URL:** `http://localhost:8080/transaction-service/api/transactions/transfer`
- **Headers:** `Authorization: Bearer <YOUR_TOKEN>`, `Content-Type: application/json`
- **Body / Raw JSON:**
  ```json
  {
    "sourceAccountId": "<CHECKING_ACCOUNT_ID>",
    "destinationAccountNumber": "<SAVINGS_ACCOUNT_NUMBER>",
    "amount": 250.00,
    "description": "Rent payment test"
  }
  ```
- **Expected Output (200 OK):**
  ```json
  {
    "message": "Transfer completed successfully",
    "transaction": {
      "id": "txn-uuid",
      "referenceNumber": "VUB20260421000001",
      "transactionType": "INTERNAL_TRANSFER",
      "sourceAccountNumber": "VUS...",
      "destinationAccountNumber": "VUS...",
      "amount": 250.00,
      "status": "COMPLETED",
      "initiatedAt": "2026-04-21T..."
    }
  }
  ```
- **Verify:** Go back and run **Test Case 4** for the checking account balance. It should now show `4750.00` instead of `5000.00`.

---

### Test Case 7: Verify Balance Changed After Transfer

Re-run the balance check to confirm the debit happened:

- **Method:** GET
- **URL:** `http://localhost:8080/account-service/api/accounts/<CHECKING_ACCOUNT_ID>/balance`
- **Headers:** `Authorization: Bearer <YOUR_TOKEN>`
- **Expected:** `availableBalance: 4750.00` (was 5000, minus the 250 transfer)

---

### Test Case 8: Attempt Overdraft (Insufficient Funds)

**What this proves:** The service's balance guard rejects transfers where the amount exceeds available balance and does NOT debit the account.

- **Method:** POST
- **URL:** `http://localhost:8080/transaction-service/api/transactions/transfer`
- **Headers:** `Authorization: Bearer <YOUR_TOKEN>`, `Content-Type: application/json`
- **Body:**
  ```json
  {
    "sourceAccountId": "<CHECKING_ACCOUNT_ID>",
    "destinationAccountNumber": "<SAVINGS_ACCOUNT_NUMBER>",
    "amount": 99999.00,
    "description": "Overdraft attempt"
  }
  ```
- **Expected:** `400 Bad Request`
  ```json
  { "message": "Transfer failed: Insufficient balance" }
  ```

---

### Test Case 9: View Full Transaction History

- **Method:** GET
- **URL:** `http://localhost:8080/transaction-service/api/transactions`
- **Headers:** `Authorization: Bearer <YOUR_TOKEN>`
- **Expected:** A JSON array containing the transfer you made in Test Case 6, ordered newest first.
  ```json
  [
    {
      "referenceNumber": "VUB20260421000001",
      "transactionType": "INTERNAL_TRANSFER",
      "amount": 250.00,
      "status": "COMPLETED"
    }
  ]
  ```

---

### Test Case 10: Cross-Account Ownership Attack (Security Check)

**What this proves:** A user cannot transfer money from someone else's account. Even if they know the account ID, the ownership check blocks it.

1. Register a **second user** (`attacker@vinusbank.com`) via `/auth-service/api/auth/register` and log in to get their token.
2. Try to transfer from the *first* user's checking account using the *attacker's token*.

- **Method:** POST
- **URL:** `http://localhost:8080/transaction-service/api/transactions/transfer`
- **Headers:** `Authorization: Bearer <ATTACKER_TOKEN>`
- **Body:**
  ```json
  {
    "sourceAccountId": "<FIRST_USER_CHECKING_ACCOUNT_ID>",
    "destinationAccountNumber": "any-valid-number",
    "amount": 100.00,
    "description": "Unauthorized transfer"
  }
  ```
- **Expected:** `400 Bad Request`
  ```json
  { "message": "Access denied: you do not own this account" }
  ```

---

## 🏠 SECTION 3: Loan Service Tests

---

### Test Case 11: EMI Calculator (No Login Needed for GET Params)

**What this proves:** The EMI endpoint at `POST /calculate` correctly implements the compound interest formula: `EMI = P * r * (1+r)^n / ((1+r)^n - 1)` where `r` is monthly rate (8.99% / 12).

- **Method:** POST
- **URL:** `http://localhost:8080/loan-service/api/loans/calculate?principal=10000&tenureMonths=24`
- **Headers:** `Authorization: Bearer <YOUR_TOKEN>`
- **No Body required (uses query params)**
- **Expected Output:**
  ```json
  {
    "principal": 10000,
    "tenureMonths": 24,
    "annualInterestRate": 0.0899,
    "monthlyEmi": 457.68,
    "totalInterest": 984.32,
    "totalPayable": 10984.32
  }
  ```
  *(Exact values may differ slightly by ±$0.01 due to rounding)*

---

### Test Case 12: Apply for a Small Loan (Auto-Approved & Disbursed)

**What this proves:** The auto-approval rule — loans under $10,000 are instantly approved and the principal is credited to the linked account via Feign.

- **Method:** POST
- **URL:** `http://localhost:8080/loan-service/api/loans/apply`
- **Headers:** `Authorization: Bearer <YOUR_TOKEN>`, `Content-Type: application/json`
- **Body / Raw JSON:**
  ```json
  {
    "accountId": "<CHECKING_ACCOUNT_ID>",
    "loanType": "PERSONAL",
    "principalAmount": 5000.00,
    "tenureMonths": 12,
    "purpose": "Home renovation"
  }
  ```
- **Expected Output (200 OK):**
  ```json
  {
    "message": "Loan application submitted",
    "loan": {
      "loanNumber": "LN20260421XXXX",
      "loanType": "PERSONAL",
      "principalAmount": 5000.00,
      "interestRate": 0.0899,
      "tenureMonths": 12,
      "emiAmount": 435.96,
      "totalPayable": 5231.52,
      "status": "DISBURSED",
      "approvedAt": "2026-04-21T...",
      "disbursedAt": "2026-04-21T..."
    }
  }
  ```
- **Verify disbursement:** Check the checking account balance — it should have **increased by $5000** from the loan disbursement!

---

### Test Case 13: Apply for a Large Loan (Goes to Review)

**What this proves:** The `UNDER_REVIEW` path for loans ≥ $10,000.

- **Method:** POST
- **URL:** `http://localhost:8080/loan-service/api/loans/apply`
- **Headers:** `Authorization: Bearer <YOUR_TOKEN>`, `Content-Type: application/json`
- **Body:**
  ```json
  {
    "accountId": "<CHECKING_ACCOUNT_ID>",
    "loanType": "HOME",
    "principalAmount": 50000.00,
    "tenureMonths": 120,
    "purpose": "Home purchase"
  }
  ```
- **Expected:** Status is `UNDER_REVIEW` (not auto-approved), balance is NOT credited.
  ```json
  { "loan": { "status": "UNDER_REVIEW", "approvedAt": null, "disbursedAt": null } }
  ```

---

### Test Case 14: View All My Loans

- **Method:** GET
- **URL:** `http://localhost:8080/loan-service/api/loans`
- **Headers:** `Authorization: Bearer <YOUR_TOKEN>`
- **Expected:** Array with both loans — the `DISBURSED` personal loan and the `UNDER_REVIEW` home loan.

---

### Test Case 15: Invalid Loan Type (Validation Check)

**What this proves:** Server-side validation correctly rejects bad enum values.

- **Method:** POST
- **URL:** `http://localhost:8080/loan-service/api/loans/apply`
- **Body:**
  ```json
  {
    "accountId": "<ANY_ID>",
    "loanType": "MORTGAGE",
    "principalAmount": 1000.00,
    "tenureMonths": 12,
    "purpose": "Test"
  }
  ```
- **Expected:** `400 Bad Request`
  ```json
  { "message": "Invalid loan type: MORTGAGE" }
  ```
  *(Valid types: PERSONAL, HOME, AUTO, EDUCATION, BUSINESS)*

---

## 💳 SECTION 4: Card Service Tests

---

### Test Case 16: Request a Virtual Debit Card

**What this proves:** The service generates a unique masked card number, sets expiry 3 years in the future, and starts the card in `PENDING` status awaiting activation.

- **Method:** POST
- **URL:** `http://localhost:8080/card-service/api/cards`
- **Headers:** `Authorization: Bearer <YOUR_TOKEN>`, `Content-Type: application/json`
- **Body / Raw JSON:**
  ```json
  {
    "accountId": "<CHECKING_ACCOUNT_ID>",
    "cardholderName": "VINEET KUMAR"
  }
  ```
- **Expected Output (200 OK):**
  ```json
  {
    "message": "Card requested successfully",
    "card": {
      "id": "card-uuid",
      "cardNumberMasked": "****-****-****-7823",
      "cardholderName": "VINEET KUMAR",
      "cardType": "VIRTUAL_DEBIT",
      "expiryMonth": 4,
      "expiryYear": 2029,
      "status": "PENDING",
      "onlineEnabled": true,
      "internationalEnabled": false
    }
  }
  ```
- **📋 Action:** Copy the `card.id` for the next tests.

---

### Test Case 17: List My Cards

- **Method:** GET
- **URL:** `http://localhost:8080/card-service/api/cards`
- **Headers:** `Authorization: Bearer <YOUR_TOKEN>`
- **Expected:** Array with 1 card in `PENDING` status.

---

### Test Case 18: Activate the Card

**What this proves:** Card moves from `PENDING → ACTIVE` and `activatedAt` timestamp is set.

- **Method:** POST
- **URL:** `http://localhost:8080/card-service/api/cards/<CARD_ID>/activate`
- **Headers:** `Authorization: Bearer <YOUR_TOKEN>`
- **No Body required**
- **Expected Output (200 OK):**
  ```json
  {
    "message": "Card activated successfully",
    "card": {
      "status": "ACTIVE",
      "activatedAt": "2026-04-21T..."
    }
  }
  ```

---

### Test Case 19: Block the Card

- **Method:** POST
- **URL:** `http://localhost:8080/card-service/api/cards/<CARD_ID>/block?reason=Lost card`
- **Headers:** `Authorization: Bearer <YOUR_TOKEN>`
- **Expected:** Status becomes `BLOCKED`, `blockedAt` timestamp is set.
  ```json
  { "card": { "status": "BLOCKED" } }
  ```

---

### Test Case 20: Unblock the Card

- **Method:** POST
- **URL:** `http://localhost:8080/card-service/api/cards/<CARD_ID>/unblock`
- **Headers:** `Authorization: Bearer <YOUR_TOKEN>`
- **Expected:** Status returns to `ACTIVE`, `blockReason` is cleared.
  ```json
  { "card": { "status": "ACTIVE" } }
  ```

---

### Test Case 21: Try to Activate an Already-Active Card (Guard Check)

**What this proves:** The state machine correctly rejects invalid transitions.

- **Method:** POST
- **URL:** `http://localhost:8080/card-service/api/cards/<CARD_ID>/activate`
- **Expected:** `400 Bad Request`
  ```json
  { "message": "Card is not in PENDING state" }
  ```

---

## 🔍 Swagger UI Quick Reference

Each service exposes its own Swagger docs. Use these to try the APIs visually without Postman:

| Service | Swagger URL |
|---------|-------------|
| account-service | `http://localhost:8083/swagger-ui.html` |
| transaction-service | `http://localhost:8084/swagger-ui.html` |
| loan-service | `http://localhost:8085/swagger-ui.html` |
| card-service | `http://localhost:8086/swagger-ui.html` |

> [!NOTE]
> Swagger calls the services **directly** (not through the Gateway), so the `X-User-Email` header won't be injected automatically. In Swagger, manually add `X-User-Email: customer@vinusbank.com` to all requests. For Gateway testing with real JWT validation, always use Postman.

---

## ✅ Full End-to-End Test Checklist

| # | Test Case | Service | Method | Status |
|---|-----------|---------|--------|--------|
| 1 | Open Checking Account | account | POST | ☐ |
| 2 | Open Savings Account | account | POST | ☐ |
| 3 | List My Accounts | account | GET | ☐ |
| 4 | Get Account Balance | account | GET | ☐ |
| 5 | 401 Without Token | account | GET | ☐ |
| 6 | Transfer Money | transaction | POST | ☐ |
| 7 | Verify Balance Changed | account | GET | ☐ |
| 8 | Overdraft Rejection | transaction | POST | ☐ |
| 9 | View Transaction History | transaction | GET | ☐ |
| 10 | Cross-Account Attack | transaction | POST | ☐ |
| 11 | EMI Calculator | loan | POST | ☐ |
| 12 | Small Loan (Auto-Approved) | loan | POST | ☐ |
| 13 | Large Loan (Under Review) | loan | POST | ☐ |
| 14 | View My Loans | loan | GET | ☐ |
| 15 | Invalid Loan Type | loan | POST | ☐ |
| 16 | Request Virtual Card | card | POST | ☐ |
| 17 | List My Cards | card | GET | ☐ |
| 18 | Activate Card | card | POST | ☐ |
| 19 | Block Card | card | POST | ☐ |
| 20 | Unblock Card | card | POST | ☐ |
| 21 | Double-Activate Guard | card | POST | ☐ |

---

## ⏭️ Next: Phase 3 — Compliance & Security

With Phase 2 core banking fully working, the next phase adds:
- **`compliance-service`** — KYC workflow, AML screening, SAR/CTR generation
- **`notification-service`** — Email/in-app alerts for every transaction
- **Enhanced JWT** — Role-based access (ROLE_CUSTOMER, ROLE_STAFF, ROLE_ADMIN)
- **MFA (TOTP)** — Two-factor authentication on login
