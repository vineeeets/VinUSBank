# 🤖 VinUSBank — AI Bots Detailed Design
## AI Services Architecture & Implementation Guide

**Version:** 1.0  
**Date:** April 11, 2026  
**Author:** Vineet  
**Status:** Draft — Awaiting Review

---

## 1. AI Services Overview

VinUSBank integrates three AI-powered services, all built with **Python + FastAPI** and communicating with the Spring Boot backend via REST and Kafka.

| Bot | Service | Port | Purpose |
|-----|---------|------|---------|
| 🗣️ **Vin Assistant** | chatbot-service | 9001 | Conversational banking assistant |
| 🔍 **FraudGuard** | fraud-detection-service | 9002 | Real-time transaction fraud scoring |
| 📊 **WealthWise** | advisor-service | 9003 | Financial insights & recommendations |

---

## 2. Vin Assistant — AI Chatbot

### 2.1 Technology Stack

| Component | Technology | Purpose |
|-----------|------------|---------|
| Framework | FastAPI | REST API server |
| LLM Orchestration | LangChain 0.3.x | Agent framework with tools |
| LLM Provider | OpenAI GPT-4o / Ollama (local) | Language model |
| Memory | Redis | Conversation history |
| Vector Store | ChromaDB (embedded) | FAQ & knowledge search |
| Guardrails | Custom middleware | Response safety filtering |

### 2.2 Architecture

