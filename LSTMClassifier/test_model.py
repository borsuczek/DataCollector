import os
import pickle
import shutil
import numpy as np
import seaborn as sns
import matplotlib.pyplot as plt
from scipy.stats import mode
from sklearn.metrics import confusion_matrix, accuracy_score
from tensorflow.keras.models import load_model

from utils import process_csv, create_time_windows, scale_data

DATASET_ROOT_DIR = "splitted_data"
ACTIVITY_CLASSES = ["10steps", "elevatordown", "elevatorup", "stairsdown", "stairsup", "standing"]
WINDOW_SIZE = 500
STEP = 100


model = load_model("lstm_model.keras")
with open('encoder.pkl', 'rb') as f:
    encoder = pickle.load(f)
with open('scaler.pkl', 'rb') as f:
    scaler = pickle.load(f)
with open('training_history.pkl', 'rb') as f:
    history = pickle.load(f)

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

test_sequences, true_labels = [], []
file_paths = []

test_folder = os.path.join(DATASET_ROOT_DIR, "test")
for label in ACTIVITY_CLASSES:
    folder_path = os.path.join(test_folder, label)
    if not os.path.exists(folder_path):
        continue
    for file in os.listdir(folder_path):
        if file.endswith(".csv"):
            file_path = os.path.join(folder_path, file)
            sequence = process_csv(file_path)
            test_sequences.append(sequence)
            true_labels.append(label)
            file_paths.append(file_path)

true_labels = encoder.transform(true_labels)

X_test, y_test = create_time_windows(test_sequences, true_labels, WINDOW_SIZE, STEP)
X_test = scale_data(X_test, scaler, False)

test_loss, test_acc = model.evaluate(X_test, y_test)
print(f"Time windows accuracy: {test_acc}")

# Confusion Matrix for time windows
y_pred = np.argmax(model.predict(X_test), axis=1)
cm = confusion_matrix(y_test, y_pred)

plt.figure(figsize=(10, 8))
sns.heatmap(cm, annot=True, fmt='d', cmap='Blues', xticklabels=ACTIVITY_CLASSES, yticklabels=ACTIVITY_CLASSES)
plt.xlabel('Predicted label')
plt.ylabel('True label')
plt.title('Confusion Matrix for Time Windows')
plt.savefig("confusion_matrix_time_windows.png")
plt.show()

# Full sequence prediction
sequence_indices = []
current_idx = 0
for seq in test_sequences:
    n_windows = (len(seq) - WINDOW_SIZE) // STEP + 1
    sequence_indices.append(y_pred[current_idx:current_idx + n_windows])
    current_idx += n_windows
y_pred_full = [mode(pred)[0] for pred in sequence_indices]

full_seq_acc = accuracy_score(true_labels, y_pred_full)
print(f"Full sequence accuracy: {full_seq_acc}")

# Confusion Matrix for full sequences
cm_full = confusion_matrix(true_labels, y_pred_full)

plt.figure(figsize=(10, 8))
sns.heatmap(cm_full, annot=True, fmt='d', cmap='Blues', xticklabels=ACTIVITY_CLASSES, yticklabels=ACTIVITY_CLASSES)
plt.xlabel('Predicted label')
plt.ylabel('True label')
plt.title('Confusion Matrix for Full Sequences')
plt.savefig("confusion_matrix_full_sequences.png")
plt.show()

# Save misclassified examples
misclassified_folder = "misclassified"
os.makedirs(misclassified_folder, exist_ok=True)

for true_label, pred_label, file_path in zip(true_labels, y_pred_full, file_paths):
    if true_label != pred_label:
        class_folder = os.path.join(misclassified_folder, ACTIVITY_CLASSES[true_label])
        os.makedirs(class_folder, exist_ok=True)
        new_file_name = f"{os.path.basename(file_path)}_{ACTIVITY_CLASSES[true_label]}_pred_{ACTIVITY_CLASSES[pred_label]}"
        shutil.copy(file_path, os.path.join(class_folder, new_file_name))