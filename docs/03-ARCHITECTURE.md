# 🏦 VinUSBank — US Banking System with AI Bots
## Architecture Document

**Version:** 1.0  
**Date:** April 11, 2026  
**Author:** Vineet  
**Status:** Draft — Awaiting Review

---

## 1. Architecture Overview

VinUSBank follows a **Microservices Architecture** with the following key architectural patterns:

| Pattern | Usage |
|---------|-------|
| **Microservices** | Independent services per business domain |
| **API Gateway** | Single entry point for all client requests |
| **Service Discovery** | Dynamic service registration & lookup |
| **Event-Driven (Kafka)** | Asynchronous inter-service communication |
| **CQRS (lightweight)** | Separate read/write patterns where beneficial |
| **Database per Service** | Each service owns its data schema |
| **Circuit Breaker** | Resilient inter-service communication |
| **JWT-based Auth** | Stateless authentication across services |

---

## 2. Architecture Diagram

```
                                    ┌──────────────────┐
                                    │   LOAD BALANCER   │
                                    │  (Nginx / Cloud)  │
                                    └────────┬─────────┘
                                             │
                    ┌────────────────────────┼────────────────────────┐
                    │                        │                        │
          ┌─────────▼─────────┐   ┌─────────▼─────────┐  ┌──────────▼────────┐
          │  Angular Frontend  │   │  Angular Frontend  │  │   Angular Admin   │
          │  (Customer Portal) │   │  (Staff Portal)    │  │      Portal       │
          │     Port: 4200     │   │     Port: 4201     │  │    Port: 4202     │
          └─────────┬─────────┘   └─────────┬─────────┘  └──────────┬────────┘
                    │                        │                        │
                    └────────────────────────┼────────────────────────┘
                                             │ HTTPS
                    ┌────────────────────────▼────────────────────────┐
                    │              API GATEWAY (Spring Cloud)          │
                    │                   Port: 8080                    │
                    │  ┌──────────────────────────────────────────┐   │
                    │  │  • Route discovery & forwarding           │   │
                    │  │  • JWT validation & propagation           │   │
                    │  │  • Rate limiting (Redis-backed)           │   │
                    │  │  • CORS configuration                    │   │
                    │  │  • Request/Response logging               │   │
                    │  │  • Circuit breaker (Resilience4j)         │   │
                    │  └──────────────────────────────────────────┘   │
                    └──────────────────┬──────────────────────────────┘
                                       │
              ┌────────────────────────┼────────────────────────┐
              │                        │          Service       │
              │                        │         Discovery      │
              │                 ┌──────▼──────┐  (Eureka)      │
              │                 │ Eureka Server│  Port: 8761    │
              │                 │             │                 │
              │                 └──────┬──────┘                │
              │                        │                        │
    ┌─────────┼────────────────────────┼────────────────────────┼──────────┐
    │         │                        │    SPRING BOOT         │          │
    │         │                        │   MICROSERVICES        │          │
    │  ┌──────▼──────┐  ┌──────────────▼──┐  ┌──────────────┐ │          │
    │  │    Auth     │  │    Customer     │  │   Account    │ │          │
    │  │  Service    │  │    Service      │  │   Service    │ │          │
    │  │  :8081      │  │    :8082        │  │   :8083      │ │          │
    │  └──────┬──────┘  └──────────┬──────┘  └──────┬───────┘ │          │
    │         │                    │                 │          │          │
    │  ┌──────▼──────┐  ┌──────────▼──────┐  ┌──────▼───────┐ │          │
    │  │ Transaction │  │    Loan         │  │   Card       │ │          │
    │  │  Service    │  │    Service      │  │   Service    │ │          │
    │  │  :8084      │  │    :8085        │  │   :8086      │ │          │
    │  └──────┬──────┘  └──────────┬──────┘  └──────┬───────┘ │          │
    │         │                    │                 │          │          │
    │  ┌──────▼──────┐  ┌──────────▼──────┐         │          │          │
    │  │ Compliance  │  │  Notification   │         │          │          │
    │  │  Service    │  │    Service      │         │          │          │
    │  │  :8087      │  │    :8088        │         │          │          │
    │  └─────────────┘  └────────────────┘         │          │          │
    │                                               │          │          │
    └───────────────────────────────────────────────┼──────────┘          │
                                                    │                     │
    ┌───────────────────────────────────────────────┼──────────────────┐  │
    │              AI SERVICES (Python + FastAPI)    │                  │  │
    │                                               │                  │  │
    │  ┌──────────────┐  ┌──────────────┐  ┌───────▼──────┐          │  │
    │  │   Chatbot    │  │    Fraud     │  │  Financial   │          │  │
    │  │   Service    │  │  Detection   │  │   Advisor    │          │  │
    │  │   :9001      │  │   :9002      │  │   :9003      │          │  │
    │  └──────────────┘  └──────────────┘  └──────────────┘          │  │
    │                                                                  │  │
    └──────────────────────────────────────────────────────────────────┘  │
                                                                          │
    ┌──────────────────────── DATA LAYER ──────────────────────────────┐  │
    │                                                                   │  │
    │  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐           │  │
    │  │   MySQL 8.x   │  │   Redis 7.x  │  │ Apache Kafka │           │  │
    │  │   :3306       │  │   :6379      │  │   :9092      │           │  │
    │  │              │  │              │  │              │           │  │
    │  │ 8 databases   │  │ Cache +      │  │ Event bus +  │           │  │
    │  │ (logical)     │  │ Sessions     │  │ Streaming    │           │  │
    │  └──────────────┘  └──────────────┘  └──────────────┘           │  │
    │                                                                   │  │
    │  ┌──────────────┐                                                │  │
    │  │  Zookeeper   │                                                │  │
    │  │   :2181      │                                                │  │
    │  └──────────────┘                                                │  │
    │                                                                   │  │
    └───────────────────────────────────────────────────────────────────┘  │
                                                                          │
    ┌──────────────────── OBSERVABILITY ────────────────────────────────┐  │
    │                                                                   │  │
    │  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐           │  │
    │  │  Spring Boot  │  │   Logback    │  │  Actuator    │           │  │
    │  │   Admin       │  │   (JSON)     │  │  Endpoints   │           │  │
    │  │   :9090       │  │              │  │  /health     │           │  │
    │  └──────────────┘  └──────────────┘  └──────────────┘           │  │
    │                                                                   │  │
    └───────────────────────────────────────────────────────────────────┘  │
                                                                          │
└─────────────────────────────────────────────────────────────────────────┘
```