```
┌─────────────────────────────────────────────────────────────────────────┐
│                        Vin Assistant SERVICE                           │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                         │
│  ┌─────────────────────────────────────────────────────────────────┐   │
│  │                    FastAPI Application                           │   │
│  │                                                                  │   │
│  │  POST /api/v1/ai/chat                                           │   │
│  │  GET  /api/v1/ai/chat/history                                   │   │
│  │  POST /api/v1/ai/chat/feedback                                  │   │
│  │  GET  /api/v1/ai/health                                         │   │
│  └────────────────────────────┬────────────────────────────────────┘   │
│                               │                                         │
│  ┌────────────────────────────▼────────────────────────────────────┐   │
│  │                  Authentication Middleware                       │   │
│  │  - Validates JWT from X-User-Id header (passed by Gateway)      │   │
│  │  - Extracts user context (userId, customerId, roles)            │   │
│  └────────────────────────────┬────────────────────────────────────┘   │
│                               │                                         │
│  ┌────────────────────────────▼────────────────────────────────────┐   │
│  │                 Intent Classifier (Pre-LLM)                     │   │
│  │                                                                  │   │
│  │  Fast keyword/pattern matching for common intents:               │   │
│  │  ┌──────────────────┬──────────────────────────────────────────┐ │   │
│  │  │ Intent           │ Action                                    │ │   │
│  │  ├──────────────────┼──────────────────────────────────────────┤ │   │
│  │  │ BALANCE_INQUIRY  │ → Direct tool call (skip LLM)            │ │   │
│  │  │ TRANSACTION_LIST │ → Direct tool call (skip LLM)            │ │   │
│  │  │ HELP / FAQ       │ → Vector search + LLM summary            │ │   │
│  │  │ COMPLEX_QUERY    │ → Full LLM agent pipeline                │ │   │
│  │  │ ESCALATE         │ → Route to human agent                   │ │   │
│  │  │ OUT_OF_SCOPE     │ → Polite decline                         │ │   │
│  │  └──────────────────┴──────────────────────────────────────────┘ │   │
│  └────────────────────────────┬────────────────────────────────────┘   │
│                               │                                         │
│  ┌────────────────────────────▼────────────────────────────────────┐   │
│  │                  LangChain ReAct Agent                          │   │
│  │                                                                  │   │
│  │  System Prompt:                                                  │   │
│  │  "You are Nova, VinUSBank's AI banking assistant.                 │   │
│  │   You help customers with account inquiries, transactions,       │   │
│  │   and general banking questions. You NEVER provide specific      │   │
│  │   financial advice or investment recommendations. You ALWAYS     │   │
│  │   confirm before executing financial transactions. You NEVER     │   │
│  │   reveal internal system details or customer PII in responses."  │   │
│  │                                                                  │   │
│  │  Available Tools:                                                │   │
│  │  ┌────────────────────────────────────────────────────────────┐  │   │
│  │  │ check_balance(account_id?)        → Account Service        │  │   │
│  │  │ get_transactions(account_id, n?)  → Transaction Service    │  │   │
│  │  │ get_account_details(account_id?)  → Account Service        │  │   │
│  │  │ get_loan_status(loan_id?)         → Loan Service           │  │   │
│  │  │ calculate_emi(amount, rate, term) → Loan Service           │  │   │
│  │  │ get_card_info(card_id?)           → Card Service           │  │   │
│  │  │ search_faq(query)                → ChromaDB vector search  │  │   │
│  │  │ get_spending_summary(period)      → Advisor Service        │  │   │
│  │  │ escalate_to_human(reason)         → Notification Service   │  │   │
│  │  └────────────────────────────────────────────────────────────┘  │   │
│  └────────────────────────────┬────────────────────────────────────┘   │
│                               │                                         │
│  ┌────────────────────────────▼────────────────────────────────────┐   │
│  │                  Response Guardrails                             │   │
│  │                                                                  │   │
│  │  1. PII Detection & Masking                                     │   │
│  │     - SSN patterns → ****                                       │   │
│  │     - Full account numbers → ****1234                           │   │
│  │     - Email/phone → partial mask                                │   │
│  │                                                                  │   │
│  │  2. Financial Advice Filter                                     │   │
│  │     - Block investment advice                                   │   │
│  │     - Block interest rate predictions                           │   │
│  │     - Block specific stock/crypto recommendations               │   │
│  │                                                                  │   │
│  │  3. Hallucination Check                                         │   │
│  │     - Verify amounts match tool outputs                         │   │
│  │     - Cross-check dates and account numbers                     │   │
│  │     - Flag unverifiable claims                                  │   │
│  │                                                                  │   │
│  │  4. Tone & Professionalism                                      │   │
│  │     - Ensure professional banking language                      │   │
│  │     - Remove any inappropriate content                          │   │
│  └────────────────────────────┬────────────────────────────────────┘   │
│                               │                                         │
│  ┌────────────────────────────▼────────────────────────────────────┐   │
│  │                  Conversation Memory (Redis)                    │   │
│  │                                                                  │   │
│  │  Key: chat:session:{sessionId}                                  │   │
│  │  TTL: 30 minutes                                                │   │
│  │  Max Messages: 20 (sliding window)                              │   │
│  │                                                                  │   │
│  │  Structure:                                                     │   │
│  │  {                                                              │   │
│  │    "userId": "user-uuid",                                      │   │
│  │    "customerId": "cust-uuid",                                  │   │
│  │    "messages": [                                                │   │
│  │      {"role": "user", "content": "...", "timestamp": "..."},   │   │
│  │      {"role": "assistant", "content": "...", "timestamp": "..."}│   │
│  │    ],                                                           │   │
│  │    "context": {                                                 │   │
│  │      "lastIntent": "BALANCE_INQUIRY",                          │   │
│  │      "accountsAccessed": ["acc-123"]                            │   │
│  │    }                                                            │   │
│  │  }                                                              │   │
│  └─────────────────────────────────────────────────────────────────┘   │
│                                                                         │
└─────────────────────────────────────────────────────────────────────────┘
```

### 2.3 Python Project Structure

