# UPIShield V1 API

## Spring Boot

Base URL:

`http://localhost:8080/api/v1`

### GET /health

Response:

```json
{
  "status": "UP",
  "service": "upishield-backend"
}
```

### POST /transactions

Request:

```json
{
  "userId": 1,
  "amount": 50000,
  "merchantId": "M1001",
  "deviceId": "D_NEW",
  "hour": 2,
  "transactionsLast5Min": 7,
  "avgUserAmount": 1200,
  "newDevice": true,
  "newBeneficiary": true
}
```

Response:

```json
{
  "transactionId": 1,
  "fraudProbability": 0.91,
  "anomalyScore": 0.84,
  "riskScore": 0.889,
  "riskLevel": "HIGH",
  "decision": "FLAG"
}
```

### GET /transactions

Returns latest transactions.

### GET /transactions/{id}/prediction

Returns the stored ML prediction.

### GET /fraud/alerts

Returns latest high-risk alerts.

### GET /dashboard/summary

Returns dashboard counts.

## FastAPI

Base URL:

`http://localhost:8000`

### GET /health

### POST /predict

Request:

```json
{
  "amount": 50000,
  "hour": 2,
  "transactionsLast5Min": 7,
  "avgUserAmount": 1200,
  "isNewDevice": true,
  "isNewBeneficiary": true,
  "accountAgeDays": 365
}
```

Response:

```json
{
  "fraudProbability": 0.91,
  "anomalyScore": 0.84
}
```
