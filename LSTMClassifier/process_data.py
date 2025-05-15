import pandas as pd
import numpy as np


def process_csv(file_path):
    df = pd.read_csv(file_path)
    df = df.drop_duplicates(subset=['timestamp'], keep='first')
    df['timestamp'] = pd.to_datetime(df['timestamp'], format='mixed', errors='coerce')
    df = df.set_index('timestamp').resample('2ms').interpolate(method='linear', order=1).dropna().reset_index()
    df['timestamp_difference'] = df['timestamp'].diff().dt.total_seconds() * 1000
    df['timestamp_difference'] = df['timestamp_difference'].fillna(0)
    df['velocity'] = 0.0
    for i in range(1, len(df)):
        t = df.loc[i, 'timestamp_difference'] / 1000
        a = df.loc[i, 'z']
        v0 = df.loc[i - 1, 'velocity']
        df.loc[i, 'velocity'] = v0 + a * t
    return df[['velocity']].values


def create_time_windows(sequences, labels, window_size, step):
    X, y = [], []
    for sequence, label in zip(sequences, labels):
        for start in range(0, len(sequence) - window_size + 1, step):
            window = sequence[start:start + window_size]
            X.append(window)
            y.append(label)
    return np.array(X), np.array(y)


def scale_data(data, scaler, fit=True):
    data_flat = data.reshape(-1, data.shape[-1])
    if fit:
        data_scaled = scaler.fit_transform(data_flat)
    else:
        data_scaled = scaler.transform(data_flat)
    return data_scaled.reshape(data.shape)