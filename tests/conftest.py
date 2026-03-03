"""
Pytest configuration and shared fixtures for Case 1 (Python/FastAPI) unit tests.
"""
import pytest
from unittest.mock import MagicMock, patch


@pytest.fixture(autouse=True)
def reset_settings():
    """Reset any cached settings between tests."""
    yield


@pytest.fixture
def mock_settings():
    """Provides a pre-configured mock settings object."""
    settings = MagicMock()
    settings.AUTH_USERNAME = "admin"
    settings.AUTH_PASSWORD.get_secret_value.return_value = "admin123"
    settings.JWT_SECRET_KEY.get_secret_value.return_value = "test-secret-key-32-chars-padded!!"
    settings.JWT_ALGORITHM = "HS256"
    settings.JWT_EXPIRATION_MINUTES = 60
    return settings