---

## 3. Service Communication Matrix

### 3.1 Synchronous Communication (REST/HTTP)

```
┌────────────────────┬──────────────┬──────────────┬───────────────────────────────┐
│ From               │ To           │ Protocol     │ Purpose                       │
├────────────────────┼──────────────┼──────────────┼───────────────────────────────┤
│ API Gateway        │ All services │ HTTP/REST    │ Route client requests         │
│ Transaction Svc    │ Account Svc  │ HTTP/REST    │ Debit/Credit operations       │
│ Transaction Svc    │ Fraud Svc    │ HTTP/REST    │ Real-time fraud check         │
│ Chatbot Svc        │ Account Svc  │ HTTP/REST    │ Balance inquiries             │
│ Chatbot Svc        │ Transaction  │ HTTP/REST    │ Transaction queries           │
│ Chatbot Svc        │ Loan Svc     │ HTTP/REST    │ Loan information              │
│ Compliance Svc     │ Customer Svc │ HTTP/REST    │ Customer data for reviews     │
│ Loan Svc           │ Account Svc  │ HTTP/REST    │ Disbursement to account       │
│ Advisor Svc        │ Transaction  │ HTTP/REST    │ Spending data analysis        │
│ Advisor Svc        │ Account Svc  │ HTTP/REST    │ Balance data for insights     │
└────────────────────┴──────────────┴──────────────┴───────────────────────────────┘
```

### 3.2 Asynchronous Communication (Kafka Events)

