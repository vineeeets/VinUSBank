# 🧠 Agent Context — VinUS Bank Project

> **Purpose:** This file captures the COMPLETE context from ALL previous Antigravity AI chat sessions so that any new IDE session can pick up exactly where we left off. **Read this file first** to understand the project, its full history, and user preferences.

---

## 👤 User Profile

- **Name:** Vineet
- **OS:** Windows
- **IDE:** Eclipse (for Java/Spring Boot backend), VS Code with Antigravity extension (for Angular frontend & AI chat)
- **Workspace Path:** `D:\Antigravity Projects`
- **GitHub Repo:** `https://github.com/vineeeets/VinUSBank.git` (monorepo, branch: `main`)

---

## 📦 Project Overview — VinUSBank

VinUSBank is a **full-featured US digital banking platform** built on a microservices architecture. Originally named "NovaBanK", it was renamed to VinUSBank. The system simulates a real-world US banking experience with regulatory compliance (KYC/AML/BSA), real-time transaction processing, and AI-powered bots.

### Tech Stack

| Layer | Technology |
|-------|------------|
| Frontend | Angular 18+ (TypeScript) |
| API Gateway | Spring Cloud Gateway |
| Backend | Spring Boot 3.x (Java 18) |
| AI Services | Python 3.12 + FastAPI (planned) |
| Database | MySQL 8.x (separate DB per microservice) |
| Cache | Redis 7.x (via Docker) |
| Message Broker | Apache Kafka (via Docker) |
| Auth | Spring Security + JWT |
| Service Discovery | Eureka |
| LLM | Ollama (local, free) with Llama 3.1:8b |
| Containerization | Docker + Docker Compose |

### Project Structure

```
D:\Antigravity Projects\
├── backend\
│   ├── pom.xml                    # Parent Maven POM (multi-module)
│   ├── discovery-service\         # Eureka Service Registry (:8761)
│   ├── api-gateway\               # Spring Cloud Gateway (:8080) + JWT AuthenticationFilter
│   ├── auth-service\              # Login, Register, JWT generation (:8081)
│   ├── customer-service\          # Customer profiles, KYC (:8082)
│   ├── account-service\           # Bank accounts, balance, internal debit/credit (:8083)
│   ├── transaction-service\       # Fund transfers via Feign (:8084)
│   ├── loan-service\              # Loan applications, EMI calculator (:8085)
│   └── card-service\              # Virtual debit cards (:8086)
├── frontend\                      # Angular SPA (dark mode, glassmorphism)
│   ├── src\
│   ├── proxy.conf.json            # Proxies API calls → localhost:8080
│   ├── angular.json
│   └── package.json
├── infrastructure\
│   ├── docker-compose.infra.yml   # MySQL(:3307), Redis(:6379), Kafka(:9092), Zookeeper(:2181)
│   └── mysql-init.sql             # Creates separate schemas per microservice
├── docs\                          # 10 comprehensive documents
│   ├── 01-HIGH-LEVEL-DESIGN.md
│   ├── 02-LOW-LEVEL-DESIGN.md
│   ├── 03-ARCHITECTURE.md
│   ├── 04-AI-BOTS-DESIGN.md
│   ├── 05-SETUP-GUIDE.md
│   ├── 06-PHASE1-TESTING-GUIDE.md
│   ├── 07-TROUBLESHOOTING-LOG.md
│   ├── 08-LEARNINGS-AND-PATTERNS.md
│   ├── 09-FRONTEND-TROUBLESHOOTING.md
│   └── 10-PHASE2-TESTING-GUIDE.md
└── AGENT_CONTEXT.md               # ← This file
```

### Services & Ports

| Service | Port | Status |
|---------|------|--------|
| API Gateway | 8080 | ✅ Built (⚠️ port conflict with AgentService.exe — see Session 7) |
| Discovery Service (Eureka) | 8761 | ✅ Built |
| Auth Service | 8081 | ✅ Built |
| Customer Service | 8082 | ✅ Built |
| Account Service | 8083 | ✅ Built |
| Transaction Service | 8084 | ✅ Built |
| Loan Service | 8085 | ✅ Built |
| Card Service | 8086 | ✅ Built |
| Compliance Service | 8087 | 🔲 Planned (Phase 3) |
| Notification Service | 8088 | 🔲 Planned (Phase 3) |
| Chatbot Service (Python) | 9001 | 🔲 Planned (Phase 4) |
| Fraud Detection (Python) | 9002 | 🔲 Planned (Phase 4) |
| Advisor Service (Python) | 9003 | 🔲 Planned (Phase 4) |

### MySQL Databases (Docker on port 3307, NOT 3306)

