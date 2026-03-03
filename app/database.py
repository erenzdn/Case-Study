from typing import Generator

from sqlalchemy import create_engine, Engine
from sqlalchemy.orm import sessionmaker, declarative_base, Session
from sqlalchemy.pool import QueuePool

from app.config import get_settings

settings = get_settings()

# System database engine with connection pooling
engine: Engine = create_engine(
    settings.system_database_url,
    pool_pre_ping=True,
    pool_size=5,
    max_overflow=10,
    pool_recycle=3600,
    poolclass=QueuePool,
)

# Session factory
SessionLocal = sessionmaker(autocommit=False, autoflush=False, bind=engine)

# Base model class
Base = declarative_base()


def get_db() -> Generator[Session, None, None]:
    """FastAPI dependency: yields a database session and ensures cleanup."""
    db = SessionLocal()
    try:
        yield db
    finally:
        db.close()


def create_target_engine(
    host: str, port: int, database: str, username: str, password: str
) -> Engine:
    """Create a disposable SQLAlchemy engine for a target database connection."""
    url = f"postgresql://{username}:{password}@{host}:{port}/{database}"
    return create_engine(
        url,
        pool_pre_ping=True,
        pool_size=2,
        max_overflow=3,
        pool_recycle=1800,
    )
