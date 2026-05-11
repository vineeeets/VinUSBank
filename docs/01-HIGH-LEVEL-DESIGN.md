# 🏦 VinUSBank — US Banking System with AI Bots
## High-Level Design (HLD) Document

**Version:** 1.0  
**Date:** April 11, 2026  
**Author:** Vineet  
**Status:** Draft — Awaiting Review

---

## 1. Executive Summary

**VinUSBank** is a full-featured US digital banking platform built on a microservices architecture using **Spring Boot** (backend), **Angular** (frontend), **MySQL** (primary database), and **Python-based AI services** (chatbot, fraud detection, financial insights). The system is designed to simulate a real-world US banking experience with regulatory compliance features (KYC/AML/BSA), real-time transaction processing, and intelligent AI-powered bots.

---

## 2. System Goals & Objectives

| Goal | Description |
|------|-------------|
| **Digital Banking** | Full account management, fund transfers, bill payments, loans |
| **US Compliance** | KYC, AML, BSA, CTR/SAR reporting, OFAC screening |
| **AI Integration** | Customer support chatbot, fraud detection, financial advisor bot |
| **Security** | OAuth2/JWT, AES-256 encryption, role-based access control |
| **Scalability** | Microservices architecture, event-driven communication |
| **User Experience** | Modern, responsive Angular SPA with real-time notifications |

---

## 3. System Context Diagram

```
┌─────────────────────────────────────────────────────────────────────────┐
│                          EXTERNAL SYSTEMS                               │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐ │
│  │  OFAC    │  │  Credit  │  │  Email   │  │  SMS     │  │  LLM     │ │
│  │  Lists   │  │  Bureau  │  │  (SMTP)  │  │  Gateway │  │  (OpenAI)│ │
│  └────┬─────┘  └────┬─────┘  └────┬─────┘  └────┬─────┘  └────┬─────┘ │
└───────┼──────────────┼──────────────┼──────────────┼──────────────┼──────┘
        │              │              │              │              │
┌───────┴──────────────┴──────────────┴──────────────┴──────────────┴──────┐
│                                                                          │
│                        🏦 VinUSBank PLATFORM                              │
│                                                                          │
│  ┌──────────────┐    ┌───────────────────────────────────────────────┐   │
│  │   Angular    │    │           Spring Boot Microservices           │   │
│  │   Frontend   │◄──►│  (API Gateway, Auth, Accounts, Transactions, │   │
│  │   (SPA)      │    │   Loans, Compliance, Notifications)          │   │
│  └──────────────┘    └───────────────────┬───────────────────────────┘   │
│                                          │                               │
│                      ┌───────────────────┴───────────────────────────┐   │
│                      │          AI Services (Python/FastAPI)         │   │
│                      │  (Chatbot, Fraud Detection, Financial Advisor)│   │
│                      └───────────────────┬───────────────────────────┘   │
│                                          │                               │
│                      ┌───────────────────┴───────────────────────────┐   │
│                      │           Data Layer                          │   │
│                      │  ┌─────────┐  ┌──────────┐  ┌─────────────┐  │   │
│                      │  │  MySQL  │  │  Redis   │  │  Kafka      │  │   │
│                      │  │  (Main) │  │  (Cache) │  │  (Events)   │  │   │
│                      │  └─────────┘  └──────────┘  └─────────────┘  │   │
│                      └───────────────────────────────────────────────┘   │
│                                                                          │
│  ┌──────────────┐    ┌──────────────┐    ┌──────────────┐               │
│  │  Admin       │    │  Bank        │    │  Mobile      │               │
│  │  Portal      │    │  Staff       │    │  (Future)    │               │
│  │  (Angular)   │    │  Portal      │    │              │               │
│  └──────────────┘    └──────────────┘    └──────────────┘               │
└──────────────────────────────────────────────────────────────────────────┘
```

---

## 4. User Roles & Actors

| Role | Description | Key Actions |
|------|-------------|-------------|
| **Customer** | End-user with a bank account | Open account, transfer funds, pay bills, chat with AI |
| **Bank Staff** | Branch/call center employee | View customer info, process requests, manage complaints |
| **Compliance Officer** | Regulatory compliance team | Review SAR/CTR, manage KYC, OFAC screening |
| **Admin** | System administrator | Manage users, roles, system config, audit logs |
| **AI Bots** | Automated agents | Customer support, fraud alerts, financial advice |

