# projectX-data

projectX-data is the backend server component for the **DataCollector** mobile application. It is responsible for:
- User authentication (via JWT)
- Receiving and storing sensor data in a PostgreSQL database
- Managing activities metadata
- Providing RESTful endpoints for interacting with the collected data
  
This service is implemented using FastAPI and integrates with a PostgreSQL database using SQLAlchemy ORM.

## Docker

You can pull and run the server using Docker from [DockerHub](https://hub.docker.com/r/borsuczek/projectx-data-server-backend):

```bash
docker pull borsuczek/projectx-data-server-backend
```

## API Description

### /token

#### POST  
**Summary:** Login for Access Token

**Request**  
Content-Type: `application/x-www-form-urlencoded`

| Parameter | Type   |
| --------- | ------ |
| username  | string |
| password  | string |

**Responses**

| Code | Description         |
| ---- | ------------------- |
| 200  | Successful Response |
| 422  | Validation Error    |

```json
{
  "access_token": "your_access_token",
  "token_type": "bearer"
}
```

---

### /me

#### GET  
**Summary:**  
Returns current authenticated user.

**Responses**

| Code | Description         |
| ---- | ------------------- |
| 200  | Successful Response |

---

### /logout

#### GET  
**Summary:**  
Clears the access token.

**Responses**

| Code | Description         |
| ---- | ------------------- |
| 200  | Successful Response |

---

### /public

#### GET  
**Summary:**  
Public test endpoint. No authentication required.

**Responses**

| Code | Description         |
| ---- | ------------------- |
| 200  | Successful Response |

---

### /private

#### GET  
**Summary:**  
Private test endpoint. Requires JWT bearer token.

**Responses**

| Code | Description         |
| ---- | ------------------- |
| 200  | Successful Response |

---
### /upload

#### POST  
**Summary:**  
Upload a file to the server.

**Request**  
Content-Type: multipart/form-data

| Field | Type |
|-------|------|
| file  | file |

**Responses**

| Code | Description               |
|------|---------------------------|
| 200  | File uploaded successfully|

---

### /insert_data

#### POST  
**Summary:**  
Insert sensor or metadata rows into a specified table.

**Request**

```json
{
  "table_name": "string",
  "data": "value1;value2;value3\nvalue1;value2;value3"
}
```
**Responses**

| Code | Description     |
|-------|-----------------|
| 200   | Data inserted   |
| 400   | Error in request|

---

### /last_id

#### GET  
**Summary:**  
Fetch the last inserted primary key from a table.

**Query Parameters**

| Name       | Type   |
|------------|--------|
| table_name | string |

**Responses**

| Code | Description     |
|-------|-----------------|
| 200   | Last ID returned|
| 400   | Error           |

---

### /delete_data

#### DELETE  
**Summary:**  
Delete a row from a specified table by ID.

**Request Body**

```json
{
  "table_name": "string",
  "id": 1
}
```
**Responses**

| Code | Description                   |
|------|-------------------------------|
| 200  | Row successfully deleted      |
| 400  | Error deleting row            |
| 404  | Row not found in the given table |


## Server Behavior

On startup, the server inserts static activities into the `activities` table if not already present.

**Static activities include:**  
- Standing  
- 10 Steps  
- Stairs Up  
- Stairs Down  
- Turning 90 Degrees Left  
- Turning 90 Degrees Right  
- Elevator Up  
- Elevator Down  

## Environment Variables

- `USERS_JSON` — Path to the JSON file with usernames and bcrypt-hashed passwords.

