import json
import os
import numpy as np
from sklearn.ensemble import RandomForestClassifier
from sklearn.model_selection import train_test_split
from sklearn.metrics import accuracy_score, classification_report
import pickle

def extract_features(values):
    """Turn a raw [x,y,z] time series into a fixed-size feature vector."""
    values = np.array(values)
    x, y, z = values[:, 0], values[:, 1], values[:, 2]
    magnitude = np.sqrt(x**2 + y**2 + z**2)

    features = [
        np.mean(x), np.std(x), np.max(x), np.min(x),
        np.mean(y), np.std(y), np.max(y), np.min(y),
        np.mean(z), np.std(z), np.max(z), np.min(z),
        np.mean(magnitude), np.std(magnitude), np.max(magnitude), np.min(magnitude),
    ]
    return features

def load_folder(folder_path):
    X = []
    y = []
    for filename in os.listdir(folder_path):
        if not filename.endswith(".json"):
            continue
        filepath = os.path.join(folder_path, filename)
        with open(filepath, "r") as f:
            data = json.load(f)

        values = data["payload"]["values"]
        features = extract_features(values)

        # Label comes from filename prefix, e.g. "FALL.F15..." or "ADL.xxx..."
        label = filename.split(".")[0]

        X.append(features)
        y.append(label)

    return X, y

# ---- Load both training and testing folders ----
TRAINING_PATH = "training"
TESTING_PATH = "testing"

X_train, y_train = load_folder(TRAINING_PATH)
X_test, y_test = load_folder(TESTING_PATH)

print(f"Training samples: {len(X_train)}")
print(f"Testing samples: {len(X_test)}")
print(f"Labels found: {set(y_train)}")

# ---- Train ----
model = RandomForestClassifier(n_estimators=100, random_state=42)
model.fit(X_train, y_train)

# ---- Evaluate ----
y_pred = model.predict(X_test)
print("Accuracy:", accuracy_score(y_test, y_pred))
print(classification_report(y_test, y_pred))

# ---- Save the trained model ----
with open("fall_detection_model.pkl", "wb") as f:
    pickle.dump(model, f)

print("Model saved as fall_detection_model.pkl")