```
chatbot-service/
├── app/
│   ├── __init__.py
│   ├── main.py                    # FastAPI application entry
│   ├── config.py                  # Configuration & env vars
│   │
│   ├── routes/
│   │   ├── __init__.py
│   │   ├── chat.py                # Chat endpoints
│   │   └── health.py              # Health check
│   │
│   ├── services/
│   │   ├── __init__.py
│   │   ├── chat_service.py        # Main chat orchestration
│   │   ├── intent_classifier.py   # Pre-LLM intent detection
│   │   ├── memory_service.py      # Redis conversation memory
│   │   └── guardrails.py          # Response safety filters
│   │
│   ├── agents/
│   │   ├── __init__.py
│   │   ├── banking_agent.py       # LangChain ReAct agent
│   │   └── prompts.py             # System & tool prompts
│   │
│   ├── tools/
│   │   ├── __init__.py
│   │   ├── account_tools.py       # Balance, account detail tools
│   │   ├── transaction_tools.py   # Transaction history tools
│   │   ├── loan_tools.py          # Loan info & calculator tools
│   │   ├── card_tools.py          # Card info tools
│   │   ├── faq_tools.py           # FAQ vector search tool
│   │   └── escalation_tools.py    # Human escalation tool
│   │
│   ├── clients/
│   │   ├── __init__.py
│   │   ├── account_client.py      # HTTP client → Account Service
│   │   ├── transaction_client.py  # HTTP client → Transaction Service
│   │   ├── loan_client.py         # HTTP client → Loan Service
│   │   └── base_client.py         # Base HTTP client with retry
│   │
│   ├── models/
│   │   ├── __init__.py
│   │   ├── chat_models.py         # Pydantic request/response models
│   │   └── intent_models.py       # Intent classification models
│   │
│   ├── middleware/
│   │   ├── __init__.py
│   │   ├── auth_middleware.py     # JWT validation
│   │   └── logging_middleware.py  # Request/response logging
│   │
│   └── data/
│       └── banking_faq.json       # FAQ knowledge base
│
├── tests/
│   ├── test_chat_service.py
│   ├── test_guardrails.py
│   └── test_intent_classifier.py
│
├── requirements.txt
├── Dockerfile
└── .env.example
```

### 2.4 Key Implementation Details

**Chat Flow pseudocode:**
```python
async def process_message(user_message: str, session_id: str, user_context: dict):
    # 1. Load conversation history from Redis
    history = await memory_service.get_history(session_id)
    
    # 2. Pre-classify intent (fast path for simple queries)
    intent = intent_classifier.classify(user_message)
    
    if intent in DIRECT_TOOL_INTENTS:
        # 3a. Direct tool call (skip LLM for speed)
        result = await execute_direct_tool(intent, user_context)
        response = format_direct_response(intent, result)
    else:
        # 3b. Full LangChain agent pipeline
        agent = create_banking_agent(user_context)
        response = await agent.ainvoke({
            "input": user_message,
            "chat_history": history,
            "user_context": user_context
        })
    
    # 4. Apply guardrails
    safe_response = guardrails.filter(response)
    
    # 5. Save to conversation memory
    await memory_service.add_message(session_id, user_message, safe_response)
    
    # 6. Return response with suggested actions
    return ChatResponse(
        response=safe_response,
        intent=intent,
        suggested_actions=get_suggestions(intent)
    )
```

---

## 3. FraudGuard — Fraud Detection Service

### 3.1 Technology Stack

| Component | Technology | Purpose |
|-----------|------------|---------|
| Framework | FastAPI | REST API server |
| ML Model | scikit-learn (Random Forest) | Fraud classification |
| Feature Engineering | pandas, numpy | Data preprocessing |
| Model Serving | joblib | Model serialization |
| Training | Jupyter notebooks | Model development |
| Data | Synthetic dataset | Training & validation |

### 3.2 Architecture

