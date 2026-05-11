# 🛠️ VinUSBank — Complete Setup Guide (Windows)
## Docker, Redis, Kafka & Ollama — Beginner Friendly

**For:** Developers who are new to Docker, Redis, Kafka, and Ollama  
**OS:** Windows  
**Time Required:** ~30 minutes

---

## 📖 What Are These Tools? (Quick Explanation)

Before installing, let's understand **why** we need each tool:

### 🐳 Docker
**What:** Docker is like a "virtual box" that runs software in isolated containers. Instead of installing Redis, Kafka, etc. directly on your Windows machine (which is messy and complicated), Docker lets you run them in clean, isolated containers with a single command.

**Analogy:** Think of Docker as running apps in separate "sealed rooms" inside your computer. Each room has exactly what the app needs, and when you're done, you can delete the room without affecting anything else on your PC.

**Why we need it:** To run Redis and Kafka easily without complex Windows installations.

---

### 🔴 Redis
**What:** Redis is an ultra-fast **in-memory database** (stores data in RAM, not disk). It's used for:
- **Caching** — Store frequently accessed data (like user sessions, account balances) in memory so we don't hit MySQL every time
- **Session Storage** — Store login sessions (JWT refresh tokens)
- **Rate Limiting** — Track how many API requests a user has made

**Analogy:** MySQL is like a filing cabinet (permanent but slow to search). Redis is like sticky notes on your desk (super fast to read but temporary).

**In VinUSBank:** When a user checks their balance, we cache it in Redis for 30 seconds. If they refresh the page, we serve it from Redis (instant) instead of querying MySQL again.

---

### 📨 Apache Kafka
**What:** Kafka is a **message broker** — it passes messages between our microservices asynchronously.

**Problem it solves:** When a user makes a transfer, we need to:
1. Update the account balance
2. Send a notification email
3. Check for fraud
4. Create an audit log
5. Generate a CTR if amount > $10,000

Without Kafka, the transfer API would have to do ALL of these things before responding to the user (slow!).

With Kafka, the transfer API just publishes a "transaction completed" message to Kafka, and all other services pick it up independently. The user gets an instant response.

**Analogy:** Kafka is like a post office. Service A drops a letter (event) in the mailbox, and Services B, C, D each get a copy. Nobody has to wait for anyone else.

**In VinUSBank:** 
```
Transaction Service → publishes "txn.completed" → Kafka
                                                      ├── Notification Service picks up → sends email
                                                      ├── Fraud Service picks up → runs ML check
                                                      └── Compliance Service picks up → checks if CTR needed
```

---

### 🦙 Ollama
**What:** Ollama lets you run **AI language models** (like ChatGPT) **locally on your PC** for free. No API keys, no internet required, no cost.

**Why not OpenAI/ChatGPT API?** 
- OpenAI costs money ($0.01+ per request)
- Requires internet connection
- Your banking data goes to OpenAI's servers (privacy concern)

**With Ollama:**
- Completely FREE
- Runs on YOUR machine
- Data never leaves your PC
- Works offline

**In VinUSBank:** Powers the "Vin Assistant" chatbot that helps customers check balances, understand transactions, and get financial advice — all processed locally.

---

## 🚀 Installation Steps

---

### Step 1: Install Docker Desktop

1. **Download Docker Desktop:**
   - Go to: **https://www.docker.com/products/docker-desktop/**
   - Click **"Download for Windows"**
   - Run the downloaded `.exe` installer

2. **During Installation:**
   - ✅ Check "Use WSL 2 instead of Hyper-V" (recommended)
   - Click "Ok" and let it install
   - **Restart your PC** when prompted

3. **After Restart:**
   - Docker Desktop should auto-launch (look for the 🐳 whale icon in your system tray)
   - Wait until the whale icon stops animating (Docker is starting up)
   - First launch may take 2-3 minutes

4. **If WSL 2 error appears:**
   - Docker may ask you to install/update WSL 2
   - Open PowerShell as Admin and run:
   ```powershell
   wsl --install
   ```
   - Restart your PC again

5. **Verify Docker is working:**
   - Open PowerShell and run:
   ```powershell
   docker --version
   ```
   - Expected output: `Docker version 27.x.x` (or similar)
   ```powershell
   docker compose version
   ```
   - Expected output: `Docker Compose version v2.x.x`

