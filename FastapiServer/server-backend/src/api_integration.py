import logging

import fastapi
import httpx
from httpx_auth import Basic

import utils

logger: logging.Logger = logging.getLogger("uvicorn.error").getChild(f'cb-{__name__}')


def _setup_client() -> tuple[httpx.AsyncClient, str]:
    """ This method sets up a client to connect to another api with variables set in docker compose.
     If another api is not available, these methods do nothing. """
    try:
        api_endpoint = utils.get_parameter('API_ENDPOINT')
        api_login = utils.get_parameter('API_LOGIN')
        api_password = utils.get_parameter('API_PASSWORD')
        client = httpx.AsyncClient(auth=Basic(api_login, api_password))
        return client, api_endpoint
    except Exception as ex:
        logger.error("Failed to create api client", exc_info=ex)
        raise fastapi.HTTPException(status_code=500, detail="Internal fail - see logs")


async def get_list():
    client, api_endpoint = _setup_client()
    url = f"{api_endpoint}/api/list"
    r = await client.get(url)
    return r.json()

async def send_message(id: str, message: str) -> str:
    client, api_endpoint = _setup_client()

    r = await client.post(url)
    return r