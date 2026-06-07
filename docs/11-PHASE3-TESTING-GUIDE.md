# 🛡️ Phase 3: Compliance, Notifications & MFA — Testing Guide

Welcome to the VinUSBank Phase 3 testing guide! We've added two new event-driven microservices: **compliance-service** and **notification-service**, integrated **Apache Kafka**, and enhanced the authentication flow with **Multi-Factor Authentication (MFA)** via TOTP. This document covers every test case you need to validate via Postman.

---

## 📁 What Code Was Written & Why?

### 1. `compliance-service` (Port 8087)
- **Why?** Regulators require banks to monitor transactions and verify identities. This service handles KYC approvals/rejections and listens to Kafka for completed transactions to automatically generate Currency Transaction Reports (CTR) if the amount exceeds $10,000. It also tracks suspicious activities (SAR) and security audit trails.

### 2. `notification-service` (Port 8088)
- **Why?** Customers need to know when money moves or when their KYC status changes. This service listens to `txn.completed` and `kyc.updated` Kafka events and generates in-app notifications asynchronously, decoupled from the core transaction flow.

### 3. Kafka Integration (`txn.completed`, `kyc.updated`)
- **Why?** Synchronous REST calls between services can fail and slow down the main user request. By publishing events to Kafka, the `transaction-service` returns immediately, while `notification-service` and `compliance-service` process the events in the background. Similarly, `compliance-service` publishes `kyc.updated`, allowing `customer-service` and `account-service` to update profile and account statuses reactively.

### 4. MFA & TOTP in `auth-service`
- **Why?** Passwords are not enough for financial applications. We added `jboss-aerogear-otp` to generate a Base32 secret for Google Authenticator/Authy and enforce a two-step login process when enabled.

---

## 🚀 Startup Order (CRITICAL)

With Event-Driven architecture, services are more resilient, but the Kafka Broker MUST be running.

| # | Service | Port | Start When |
|---|---------|------|------------|
| 1 | Docker infra (MySQL + Kafka) | — | Already running (`localhost:9092` for Kafka) |
| 2 | `discovery-service` | 8761 | First |
| 3 | `api-gateway` | 8080 | After Eureka |
| 4 | `auth-service` | 8081 | After Gateway |
| 5 | `customer-service` | 8082 | Independent |
| 6 | `account-service` | 8083 | Independent |
| 7 | `transaction-service` | 8084 | After accounts |
| 8 | **`compliance-service`** | 8087 | Independent (Needs Kafka) |
| 9 | **`notification-service`** | 8088 | Independent (Needs Kafka) |

> [!IMPORTANT]
> If Kafka is down, Spring Boot may fail to start the `compliance-service` and `notification-service` if it cannot connect to `localhost:9092`.

---

## 🔐 Global Prerequisite: Normal JWT Token

For most operations, you still need a standard JWT token.

**Login to get a token (Assuming MFA is OFF initially):**
- **Method:** POST  
- **URL:** `http://localhost:8080/auth-service/api/auth/login`
- **Body:**
  ```json
  {
    "email": "customer@vinusbank.com",
    "password": "SecurePassword123!"
  }
  ```
- **Copy the `token` value** from the response for standard authenticated requests.

---

## 🔒 SECTION 1: MFA & Auth Service Tests

---

### Test Case 1: Setup MFA (Initialize Secret)

**What this proves:** Generates a unique Base32 TOTP secret for the user and returns a QR URL format.

- **Method:** POST
- **URL:** `http://localhost:8080/auth-service/api/auth/mfa/setup`
- **Headers:** `Content-Type: application/json` (No JWT needed, requires password)
- **Body / Raw JSON:**
  ```json
  {
    "email": "customer@vinusbank.com",
    "password": "SecurePassword123!"
  }
  ```
