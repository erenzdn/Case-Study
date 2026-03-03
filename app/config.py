from pydantic import SecretStr
from pydantic_settings import BaseSettings, SettingsConfigDict
from functools import lru_cache


class Settings(BaseSettings):
    """Application settings loaded from environment variables."""

    # System Database
    SYSTEM_DB_HOST: str = "system_db"
    SYSTEM_DB_PORT: int = 5432
    SYSTEM_DB_NAME: str = "system_database"
    SYSTEM_DB_USER: str = "postgres"
    SYSTEM_DB_PASSWORD: SecretStr = SecretStr("postgres123")

    # Authentication
    AUTH_USERNAME: str = "admin"
    AUTH_PASSWORD: SecretStr = SecretStr("admin123")
    JWT_SECRET_KEY: SecretStr = SecretStr("change-this-to-a-random-secret-key")
    JWT_ALGORITHM: str = "HS256"
    JWT_EXPIRATION_MINUTES: int = 60

    # OpenAI Compatible API
    OPENAI_API_KEY: SecretStr = SecretStr("your-openai-api-key-here")
    OPENAI_BASE_URL: str = "https://api.openai.com/v1"
    OPENAI_MODEL: str = "gpt-4o-mini"

    @property
    def system_database_url(self) -> str:
        """Build PostgreSQL connection URL for the system database."""
        return (
            f"postgresql://{self.SYSTEM_DB_USER}:{self.SYSTEM_DB_PASSWORD.get_secret_value()}"
            f"@{self.SYSTEM_DB_HOST}:{self.SYSTEM_DB_PORT}/{self.SYSTEM_DB_NAME}"
        )

    model_config = SettingsConfigDict(
        env_file=".env",
        env_file_encoding="utf-8",
        case_sensitive=True,
    )


@lru_cache()
def get_settings() -> Settings:
    """Get cached application settings singleton."""
    return Settings()
