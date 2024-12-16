# DataCollector

## Description
**DataCollector** is a mobile application designed to gather data from various sensors, such as the accelerometer and gyroscope. The application enables users to collect sensor data for specific activities (e.g., standing, taking 10 steps, climbing stairs, turning 90 degrees left or right, or using the elevator) and stores it in a PostgreSQL database hosted on a server. If the server is unavailable, the data is cached locally and pushed to the server when the application is reopened and the server is accessible. Users can analyze how sensor data changes across different activities and environments using tools like PGAdmin to examine the database.


## Usage
### Step-by-Step Instructions

1. **Enter Description**  
   On the start of application you will be promped to enter a description for the test group. A description is necessary for further data analysis. For example, it could specify the place where the tests are being conducted.
   
3. **Choose an Activity**  
   Pick an activity from the list (e.g., standing, 10 steps, etc.). Once selected, a timer will begin.

4. **Start Data Collection**  
   When the timer finishes, the application will automatically start collecting sensor data.

5. **Stop Data Collection**  
   When you have completed the activity, tap the **Stop** button to end the data collection.

6. **Save or Discard Data**  
   After stopping, you’ll be prompted to either:
   - **Save** the collected data to the database (or locally if offline).
   - **Discard** the data and return to the activity selection screen.

7. **Repeat or Modify Tests**  
   - To start another activity within the same test group, select it from the list and repeat the process.
   - If you want to change tests group press the **Back** button to return to the test description screen.