```
┌─────────────────────────────────────────────────────────────────────────┐
│                      FRAUDGUARD SERVICE                                 │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                         │
│  ┌─────────────────────────────────────────────────────────────────┐   │
│  │                    FastAPI Endpoints                             │   │
│  │                                                                  │   │
│  │  POST /api/v1/ai/fraud/check      → Real-time fraud scoring    │   │
│  │  GET  /api/v1/ai/fraud/alerts     → Get fraud alerts           │   │
│  │  POST /api/v1/ai/fraud/feedback   → Confirm/deny fraud flag    │   │
│  │  GET  /api/v1/ai/fraud/stats      → Fraud statistics           │   │
│  │  GET  /api/v1/ai/health           → Health check               │   │
│  └────────────────────────────┬────────────────────────────────────┘   │
│                               │                                         │
│  ┌────────────────────────────▼────────────────────────────────────┐   │
│  │                  Feature Engineering Pipeline                   │   │
│  │                                                                  │   │
│  │  Raw Transaction Data → Feature Vector                          │   │
│  │                                                                  │   │
│  │  Features Extracted:                                            │   │
│  │  ┌──────────────────────────────────────────────────────────┐   │   │
│  │  │ 1.  amount                 (normalized)                  │   │   │
│  │  │ 2.  amount_deviation       (vs user's avg)               │   │   │
│  │  │ 3.  hour_of_day            (0-23)                        │   │   │
│  │  │ 4.  day_of_week            (0-6)                         │   │   │
│  │  │ 5.  is_weekend             (0/1)                         │   │   │
│  │  │ 6.  is_night               (0/1, 11pm-5am)              │   │   │
│  │  │ 7.  txn_count_last_1h      (velocity)                   │   │   │
│  │  │ 8.  txn_count_last_24h     (velocity)                   │   │   │
│  │  │ 9.  txn_amount_last_1h     (cumulative)                 │   │   │
│  │  │ 10. avg_txn_amount_30d     (baseline)                   │   │   │
│  │  │ 11. max_txn_amount_30d     (baseline)                   │   │   │
│  │  │ 12. is_new_device          (0/1)                        │   │   │
│  │  │ 13. is_new_location        (0/1)                        │   │   │
│  │  │ 14. distance_from_home     (km, bucketed)               │   │   │
│  │  │ 15. account_age_days       (maturity)                   │   │   │
│  │  │ 16. previous_fraud_count   (history)                    │   │   │
│  │  │ 17. merchant_risk_score    (category-based)             │   │   │
│  │  │ 18. transaction_type_enc   (one-hot encoded)            │   │   │
│  │  │ 19. is_international       (0/1)                        │   │   │
│  │  │ 20. amount_round_flag      (suspicious round amounts)   │   │   │
│  │  └──────────────────────────────────────────────────────────┘   │   │
│  └────────────────────────────┬────────────────────────────────────┘   │
│                               │                                         │
│  ┌────────────────────────────▼────────────────────────────────────┐   │
│  │                  ML Model Inference                              │   │
│  │                                                                  │   │
│  │  Model: Random Forest Classifier (or Gradient Boosting)         │   │
│  │  Input: 20-feature vector                                       │   │
│  │  Output: fraud_probability (0.0 → 1.0)                         │   │
│  │                                                                  │   │
│  │  Decision Thresholds:                                           │   │
│  │  ┌────────────────┬───────────┬────────────────────────────┐   │   │
│  │  │ Score Range     │ Action    │ Description                │   │   │
│  │  ├────────────────┼───────────┼────────────────────────────┤   │   │
│  │  │ 0.00 - 0.30    │ APPROVE   │ Low risk, proceed          │   │   │
│  │  │ 0.30 - 0.70    │ REVIEW    │ Medium risk, flag + hold   │   │   │
│  │  │ 0.70 - 1.00    │ BLOCK     │ High risk, block + alert   │   │   │
│  │  └────────────────┴───────────┴────────────────────────────┘   │   │
│  │                                                                  │   │
│  │  Explainability: Feature importance + top contributing factors  │   │
│  └────────────────────────────┬────────────────────────────────────┘   │
│                               │                                         │
│  ┌────────────────────────────▼────────────────────────────────────┐   │
│  │                  Response Builder                                │   │
│  │                                                                  │   │
│  │  {                                                              │   │
│  │    "transactionId": "txn-uuid",                                │   │
│  │    "fraudScore": 0.82,                                         │   │
│  │    "riskLevel": "HIGH",                                        │   │
│  │    "action": "BLOCK",                                          │   │
│  │    "reasons": [                                                 │   │
│  │      "Unusual transaction amount (5x above average)",           │   │
│  │      "Transaction from unrecognized device",                    │   │
│  │      "Transaction at unusual hour (3:15 AM)"                    │   │
│  │    ],                                                           │   │
│  │    "topFeatures": {                                             │   │
│  │      "amount_deviation": 0.35,                                  │   │
│  │      "is_new_device": 0.25,                                    │   │
│  │      "is_night": 0.20                                          │   │
│  │    }                                                            │   │
│  │  }                                                              │   │
│  └─────────────────────────────────────────────────────────────────┘   │
│                                                                         │
└─────────────────────────────────────────────────────────────────────────┘
```