```
┌────────────────────┬────────────────────┬────────────────────┬──────────────────────────┐
│ Event              │ Producer           │ Consumer(s)        │ Purpose                  │
├────────────────────┼────────────────────┼────────────────────┼──────────────────────────┤
│ txn.created        │ Transaction Svc    │ Fraud, Compliance  │ New transaction alert    │
│ txn.completed      │ Transaction Svc    │ Notification, Acct │ Transaction done         │
│ txn.failed         │ Transaction Svc    │ Notification       │ Transaction failure      │
│ account.created    │ Account Svc        │ Notification       │ New account opened       │
│ account.updated    │ Account Svc        │ Notification       │ Account status change    │
│ kyc.submitted      │ Customer Svc       │ Compliance         │ New KYC for review       │
│ kyc.approved       │ Compliance Svc     │ Customer, Account  │ KYC approved             │
│ kyc.rejected       │ Compliance Svc     │ Customer, Notif    │ KYC rejected             │
│ fraud.alert        │ Fraud Svc          │ Compliance, Notif  │ Fraud detected           │
│ loan.applied       │ Loan Svc           │ Notification       │ Loan application         │
│ loan.approved      │ Loan Svc           │ Notification, Acct │ Loan approved            │
│ loan.disbursed     │ Loan Svc           │ Notification, Acct │ Loan disbursed           │
│ user.registered    │ Auth Svc           │ Notification       │ Welcome email            │
│ user.locked        │ Auth Svc           │ Notification       │ Account locked alert     │
│ card.activated     │ Card Svc           │ Notification       │ Card activation confirm  │
│ card.blocked       │ Card Svc           │ Notification       │ Card blocked alert       │
└────────────────────┴────────────────────┴────────────────────┴──────────────────────────┘
```

---

## 4. Authentication & Authorization Flow

```
┌──────────┐     ┌──────────┐     ┌──────────┐     ┌──────────────┐
│  Angular  │     │   API    │     │   Auth   │     │   Target     │
│  Client   │     │ Gateway  │     │ Service  │     │  Microservice│
└─────┬─────┘     └────┬─────┘     └────┬─────┘     └──────┬───────┘
      │                │               │                    │
      │ 1. POST /auth/login            │                    │
      │────────────────►               │                    │
      │                │───────────────►                    │
      │                │  Forward to   │                    │
      │                │  auth-service │                    │
      │                │               │                    │
      │                │  Validate credentials              │
      │                │  Generate JWT + Refresh Token      │
      │                │◄───────────────                    │
      │◄────────────────               │                    │
      │  { accessToken, refreshToken } │                    │
      │                │               │                    │
      │ Store tokens in│               │                    │
      │ memory + httpOnly cookie       │                    │
      │                │               │                    │
      │ 2. GET /api/v1/accounts        │                    │
      │  Authorization: Bearer <JWT>   │                    │
      │────────────────►               │                    │
      │                │               │                    │
      │                │ Validate JWT  │                    │
      │                │ (public key)  │                    │
      │                │               │                    │
      │                │ Extract user claims               │
      │                │ Add X-User-Id, X-User-Roles       │
      │                │ to request headers                │
      │                │                                    │
      │                │────────────────────────────────────►
      │                │  Forward with user context         │
      │                │◄────────────────────────────────────
      │◄────────────────               │                    │
      │  { accounts data }             │                    │
      │                │               │                    │
      │ 3. Token Refresh (when expired)│                    │
      │ POST /auth/refresh-token       │                    │
      │  { refreshToken }              │                    │
      │────────────────►               │                    │
      │                │───────────────►                    │
      │                │  Validate + rotate                 │
      │                │◄───────────────                    │
      │◄────────────────               │                    │
      │  { new accessToken, new refreshToken }              │
```

### JWT Token Structure

```json
{
  "header": {
    "alg": "RS256",
    "typ": "JWT"
  },
  "payload": {
    "sub": "user-uuid",
    "email": "john@email.com",
    "roles": ["ROLE_CUSTOMER"],
    "permissions": ["ACCOUNT_READ", "ACCOUNT_WRITE", "TXN_CREATE"],
    "customerId": "cust-uuid",
    "kycStatus": "VERIFIED",
    "iat": 1712836800,
    "exp": 1712837700,
    "iss": "vinusbank-auth"
  }
}
```

