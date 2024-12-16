from __future__ import annotations

import json
import os
import secrets
from datetime import datetime, timedelta
from typing import TypedDict

import bcrypt
from fastapi import FastAPI, Request, Depends, HTTPException, status, UploadFile, File
from fastapi.middleware.cors import CORSMiddleware
from fastapi.security import HTTPBasic, OAuth2PasswordRequestForm, HTTPBearer
from jose import JWTError, jwt
from pydantic import BaseModel
from starlette.responses import Response

from sqlalchemy import create_engine, MetaData, Table, desc
from sqlalchemy.orm import sessionmaker, Session

from database_models import Base, Activity

import api_integration

DATABASE_URL = "postgresql://user:password@db/mydatabase"

engine = create_engine(DATABASE_URL, pool_pre_ping=True)
SessionLocal = sessionmaker(autocommit=False, autoflush=False, bind=engine)

metadata = MetaData()


class ChatMessage(BaseModel):
    id: str
    message: str


app = FastAPI()
security = HTTPBasic()
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)


class Token(BaseModel):
    access_token: str
    token_type: str


class TokenData(BaseModel):
    username: str | None = None


class User(BaseModel):
    username: str


class UserDBEntry(TypedDict):
    username: str
    hashed_password: str


SECRET_KEY = secrets.token_hex(32)
ALGORITHM = "HS256"
auth_scheme = HTTPBearer()

PATH_TO_USERS_JSON = (os.getenv('USERS_JSON'))

with open(PATH_TO_USERS_JSON, encoding="utf-8") as f:
    users_db: dict[str, UserDBEntry] = json.load(f)


def verify_password(plain_password: str, hashed_password: str) -> bool:
    return bcrypt.checkpw(plain_password.encode('utf-8'), hashed_password.encode('utf-8'))


def authenticate_user(username: str, password: str):
    try:
        user = users_db[username]
    except KeyError:
        return False
    correct_password = user["hashed_password"]
    is_correct_password = verify_password(password, correct_password)
    if not is_correct_password:
        return False
    return User(username=username)


def create_access_token(data: dict, expires_delta: timedelta | None = None):
    to_encode = data.copy()
    if expires_delta:
        expire = datetime.utcnow() + expires_delta
    else:
        expire = datetime.utcnow() + timedelta(minutes=15)
    to_encode.update({"exp": expire})
    encoded_jwt = jwt.encode(to_encode, SECRET_KEY, algorithm=ALGORITHM)
    return encoded_jwt


async def get_current_user(request: Request):
    token = request.cookies.get("access_token")
    if not token:
        auth_header = request.headers.get("Authorization")
        if auth_header and auth_header.startswith("Bearer "):
            token = auth_header.split(" ")[1]
    if not token:
        raise unauthorized_exception("Not authenticated")
    try:
        payload = jwt.decode(token, SECRET_KEY, algorithms=[ALGORITHM])
        username: str = payload.get("sub")
        if username is None:
            raise unauthorized_exception("Not authenticated")

        token_data = TokenData(username=username)
    except JWTError:
        raise unauthorized_exception("Not authenticated")
    if token_data.username is None:
        raise unauthorized_exception("Not authenticated")

    return User(username=username)


def unauthorized_exception(message) -> HTTPException:
    return HTTPException(
        status_code=status.HTTP_401_UNAUTHORIZED,
        detail=message,
        headers={"WWW-Authenticate": "Bearer"},
    )


@app.post("/token", response_model=Token)
async def login_for_access_token(response: Response, form_data: OAuth2PasswordRequestForm = Depends()):
    user = authenticate_user(form_data.username, form_data.password)
    if not user:
        raise unauthorized_exception("Incorrect username or password")

    access_token_expires = timedelta(minutes=240)
    access_token = create_access_token(
        data={"sub": user.username}, expires_delta=access_token_expires
    )
    response.set_cookie(key="access_token", value=access_token, httponly=True, secure=True)
    return {"access_token": access_token, "token_type": "bearer"}


@app.get("/me")
async def me(_current_user: User = Depends(get_current_user)):
    return True


@app.get("/logout")
async def logout(response: Response, _current_user: User = Depends(get_current_user)):
    response.delete_cookie(key="access_token")
    return {"message": "Successfully logged out"}


@app.get("/public")
async def public_test():
    return "Public endpoint available"


@app.get("/private")
async def private_test(_current_user: User = Depends(get_current_user)):
    return "Private endpoint available"


