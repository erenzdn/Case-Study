"""
Unit tests for authentication module (app/auth.py)
Tests: credential verification, JWT token creation
"""
import pytest
from unittest.mock import MagicMock, patch
from fastapi import HTTPException
from fastapi.security import HTTPBasicCredentials
from jose import jwt


class TestVerifyCredentials:
    """Tests for verify_credentials() function."""

    def test_correct_credentials_returns_username(self):
        """Valid credentials should return the authenticated username."""
        from app.auth import verify_credentials

        credentials = HTTPBasicCredentials(username="admin", password="admin123")

        with patch("app.auth.settings") as mock_settings:
            mock_settings.AUTH_USERNAME = "admin"
            mock_settings.AUTH_PASSWORD.get_secret_value.return_value = "admin123"
            result = verify_credentials(credentials)

        assert result == "admin"

    def test_wrong_username_raises_401(self):
        """Wrong username should raise HTTP 401."""
        from app.auth import verify_credentials

        credentials = HTTPBasicCredentials(username="hacker", password="admin123")

        with patch("app.auth.settings") as mock_settings:
            mock_settings.AUTH_USERNAME = "admin"
            mock_settings.AUTH_PASSWORD.get_secret_value.return_value = "admin123"

            with pytest.raises(HTTPException) as exc_info:
                verify_credentials(credentials)

        assert exc_info.value.status_code == 401

    def test_wrong_password_raises_401(self):
        """Wrong password should raise HTTP 401."""
        from app.auth import verify_credentials

        credentials = HTTPBasicCredentials(username="admin", password="wrongpass")

        with patch("app.auth.settings") as mock_settings:
            mock_settings.AUTH_USERNAME = "admin"
            mock_settings.AUTH_PASSWORD.get_secret_value.return_value = "admin123"

            with pytest.raises(HTTPException) as exc_info:
                verify_credentials(credentials)

        assert exc_info.value.status_code == 401

    def test_empty_credentials_raises_401(self):
        """Empty credentials should raise HTTP 401."""
        from app.auth import verify_credentials

        credentials = HTTPBasicCredentials(username="", password="")

        with patch("app.auth.settings") as mock_settings:
            mock_settings.AUTH_USERNAME = "admin"
            mock_settings.AUTH_PASSWORD.get_secret_value.return_value = "admin123"

            with pytest.raises(HTTPException) as exc_info:
                verify_credentials(credentials)

        assert exc_info.value.status_code == 401

    def test_wrong_credentials_includes_www_authenticate_header(self):
        """401 response must include WWW-Authenticate header (RFC 7617)."""
        from app.auth import verify_credentials

        credentials = HTTPBasicCredentials(username="wrong", password="wrong")

        with patch("app.auth.settings") as mock_settings:
            mock_settings.AUTH_USERNAME = "admin"
            mock_settings.AUTH_PASSWORD.get_secret_value.return_value = "admin123"

            with pytest.raises(HTTPException) as exc_info:
                verify_credentials(credentials)

        assert "WWW-Authenticate" in exc_info.value.headers


class TestCreateAccessToken:
    """Tests for create_access_token() function."""

    def test_token_is_valid_jwt(self):
        """Token must be a valid, decodable JWT."""
        from app.auth import create_access_token

        secret = "test-secret-key-at-least-32-chars!!"
        with patch("app.auth.settings") as mock_settings:
            mock_settings.JWT_SECRET_KEY.get_secret_value.return_value = secret
            mock_settings.JWT_ALGORITHM = "HS256"
            mock_settings.JWT_EXPIRATION_MINUTES = 60

            token = create_access_token("admin")

        decoded = jwt.decode(token, secret, algorithms=["HS256"])
        assert decoded["sub"] == "admin"

    def test_token_contains_sub_claim(self):
        """Token payload must contain 'sub' claim with username."""
        from app.auth import create_access_token

        secret = "test-secret-key-at-least-32-chars!!"
        with patch("app.auth.settings") as mock_settings:
            mock_settings.JWT_SECRET_KEY.get_secret_value.return_value = secret
            mock_settings.JWT_ALGORITHM = "HS256"
            mock_settings.JWT_EXPIRATION_MINUTES = 60

            token = create_access_token("testuser")

        decoded = jwt.decode(token, secret, algorithms=["HS256"])
        assert decoded["sub"] == "testuser"

    def test_token_has_expiration(self):
        """Token must include 'exp' claim."""
        from app.auth import create_access_token

        secret = "test-secret-key-at-least-32-chars!!"
        with patch("app.auth.settings") as mock_settings:
            mock_settings.JWT_SECRET_KEY.get_secret_value.return_value = secret
            mock_settings.JWT_ALGORITHM = "HS256"
            mock_settings.JWT_EXPIRATION_MINUTES = 60

            token = create_access_token("admin")

        decoded = jwt.decode(token, secret, algorithms=["HS256"])
        assert "exp" in decoded

    def test_different_users_get_different_tokens(self):
        """Different usernames should produce distinct tokens."""
        from app.auth import create_access_token

        secret = "test-secret-key-at-least-32-chars!!"
        with patch("app.auth.settings") as mock_settings:
            mock_settings.JWT_SECRET_KEY.get_secret_value.return_value = secret
            mock_settings.JWT_ALGORITHM = "HS256"
            mock_settings.JWT_EXPIRATION_MINUTES = 60

            token1 = create_access_token("admin")
            token2 = create_access_token("other")

        assert token1 != token2