### 3.3 Python Project Structure

```
fraud-detection-service/
├── app/
│   ├── __init__.py
│   ├── main.py                       # FastAPI entry point
│   ├── config.py                     # Configuration
│   │
│   ├── routes/
│   │   ├── __init__.py
│   │   ├── fraud_check.py            # Fraud check endpoint
│   │   ├── fraud_alerts.py           # Alert management
│   │   └── health.py                 # Health check
│   │
│   ├── services/
│   │   ├── __init__.py
│   │   ├── fraud_service.py          # Main fraud detection logic
│   │   ├── feature_service.py        # Feature engineering
│   │   └── model_service.py          # Model loading & inference
│   │
│   ├── features/
│   │   ├── __init__.py
│   │   ├── transaction_features.py   # Transaction-based features
│   │   ├── user_features.py          # User behavioral features
│   │   └── device_features.py        # Device/location features
│   │
│   ├── models/
│   │   ├── __init__.py
│   │   └── schemas.py                # Pydantic models
│   │
│   └── ml_models/
│       └── fraud_model_v1.joblib     # Trained model file
│
├── training/
│   ├── generate_synthetic_data.py    # Generate training data
│   ├── train_model.py                # Model training script
│   ├── evaluate_model.py             # Model evaluation
│   └── notebooks/
│       └── fraud_detection_eda.ipynb # Exploratory analysis
│
├── data/
│   └── synthetic_transactions.csv    # Synthetic dataset
│
├── tests/
│   ├── test_fraud_service.py
│   └── test_feature_engineering.py
│
├── requirements.txt
├── Dockerfile
└── .env.example
```

### 3.4 Synthetic Training Data Schema

Since we won't have real banking data, we generate synthetic data:

```python
# Synthetic transaction record schema
{
    "transaction_id": "uuid",
    "amount": float,              # 0.01 - 50000.00
    "hour_of_day": int,           # 0-23
    "day_of_week": int,           # 0-6
    "merchant_category": str,     # "retail", "food", "travel", etc.
    "transaction_type": str,      # "INTERNAL", "ACH", "WIRE", etc.
    "is_international": bool,
    "device_type": str,           # "web", "mobile", "atm"
    "is_new_device": bool,
    "geo_latitude": float,
    "geo_longitude": float,
    "account_age_days": int,
    "avg_amount_30d": float,
    "max_amount_30d": float,
    "txn_count_1h": int,
    "txn_count_24h": int,
    "previous_frauds": int,
    "is_fraud": bool              # Target variable (0 or 1)
}
```

**Synthetic data distribution:**
- 97% legitimate transactions
- 3% fraudulent transactions (class imbalance, realistic)
- 100,000 records for training, 20,000 for validation

---

## 4. WealthWise — Financial Advisor Bot

### 4.1 Technology Stack

| Component | Technology | Purpose |
|-----------|------------|---------|
| Framework | FastAPI | REST API server |
| Analysis | pandas, numpy | Data processing |
| Visualization | matplotlib (server-side) | Chart generation |
| LLM | OpenAI / Ollama | Natural language insights |

### 4.2 Architecture

