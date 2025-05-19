# DataCollector
**DataCollector** is a mobile application designed to gather data from various sensors, such as the accelerometer and gyroscope. The application enables users to collect sensor data for specific activities (standing, 10 steps, stairs up, stairs down, turning 90 degrees left, turning 90 degrees right, elevator up, elevator down) and stores it in a PostgreSQL database hosted on a server. If the server is unavailable, the data is cached locally and pushed to the server when the application is reopened and the server is accessible. Users can analyze how sensor data changes across different activities and environments using tools like PGAdmin to examine the database. Code for setting up the server and database is available in the `projectX-data` folder.

## Usage
1. **Start Application**  
   On the start of application you will be promped to enter a description for the test group. A description is necessary for further data analysis. For example, it could specify the place where the tests are being conducted.
   
2. **Choose an Activity**  
   Pick an activity from the list (e.g., standing, 10 steps, etc.). Once selected, a timer will begin.

3. **Start Data Collection**  
   When the timer finishes, the application will automatically start collecting sensor data.

4. **Stop Data Collection**  
   When you have completed the activity, tap the **Stop** button to end the data collection.

5. **Save or Discard Data**  
   After stopping, you’ll be prompted to either:
   - **Save** the collected data to the database (or locally if offline).
   - **Discard** the data and return to the activity selection screen.

6. **Repeat Tests**  
   - To start another activity within the same test group, select it from the list and repeat the process.
   - If you want to change tests group press the **Back** button to return to the test description screen.
  
## Database Structure
Data is stored in a relational database PostgreSQL. It consists of several tables:
1. `test_descriptions` - stores metadata about the test group
   - `description_id` (UUID): primary key
   - `description` (String): e.g. location
   - `model` (String): device model
   - `timestamp` (DateTime): creation time
   - `username` (String): username of the person logged in to the server
2. `activities` - stores a list of activities
   - `activity_id` (BigInteger): primary key
   - `activity` (String): activity name
3. `tests` - describes single test (links description of test group and activity)
   - `test_id` (UUID): primary key
   - `activity_id` (BigInteger): foreign key referencing to activities table
   - `description_id` (UUID): foreign key referencing to test_descriptions table
4. `accelerometer`, `gyroscope`, `magnetic_field`, `gravity`, `proximity`, `pressure`, `linear_acceleration`, `rotation_vector`, `wifi`, `location` - sensor data tables
   - `id` (BigInteger): primary key
   - `timestamp` (DateTime): time of getting data from sensor
   - sensor-specific fields: e.g. `x`, `y`, `z` 
   - `test_id` (UUID): foreign key referencing to tests table