- **Expected Output (200 OK):**
  ```json
  {
    "message": "MFA setup initialized",
    "secret": "V2XXYZZABCDEFG...",
    "qrUrl": "otpauth://totp/VinUSBank:customer@vinusbank.com?secret=..."
  }
  ```
- **📋 Action:** Copy the `secret`. You will need to put this into an Authenticator App, or use a tool to generate a 6-digit code for the next step.

---

### Test Case 2: Enable MFA

**What this proves:** Validates the first TOTP code to ensure the user configured their app correctly, then permanently flags `mfaEnabled = true` in the database.

- **Method:** POST
- **URL:** `http://localhost:8080/auth-service/api/auth/mfa/enable`
- **Headers:** `Content-Type: application/json`
- **Body / Raw JSON:**
  ```json
  {
    "email": "customer@vinusbank.com",
    "password": "SecurePassword123!",
    "code": "<6_DIGIT_TOTP_CODE>"
  }
  ```
- **Expected Output (200 OK):**
  ```json
  {
    "message": "MFA enabled successfully"
  }
  ```

---

### Test Case 3: Login Challenge (MFA Required)

**What this proves:** Now that MFA is enabled, a standard login request no longer returns the JWT immediately.

- **Method:** POST
- **URL:** `http://localhost:8080/auth-service/api/auth/login`
- **Body / Raw JSON:**
  ```json
  {
    "email": "customer@vinusbank.com",
    "password": "SecurePassword123!"
  }
  ```
- **Expected Output (200 OK):**
  ```json
  {
    "message": "MFA required",
    "mfaRequired": true
  }
  ```
*(Notice there is no `token` returned here!)*

---

### Test Case 4: Verify MFA & Get JWT Token

**What this proves:** Submitting the TOTP code alongside valid credentials successfully bypasses the MFA challenge and releases the final JWT.

- **Method:** POST
- **URL:** `http://localhost:8080/auth-service/api/auth/mfa/verify`
- **Body / Raw JSON:**
  ```json
  {
    "email": "customer@vinusbank.com",
    "password": "SecurePassword123!",
    "code": "<NEW_6_DIGIT_TOTP_CODE>"
  }
  ```
- **Expected Output (200 OK):**
  ```json
  {
    "message": "Login successful",
    "token": "eyJhbGciOiJIUzI1NiJ..."
  }
  ```

---

## 🔔 SECTION 2: Event-Driven Notifications

*(You need your JWT token from Test Case 4 for the following tests)*

---

### Test Case 5: Perform a Normal Transfer

**What this proves:** Initiates a transfer that will trigger a background Kafka `txn.completed` event.

- **Method:** POST
- **URL:** `http://localhost:8080/transaction-service/api/transactions/transfer`
- **Headers:** `Authorization: Bearer <YOUR_TOKEN>`
- **Body:**
  ```json
  {
    "sourceAccountId": "<YOUR_CHECKING_ACCOUNT_ID>",
    "destinationAccountNumber": "<YOUR_SAVINGS_ACCOUNT_NUMBER>",
    "amount": 100.00,
    "description": "Test Transfer for Notification"
  }
  ```
- **Expected:** `200 OK` (Transfer successful)

---

### Test Case 6: Verify Transfer Notification Was Generated

**What this proves:** The `notification-service` successfully consumed the `txn.completed` event asynchronously and saved a notification for the user.

- **Method:** GET
- **URL:** `http://localhost:8080/notification-service/api/v1/notifications`
- **Headers:** `Authorization: Bearer <YOUR_TOKEN>`
- **Expected Output (200 OK):**
  ```json
  [
    {
      "id": 1,
      "type": "TRANSACTION",
      "category": "ALERT",
      "title": "Transfer Completed",
      "message": "You have successfully transferred $100.00. Reference: VUB...",
      "read": false,
      "sentAt": "2026-04-21T..."
    }
  ]
  ```

---

### Test Case 7: Mark Notification as Read

