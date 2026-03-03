import logging
from datetime import datetime, timedelta, timezone

from fastapi import Depends, HTTPException, status
from fastapi.security import HTTPBasic, HTTPBasicCredentials
from jose import jwt
import secrets

from app.config import get_settings

logger = logging.getLogger("app.auth")

security = HTTPBasic()
settings = get_settings()


def verify_credentials(credentials: HTTPBasicCredentials = Depends(security)) -> str:
    """
    Verify Basic Auth credentials using constant-time comparison.
    Returns authenticated username.
    """
    correct_username = secrets.compare_digest(
        credentials.username, settings.AUTH_USERNAME
    )
    correct_password = secrets.compare_digest(
        credentials.password, settings.AUTH_PASSWORD.get_secret_value()
    )

    if not (correct_username and correct_password):
        logger.warning(f"Failed authentication attempt for user: '{credentials.username}'")
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Invalid credentials",
            headers={"WWW-Authenticate": "Basic"},
        )

    logger.info(f"User '{credentials.username}' authenticated successfully")
    return credentials.username


def create_access_token(username: str) -> str:
    """Create a JWT access token with expiration."""
    expire = datetime.now(timezone.utc) + timedelta(minutes=settings.JWT_EXPIRATION_MINUTES)
    payload = {"sub": username, "exp": expire, "iat": datetime.now(timezone.utc)}
    return jwt.encode(
        payload,
        settings.JWT_SECRET_KEY.get_secret_value(),
        algorithm=settings.JWT_ALGORITHM,
    )
