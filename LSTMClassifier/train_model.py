import os
import matplotlib.pyplot as plt
import pickle
import numpy as np
from sklearn.preprocessing import LabelEncoder, StandardScaler
from tensorflow.keras.models import Sequential
from tensorflow.keras.layers import Input, LSTM, Dropout, Dense
from tensorflow.keras.callbacks import EarlyStopping
from utils import process_csv, create_time_windows, scale_data

DATASET_ROOT_DIR = "splitted_data"
SUBSETS = ["train", "val"]
ACTIVITY_CLASSES = ["10steps", "elevatordown", "elevatorup", "stairsdown", "stairsup", "standing"]
WINDOW_SIZE = 500
STEP = 100

raw_features = {"train": [], "val": []}
raw_labels = {"train": [], "val": []}
encoder = LabelEncoder()
scaler = StandardScaler()

for subset in SUBSETS:
    for label in ACTIVITY_CLASSES:
        folder_path = os.path.join(DATASET_ROOT_DIR, subset, label)
        for file in os.listdir(folder_path):
            if file.endswith(".csv"):
                file_path = os.path.join(folder_path, file)
                sequence = process_csv(file_path)
                raw_features[subset].append(sequence)
                raw_labels[subset].append(label)

    if subset == "train":
        raw_labels[subset] = encoder.fit_transform(raw_labels[subset])
    else:
        raw_labels[subset] = encoder.transform(raw_labels[subset])

X_train, y_train = create_time_windows(raw_features["train"], raw_labels["train"], WINDOW_SIZE, STEP)
X_val, y_val = create_time_windows(raw_features["val"], raw_labels["val"], WINDOW_SIZE, STEP)

X_train = scale_data(X_train, scaler, fit=True)
X_val = scale_data(X_val, scaler, fit=False)

model = Sequential([
    Input(shape=(WINDOW_SIZE, 1)),
    LSTM(128, return_sequences=True),
    Dropout(0.1),
    LSTM(64),
    Dropout(0.1),
    Dense(64, activation='relu'),
    Dropout(0.1),
    Dense(len(ACTIVITY_CLASSES), activation='softmax')
])

model.compile(loss='sparse_categorical_crossentropy', optimizer='adam', metrics=['accuracy'])

early_stopping = EarlyStopping(monitor='val_loss', patience=10, restore_best_weights=True)
history = model.fit(
    X_train, y_train,
    epochs=200,
    batch_size=32,
    validation_data=(X_val, y_val),
    callbacks=[early_stopping]
)

model.save("lstm_model.keras")

with open("training_history.pkl", "wb") as f:
    pickle.dump(history.history, f)
with open('encoder.pkl', 'wb') as f:
    pickle.dump(encoder, f)
with open('scaler.pkl', 'wb') as f:
    pickle.dump(scaler, f)

best_epoch = np.argmin(history['val_loss']) + 1
print(f"Best model was saved from epoch: {best_epoch}")
epochs = range(1, len(history['loss']) + 1)

plt.figure(figsize=(8, 6))
plt.plot(epochs, history['loss'], label='Training Loss')
plt.plot(epochs, history['val_loss'], label='Validation Loss')
plt.axvline(x=best_epoch, color='r', linestyle='--', label=f'Best Epoch ({best_epoch})')
plt.xlabel('Epoch')
plt.ylabel('Loss')
plt.title('Training and Validation Loss')
plt.legend()
plt.grid(True)
plt.savefig("loss_plot.png")