- `vinusbank_auth` — Users, credentials, JWT
- `vinusbank_customer` — Customer profiles
- `vinusbank_accounts` — Bank accounts, balances
- `vinusbank_transactions` — Transfer records
- `vinusbank_loans` — Loan applications, EMI
- `vinusbank_cards` — Virtual debit cards

> ⚠️ **Important:** Docker MySQL runs on port **3307** (not 3306) to avoid conflict with the user's native Windows MySQL on 3306. All `application.yml` files use `127.0.0.1:3307` (IPv4 explicitly, NOT `localhost`, to avoid IPv6 resolution issues).

---

## 🕰️ Complete Conversation History (Chronological)

### Session 1 — Project Planning & Design (April 11, 2026)

**What happened:** Vineet asked to build a complete US digital banking platform. We went through a full planning/design session.

**Work done:**
- Assessed user's dev environment (Java 18, Node.js 22, Python 3.13, MySQL 8.x)
- Created 4 comprehensive design documents:
  - `01-HIGH-LEVEL-DESIGN.md` — System overview, 13 microservices, security architecture
  - `02-LOW-LEVEL-DESIGN.md` — 8 databases, 25+ tables with DDL, 80+ API endpoints
  - `03-ARCHITECTURE.md` — Communication patterns, JWT auth flow, Docker setup
  - `04-AI-BOTS-DESIGN.md` — 3 AI bots (Chatbot, Fraud Detection, Financial Advisor)
- Implementation plan went through 3 revisions:
  - v0: Full Docker stack
  - v1: Adapted for no Docker (Caffeine cache, Spring Events, rule-based NLP)
  - v2 (final): Back to full Docker + Ollama after Vineet decided to install them

**Key decisions:**
- Project name: VinUSBank (originally NovaBanK)
- Phased delivery: Phase 1 → 2 → 3 → 4
- Ollama chosen for local LLM (free, private, offline)
- Docker for Redis & Kafka

---

### Session 2 — Phase 1 & 2 Full Implementation (April 14-20, 2026)

**What happened:** This was the **main build session** — the longest and most productive. Built the entire Phase 1 and Phase 2 backend + Angular frontend.

**Phase 1 Backend (all completed ✅):**
1. Infrastructure: `docker-compose.infra.yml` with MySQL, Redis, Kafka, Zookeeper
2. Parent `pom.xml` for multi-module Maven
3. `discovery-service` — Eureka Registry
4. `api-gateway` — Spring Cloud Gateway + custom `AuthenticationFilter.java` for JWT validation
5. `auth-service` — JWT generation, Spring Security, User entity, login/register
6. `customer-service` — Customer profiles with `X-User-Email` header trust

**Phase 2 Backend (all completed ✅):**
7. `account-service` — Account lifecycle, balance, internal Feign endpoints for debit/credit. Account numbers: `VUS` + 10-digit suffix
8. `transaction-service` — Fund transfers via Feign client. Reference numbers: `VUB` + date + sequential
9. `loan-service` — Applications, EMI calculator, auto-approve loans < $10,000, disbursement via account-service
10. `card-service` — Virtual debit cards, lifecycle (PENDING → ACTIVE → BLOCKED), masked card numbers

**Angular Frontend (completed ✅):**
- AuthService, JwtInterceptor, LoginComponent, RegisterComponent
- CustomerProfileComponent
- Dashboard with 5 tabs: Overview, Accounts, Transfers, Loans, Cards
- Premium dark mode design with glassmorphism aesthetics
- Proxy config routing to gateway on port 8080

**7 Major Backend Errors Diagnosed & Fixed:**
1. Docker Compose "config file not found" → needed `-f` flag
2. Port 3306 clash → remapped Docker MySQL to 3307
3. JPA dialect error → IPv6/IPv4 mismatch → use `127.0.0.1` not `localhost`
4. 401 on auth endpoints → Spring Security default lockdown → custom `SecurityConfig` with `permitAll()`
5. Eclipse Lombok compilation failure → installed Lombok plugin for Eclipse
6. Gateway `UnknownHostException: MSI.mshome.net` → Eureka hostname issue → `prefer-ip-address: true`
7. `SQLIntegrityConstraintViolationException: first_name null` → JSON deserialization fix with `@JsonProperty`

**2 Frontend Errors Diagnosed & Fixed:**
1. Blank screen → missing `<base href="/">` in `index.html`
2. Blank screen → missing `zone.js` dependency

**Key Architectural Pattern Established:**
- **Edge Security Pattern** — JWT validated ONLY at API Gateway. Gateway strips token, injects `X-User-Email` header. Downstream services are "security-dumb" and trust the header. Documented in `08-LEARNINGS-AND-PATTERNS.md`.

---

### Session 3 — Phase 2 Subagent Task (April 21, 2026)

**What happened:** A long-running subagent completed additional Phase 2 work — expanding the Angular dashboard and finalizing backend services.