- **Access Token TTL:** 15 minutes
- **Refresh Token TTL:** 7 days
- **Signing:** RS256 (asymmetric — Gateway validates with public key)

---

## 5. Data Flow Architectures

### 5.1 Account Opening Flow

```
┌─────────────────────────────────────────────────────────────────────────┐
│                       ACCOUNT OPENING FLOW                              │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                         │
│  Step 1: Registration                                                   │
│  ┌──────────┐   ┌──────────┐   ┌──────────┐                           │
│  │ Customer │──►│  Auth    │──►│ Customer │                           │
│  │ Register │   │ Service  │   │ Service  │                           │
│  └──────────┘   │ Create   │   │ Create   │                           │
│                 │ User     │   │ Profile  │                           │
│                 └──────────┘   └─────┬────┘                           │
│                                      │                                 │
│  Step 2: KYC Document Upload         │                                 │
│  ┌──────────┐   ┌──────────┐         │                                 │
│  │ Upload   │──►│ Customer │◄────────┘                                 │
│  │ Documents│   │ Service  │                                           │
│  └──────────┘   └─────┬────┘                                           │
│                        │ Kafka: kyc.submitted                          │
│  Step 3: KYC Review    ▼                                               │
│  ┌──────────┐   ┌──────────┐   ┌──────────┐                           │
│  │Compliance│──►│Compliance│──►│ OFAC     │                           │
│  │ Officer  │   │ Service  │   │ Screening│                           │
│  │ Reviews  │   └─────┬────┘   └──────────┘                           │
│  └──────────┘         │                                                │
│                        │ Kafka: kyc.approved                           │
│  Step 4: Account       ▼                                               │
│  ┌──────────┐   ┌──────────┐   ┌──────────┐   ┌──────────┐           │
│  │ Account  │──►│ Account  │──►│ Card     │──►│ Notif    │           │
│  │ Creation │   │ Service  │   │ Service  │   │ Service  │           │
│  │ Auto     │   │ Activate │   │ Issue    │   │ Welcome  │           │
│  └──────────┘   └──────────┘   │ Debit    │   │ Email    │           │
│                                │ Card     │   └──────────┘           │
│                                └──────────┘                           │
│                                                                         │
└─────────────────────────────────────────────────────────────────────────┘
```

### 5.2 Compliance Monitoring Flow

```
┌─────────────────────────────────────────────────────────────────────────┐
│                    COMPLIANCE MONITORING FLOW                           │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                         │
│  ┌───────────────┐                                                     │
│  │  Transaction   │    Kafka: txn.created                              │
│  │  Completed     │─────────────────────┐                              │
│  └───────────────┘                      │                              │
│                                          ▼                              │
│                               ┌──────────────────┐                     │
│                               │  Compliance       │                     │
│                               │  Service          │                     │
│                               └────────┬─────────┘                     │
│                                        │                                │
│                    ┌───────────────────┼───────────────────┐            │
│                    │                   │                   │            │
│                    ▼                   ▼                   ▼            │
│          ┌────────────────┐  ┌────────────────┐  ┌────────────────┐   │
│          │ Amount > $10K? │  │ Pattern Match? │  │ OFAC Match?    │   │
│          │ → Generate CTR │  │ → Generate SAR │  │ → Block + Alert│   │
│          └────────┬───────┘  └────────┬───────┘  └────────┬───────┘   │
│                   │                   │                   │            │
│                   ▼                   ▼                   ▼            │
│          ┌────────────────┐  ┌────────────────┐  ┌────────────────┐   │
│          │ Auto-file CTR  │  │ Queue for      │  │ Freeze Account │   │
│          │ to FinCEN      │  │ Compliance     │  │ Notify Customer│   │
│          │ (simulated)    │  │ Officer Review │  │ Alert Officer  │   │
│          └────────────────┘  └────────────────┘  └────────────────┘   │
│                                                                         │
└─────────────────────────────────────────────────────────────────────────┘
```