6. **Test Docker with a hello-world:**
   ```powershell
   docker run hello-world
   ```
   - If you see "Hello from Docker!" — Docker is working! ✅

---

### Step 2: Start Redis & Kafka using Docker

Once Docker Desktop is running, we'll use a single command to start Redis, Kafka, and Zookeeper.

1. **Create the Docker Compose file:**
   - We already have this file ready: `docker-compose.infra.yml` (I'll create it when we start coding)
   - For now, you can test Docker with a quick Redis container:

2. **Quick test — Run Redis:**
   ```powershell
   docker run -d --name test-redis -p 6379:6379 redis:7-alpine
   ```
   This downloads and starts Redis. Let's test it:
   ```powershell
   docker exec -it test-redis redis-cli ping
   ```
   - Expected output: `PONG` ✅
   - This means Redis is working!

3. **Clean up test container:**
   ```powershell
   docker stop test-redis
   docker rm test-redis
   ```

4. **Understanding Docker commands:**
   ```
   docker run         → Start a new container
   docker ps          → List running containers
   docker stop <name> → Stop a container
   docker rm <name>   → Remove a container
   docker compose up  → Start all services defined in a compose file
   docker compose down → Stop all services
   ```

---

### Step 3: Install Ollama

1. **Download Ollama:**
   - Go to: **https://ollama.com/download**
   - Click **"Download for Windows"**
   - Run the downloaded installer
   - Follow the installation wizard (just click Next/Install)

2. **Verify Ollama is installed:**
   - Open a **new** PowerShell window and run:
   ```powershell
   ollama --version
   ```
   - Expected output: `ollama version 0.x.x`

3. **Download an AI model:**
   - Ollama needs to download a model file (this is the actual AI brain)
   - For our chatbot, we'll use **Llama 3.1 (8B)** — a good balance of quality and speed
   
   ```powershell
   ollama pull llama3.1:8b
   ```
   
   - ⚠️ **This downloads ~4.7 GB** — will take some time depending on your internet
   - Wait until it shows "success"

4. **Test Ollama:**
   ```powershell
   ollama run llama3.1:8b "What is a checking account?"
   ```
   - You should see the AI respond with an explanation
   - Press `Ctrl+D` or type `/bye` to exit

5. **Check Ollama is running as a service:**
   - Ollama runs a local API server on port `11434`
   - Test it:
   ```powershell
   curl http://localhost:11434/api/tags
   ```
   - You should see JSON with your installed models

6. **Ollama System Requirements:**
   - **RAM:** At least 8 GB free (16 GB total recommended)
   - **Disk:** ~5 GB per model
   - **GPU:** Optional but recommended (NVIDIA GPU will make it much faster)
   - Works on CPU too, just slower responses (~5-10 seconds vs ~1-2 seconds with GPU)

---

### Step 4: Verify Everything

Open PowerShell and run these commands to confirm everything is ready:

```powershell
# 1. Java
java -version
# Expected: java version "18.x.x" ✅

# 2. Maven (comes with some Java installations, or install separately)
mvn -version
# If not found, see "Installing Maven" section below

# 3. Node.js
node -v
# Expected: v22.18.0 ✅

# 4. npm
npm -v
# Expected: 10.x.x ✅

# 5. Python
python --version
# Expected: Python 3.13.x ✅

# 6. Docker
docker --version
# Expected: Docker version 27.x.x ✅

# 7. Docker Compose
docker compose version
# Expected: Docker Compose version v2.x.x ✅

# 8. Ollama
ollama --version
# Expected: ollama version 0.x.x ✅

# 9. MySQL
mysql --version
# Expected: mysql Ver 8.x.x ✅
```

---

### Step 5: Install Maven (if not already installed)

Maven is the build tool for our Java/Spring Boot services.

1. **Check if Maven is installed:**
   ```powershell
   mvn -version
   ```

2. **If NOT installed:**
   - Go to: **https://maven.apache.org/download.cgi**
   - Download the **Binary zip archive** (e.g., `apache-maven-3.9.9-bin.zip`)
   - Extract it to `C:\Program Files\Apache\maven` (or wherever you prefer)
   - Add to PATH:
     - Open **Start Menu** → Search for **"Environment Variables"**
     - Click **"Edit the system environment variables"**
     - Click **"Environment Variables"** button
     - Under **System variables**, find **Path** → Click **Edit**
     - Click **New** → Add: `C:\Program Files\Apache\maven\bin`
     - Click OK on all dialogs
   - Open a **new** PowerShell window and verify:
   ```powershell
   mvn -version
   ```

3. **Also set JAVA_HOME** (Maven needs this):
   - In Environment Variables → System variables → **New**:
     - Variable name: `JAVA_HOME`
     - Variable value: Your JDK path (e.g., `C:\Program Files\Java\jdk-18`)
   - Verify:
   ```powershell
   echo $env:JAVA_HOME
   ```

---

### Step 6: Install Angular CLI

```powershell
npm install -g @angular/cli
```

Verify:
```powershell
ng version
```

---

## 📋 Quick Reference — Commands You'll Use Daily

### Docker Commands
```powershell
# Start VinUSBank infrastructure (Redis, Kafka, Zookeeper)
docker compose -f docker-compose.infra.yml up -d

# Check running containers
docker ps

# View logs of a container
docker logs vinusbank-redis
docker logs vinusbank-kafka

# Stop everything
docker compose -f docker-compose.infra.yml down

# Stop everything AND delete data
docker compose -f docker-compose.infra.yml down -v
```

### Redis Commands (for debugging/exploring)
```powershell
# Connect to Redis CLI inside the container
docker exec -it vinusbank-redis redis-cli

# Inside Redis CLI:
PING                          # Should return PONG
SET mykey "hello"             # Store a value
GET mykey                     # Retrieve a value → "hello"
KEYS *                        # List all keys
DEL mykey                     # Delete a key
TTL mykey                     # Check time-to-live
EXIT                          # Exit CLI
```

### Kafka Commands (for debugging/exploring)
```powershell
# List all topics
docker exec vinusbank-kafka kafka-topics --bootstrap-server localhost:9092 --list

# Create a topic manually
docker exec vinusbank-kafka kafka-topics --bootstrap-server localhost:9092 \
  --create --topic test-topic --partitions 1 --replication-factor 1

# Send a message to a topic (producer)
docker exec -it vinusbank-kafka kafka-console-producer \
  --bootstrap-server localhost:9092 --topic test-topic
# Type a message and press Enter

# Read messages from a topic (consumer)
docker exec -it vinusbank-kafka kafka-console-consumer \
  --bootstrap-server localhost:9092 --topic test-topic --from-beginning
```

### Ollama Commands
```powershell
# List installed models
ollama list

# Run a model interactively (chat mode)
ollama run llama3.1:8b

# Pull a new model
ollama pull llama3.1:8b

# Remove a model (free disk space)
ollama rm llama3.1:8b

# Check if Ollama API is running
curl http://localhost:11434/api/tags
```

---

## ⚠️ Troubleshooting

### Docker Desktop won't start
- Make sure **Virtualization** is enabled in BIOS
- Make sure **WSL 2** is installed: `wsl --install`
- Restart your PC after installing WSL 2

### Kafka won't start
- Make sure Zookeeper starts first (Docker Compose handles this automatically)
- Check logs: `docker logs vinusbank-kafka`
- If port 9092 is in use: `netstat -ano | findstr :9092`

### Ollama is slow
- It runs on CPU by default. If you have an NVIDIA GPU:
  - Install latest NVIDIA drivers
  - Ollama will auto-detect and use GPU
- Try a smaller model: `ollama pull llama3.2:3b` (faster but less capable)

### Redis connection refused
- Make sure Docker container is running: `docker ps`
- Check if port 6379 is free: `netstat -ano | findstr :6379`

### MySQL connection issues
- Default connection: `localhost:3306`
- User: `root` (or whatever you configured)
- Make sure MySQL service is running in Windows Services

---

## ✅ Ready Checklist

Before we start coding, confirm ALL of these:

| # | Item | Command to Verify | Expected |
|---|------|-------------------|----------|
| 1 | Java 18 | `java -version` | java 18.x |
| 2 | Maven | `mvn -version` | Apache Maven 3.9.x |
| 3 | Node.js 22 | `node -v` | v22.18.x |
| 4 | Angular CLI | `ng version` | Angular CLI 18.x |
| 5 | Python 3.13 | `python --version` | Python 3.13.x |
| 6 | MySQL running | `mysql -u root -p -e "SELECT 1"` | 1 |
| 7 | Docker running | `docker ps` | No error |
| 8 | Ollama running | `ollama list` | Shows llama3.1:8b |

**Once all 8 items show ✅, tell me and we'll start building VinUSBank!** 🚀