@app.post("/send_message")
async def send_message(payload: ChatMessage, _current_user: User = Depends(get_current_user)) -> str:
    return await api_integration.send_message(payload.id, payload.message)


@app.get('/lists')
async def get_list(_current_user: User = Depends(get_current_user)):
    return await api_integration.get_list()


@app.post("/upload")
async def upload_file(file: UploadFile = File(...), _current_user: User = Depends(get_current_user)):
    file_location = f"uploads/{file.filename}"

    os.makedirs(os.path.dirname(file_location), exist_ok=True)

    with open(file_location, "wb+") as file_object:
        file_object.write(await file.read())

    return {"info": f"File '{file.filename}' uploaded successfully"}


def get_db():
    db = SessionLocal()
    try:
        yield db
    finally:
        db.close()


@app.on_event("startup")
def startup(db: Session = Depends(get_db)):
    Base.metadata.create_all(bind=engine)
    db = SessionLocal()
    try:
        static_activities = [
            {"activity": "Standing"},
            {"activity": "10 Steps"},
            {"activity": "Stairs Up"},
            {"activity": "Stairs Down"},
            {"activity": "Turning 90 Degrees Left"},
            {"activity": "Turning 90 Degrees Right"},
            {"activity": "Elevator up"},
            {"activity": "Elevator Down"},
        ]

        if db.query(Activity).count() == 0:
            db.bulk_insert_mappings(Activity, static_activities)
            db.commit()
            print("Static activities inserted into the database")
        else:
            print("Static activities already exist in the database")
    finally:
        db.close()


def insert_data(table, rows: list[dict], db: Session):
    primary_key_column = [column for column in table.columns if column.primary_key][0]
    try:
        result = db.execute(table.insert().values(rows).returning(primary_key_column))
        db.commit()
        inserted_id = result.fetchone()[0]

        return {"message": f"Data inserted into {table.name}", "inserted_id": inserted_id}
    except Exception as e:
        db.rollback()
        raise Exception(f"Error inserting data: {str(e)}")


@app.post("/insert_data")
async def insert_dynamic_data(payload: dict, _current_user: User = Depends(get_current_user), db: Session = Depends(get_db)):
    table_name = payload.get("table_name")
    data_string = payload.get("data")  # "value1;value2;value3\nvalue1;value2;value3"

    if not table_name or not data_string:
        raise HTTPException(status_code=400, detail="Missing 'table_name' or 'data'")

    metadata = MetaData()
    table = Table(table_name, metadata, autoload_with=db.bind)
    column_names = [column.name for column in table.columns if not column.autoincrement]

    rows = []
    data_lines = data_string.strip().split("\n")
    for line in data_lines:
        values = line.split(";")

        row = {column: (value if value != "None" else None) for column, value in zip(column_names, values)}
        if table_name == "test_descriptions":
            row["username"] = _current_user.username

        rows.append(row)

    try:
        result = insert_data(table, rows, db)
        return result
    except Exception as e:
        raise HTTPException(status_code=400, detail=str(e))


@app.get("/last_id")
async def get_last_id(table_name: str, _current_user: User = Depends(get_current_user), db: Session = Depends(get_db)):
    try:
        metadata = MetaData()
        table = Table(table_name, metadata, autoload_with=db.bind)

        primary_key_column = table.primary_key.columns.values()[0]

        last_id = db.query(primary_key_column).order_by(desc(primary_key_column)).first()

        if last_id is not None:
            return {"last_id": last_id[0]}
        else:
            return {"last_id": None}
    except Exception as e:
        raise HTTPException(status_code=400, detail=str(e))


@app.delete("/delete_data")
async def delete_data(payload: dict, _current_user: User = Depends(get_current_user), db: Session = Depends(get_db)):
    try:
        metadata = MetaData()
        table_name = payload.get("table_name")
        id_to_delete = int(payload.get("id"))

        table = Table(table_name, metadata, autoload_with=db.bind)

        primary_key_column = list(table.primary_key.columns)[0]

        delete_row = table.delete().where(primary_key_column == id_to_delete)
        result = db.execute(delete_row)
        db.commit()

        if result.rowcount == 0:
            raise HTTPException(status_code=404, detail=f"No row found in table '{table_name}' with id '{id_to_delete}'")

        return {"message": f"Row with id {id_to_delete} successfully deleted from table '{table_name}'"}

    except Exception as e:
        db.rollback()
        raise HTTPException(status_code=400, detail=f"Error: {str(e)}")


