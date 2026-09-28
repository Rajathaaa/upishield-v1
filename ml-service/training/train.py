import argparse
from pathlib import Path
import numpy as np
import pandas as pd
import joblib

from sklearn.model_selection import train_test_split
from sklearn.preprocessing import StandardScaler
from sklearn.metrics import classification_report, average_precision_score, roc_auc_score
from xgboost import XGBClassifier

from tensorflow import keras
from tensorflow.keras import layers

BASE = Path(__file__).resolve().parents[1]
DATA_DIR = BASE / "data"
MODEL_DIR = BASE / "models"
MODEL_DIR.mkdir(exist_ok=True)
DATA_DIR.mkdir(exist_ok=True)

# FEATURES = [
#     "amount",
#     "hour",

#     "old_balance_org",
#     "new_balance_org",

#     "old_balance_dest",
#     "new_balance_dest",

#     "balance_change_org",
#     "balance_change_dest",

#     "amount_to_balance_ratio",

#     "origin_transaction_count",
#     "destination_transaction_count",

#     "type_CASH_IN",
#     "type_CASH_OUT",
#     "type_DEBIT",
#     "type_PAYMENT",
#     "type_TRANSFER",
# ]

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


def make_synthetic(n=12000, seed=42):
    """Creates an educational UPI-like dataset when PaySim is unavailable."""
    rng = np.random.default_rng(seed)

    amount = rng.lognormal(mean=6.5, sigma=1.0, size=n)
    hour = rng.integers(0, 24, size=n)
    velocity = rng.poisson(1.5, size=n)
    avg_amount = rng.lognormal(mean=6.0, sigma=0.65, size=n)
    account_age = rng.integers(5, 2000, size=n)
    new_device = rng.binomial(1, 0.08, size=n)
    new_beneficiary = rng.binomial(1, 0.12, size=n)

    amount_ratio = amount / np.maximum(avg_amount, 1)

    # Synthetic fraud-generating mechanism.
    logit = (
        -5.2
        + 0.75 * np.log1p(amount_ratio)
        + 0.16 * velocity
        + 0.9 * new_device
        + 0.8 * new_beneficiary
        + 0.9 * ((hour <= 4) | (hour >= 23))
        - 0.00025 * account_age
    )
    probability = 1 / (1 + np.exp(-logit))
    fraud = rng.binomial(1, probability)

    return pd.DataFrame({
        "amount": amount,
        "hour": hour,
        "transactions_last_5min": velocity,
        "avg_user_amount": avg_amount,
        "amount_ratio": amount_ratio,
        "is_new_device": new_device,
        "is_new_beneficiary": new_beneficiary,
        "account_age_days": account_age,
        "is_fraud": fraud,
    })


# def paysim_to_features(df):

#     df = df.copy()

#     # -----------------------------
#     # Basic transaction information
#     # -----------------------------

#     features = pd.DataFrame()

#     features["amount"] = df["amount"].astype(float)

#     # PaySim "step" represents a time step.
#     # We convert it into a 24-hour cycle.
#     features["hour"] = df["step"].astype(int) % 24

#     # -----------------------------
#     # Account balance features
#     # -----------------------------

#     features["old_balance_org"] = df["oldbalanceOrg"].astype(float)

#     features["new_balance_org"] = df["newbalanceOrig"].astype(float)

#     features["old_balance_dest"] = df["oldbalanceDest"].astype(float)

#     features["new_balance_dest"] = df["newbalanceDest"].astype(float)

#     # How much money left the sender
#     features["balance_change_org"] = (
#         features["old_balance_org"]
#         - features["new_balance_org"]
#     )

#     # How much money arrived at destination
#     features["balance_change_dest"] = (
#         features["new_balance_dest"]
#         - features["old_balance_dest"]
#     )

#     # -----------------------------
#     # Amount relative to balance
#     # -----------------------------

#     features["amount_to_balance_ratio"] = (
#         features["amount"]
#         / features["old_balance_org"].clip(lower=1)
#     )

#     # -----------------------------
#     # Transaction type
#     # -----------------------------

#     # Convert transaction type into numerical columns
#     type_dummies = pd.get_dummies(
#         df["type"],
#         prefix="type"
#     ).astype(int)

#     features = pd.concat(
#         [features, type_dummies],
#         axis=1
#     )

#     # -----------------------------
#     # Account activity
#     # -----------------------------

#     if "nameOrig" in df.columns:

#         origin_counts = (
#             df.groupby("nameOrig")["amount"]
#             .transform("count")
#         )

#         features["origin_transaction_count"] = origin_counts

#     else:
#         features["origin_transaction_count"] = 1

#     if "nameDest" in df.columns:

#         destination_counts = (
#             df.groupby("nameDest")["amount"]
#             .transform("count")
#         )

#         features["destination_transaction_count"] = (
#             destination_counts
#         )

#     else:
#         features["destination_transaction_count"] = 1

#     # -----------------------------
#     # Target
#     # -----------------------------

#     features["is_fraud"] = (
#         df["isFraud"]
#         .astype(int)
#     )

#     return features