```
┌─────────────────────────────────────────────────────────────────────────┐
│                       WEALTHWISE SERVICE                                │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                         │
│  ┌─────────────────────────────────────────────────────────────────┐   │
│  │                    FastAPI Endpoints                             │   │
│  │                                                                  │   │
│  │  GET  /api/v1/ai/insights/spending?period=30d                   │   │
│  │  GET  /api/v1/ai/insights/savings                               │   │
│  │  GET  /api/v1/ai/insights/health-score                          │   │
│  │  GET  /api/v1/ai/insights/summary                               │   │
│  │  GET  /api/v1/ai/health                                         │   │
│  └────────────────────────────┬────────────────────────────────────┘   │
│                               │                                         │
│  ┌────────────────────────────▼────────────────────────────────────┐   │
│  │              DATA COLLECTION LAYER                              │   │
│  │                                                                  │   │
│  │  Fetches data from:                                             │   │
│  │  - Transaction Service → spending history                       │   │
│  │  - Account Service → balances, account types                    │   │
│  │  - Loan Service → outstanding loans, EMIs                       │   │
│  └────────────────────────────┬────────────────────────────────────┘   │
│                               │                                         │
│  ┌──────────────┬─────────────┴──────────────┬──────────────────┐     │
│  │              │                             │                  │     │
│  ▼              ▼                             ▼                  ▼     │
│  ┌────────┐  ┌────────────┐  ┌──────────────────┐  ┌──────────────┐  │
│  │Spending│  │  Savings   │  │ Financial Health │  │  Monthly    │  │
│  │Analyzer│  │  Advisor   │  │ Score Calculator │  │  Summary   │  │
│  │        │  │            │  │                  │  │  Generator │  │
│  ├────────┤  ├────────────┤  ├──────────────────┤  ├────────────┤  │
│  │Category│  │Subscription│  │Income vs Expense │  │ LLM-based  │  │
│  │breakdwn│  │detection   │  │ratio            │  │ narrative  │  │
│  │        │  │            │  │                  │  │ summary    │  │
│  │MoM     │  │Savings     │  │Savings rate      │  │            │  │
│  │compari-│  │opportunity │  │                  │  │ Actionable │  │
│  │son     │  │detection   │  │Debt-to-income    │  │ recommen-  │  │
│  │        │  │            │  │ratio            │  │ dations    │  │
│  │Top     │  │Budget      │  │                  │  │            │  │
│  │merch-  │  │recommend-  │  │Account diversity │  │ Trend      │  │
│  │ants    │  │ations      │  │                  │  │ analysis   │  │
│  │        │  │            │  │Combined score    │  │            │  │
│  │Trend   │  │Goal tracker│  │(0-100)           │  │            │  │
│  │analysis│  │            │  │                  │  │            │  │
│  └────────┘  └────────────┘  └──────────────────┘  └────────────┘  │
│                                                                         │
└─────────────────────────────────────────────────────────────────────────┘
```

### 4.3 Analysis Outputs

**Spending Analysis Response:**
```json
{
  "period": "2026-03-11 to 2026-04-11",
  "totalSpending": 4250.00,
  "categoryBreakdown": [
    {"category": "Housing", "amount": 1500.00, "percentage": 35.3, "trend": "STABLE"},
    {"category": "Food & Dining", "amount": 850.00, "percentage": 20.0, "trend": "UP"},
    {"category": "Transportation", "amount": 450.00, "percentage": 10.6, "trend": "DOWN"},
    {"category": "Shopping", "amount": 600.00, "percentage": 14.1, "trend": "UP"},
    {"category": "Utilities", "amount": 350.00, "percentage": 8.2, "trend": "STABLE"},
    {"category": "Entertainment", "amount": 300.00, "percentage": 7.1, "trend": "UP"},
    {"category": "Other", "amount": 200.00, "percentage": 4.7, "trend": "STABLE"}
  ],
  "topMerchants": [
    {"name": "Rent Payment", "amount": 1500.00, "count": 1},
    {"name": "Whole Foods", "amount": 320.00, "count": 8},
    {"name": "Amazon", "amount": 285.00, "count": 5}
  ],
  "comparedToLastMonth": {
    "changeAmount": 350.00,
    "changePercent": 8.97,
    "direction": "INCREASE"
  },
  "unusualSpending": [
    {
      "category": "Shopping",
      "message": "Shopping spending is 45% higher than your 3-month average",
      "suggestion": "Consider reviewing recent purchases"
    }
  ],
  "insights": "Your spending increased 9% this month, primarily driven by higher shopping and dining expenses. Housing remains your largest expense at 35% of total spending."
}
```

