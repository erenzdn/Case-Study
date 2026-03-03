from fastapi import APIRouter, Depends

from app.auth import verify_credentials, create_access_token
from app.schemas import TokenResponse

router = APIRouter(tags=["Authentication"])


@router.post("/auth", response_model=TokenResponse)
def authenticate(username: str = Depends(verify_credentials)):
    """
    Authenticate user with Basic Auth credentials.
    Returns a JWT access token.
    """
    token = create_access_token(username)
    return TokenResponse(access_token=token)