def paysim_to_features(df):

    df = df.copy()

    features = pd.DataFrame(index=df.index)

    # -----------------------------
    # 1. Transaction amount
    # -----------------------------

    features["amount"] = df["amount"].astype(float)

    # -----------------------------
    # 2. Transaction hour
    # -----------------------------

    # PaySim "step" represents simulated time.
    # We convert it into a 24-hour cycle.
    features["hour"] = df["step"].astype(int) % 24

    # -----------------------------
    # 3. Transaction velocity
    # -----------------------------

    # Number of transactions made by
    # the same sender at the same step.
    if "nameOrig" in df.columns:

        velocity = (
            df.groupby(["nameOrig", "step"])["amount"]
            .transform("count")
        )

        features["transactions_last_5min"] = velocity

    else:

        features["transactions_last_5min"] = 1

    # -----------------------------
    # 4. Average transaction amount
    # -----------------------------

    if "nameOrig" in df.columns:

        avg_amount = (
            df.groupby("nameOrig")["amount"]
            .transform("mean")
        )

        features["avg_user_amount"] = avg_amount

    else:

        features["avg_user_amount"] = df["amount"].mean()

    # Avoid division by zero
    features["avg_user_amount"] = (
        features["avg_user_amount"].clip(lower=1)
    )

    # -----------------------------
    # 5. Amount ratio
    # -----------------------------

    features["amount_ratio"] = (
        features["amount"]
        / features["avg_user_amount"]
    )

    # -----------------------------
    # 6. New device
    # -----------------------------

    # PaySim does NOT contain device information.
    #
    # Therefore we do NOT pretend this is a real
    # device feature.
    #
    # For V1, we keep it as 0.
    features["is_new_device"] = 0

    # -----------------------------
    # 7. New beneficiary
    # -----------------------------

    # PaySim doesn't explicitly contain this concept.
    #
    # We approximate it using whether the sender
    # is interacting with a destination for the
    # first time.

    if "nameOrig" in df.columns and "nameDest" in df.columns:

        pair = (
            df["nameOrig"].astype(str)
            + "_"
            + df["nameDest"].astype(str)
        )

        features["is_new_beneficiary"] = (
            ~pair.duplicated()
        ).astype(int)

    else:

        features["is_new_beneficiary"] = 0

    # -----------------------------
    # 8. Account age
    # -----------------------------

    if "nameOrig" in df.columns:

        first_step = (
            df.groupby("nameOrig")["step"]
            .transform("min")
        )

        features["account_age_days"] = (
            df["step"] - first_step + 1
        )

    else:

        features["account_age_days"] = 365

    # -----------------------------
    # Fraud label
    # -----------------------------

    features["is_fraud"] = (
        df["isFraud"].astype(int)
    )

    return features


def train_xgboost(X_train, y_train, X_test, y_test):
    positives = max(int(y_train.sum()), 1)
    negatives = max(int(len(y_train) - positives), 1)
    scale_pos_weight = negatives / positives

    model = XGBClassifier(
        n_estimators=250,
        max_depth=5,
        learning_rate=0.06,
        subsample=0.85,
        colsample_bytree=0.85,
        objective="binary:logistic",
        eval_metric="aucpr",
        scale_pos_weight=scale_pos_weight,
        random_state=42,
        n_jobs=4,
    )

    model.fit(X_train, y_train)

    probabilities = model.predict_proba(X_test)[:, 1]
    predictions = (probabilities >= 0.5).astype(int)

    print("\n=== XGBoost evaluation ===")
    print(classification_report(y_test, predictions, digits=4))
    print("ROC-AUC:", round(roc_auc_score(y_test, probabilities), 4))
    print("PR-AUC :", round(average_precision_score(y_test, probabilities), 4))

    joblib.dump(model, MODEL_DIR / "xgboost_model.joblib")
    return model


def train_autoencoder(X_normal):
    scaler = StandardScaler()
    X_scaled = scaler.fit_transform(X_normal)

    input_dim = X_scaled.shape[1]

    model = keras.Sequential([
        layers.Input(shape=(input_dim,)),
        layers.Dense(32, activation="relu"),
        layers.Dense(12, activation="relu"),
        layers.Dense(32, activation="relu"),
        layers.Dense(input_dim, activation="linear"),
    ])

    model.compile(optimizer="adam", loss="mse")

    model.fit(
        X_scaled,
        X_scaled,
        epochs=25,
        batch_size=128,
        validation_split=0.1,
        verbose=1,
    )

    reconstructed = model.predict(X_scaled, verbose=0)
    errors = np.mean(np.square(X_scaled - reconstructed), axis=1)

    # 95th percentile of normal reconstruction error.
    threshold = float(np.percentile(errors, 95))

    model.save(MODEL_DIR / "autoencoder.keras")
    joblib.dump(
        {"scaler": scaler, "threshold": threshold},
        MODEL_DIR / "autoencoder_meta.joblib",
    )

    print("Autoencoder anomaly threshold:", round(threshold, 6))


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--csv", default=str(DATA_DIR / "paysim.csv"))
    args = parser.parse_args()

    csv_path = Path(args.csv)

    if csv_path.exists():
        print("Loading:", csv_path)
        raw = pd.read_csv(csv_path)
        data = paysim_to_features(raw)
    else:
        print("PaySim CSV not found.")
        print("Creating synthetic UPI-like training data...")
        data = make_synthetic()

    data = data.replace([np.inf, -np.inf], np.nan).dropna()

    X = data[FEATURES]
    y = data["is_fraud"].astype(int)

    X_train, X_test, y_train, y_test = train_test_split(
        X, y,
        test_size=0.2,
        random_state=42,
        stratify=y,
    )

    train_xgboost(X_train, y_train, X_test, y_test)

    # Autoencoder is trained only on legitimate training transactions.
    normal_train = X_train[y_train == 0]
    train_autoencoder(normal_train)

    print("\nModels saved in:", MODEL_DIR)


if __name__ == "__main__":
    main()