---

## 6. Infrastructure Components

### 6.1 Spring Cloud Gateway Configuration

```yaml
# api-gateway application.yml
spring:
  cloud:
    gateway:
      routes:
        - id: auth-service
          uri: lb://AUTH-SERVICE
          predicates:
            - Path=/api/v1/auth/**
          filters:
            - StripPrefix=0

        - id: customer-service
          uri: lb://CUSTOMER-SERVICE
          predicates:
            - Path=/api/v1/customers/**
          filters:
            - StripPrefix=0
            - JwtAuthenticationFilter

        - id: account-service
          uri: lb://ACCOUNT-SERVICE
          predicates:
            - Path=/api/v1/accounts/**
          filters:
            - StripPrefix=0
            - JwtAuthenticationFilter

        - id: transaction-service
          uri: lb://TRANSACTION-SERVICE
          predicates:
            - Path=/api/v1/transactions/**
          filters:
            - StripPrefix=0
            - JwtAuthenticationFilter

        - id: loan-service
          uri: lb://LOAN-SERVICE
          predicates:
            - Path=/api/v1/loans/**
          filters:
            - StripPrefix=0
            - JwtAuthenticationFilter

        - id: card-service
          uri: lb://CARD-SERVICE
          predicates:
            - Path=/api/v1/cards/**
          filters:
            - StripPrefix=0
            - JwtAuthenticationFilter

        - id: compliance-service
          uri: lb://COMPLIANCE-SERVICE
          predicates:
            - Path=/api/v1/compliance/**
          filters:
            - StripPrefix=0
            - JwtAuthenticationFilter
            - RoleAuthorizationFilter=ROLE_COMPLIANCE,ROLE_ADMIN

        - id: notification-service
          uri: lb://NOTIFICATION-SERVICE
          predicates:
            - Path=/api/v1/notifications/**
          filters:
            - StripPrefix=0
            - JwtAuthenticationFilter

        - id: chatbot-service
          uri: http://localhost:9001
          predicates:
            - Path=/api/v1/ai/chat/**
          filters:
            - StripPrefix=0
            - JwtAuthenticationFilter

        - id: fraud-service
          uri: http://localhost:9002
          predicates:
            - Path=/api/v1/ai/fraud/**
          filters:
            - StripPrefix=0
            - JwtAuthenticationFilter

        - id: advisor-service
          uri: http://localhost:9003
          predicates:
            - Path=/api/v1/ai/insights/**
          filters:
            - StripPrefix=0
            - JwtAuthenticationFilter

      default-filters:
        - name: RequestRateLimiter
          args:
            redis-rate-limiter.replenishRate: 10
            redis-rate-limiter.burstCapacity: 20
            key-resolver: "#{@userKeyResolver}"
```

### 6.2 Docker Compose (Development)

```yaml
# docker-compose.yml
version: '3.8'

services:
  # ── Infrastructure ──────────────────────────
  mysql:
    image: mysql:8.0
    container_name: vinusbank-mysql
    ports:
      - "3306:3306"
    environment:
      MYSQL_ROOT_PASSWORD: root123
    volumes:
      - mysql_data:/var/lib/mysql
      - ./scripts/init-databases.sql:/docker-entrypoint-initdb.d/init.sql
    networks:
      - vinusbank-network

  redis:
    image: redis:7-alpine
    container_name: vinusbank-redis
    ports:
      - "6379:6379"
    networks:
      - vinusbank-network

  zookeeper:
    image: confluentinc/cp-zookeeper:7.5.0
    container_name: vinusbank-zookeeper
    ports:
      - "2181:2181"
    environment:
      ZOOKEEPER_CLIENT_PORT: 2181
    networks:
      - vinusbank-network

  kafka:
    image: confluentinc/cp-kafka:7.5.0
    container_name: vinusbank-kafka
    ports:
      - "9092:9092"
    environment:
      KAFKA_BROKER_ID: 1
      KAFKA_ZOOKEEPER_CONNECT: zookeeper:2181
      KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://localhost:9092
      KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR: 1
      KAFKA_AUTO_CREATE_TOPICS_ENABLE: "true"
    depends_on:
      - zookeeper
    networks:
      - vinusbank-network

volumes:
  mysql_data:

networks:
  vinusbank-network:
    driver: bridge
```