---

## 5. Core Functional Modules

### 5.1 Customer & Identity Management
- User registration with multi-step KYC
- Identity verification (SSN, ID upload, address proof)
- Customer profile management
- Multi-factor authentication (MFA)

### 5.2 Account Management
- Account types: Checking, Savings, Money Market, CD
- Account opening/closing workflows
- Account statements & summaries
- Interest calculation & accrual
- Dormant account management

### 5.3 Transaction Management
- Internal transfers (between own accounts)
- Domestic transfers (ACH, wire)
- Bill payments
- Transaction history with search/filter
- Real-time balance updates
- Transaction receipts & confirmations

### 5.4 Loan & Credit Management
- Personal loan application & approval workflow
- Loan amortization schedule
- EMI calculation & tracking
- Loan status tracking
- Credit score integration (simulated)

### 5.5 Card Management
- Virtual debit card generation
- Card activation/deactivation
- Spending limits
- Card transaction history

### 5.6 Compliance & Regulatory
- KYC/CDD workflow with approval stages
- AML transaction screening
- OFAC sanctions list screening (simulated)
- CTR generation (transactions > $10,000)
- SAR filing workflow
- Audit trail for all actions

### 5.7 Notifications & Alerts
- Email notifications (account activity, alerts)
- In-app notifications
- SMS alerts (configurable)
- Push notifications (future mobile)

### 5.8 AI-Powered Features
- **Customer Support Chatbot** — NLP-driven conversational assistant
- **Fraud Detection Engine** — Real-time transaction anomaly detection
- **Financial Advisor Bot** — Spending analysis, savings recommendations
- **Document Analyzer** — AI-powered document verification for KYC

---

## 6. Technology Stack

| Layer | Technology | Purpose |
|-------|------------|---------|
| **Frontend** | Angular 18+ | Customer & Admin SPA |
| **API Gateway** | Spring Cloud Gateway | Routing, rate limiting, auth |
| **Backend** | Spring Boot 3.x (Java 21) | Core microservices |
| **AI Services** | Python 3.12 + FastAPI | AI/ML model serving |
| **AI/NLP** | LangChain + OpenAI/Ollama | Chatbot & NLU |
| **Fraud ML** | scikit-learn / PyTorch | Fraud detection models |
| **Database** | MySQL 8.x | Primary data store |
| **Cache** | Redis 7.x | Session cache, rate limiting |
| **Message Broker** | Apache Kafka | Event streaming |
| **Auth** | Spring Security + OAuth2/JWT | Authentication & authorization |
| **API Docs** | SpringDoc OpenAPI | API documentation |
| **Build** | Maven / npm | Build tools |
| **Containerization** | Docker + Docker Compose | Local dev environment |

---

## 7. Microservices Inventory

```
┌─────────────────────────────────────────────────────────────────┐
│                    MICROSERVICES LANDSCAPE                       │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│  ┌─────────────────┐    ┌─────────────────┐                     │
│  │  api-gateway     │    │  discovery-      │                     │
│  │  :8080           │    │  service :8761   │                     │
│  └────────┬─────────┘    └─────────────────┘                     │
│           │                                                      │
│  ┌────────┴──────────────────────────────────────┐              │
│  │                                                │              │
│  │  ┌──────────────┐  ┌──────────────┐           │              │
│  │  │  auth-service │  │  customer-   │           │              │
│  │  │  :8081        │  │  service     │           │              │
│  │  │              │  │  :8082        │           │              │
│  │  └──────────────┘  └──────────────┘           │              │
│  │                                                │              │
│  │  ┌──────────────┐  ┌──────────────┐           │              │
│  │  │  account-     │  │  transaction-│           │              │
│  │  │  service      │  │  service     │           │              │
│  │  │  :8083        │  │  :8084        │           │              │
│  │  └──────────────┘  └──────────────┘           │              │
│  │                                                │              │
│  │  ┌──────────────┐  ┌──────────────┐           │              │
│  │  │  loan-service │  │  card-service│           │              │
│  │  │  :8085        │  │  :8086        │           │              │
│  │  └──────────────┘  └──────────────┘           │              │
│  │                                                │              │
│  │  ┌──────────────┐  ┌──────────────┐           │              │
│  │  │  compliance-  │  │  notification│           │              │
│  │  │  service      │  │  -service    │           │              │
│  │  │  :8087        │  │  :8088        │           │              │
│  │  └──────────────┘  └──────────────┘           │              │
│  │                                                │              │
│  └────────────────────────────────────────────────┘              │
│                                                                  │
│  ┌─────────────── AI Services (Python) ─────────────────┐       │
│  │  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐│       │
│  │  │  chatbot-     │  │  fraud-       │  │  advisor-    ││       │
│  │  │  service      │  │  detection-   │  │  service     ││       │
│  │  │  :9001        │  │  service      │  │  :9003       ││       │
│  │  │              │  │  :9002        │  │              ││       │
│  │  └──────────────┘  └──────────────┘  └──────────────┘│       │
│  └───────────────────────────────────────────────────────┘       │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
```

