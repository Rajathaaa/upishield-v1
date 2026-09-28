# UPIShield V1 — Simple UPI Fraud Detection System

A student-friendly, end-to-end fraud detection project:

Angular
  -> Spring Boot REST API
  -> PostgreSQL
  -> Python FastAPI ML service
  -> XGBoost + Autoencoder
  -> Risk score / decision
  -> Angular dashboard

## Architecture

```text
Angular
   |
   | REST
   v
Spring Boot
   | \
   |  \ HTTP /predict
   |   v
   |  FastAPI
   |   | \
   |   |  +-- XGBoost
   |   |  +-- Autoencoder
   |   |
   v   v
PostgreSQL
```

This V1 intentionally does NOT include Kafka, Redis, Neo4j, GraphSAGE/GAT, or an LLM agent.
Those should be added only after you understand this version.

## Prerequisites

- Java 17+
- Maven 3.9+
- Python 3.10 or 3.11
- Node.js 20+ and npm
- Angular CLI (`npm install -g @angular/cli`)
- Docker Desktop (recommended for PostgreSQL)

## 1. Start PostgreSQL

From this project directory:

```bash
docker compose up -d postgres
```

PostgreSQL:
- host: localhost
- port: 5432
- database: upishield
- username: upi
- password: upi_password

## 2. Start ML service

Windows:

```bash
cd ml-service
python -m venv .venv
.venv\Scripts\activate
pip install -r requirements.txt
python training/train.py
uvicorn app.main:app --reload --port 8000
```

macOS/Linux:

```bash
cd ml-service
python3 -m venv .venv
source .venv/bin/activate
pip install -r requirements.txt
python training/train.py
uvicorn app.main:app --reload --port 8000
```

If `data/paysim.csv` does not exist, `train.py` automatically creates a synthetic UPI-like dataset.
This makes the project runnable immediately.

### Using PaySim

Download the PaySim CSV from Kaggle:
https://www.kaggle.com/datasets/ealaxi/paysim1

Rename it to:

```text
paysim.csv
```

and place it here:

```text
ml-service/data/paysim.csv
```

Then run:

```bash
python training/train.py --csv data/paysim.csv
```

The training script maps PaySim fields into a simpler UPI-style feature set.

## 3. Start Spring Boot

```bash
cd backend
mvn spring-boot:run
```

Backend:
http://localhost:8080

Health:
http://localhost:8080/api/v1/health

## 4. Start Angular

```bash
cd frontend
npm install
npm start
```

Frontend:
http://localhost:4200

## 5. Test the system

Open the Angular application and create a transaction.

Example:
- User ID: 1
- Amount: 50000
- Merchant ID: M1001
- Device ID: D_NEW
- New beneficiary: true

The flow is:

1. Angular sends transaction to Spring Boot.
2. Spring Boot saves the transaction.
3. Spring Boot calls FastAPI.
4. FastAPI calculates fraud probability and anomaly score.
5. Spring Boot combines the scores.
6. Spring Boot saves the prediction.
7. Angular displays the result.

## REST API

### Health

GET `/api/v1/health`

### Dashboard

GET `/api/v1/dashboard/summary`

### Transactions

POST `/api/v1/transactions`
GET `/api/v1/transactions`
GET `/api/v1/transactions/{id}`

### Fraud

POST `/api/v1/fraud/analyze`
GET `/api/v1/fraud/alerts`

### ML

GET `http://localhost:8000/health`
POST `http://localhost:8000/predict`

## Example ML request

```json
{
  "amount": 50000,
  "hour": 2,
  "transactionsLast5Min": 7,
  "avgUserAmount": 1200,
  "isNewDevice": true,
  "isNewBeneficiary": true,
  "accountAgeDays": 30
}
```

## Example ML response

```json
{
  "fraudProbability": 0.91,
  "anomalyScore": 0.84
}
```

## Important interview explanation

XGBoost is used for supervised classification because the input is tabular transaction data.

The autoencoder is used for anomaly detection. It learns normal transaction patterns and uses reconstruction error as an anomaly signal.

The final risk score in V1 is:

```text
risk = 0.7 * fraudProbability + 0.3 * anomalyScore
```

This weighting is a project design choice, not a universal banking rule.

## V1 limitations

This is an educational system, not a real payment gateway.

- No real UPI integration
- No real bank credentials
- No production authentication
- No Kafka/Redis
- No graph database
- No real-time streaming
- Synthetic fallback data is not real UPI data
- Risk thresholds should be calibrated using real operational costs in production

## Next versions

V2:
- Kafka
- Redis
- SHAP explanations

V3:
- Neo4j
- graph-based fraud features
- GraphSAGE/GAT

V4:
- tool-using fraud investigation agent
- LLM-generated case summaries
