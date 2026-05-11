# 🛠️ VinUSBank Troubleshooting Log

This document tracks the errors we encounter during the development of VinUSBank, documenting exactly *why* they happened, *how* we diagnosed them, and the *exact steps* we took to fix them. This builds a robust knowledge base for our platform.

---

## Error 1: Docker Compose "Config File Not Found"

**Symptom:**
When running `docker compose up -d` in the `infrastructure` folder, the terminal returned: `no configuration file provided: not found`.

**Why it happened:**
By default, Docker expects the compose file to be named `docker-compose.yml`. Because we named our file `docker-compose.infra.yml` (to clarify that it's specifically for infrastructure), Docker didn't automatically detect it.

**How we diagnosed it:**
The user's screenshot showed the command run was exactly `docker-compose.infra.yml up -d`. In Windows, typing a filename directly tries to open it, instead of passing it to the `docker` command.

**The Solution:**
We explicitly passed the filename flag (`-f`) to the Docker Compose command.
```powershell
docker compose -f docker-compose.infra.yml up -d
```

---

## Error 2: Docker Port 3306 Binding Clash

**Symptom:**
Docker outputted: `ports are not available: exposing port TCP 0.0.0.0:3306 -> 127.0.0.1:0: listen tcp 0.0.0.0:3306: bind: Only one usage of each socket address is normally permitted.`

**Why it happened:**
Port `3306` is the universal default port for MySQL. The developer's Windows machine already had a native MySQL Server running in the background. When Docker tried to expose its containerized MySQL on the same port, Windows blocked it because two applications cannot share the exact same port simultaneously.

**How we diagnosed it:**
The phrase `Only one usage of each socket address is normally permitted` explicitly means the port is already occupied by a host process. 

**The Solution:**
Instead of forcing the user to shut down their local Windows MySQL service (which might disrupt their other projects), we re-routed the Docker MySQL out to port `3307`:
1. Updated `docker-compose.infra.yml`: mapped `3307:3306`.
2. Updated the `application.yml` files in both the `auth-service` and `customer-service` to connect to `3307` instead.
3. Restarted docker containers.

---

## Error 3: Spring Boot JPA Unable to Determine Dialect

**Symptom:**
When launching `AuthServiceApplication` from Eclipse, Spring Boot crashed with:
`Unable to determine Dialect without JDBC metadata (please set 'jakarta.persistence.jdbc.url' for common cases or 'hibernate.dialect'...)`

**Why it happened:**
This is a notoriously misleading error. It does not mean the Hibernate dialect is wrong; it means **Spring Boot failed to establish a connection to the database entirely**. As a result, it couldn't auto-detect the MySQL version to pick the dialect.
Furthermore, the database *was* running. So why the block? Because Java/Spring Boot usually resolves the string `localhost` to an **IPv6 address (`::1`)**, while Docker on Windows typically binds exposed ports to **IPv4 (`127.0.0.1`)**. 

**How we diagnosed it:**
1. Ran `docker ps -a` via terminal to visually confirm `vinusbank-mysql` was "healthy" and actively listening on port `3307`.
2. Ran `docker exec vinusbank-mysql mysql -uroot -prootpassword -e "SHOW DATABASES;"` to prove the inner container database and users were generated perfectly.
3. This narrowed the failure point exclusively to the JDBC network resolution mapping between Windows and Docker.

**The Solution:**
We forced Spring Boot to bypass IPv6 resolution by replacing the word `localhost` with the exact IPv4 address in the database connection string.

*Before (Failed):*
```yaml
url: jdbc:mysql://localhost:3307/vinusbank_auth?createDatabaseIfNotExist=true
```

*After (Fixed):*
```yaml
url: jdbc:mysql://127.0.0.1:3307/vinusbank_auth?createDatabaseIfNotExist=true
```
After making this change, Spring Boot established the connection flawlessly.

---

## Error 4: Postman Returns `401 Unauthorized` on Login/Register APIs

**Symptom:**
When testing the newly generated authentication endpoints via Postman (`POST /api/auth/register`), the response was a `401 Unauthorized`, despite providing the correct JSON payload.

**Why it happened:**
We added the `spring-boot-starter-security` dependency to our `pom.xml`. By design, the moment Spring Security detects it is on the classpath, it initiates a global lockdown. It automatically requires authentication for *every single endpoint* in the application.

**How we diagnosed it:**
The user successfully hit the gateway port, and the request was accurately routed to the `auth-service`, but Spring Security intercepted the request before it even reached the `AuthController` because the default `SecurityFilterChain` demands a password.

**The Solution:**
We created a custom `SecurityConfig` configuration file to selectively bypass this lockdown for specific public endpoints:
1. Disabled CSRF (since we will be using stateless JWTs).
2. Defined a custom `SecurityFilterChain`.
3. Granted exact `.permitAll()` routing to `/api/auth/**` and the Swagger UI paths `("/swagger-ui/**", "/v3/api-docs/**")`.

This setup effectively whitelisted our registration and login endpoints while keeping everything else secure!

---

## Error 5: Eclipse IDE Lombok Compilation Failure

**Symptom:**
Eclipse threw compile-time errors: `The method builder() is undefined for the type User` and `The method getEmail() is undefined for the type User`. Concurrently, Postman returned a `403 Forbidden` when attempting to hit the API endpoints.

**Why it happened:**
Project Lombok uses annotations (`@Data`, `@Builder`) to auto-generate getters, setters, and builders at compile-time. However, IDEs like Eclipse do not support Lombok natively out-of-the-box and require a custom plugin to understand these annotations. Because the plugin was missing, the Java files failed to compile, causing the Spring Application context to crash/fallback. The `403 Forbidden` simply indicated that the custom AuthController logic was offline and Spring Security reverted to defaults on the `/error` path.

**The Solution:**
The user correctly ran the Lombok installer `.jar` against their Eclipse installation (`eclipse.ini`). Once Eclipse rebooted with full Lombok support, the `@Builder` pattern immediately compiled successfully.

---

## Error 6: Gateway Throws `UnknownHostException: Failed to resolve 'MSI.mshome.net'`

**Symptom:**
When calling `http://localhost:8080/auth-service/api/auth/register`, Gateway returns a `500 Server Error`, and the Eclipse console prints: `java.net.UnknownHostException: Failed to resolve 'MSI.mshome.net'`.

**Why it happened:**
The API Gateway dynamically routes requests by asking the Eureka server for the destination's address. By default, Spring Cloud Eureka registers a microservice using its system **hostname**. On some Windows machines, the hostname resolves to a local Hyper-V or WSL alias (like `MSI.mshome.net`). The API Gateway attempts to parse this alias via DNS, fails, and cannot forward the HTTP traffic to the underlying microservice.

**How we diagnosed it:**
The stack trace explicitly cited `DnsResolveContext.finishResolve` failing to find the A-records for `MSI.mshome.net`.

**The Solution:**
We enforced IP-address-based registry instead of hostname registries so that Eureka passes standard, guaranteed IP strings (like `127.0.0.1`) back to the Gateway.
To do this, we updated the `application.yml` in all Eureka-linked modules (`auth-service`, `customer-service`, `api-gateway`):
```yaml
eureka:
  instance:
    prefer-ip-address: true
```
Upon restarting the Gateway and Microservices, routing was successfully fulfilled via the raw IP address.

---

## Error 7: `SQLIntegrityConstraintViolationException: Column 'first_name' cannot be null`

**Symptom:**
When calling the Customer Profile creation API (`POST http://localhost:8080/customer-service/api/customer/profile`) with a valid JWT, the service returns a `500 Internal Server Error`. The Eclipse console logs show a deep stack trace ending in `java.sql.SQLIntegrityConstraintViolationException: Column 'first_name' cannot be null`.

**Why it happened:**
In Java, the field was named `firstName`, but in the MySQL database, Hibernate automatically mapped it to `first_name`. The API was receiving a JSON payload, but due to subtle naming strategy mismatches or empty request bodies, Jackson (the JSON parser) was not populating the `firstName` field in the Java object.
Since the `Customer` entity had `@Column(nullable = false)`, Hibernate tried to execute an `INSERT` statement with a `NULL` value for `first_name`, which MySQL correctly rejected, causing the application to crash with a 500 error.

**How we diagnosed it:**
1.  **Log Analysis:** Looked at the Eclipse console for the `customer-service`. We found the exact SQL exception which pointed directly to the `first_name` column.
2.  **Entity Verification:** Checked `Customer.java` and confirmed that `firstName` was indeed marked as non-nullable. 
3.  **Trace Investigation:** Since the API was called through the Gateway, we verified that the `X-User-Email` header was reaching the service, but the `@RequestBody` object had null properties. This indicated a **JSON Deserialization failure**.
4.  **Hypothesis Testing:** We hypothesized that Jackson was unable to link the JSON key `"firstName"` to the Java field `firstName` consistently across different environments/naming strategies.

**The Solution:**
1.  **Explicit Mapping:** We added `@JsonProperty("firstName")` and `@JsonProperty("lastName")` to the `Customer` entity. This provides an absolute instruction to Jackson on how to map the JSON, removing any ambiguity.
2.  **Defensive Validation:** We added a manual validation check in `CustomerController.java`. Instead of letting the code proceed to a database crash, we added:
    ```java
    if (request.getFirstName() == null || request.getLastName() == null) {
        return ResponseEntity.badRequest().body("Error: firstName and lastName are required fields!");
    }
    ```
    This turns a "Server Crash" (500) into a "User Error" (400), which is a much safer and cleaner architectural pattern.

**How to fix in Postman:**
Ensure your JSON Body is set to "raw" and "JSON" and uses the correct CamelCase keys:
```json
{
    "firstName": "Vineet",
    "lastName": "Kumar",
    "phoneNumber": "1234567890",
    "address": "123 Wall Street, NY"
}
```
