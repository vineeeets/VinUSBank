# 🚀 Phase 1: Foundation – Testing & Code Guide

Welcome to the VinUSBank API Phase 1! We've established the Microservices backbone.

## 📁 What Code Was Written & Why?

### 1. `docker-compose.infra.yml` & `mysql-init.sql`
- **Why?** It automatically builds our exact infrastructure. It spins up MySQL, Redis, Zookeeper, and Kafka simultaneously.
- **Why mysql-init?** It automatically creates separate databases (`vinusbank_auth`, `vinusbank_customer`, etc.) on boot so that microservices remain decoupled.

### 2. `pom.xml` (Parent)
- **Why?** Spring Boot multi-module management. If we upgrade Spring Boot or OpenAPI versions, we only change it in this *root* `pom.xml`, and all services (`auth-service`, `api-gateway`, etc.) automatically update.

### 3. `discovery-service` (Port 8761)
- **Why?** It's the Eureka Server. Instead of hardcoding `http://localhost:8081` in the gateway, services register themselves here. The gateway asks Eureka: *"Where is the auth-service?"*

### 4. `api-gateway` (Port 8080)
- **Why?** The single entry point for our Angular Frontend and Postman. By configuring CORS here, we don't have to configure CORS in every individual microservice. It proxies routes dynamically.

### 5. `auth-service` (Port 8081)
- **Why?** Core authentication service containing basic Registration and Login logic (currently mock outputs preparing for Security Context). It connects directly to its own DB schema.

### 6. `customer-service` (Port 8082)
- **Why?** Maintains customer profiles.

---

## 🧪 Postman Testing Plan for Phase 1

### Prerequisite: Start the Infrastructure
1. Open PowerShell and navigate to the project infrastructure folder:
   ```powershell
   cd "D:\Antigravity Projects\infrastructure"
   docker-compose -f docker-compose.infra.yml up -d
   ```
2. Open **Eclipse IDE**.
3. Import the parent `pom.xml` project into Eclipse (`File > Import > Existing Maven Projects`).
4. Click "Run" on `DiscoveryServiceApplication`, `ApiGatewayApplication`, and `AuthServiceApplication`.

---

### Test Case 1: Validate infrastructure running
- **Tool:** Browser
- **Action:** Go to `http://localhost:8761`
- **Expected Result:** The Eureka UI dashboard appears.

### Test Case 2: Validate Gateway & Service Auto-Discovery
Before hitting the `auth-service` directly (port 8081), we hit it through the Gateway (port 8080) to prove routing works.
- **Tool:** Postman
- **Method:** POST
- **URL:** `http://localhost:8080/auth-service/api/auth/register`
- **Headers:** `Content-Type: application/json`
- **Body / Raw:**
  ```json
  {
      "email": "vineet@vinusbank.com",
      "password": "Password123!"
  }
  ```
- **Expected Output (200 OK):**
  ```json
  {
      "message": "User registered successfully (Mock Endpoint)",
      "email": "vineet@vinusbank.com"
  }
  ```

### Test Case 3: Verify Dynamic OpenAPI (Swagger UI) Dashboard
- **Tool:** Browser
- **Action:** Go to `http://localhost:8081/swagger-ui.html`
- **Expected Result:** A beautiful Swagger UI showing the `/api/auth/register` and `/api/auth/login` endpoints visually.

## 7. Core JWT Security Infrastructure (Added)
- **`JwtUtil.java`**: Responsible for actually building, signing, and parsing the JSON Web Tokens.
- **`UserDetailsServiceImpl.java`**: Spring Security's bridge to our MySQL database. It fetches the user by email so Spring knows who is logging in.
- **`SecurityConfig.java`**: The global lock manager. It disables CSRF, forces the app to be stateless, and dictates exactly which routes skip the 401 Unauthorized wall (e.g., `/api/auth/**`).
- **`AuthController.java`**: Receives the raw JSON, checks the Database through Spring Security Authentication Manager, hashes passwords using `BCrypt`, saves them, and returns the final JWT token to the front-end.

---

## 🧪 Postman Testing Plan for Phase 1 (JWT Update)