### 6.3 Database Initialization Script

```sql
-- scripts/init-databases.sql
CREATE DATABASE IF NOT EXISTS vinusbank_auth;
CREATE DATABASE IF NOT EXISTS vinusbank_customer;
CREATE DATABASE IF NOT EXISTS vinusbank_account;
CREATE DATABASE IF NOT EXISTS vinusbank_transaction;
CREATE DATABASE IF NOT EXISTS vinusbank_loan;
CREATE DATABASE IF NOT EXISTS vinusbank_card;
CREATE DATABASE IF NOT EXISTS vinusbank_compliance;
CREATE DATABASE IF NOT EXISTS vinusbank_notification;

-- Create application user
CREATE USER IF NOT EXISTS 'vinusbank_app'@'%' IDENTIFIED BY 'VinUSBank@2026';

GRANT ALL PRIVILEGES ON vinusbank_auth.* TO 'vinusbank_app'@'%';
GRANT ALL PRIVILEGES ON vinusbank_customer.* TO 'vinusbank_app'@'%';
GRANT ALL PRIVILEGES ON vinusbank_account.* TO 'vinusbank_app'@'%';
GRANT ALL PRIVILEGES ON vinusbank_transaction.* TO 'vinusbank_app'@'%';
GRANT ALL PRIVILEGES ON vinusbank_loan.* TO 'vinusbank_app'@'%';
GRANT ALL PRIVILEGES ON vinusbank_card.* TO 'vinusbank_app'@'%';
GRANT ALL PRIVILEGES ON vinusbank_compliance.* TO 'vinusbank_app'@'%';
GRANT ALL PRIVILEGES ON vinusbank_notification.* TO 'vinusbank_app'@'%';

FLUSH PRIVILEGES;
```

---

## 7. Shared Library Design

To avoid code duplication across microservices, create a shared library (Maven module):

### `vinusbank-common` Library

```
vinusbank-common/
├── src/main/java/com/vinusbank/common/
│   ├── dto/
│   │   ├── ApiResponse.java             // Standard response wrapper
│   │   ├── PagedResponse.java           // Pagination wrapper
│   │   └── ErrorResponse.java           // Error response
│   ├── exception/
│   │   ├── BaseException.java           // Base exception class
│   │   ├── ResourceNotFoundException.java
│   │   ├── BusinessException.java
│   │   ├── AuthenticationException.java
│   │   └── GlobalExceptionHandler.java  // @ControllerAdvice
│   ├── event/
│   │   ├── BaseEvent.java               // Base Kafka event
│   │   ├── TransactionEvent.java
│   │   ├── AccountEvent.java
│   │   ├── KycEvent.java
│   │   ├── FraudAlertEvent.java
│   │   └── NotificationEvent.java
│   ├── security/
│   │   ├── JwtTokenProvider.java        // JWT utilities
│   │   ├── UserContext.java             // Thread-local user context
│   │   └── SecurityConstants.java
│   ├── audit/
│   │   ├── AuditEvent.java
│   │   ├── Auditable.java              // @interface
│   │   └── AuditAspect.java            // AOP audit logging
│   ├── config/
│   │   ├── KafkaProducerConfig.java
│   │   └── RedisConfig.java
│   └── util/
│       ├── DateUtil.java
│       ├── MaskingUtil.java             // PII masking
│       ├── EncryptionUtil.java          // AES-256 helpers
│       └── ReferenceNumberGenerator.java
└── pom.xml
```

---

## 8. Project Structure (Repository Layout)