**Financial Health Score Response:**
```json
{
  "overallScore": 72,
  "grade": "B",
  "breakdown": {
    "savingsRate": {"score": 65, "value": "15%", "benchmark": "20%", "status": "BELOW_AVERAGE"},
    "debtToIncome": {"score": 80, "value": "25%", "benchmark": "36%", "status": "GOOD"},
    "emergencyFund": {"score": 55, "value": "2.1 months", "benchmark": "6 months", "status": "NEEDS_WORK"},
    "spendingControl": {"score": 78, "value": "Within budget", "status": "GOOD"},
    "accountDiversity": {"score": 85, "value": "Checking + Savings + CD", "status": "EXCELLENT"}
  },
  "recommendations": [
    {
      "priority": "HIGH",
      "title": "Build Emergency Fund",
      "description": "You have 2.1 months of expenses saved. Aim for 6 months ($12,750).",
      "actionable": "Set up automatic transfer of $200/month to savings"
    },
    {
      "priority": "MEDIUM",
      "title": "Increase Savings Rate",
      "description": "Your current savings rate is 15%. The recommended minimum is 20%.",
      "actionable": "Review subscription services for potential savings of $50-100/month"
    }
  ]
}
```

---

## 5. AI Service Communication with Backend

### 5.1 Internal Service Authentication

AI services authenticate to Spring Boot services using **service-to-service tokens**:

```python
# base_client.py — HTTP client for calling Spring Boot services
class ServiceClient:
    def __init__(self, base_url: str, service_name: str):
        self.base_url = base_url
        self.service_name = service_name
        self.session = httpx.AsyncClient(timeout=10.0)
    
    async def get(self, path: str, user_id: str, params: dict = None):
        headers = {
            "X-Service-Name": self.service_name,
            "X-Service-Token": settings.SERVICE_TOKEN,  # Pre-shared key
            "X-User-Id": user_id,                       # Acting on behalf of
            "Content-Type": "application/json"
        }
        response = await self.session.get(
            f"{self.base_url}{path}",
            headers=headers,
            params=params
        )
        response.raise_for_status()
        return response.json()
```

### 5.2 Kafka Integration for AI Services

```python
# Fraud Detection Kafka Consumer (listens for new transactions)
from aiokafka import AIOKafkaConsumer

async def start_transaction_consumer():
    consumer = AIOKafkaConsumer(
        'txn.created',
        bootstrap_servers='localhost:9092',
        group_id='fraud-detection-group',
        value_deserializer=lambda m: json.loads(m.decode('utf-8'))
    )
    await consumer.start()
    
    async for message in consumer:
        transaction = message.value
        # Run fraud detection asynchronously
        fraud_result = await fraud_service.check_transaction(transaction)
        
        if fraud_result.risk_level in ('HIGH', 'CRITICAL'):
            # Publish fraud alert
            await kafka_producer.send(
                'fraud.alert',
                value=fraud_result.dict()
            )
```

---

## 6. LLM Provider Strategy

### 6.1 Flexible LLM Backend

The chatbot supports multiple LLM providers via configuration:

```python
# config.py
class Settings(BaseSettings):
    # LLM Provider: "openai" or "ollama"
    LLM_PROVIDER: str = "ollama"  # Default to local for dev
    
    # OpenAI settings
    OPENAI_API_KEY: str = ""
    OPENAI_MODEL: str = "gpt-4o"
    
    # Ollama settings (local LLM)
    OLLAMA_BASE_URL: str = "http://localhost:11434"
    OLLAMA_MODEL: str = "llama3.1:8b"
    
    # Common settings
    LLM_TEMPERATURE: float = 0.3  # Low temp for Banking (accuracy > creativity)
    LLM_MAX_TOKENS: int = 1024
```

> **Recommendation:** Use **Ollama** (free, local) for development and testing. Switch to **OpenAI** for production-quality responses. The system is architected so swapping is a config change.

---

## 7. Data Privacy & Security for AI

| Concern | Mitigation |
|---------|-----------|
| PII in LLM prompts | Tokenize/mask SSN, full account numbers before sending to LLM |
| LLM data retention | Use API-only mode (no training on our data) |
| Prompt injection | Input validation + output guardrails |
| Financial advice liability | Hard-coded refusal patterns + disclaimer injection |
| Audit trail | Log all chatbot interactions (without PII) |
| Model bias | Regular fairness audits on fraud model |
| Data leakage | Service-to-service auth; AI services can't access raw DB |

---

*This design enables progressive complexity — start with rule-based responses and simple ML, then enhance with more sophisticated models as the system matures.*