| # | Service | Port | Technology | Responsibility |
|---|---------|------|------------|----------------|
| 1 | `api-gateway` | 8080 | Spring Cloud Gateway | Request routing, rate limiting, CORS |
| 2 | `discovery-service` | 8761 | Spring Cloud Eureka | Service registry & discovery |
| 3 | `auth-service` | 8081 | Spring Boot + Spring Security | Login, registration, JWT, MFA |
| 4 | `customer-service` | 8082 | Spring Boot | Customer profiles, KYC |
| 5 | `account-service` | 8083 | Spring Boot | Account CRUD, balance, statements |
| 6 | `transaction-service` | 8084 | Spring Boot | Transfers, payments, history |
| 7 | `loan-service` | 8085 | Spring Boot | Loan applications, EMI, tracking |
| 8 | `card-service` | 8086 | Spring Boot | Virtual cards, limits, activation |
| 9 | `compliance-service` | 8087 | Spring Boot | KYC workflow, AML, SAR/CTR |
| 10 | `notification-service` | 8088 | Spring Boot | Email, SMS, push, in-app |
| 11 | `chatbot-service` | 9001 | Python + FastAPI | NLP chatbot with LLM |
| 12 | `fraud-detection-service` | 9002 | Python + FastAPI | Real-time fraud scoring |
| 13 | `advisor-service` | 9003 | Python + FastAPI | Financial insights & advice |

---

## 8. Communication Patterns

### 8.1 Synchronous (REST/HTTP)
- Frontend → API Gateway → Microservices (CRUD operations)
- Service-to-service calls for immediate responses (balance check)

### 8.2 Asynchronous (Kafka Events)
- Transaction events → Fraud Detection pipeline
- Account events → Notification triggers
- Compliance events → SAR/CTR generation
- KYC status changes → Account activation

### 8.3 Kafka Topic Design

| Topic | Publisher | Consumer(s) |
|-------|-----------|-------------|
| `txn.created` | transaction-service | fraud-detection, compliance, notification |
| `txn.completed` | transaction-service | account-service, notification |
| `account.created` | account-service | notification, compliance |
| `kyc.updated` | compliance-service | customer-service, account-service |
| `fraud.alert` | fraud-detection | compliance, notification, transaction |
| `loan.status` | loan-service | notification, customer-service |
| `notification.send` | any service | notification-service |

---

## 9. Security Architecture

```
┌────────────────────────────────────────────────────────────┐
│                    SECURITY LAYERS                          │
├────────────────────────────────────────────────────────────┤
│                                                             │
│  Layer 1: Network Security                                  │
│  ├── HTTPS/TLS 1.3 for all external communication          │
│  ├── CORS policy enforcement at API Gateway                 │
│  └── Rate limiting & DDoS protection                        │
│                                                             │
│  Layer 2: Authentication & Authorization                    │
│  ├── OAuth 2.0 + JWT tokens                                 │
│  ├── Refresh token rotation                                 │
│  ├── Multi-Factor Authentication (TOTP)                     │
│  └── Role-Based Access Control (RBAC)                       │
│                                                             │
│  Layer 3: Data Security                                     │
│  ├── AES-256 encryption for sensitive data at rest          │
│  ├── SSN/account numbers stored encrypted                   │
│  ├── Password hashing with BCrypt                           │
│  └── PII masking in logs                                    │
│                                                             │
│  Layer 4: Application Security                              │
│  ├── Input validation & sanitization                        │
│  ├── SQL injection prevention (JPA/Hibernate)               │
│  ├── XSS protection in Angular                              │
│  └── CSRF token management                                  │
│                                                             │
│  Layer 5: Audit & Monitoring                                │
│  ├── Complete audit trail for all operations                │
│  ├── Failed login attempt tracking                          │
│  └── Suspicious activity alerting                           │
│                                                             │
└────────────────────────────────────────────────────────────┘
```