```
vinusbank/
├── docs/                                 // Design documents (this)
│   ├── 01-HIGH-LEVEL-DESIGN.md
│   ├── 02-LOW-LEVEL-DESIGN.md
│   ├── 03-ARCHITECTURE.md
│   └── 04-API-REFERENCE.md
│
├── vinusbank-common/                      // Shared library
│   ├── src/
│   └── pom.xml
│
├── vinusbank-discovery-server/            // Eureka Server
│   ├── src/
│   └── pom.xml
│
├── vinusbank-api-gateway/                 // Spring Cloud Gateway
│   ├── src/
│   └── pom.xml
│
├── vinusbank-auth-service/                // Authentication & Authorization
│   ├── src/
│   └── pom.xml
│
├── vinusbank-customer-service/            // Customer Management & KYC
│   ├── src/
│   └── pom.xml
│
├── vinusbank-account-service/             // Account Management
│   ├── src/
│   └── pom.xml
│
├── vinusbank-transaction-service/         // Transactions & Payments
│   ├── src/
│   └── pom.xml
│
├── vinusbank-loan-service/                // Loan Management
│   ├── src/
│   └── pom.xml
│
├── vinusbank-card-service/                // Card Management
│   ├── src/
│   └── pom.xml
│
├── vinusbank-compliance-service/          // Compliance & Regulatory
│   ├── src/
│   └── pom.xml
│
├── vinusbank-notification-service/        // Notifications
│   ├── src/
│   └── pom.xml
│
├── vinusbank-ai-services/                 // AI Bots (Python)
│   ├── chatbot-service/
│   │   ├── app/
│   │   │   ├── main.py
│   │   │   ├── routes/
│   │   │   ├── services/
│   │   │   ├── tools/
│   │   │   ├── prompts/
│   │   │   └── config.py
│   │   ├── requirements.txt
│   │   └── Dockerfile
│   │
│   ├── fraud-detection-service/
│   │   ├── app/
│   │   │   ├── main.py
│   │   │   ├── routes/
│   │   │   ├── models/
│   │   │   ├── features/
│   │   │   └── config.py
│   │   ├── models/                      // Trained ML models
│   │   ├── data/                        // Training data
│   │   ├── notebooks/                   // Jupyter notebooks
│   │   ├── requirements.txt
│   │   └── Dockerfile
│   │
│   └── advisor-service/
│       ├── app/
│       │   ├── main.py
│       │   ├── routes/
│       │   ├── analyzers/
│       │   └── config.py
│       ├── requirements.txt
│       └── Dockerfile
│
├── vinusbank-frontend/                    // Angular Customer App
│   ├── src/
│   ├── angular.json
│   └── package.json
│
├── scripts/                              // Database & setup scripts
│   ├── init-databases.sql
│   └── seed-data.sql
│
├── docker-compose.yml                    // Local development
├── docker-compose.infra.yml             // Infrastructure only
├── pom.xml                               // Parent POM (multi-module)
├── .gitignore
└── README.md
```

---

## 9. Technology Version Matrix

### Core Stack
- **Java 17**
- **Spring Boot 3.2.4**
- **Spring Cloud 2023.0.1** (Eureka, Gateway, Feign)
- **MySQL 8.0**
- **Redis & Kafka** (prepared for Phase 3)
- **SLF4J + Logback** (app-wide structured JSON logging / daily rolling files)

| Technology | Version | Purpose | Required |
|------------|---------|---------|----------|
| Java | 21 (LTS) | Spring Boot runtime | ✅ |
| Spring Boot | 3.3.x | Backend framework | ✅ |
| Spring Cloud | 2024.0.x | Gateway, Eureka, Config | ✅ |
| Spring Security | 6.x | Authentication & authorization | ✅ |
| Spring Data JPA | 3.3.x | Database access | ✅ |
| Maven | 3.9.x | Java build tool | ✅ |
| MySQL | 8.0+ | Primary database | ✅ |
| Redis | 7.x | Caching & sessions | ✅ |
| Apache Kafka | 3.7.x | Event streaming | ✅ |
| Angular | 18.x | Frontend framework | ✅ |
| Node.js | 20.x LTS | Angular build & dev | ✅ |
| Python | 3.12.x | AI services | ✅ |
| FastAPI | 0.115.x | Python web framework | ✅ |
| LangChain | 0.3.x | LLM orchestration | ✅ |
| scikit-learn | 1.5.x | Fraud detection ML | ✅ |
| Docker | 24.x | Containerization | Recommended |
| Docker Compose | 2.x | Multi-container dev | Recommended |

