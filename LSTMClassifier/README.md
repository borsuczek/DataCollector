# Activity Classification using LSTM

This project uses LSTM neural networks to classify human activities based on velocity data derived from linear acceleration mobile phone sensor recordings. It includes data preprocessing, training, evaluation, and visualization.

## Project Structure

```
.
├── data/                     # Raw CSV data organized by activity
├── splitted_data/            # Train, validation, and test splits
├── split_data.py             # Script to split raw data into train/val/test
├── train_model.py            # Training the LSTM model
├── test_model.py             # Evaluating the model and saving results
├── process_data.py           # Preprocessing utilities
├── lstm_model.keras          # Trained model
├── training_history.pkl      # Training history (loss/accuracy)
├── scaler.pkl                # StandardScaler used for feature scaling
├── encoder.pkl               # LabelEncoder for activity labels
```

## Dataset

The dataset should be structured as follows before running any scripts:

```
data/
├── 10steps/
│   ├── seq1.csv
│   └── ...
├── elevatordown/
│   └── ...
...
```

## Preprocessing

The `process_data.py` module handles:

- Timestamp parsing and resampling at 2ms intervals.
- Velocity calculation.
- Windowing data into fixed-size segments (e.g., 500 samples with 100-step overlap).
- Standardization with `StandardScaler`.

## Data Splitting

Run:

```bash
python split_data.py
```

Splits the raw `data/` into:
- 70% training
- 15% validation
- 15% testing

Output is saved to `splitted_data/`.

## Training

Run:

```bash
python train_model.py
```

This script:

- Loads and preprocesses training and validation data.
- Trains a two-layer LSTM model with dropout and dense layers.
- Uses early stopping to avoid overfitting.
- Saves the best model, encoder, scaler, and training history.

## Testing

Run:

```bash
python test_model.py
```

This script:

- Loads the test data and performs inference.
- Evaluates the model on time windows and full sequences (via majority vote).
- Saves confusion matrices:
  - `confusion_matrix_time_windows.png`
  - `confusion_matrix_full_sequences.png`
- Identifies and saves misclassified sequences to `misclassified/`.

## Results

During testing, the model provides:

- Accuracy on time windows.
- Accuracy on full sequences (majority vote over windows).
- Confusion matrices for both evaluation types.
- Misclassified samples grouped by true class.

Example output:
```
Time windows accuracy: 0.89
Full sequence accuracy: 0.92
Best model was saved from epoch: 48
```

## Dependencies

```bash
pip install numpy pandas scikit-learn tensorflow matplotlib seaborn
```

## Notes

- `WINDOW_SIZE` and `STEP` can be adjusted in the training/testing scripts.
- Interpolation uses `2ms` resolution.
- Only the `z` axis from the accelerometer is used for velocity.
