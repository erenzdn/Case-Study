"""
Unit tests for LLM service (app/services/llm_service.py)
Tests: prompt building, probability normalization, PII category coverage
"""
import pytest
from unittest.mock import MagicMock, patch


class TestLlmServicePromptBuilding:
    """Tests for prompt construction logic."""

    def setup_method(self):
        """Create a patched LlmService instance."""
        with patch("app.services.llm_service.get_settings"):
            from app.services.llm_service import LlmService
            self.service = LlmService()

    def test_prompt_contains_column_name(self):
        """Prompt must include the column name for context."""
        prompt = self.service._build_classification_prompt(
            column_name="email",
            table_name="customers",
            data_type="character varying",
            samples=["test@example.com"]
        )
        assert "email" in prompt

    def test_prompt_contains_table_name(self):
        """Prompt must include the table name for context."""
        prompt = self.service._build_classification_prompt(
            column_name="email",
            table_name="customers",
            data_type="character varying",
            samples=["test@example.com"]
        )
        assert "customers" in prompt

    def test_prompt_contains_all_13_categories(self):
        """All 13 PII categories must appear in the prompt."""
        expected_categories = [
            "Email", "Phone Number", "SSN", "Credit Card",
            "National ID", "Full Name", "First Name", "Last Name",
            "TCKN", "Address", "Date of Birth", "IP Address", "Not PII"
        ]
        prompt = self.service._build_classification_prompt(
            column_name="data",
            table_name="test",
            data_type="text",
            samples=["sample"]
        )
        for category in expected_categories:
            assert category in prompt, f"Category '{category}' missing from prompt"

    def test_prompt_contains_samples(self):
        """Sample data must appear in the prompt."""
        samples = ["value1", "value2", "value3"]
        prompt = self.service._build_classification_prompt(
            column_name="col",
            table_name="tbl",
            data_type="text",
            samples=samples
        )
        for sample in samples:
            assert sample in prompt

    def test_prompt_mentions_turkish_context(self):
        """Prompt must include Turkish-specific context (TCKN awareness)."""
        prompt = self.service._build_classification_prompt(
            column_name="kimlik_no",
            table_name="musteriler",
            data_type="character varying",
            samples=["12345678901"]
        )
        assert "TCKN" in prompt or "Turkish" in prompt


class TestLlmServiceCategoryCount:
    """Tests for PII category definitions."""

    def test_has_exactly_13_categories(self):
        """Service must define exactly 13 PII categories (12 + Not PII)."""
        with patch("app.services.llm_service.get_settings"):
            from app.services.llm_service import LlmService
            service = LlmService()

        assert len(service.PII_CATEGORIES) == 13

    def test_not_pii_category_exists(self):
        """'Not PII' category must be present."""
        with patch("app.services.llm_service.get_settings"):
            from app.services.llm_service import LlmService
            service = LlmService()

        assert "Not PII" in service.PII_CATEGORIES