**Work done:**
- Updated `mysql-init.sql` with 4 new databases
- Updated parent `pom.xml` with 4 new modules
- Updated API Gateway routes for new services
- Updated Angular proxy config
- Created/finalized all 4 Phase 2 microservices
- Expanded Angular dashboard from 2 tabs to 5 tabs
- Created `10-PHASE2-TESTING-GUIDE.md`

---

### Session 4 — GitHub Repository Setup (May 11, 2026)

**What happened:** Vineet wanted to push the project to GitHub.

**Work done:**
- Created `.gitignore` (excludes IDE files, compiled Java, crash logs, node_modules)
- Discussed **monorepo vs multi-repo** → chose **monorepo** because:
  - Solo developer
  - Easier portfolio showcase
  - Synchronized commits
  - Modern platforms support root directory settings
- Successfully pushed 959 objects (54.33 MiB) to GitHub

**Deployment strategy discussed:**
- Frontend → Vercel (root: `frontend`)
- Backend → Railway/Render (root: `backend`)

**Result:** Code live at `https://github.com/vineeeets/VinUSBank`

---

### Session 5 — Port 8080 Conflict Debugging (May 20, 2026)

**What happened:** Vineet reported port 8080 always in use when starting API Gateway in Eclipse.

**Investigation:**
- Confirmed Docker infra does NOT use port 8080
- Found **`AgentService.exe`** (PID 4704) — a Windows background service — was occupying port 8080

**Solutions proposed (awaiting user decision):**
1. **(Recommended)** Change API Gateway to port 8090 + update Angular proxy
2. Disable/uninstall `AgentService.exe`

> ⚠️ **PENDING:** User has NOT yet decided which option to go with. This should be resolved before next backend development session.

---

### Session 6 — Restoring IDE Interface (May 20, 2026)

**What happened:** VS Code IDE interface disappeared after an update. Troubleshot and restored successfully.

---

### Session 7 — Chat History Sync Issue (May 21, 2026)

**What happened:** Antigravity extension in IDE not showing chat history. Clock icon appeared blocked/disabled. Multiple troubleshooting steps tried (reload, re-auth, check logs, restart). History still not syncing. Created this `AGENT_CONTEXT.md` file as a workaround.

---

## 🏗️ Current Project Status

| Phase | Status | Details |
|-------|--------|---------|
| Phase 1 — Foundation | ✅ Complete | Discovery, Gateway, Auth, Customer + Angular shell |
| Phase 2 — Core Banking | ✅ Complete | Account, Transaction, Loan, Card + Dashboard tabs |
| Phase 3 — Compliance & Security | 🔲 Not started | Compliance service, Notification service, Audit trail, MFA |
| Phase 4 — AI Integration | 🔲 Not started | Chatbot (LangChain + Ollama), Fraud ML, Financial Advisor |
| Phase 5 — Polish & Launch | 🔲 Not started | E2E testing, performance, documentation |

### Open Issues / Pending Decisions

1. **Port 8080 conflict** — `AgentService.exe` occupies port 8080. Need to either change Gateway to port 8090 or disable the service.
2. **No Kafka integration yet** — Phase 2 used Feign REST calls. Kafka event-driven communication planned for Phase 3.
3. **Phase 3 not started** — Compliance service, Notification service, audit trails, SAR/CTR reports.

---

## 🧩 Key Architectural Patterns to Remember

1. **Edge Security Pattern** — JWT auth only at Gateway. Internal services trust `X-User-Email` header.
2. **Feign Clients** — Inter-service communication (transaction→account, loan→account).
3. **Separate databases per service** — Each microservice has its own MySQL schema.
4. **IP-based Eureka registration** — All services use `prefer-ip-address: true` to avoid Windows hostname issues.
5. **Docker MySQL on port 3307** — Avoids conflict with native Windows MySQL on 3306.
6. **JDBC uses `127.0.0.1`** — Never `localhost`, to avoid Java IPv6 resolution to Docker IPv4.

---

## ⚙️ User Preferences

- Prefers working inside the **IDE** rather than the web interface
- Uses **Eclipse** for Java backend development
- Uses **Postman** for API testing
- Wants **premium dark mode** UI with glassmorphism for Angular frontend
- Prefers **detailed troubleshooting documentation** (why it happened + how we fixed it)
- Likes **beginner-friendly explanations** with analogies
- **Monorepo** approach for the project

---

## 🚀 How to Use This File

When starting a new chat in the IDE, say:

> "Read the `AGENT_CONTEXT.md` file in the project root to understand my project and our previous work together."

This will bring the AI agent up to speed instantly with the full project context, all 7 sessions of history, architectural decisions, open issues, and your preferences.

---

*Last updated: May 21, 2026 — Covers all 7 chat sessions*
