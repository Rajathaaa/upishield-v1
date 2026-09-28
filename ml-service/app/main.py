from pathlib import Path
import joblib
import numpy as np
from fastapi import FastAPI, HTTPException
from pydantic import BaseModel, Field
from tensorflow import keras

BASE = Path(__file__).resolve().parents[1]
MODEL_DIR = BASE / "models"

app = FastAPI(title="UPIShield ML Service", version="1.0.0")

FEATURES = [
    "amount",
    "hour",
    "transactions_last_5min",
    "avg_user_amount",
    "amount_ratio",
    "is_new_device",
    "is_new_beneficiary",
    "account_age_days",
]

xgb_path = MODEL_DIR / "xgboost_model.joblib"
ae_path = MODEL_DIR / "autoencoder.keras"
ae_meta_path = MODEL_DIR / "autoencoder_meta.joblib"

if not xgb_path.exists() or not ae_path.exists() or not ae_meta_path.exists():
    raise RuntimeError(
        "Models are missing. Run: python training/train.py"
    )

xgb_model = joblib.load(xgb_path)
autoencoder = keras.models.load_model(ae_path)
ae_meta = joblib.load(ae_meta_path)
scaler = ae_meta["scaler"]
threshold = float(ae_meta["threshold"])


class PredictionRequest(BaseModel):
    amount: float = Field(gt=0)
    hour: int = Field(ge=0, le=23)
    transactionsLast5Min: int = Field(ge=0)
    avgUserAmount: float = Field(gt=0)
    isNewDevice: bool
    isNewBeneficiary: bool
    accountAgeDays: int = Field(ge=0)


class PredictionResponse(BaseModel):
    fraudProbability: float
    anomalyScore: float


def make_features(req: PredictionRequest):
    amount_ratio = req.amount / max(req.avgUserAmount, 1.0)

    return np.array([[
        req.amount,
        req.hour,
        req.transactionsLast5Min,
        req.avgUserAmount,
        amount_ratio,
        int(req.isNewDevice),
        int(req.isNewBeneficiary),
        req.accountAgeDays,
    ]], dtype=float)


@app.get("/health")
def health():
    return {"status": "UP", "model": "xgboost+autoencoder"}


@app.post("/predict", response_model=PredictionResponse)
def predict(req: PredictionRequest):
    try:
        features = make_features(req)

        fraud_probability = float(
            xgb_model.predict_proba(features)[0][1]
        )

        scaled = scaler.transform(features)
        reconstructed = autoencoder.predict(scaled, verbose=0)
        error = float(np.mean(np.square(scaled - reconstructed)))

        # Convert reconstruction error into a bounded 0..1 anomaly score.
        anomaly_score = min(error / max(threshold, 1e-8), 1.0)

        return PredictionResponse(
            fraudProbability=round(fraud_probability, 6),
            anomalyScore=round(anomaly_score, 6),
        )
    except Exception as exc:
        raise HTTPException(status_code=500, detail=str(exc))