### Test Case 4: Verify Real User Registration to Database
- **Tool:** Postman
- **Method:** POST
- **URL:** `http://localhost:8080/auth-service/api/auth/register`
- **Headers:** `Content-Type: application/json`
- **Body / Raw JSON:**
  ```json
  {
      "email": "customer@vinusbank.com",
      "password": "SecurePassword123!"
  }
  ```
- **Expected Result:** Behind the scenes, `BCrypt` hashes the password and saves a new row to `vinusbank_auth.users`. You will get a 200 OK. Repeat calls will yield `Email is already in use!`.

### Test Case 5: Verify Login & JWT Generation
- **Method:** POST
- **URL:** `http://localhost:8080/auth-service/api/auth/login`
- **Body / Raw JSON:**
  ```json
  {
      "email": "customer@vinusbank.com",
      "password": "SecurePassword123!"
  }
  ```
- **Expected Output:**
  ```json
  {
      "message": "Login successful",
      "token": "eyJhbGciOiJIUzI1NiJ9..." 
  }
  ```
- **Why this matters:** The `token` string returned here is the master key for this user. During Phase 2, every time the customer tries to view their bank account or transfer money, they will pass this `token` strictly in the `Authorization: Bearer <token>` header of Postman.

### Test Case 6: Hitting the Customer Profile Firewall (401 Unauthorized)
Let's purposely try to break into the Customer Service without a token to prove the API Gateway edge security is actively defending it!
- **Method:** POST
- **URL:** `http://localhost:8080/customer-service/api/customer/profile`
- **Headers:** None.
- **Body / Raw JSON:**
  ```json
  {
      "firstName": "Vineet",
      "lastName": "Kumar",
      "phoneNumber": "1234567890",
      "address": "123 Wall Street, NY"
  }
  ```
- **Expected Result:** The Gateway will instantly intercept it and throw a `401 Unauthorized: Missing authorization header`. The request never even reaches the Customer Service!

### Test Case 7: Bypass the Firewall & Save Physical Profile (KYC)
Let's pass the token so the Gateway mathematically trusts us and routes us into the Customer schema.
- **Method:** POST
- **URL:** `http://localhost:8080/customer-service/api/customer/profile`
- **Headers:**
   - **Key:** `Authorization`
   - **Value:** `Bearer <PASTE_YOUR_COPIED_TOKEN_HERE>`
   - **Key:** `Content-Type`
   - **Value:** `application/json`
- **Body / Raw JSON:**
  ```json
  {
      "firstName": "Vineet",
      "lastName": "Kumar",
      "phoneNumber": "1234567890",
      "address": "123 Wall Street, NY"
  }
  ```
- **Expected Output (200 OK):**
  ```json
  {
      "message": "Customer profile saved successfully",
      "profile": {
          "id": 1,
          "email": "customer@vinusbank.com",
          "firstName": "Vineet",
          "lastName": "Kumar",
          "kycStatus": "PENDING"
      }
  }
  ```

### Test Case 8: Fetching the Profile (GET Request)
Let's prove the data persisted securely to the MySQL datastore.
- **Method:** GET
- **URL:** `http://localhost:8080/customer-service/api/customer/profile`
- **Headers:**
   - **Key:** `Authorization`
   - **Value:** `Bearer <PASTE_YOUR_COPIED_TOKEN_HERE>`
- **Expected Output:** You should instantly retrieve the exact JSON payload you saved above!

## 📝 A Note on System Logs (Phase 1 & 2)
All backend service requests and their operations are now thoroughly logged to ease debugging:
- **Console:** Each service produces vividly colored, formatted outputs indicating operation statuses (e.g. `[AUTH-CTRL] ✓ Registration successful`).
- **File System:** Rotating log files are saved automatically within the `logs/` directory of each service folder (e.g. `backend/auth-service/logs/auth-service.log`).

---

## ⏭️ Next Step: Angular Frontend
With our backend Phase 1 (Auth + Customer KYC) 100% complete and protected, we are ready to generate the **Angular Frontend Workspace**. This will allow us to physically test this backend end-to-end via a web browser!

