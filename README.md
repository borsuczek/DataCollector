# Activity Recognition: Android Data Collector + Python LSTM Classifier

This repository contains two separate components for human activity recognition:

- **Android application** – used to collect sensor data.
- **Python-based LSTM model** – used for activity classification.

The two components are not integrated and should be used separately.

## Data Collector

An Android Studio application that collects data from multiple smartphone sensors and saves it into a PostgreSQL database running on a FastAPI server.

## LSTM Classifier

A Python implementation of an LSTM model trained to classify 6 types of activities based on velocity derived from Z-axis linear acceleration data.

Activities classified: 
- Walking
- Elevator up
- Elevator down
- Climbing stairs
- Descending stairs
- Standing