- **Method:** PUT
- **URL:** `http://localhost:8080/notification-service/api/v1/notifications/1/read`
- **Headers:** `Authorization: Bearer <YOUR_TOKEN>`
- **Expected Output:**
  ```json
  { "message": "Marked as read" }
  ```

---

## ⚖️ SECTION 3: Compliance & KYC Workflow

---

### Test Case 8: Perform a Transfer > $10,000 (CTR Trigger)

**What this proves:** The `compliance-service` listens to `txn.completed` events and automatically flags transactions exceeding the $10,000 regulatory threshold.

1. Ensure your source account has at least `$12,000` balance.
2. Hit the transfer endpoint:
   - **Method:** POST
   - **URL:** `http://localhost:8080/transaction-service/api/transactions/transfer`
   - **Body:** `amount: 12000.00`
3. Then, verify the CTR generation:
   - **Method:** GET
   - **URL:** `http://localhost:8080/compliance-service/api/v1/compliance/ctr`
   - **Headers:** `Authorization: Bearer <YOUR_TOKEN>`
   - **Expected Output:**
     ```json
     [
       {
         "id": 1,
         "ctrNumber": "CTR-VUB...",
         "userEmail": "customer@vinusbank.com",
         "amount": 12000.00,
         "status": "PENDING"
       }
     ]
     ```

---

### Test Case 9: Approve Pending KYC

**What this proves:** Updating KYC status triggers a `kyc.updated` Kafka event. Both `customer-service` (updates profile) and `account-service` (activates `PENDING` accounts) react to this event asynchronously, while `notification-service` sends an alert.

*(Note: Ensure you have an account in `PENDING` status first, or the account listener won't have anything to activate)*

1. Create a dummy KYC Review in the database (since we didn't build a user-facing KYC submission UI):
   - Using MySQL or an admin tool, insert a `PENDING` record into `vinusbank_compliance.kyc_reviews` for your email.
2. **Method:** PUT
3. **URL:** `http://localhost:8080/compliance-service/api/v1/compliance/kyc/1/approve`
4. **Headers:** `Authorization: Bearer <YOUR_TOKEN>`
5. **Expected Output:** `{ "message": "KYC Approved" }`

---

### Test Case 10: Verify KYC Event Propagation

Check if the downstream microservices reacted correctly to the KYC approval!

1. **Customer Service Check:**
   - **Method:** GET
   - **URL:** `http://localhost:8080/customer-service/api/customers/profile`
   - **Expected:** `"kycStatus": "VERIFIED"`

2. **Account Service Check:**
   - **Method:** GET
   - **URL:** `http://localhost:8080/account-service/api/accounts`
   - **Expected:** Any previously `"status": "PENDING"` accounts should now be `"status": "ACTIVE"`.

3. **Notification Service Check:**
   - **Method:** GET
   - **URL:** `http://localhost:8080/notification-service/api/v1/notifications`
   - **Expected:** A new notification with `"title": "KYC Status Updated"` and `"message": "Your KYC status has been updated to: VERIFIED"`.

---

## ✅ Full End-to-End Test Checklist

| # | Test Case | Service | Method | Status |
|---|-----------|---------|--------|--------|
| 1 | Setup MFA | auth | POST | ☐ |
| 2 | Enable MFA | auth | POST | ☐ |
| 3 | Login Challenge (MFA Required) | auth | POST | ☐ |
| 4 | Verify MFA & Get JWT | auth | POST | ☐ |
| 5 | Perform Transfer | transaction | POST | ☐ |
| 6 | Verify Transfer Notification | notification | GET | ☐ |
| 7 | Mark Notification Read | notification | PUT | ☐ |
| 8 | Trigger auto-CTR (>10k) | compliance | POST (Txn) | ☐ |
| 9 | Approve Pending KYC | compliance | PUT | ☐ |
| 10| Verify KYC Propagation | cust/acc/notif | GET | ☐ |

---

## 🎉 Congratulations! Phase 3 is fully verified!