---

## 10. Development Environment Setup

### Prerequisites

```bash
# Required software
1. JDK 21 (Eclipse Temurin or Oracle)
2. Maven 3.9+
3. Node.js 20 LTS + npm
4. Python 3.12+
5. MySQL 8.0+ (already installed - MySQL Workbench)
6. Docker Desktop (recommended for Kafka, Redis)
7. Git
8. IDE: IntelliJ IDEA (Java) + VS Code (Angular, Python)
```

### Quick Start

```bash
# 1. Start infrastructure (MySQL, Redis, Kafka)
docker-compose -f docker-compose.infra.yml up -d

# 2. Build shared library
cd vinusbank-common && mvn clean install

# 3. Start Discovery Server
cd vinusbank-discovery-server && mvn spring-boot:run

# 4. Start API Gateway
cd vinusbank-api-gateway && mvn spring-boot:run

# 5. Start microservices (each in separate terminal)
cd vinusbank-auth-service && mvn spring-boot:run
cd vinusbank-customer-service && mvn spring-boot:run
cd vinusbank-account-service && mvn spring-boot:run
cd vinusbank-transaction-service && mvn spring-boot:run
# ... etc.

# 6. Start AI services
cd vinusbank-ai-services/chatbot-service && pip install -r requirements.txt && uvicorn app.main:app --port 9001
cd vinusbank-ai-services/fraud-detection-service && pip install -r requirements.txt && uvicorn app.main:app --port 9002
cd vinusbank-ai-services/advisor-service && pip install -r requirements.txt && uvicorn app.main:app --port 9003

# 7. Start Angular frontend
cd vinusbank-frontend && npm install && ng serve
```

---

## 11. Naming Conventions

| Item | Convention | Example |
|------|-----------|---------|
| Java packages | `com.vinusbank.{service}.{layer}` | `com.vinusbank.account.service` |
| REST endpoints | lowercase, hyphen-separated | `/api/v1/bill-payments` |
| Database tables | lowercase, underscore | `suspicious_activity_reports` |
| Kafka topics | lowercase, dot-separated | `txn.completed` |
| Entity classes | PascalCase, singular | `Transaction`, `Account` |
| Service classes | PascalCase + "Service" | `AccountService` |
| Repository classes | PascalCase + "Repository" | `AccountRepository` |
| Controller classes | PascalCase + "Controller" | `AccountController` |
| DTO classes | PascalCase + "Request"/"Response" | `TransferRequest` |
| Angular components | kebab-case | `balance-overview` |
| Angular services | camelCase + ".service" | `auth.service.ts` |
| CSS/SCSS | BEM naming | `card__header--active` |
| Environment vars | UPPER_SNAKE_CASE | `MYSQL_ROOT_PASSWORD` |

---

## 12. Testing Strategy

| Test Type | Tool | Coverage Target | Scope |
|-----------|------|----------------|-------|
| **Unit Tests** | JUnit 5, Mockito | 80%+ | Service layer logic |
| **Integration Tests** | Spring Boot Test, TestContainers | Key flows | API endpoints, DB |
| **API Tests** | REST Assured / Postman | All endpoints | Contract validation |
| **Frontend Tests** | Jasmine, Karma | 70%+ | Component & service |
| **E2E Tests** | Cypress / Playwright | Critical paths | User journeys |
| **AI Model Tests** | pytest | Model accuracy | ML predictions |
| **Performance Tests** | JMeter / Gatling | SLA compliance | Load & stress |

---

*This architecture is designed for a full-featured banking platform that can be developed iteratively. Start with Phase 1 (Foundation) and build up progressively.*