### RBAC Roles & Permissions Matrix

| Permission | Customer | Staff | Compliance | Admin |
|------------|----------|-------|------------|-------|
| View own account | ✅ | ✅ | ✅ | ✅ |
| Transfer funds | ✅ | ❌ | ❌ | ❌ |
| View any customer | ❌ | ✅ | ✅ | ✅ |
| Approve KYC | ❌ | ❌ | ✅ | ✅ |
| File SAR | ❌ | ❌ | ✅ | ✅ |
| Manage roles | ❌ | ❌ | ❌ | ✅ |
| System config | ❌ | ❌ | ❌ | ✅ |
| Chat with AI | ✅ | ✅ | ❌ | ✅ |

---

## 10. Deployment Architecture

```
┌──────────────────── Docker Compose (Local Dev) ──────────────────────┐
│                                                                       │
│  ┌─── Frontend ───┐  ┌─── Gateway ───┐  ┌─── Discovery ───┐        │
│  │ Angular :4200   │  │ Gateway :8080  │  │ Eureka :8761    │        │
│  └────────────────┘  └───────────────┘  └─────────────────┘        │
│                                                                       │
│  ┌─────── Spring Boot Services ──────────────────────────────┐       │
│  │ Auth:8081 | Customer:8082 | Account:8083 | Txn:8084       │       │
│  │ Loan:8085 | Card:8086 | Compliance:8087 | Notif:8088      │       │
│  └────────────────────────────────────────────────────────────┘       │
│                                                                       │
│  ┌─────── AI Services (Python) ──────────────────────────────┐       │
│  │ Chatbot:9001 | Fraud:9002 | Advisor:9003                  │       │
│  └────────────────────────────────────────────────────────────┘       │
│                                                                       │
│  ┌─────── Infrastructure ────────────────────────────────────┐       │
│  │ MySQL:3306 | Redis:6379 | Kafka:9092 | Zookeeper:2181     │       │
│  └────────────────────────────────────────────────────────────┘       │
│                                                                       │
└───────────────────────────────────────────────────────────────────────┘
```

---

## 11. Non-Functional Requirements

| Requirement | Target |
|-------------|--------|
| **Response Time** | < 500ms for API calls |
| **Availability** | 99.9% uptime target |
| **Concurrent Users** | Support 1000+ simultaneous users |
| **Data Retention** | 7 years for financial records |
| **Fraud Detection** | < 2 seconds for real-time scoring |
| **Chatbot Response** | < 3 seconds for AI responses |
| **Security** | OWASP Top 10 compliance |
| **Audit** | 100% action traceability |

---

## 12. Phase-wise Delivery Plan

### Phase 1 — Foundation (Weeks 1-3)
- Project setup & infrastructure (Docker, Kafka, MySQL, Redis)
- Discovery service & API Gateway
- Auth service with JWT & registration
- Customer service with basic KYC
- Angular app shell with auth pages

### Phase 2 — Core Banking (Weeks 4-6)
- Account service (create, view, close)
- Transaction service (transfers, payments)
- Loan service (apply, view, track)
- Card service (virtual cards)
- Angular banking dashboard

### Phase 3 — Compliance & Security (Weeks 7-8)
- Compliance service (KYC workflow, AML screening)
- Audit trail implementation
- SAR/CTR report generation
- Enhanced security (MFA, encryption)

### Phase 4 — AI Integration (Weeks 9-11)
- Customer support chatbot (LangChain + LLM)
- Fraud detection ML service
- Financial advisor bot
- AI dashboard in Angular

### Phase 5 — Polish & Launch (Week 12)
- End-to-end testing
- Performance optimization
- Documentation finalization
- Demo preparation

---

*Document continues in Low-Level Design (LLD)...*
