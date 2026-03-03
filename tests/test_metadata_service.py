"""
Unit tests for metadata service (app/services/metadata_service.py)
Tests: credential masking, connection URL building, response mapping
"""
import pytest
from unittest.mock import MagicMock, patch


class TestMetadataServiceCredentialMasking:
    """Tests that credentials are never logged in plaintext."""

    def test_password_not_in_log_output(self, caplog):
        """Database password must never appear in log output."""
        import logging
        with patch("app.services.metadata_service.get_settings"), \
             patch("app.services.metadata_service.DatabaseMetadata"), \
             patch("app.services.metadata_service.SessionLocal"):

            from app.services.metadata_service import MetadataService
            service = MetadataService()

            password = "super-secret-password"

            with caplog.at_level(logging.INFO):
                # Simulate the masking logic directly
                masked = service._mask_credential(password)

            assert password not in caplog.text
            assert masked != password

    def test_masked_credential_shows_partial(self):
        """Masked credential should show first 2 chars + asterisks."""
        with patch("app.services.metadata_service.get_settings"):
            from app.services.metadata_service import MetadataService
            service = MetadataService()

        masked = service._mask_credential("secret123")
        assert masked.startswith("se")
        assert "***" in masked
        assert "secret123" not in masked

    def test_short_credential_masking(self):
        """Very short credentials should still be masked safely."""
        with patch("app.services.metadata_service.get_settings"):
            from app.services.metadata_service import MetadataService
            service = MetadataService()

        masked = service._mask_credential("ab")
        assert "ab" not in masked or "***" in masked


class TestMetadataServiceConnectionString:
    """Tests for JDBC/psycopg2 connection string building."""

    def test_connection_string_format(self):
        """Connection string must follow postgresql:// format."""
        with patch("app.services.metadata_service.get_settings"):
            from app.services.metadata_service import MetadataService
            service = MetadataService()

        conn_str = service._build_connection_string(
            host="localhost",
            port=5432,
            database="mydb",
            username="user",
            password="pass"
        )
        assert "postgresql" in conn_str or "postgres" in conn_str
        assert "localhost" in conn_str
        assert "mydb" in conn_str
