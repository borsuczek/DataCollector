# FastapiExample
A fastapi server example with basic jwt auth.

# Api description

### /token

#### POST
##### Summary:

Login For Access Token


##### Request

Content-Type: application/x-www-form-urlencoded

| Parameter | Type |
| ---- | ----------- |
| username | string |
| password | string |

##### Responses

| Code | Description |
| ---- | ----------- |
| 200 | Successful Response |
| 422 | Validation Error |

```
{
  "access_token": "your_access_token",
  "token_type": "bearer"
}
```

### /me

#### GET
##### Summary:

Me

##### Responses

| Code | Description |
| ---- | ----------- |
| 200 | Successful Response |

### /logout

#### GET
##### Summary:

Logout

##### Responses

| Code | Description |
| ---- | ----------- |
| 200 | Successful Response |

### /public

#### GET
##### Summary:

Public Test. An unauthenticated user can make this request.

##### Responses

| Code | Description |
| ---- | ----------- |
| 200 | Successful Response |

### /private

#### GET
##### Summary:

Private Test. Add the jwt token from /token as bearer token

##### Responses

| Code | Description |
| ---- | ----------- |
| 200 | Successful Response |


## These two are example request to another api using httpx client. This client is not implemented in this project.

### /send_message

#### POST
##### Summary:

Send Message

##### Responses

| Code | Description |
| ---- | ----------- |
| 200 | Successful Response |
| 422 | Validation Error |

### /lists

#### GET
##### Summary:

Get List

##### Responses

| Code | Description |
| ---- | ----------- |
| 200 | Successful Response |

