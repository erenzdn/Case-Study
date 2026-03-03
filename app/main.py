import logging
import time

from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from sqlalchemy import text
from sqlalchemy.exc import SQLAlchemyError

from app.database import engine, Base
from app.middleware import RequestLoggingMiddleware
from app.exceptions import (
    global_exception_handler,
    sqlalchemy_exception_handler,
    value_error_handler,
)
from app.routers import auth, metadata, classify

# Configure logging
logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s | %(levelname)-8s | %(name)-25s | %(message)s",
    datefmt="%Y-%m-%d %H:%M:%S",
)
logger = logging.getLogger("app.main")

# Create FastAPI app
app = FastAPI(
    title="LLM-Based Database Data Discovery System",
    description=(
        "A system for automatic discovery and classification of data "
        "in PostgreSQL databases using LLM technologies."
    ),
    version="1.0.0",
    docs_url="/docs",
    redoc_url="/redoc",
)

# Register global exception handlers
app.add_exception_handler(Exception, global_exception_handler)
app.add_exception_handler(SQLAlchemyError, sqlalchemy_exception_handler)
app.add_exception_handler(ValueError, value_error_handler)

# Middleware (order matters: first added = outermost)
app.add_middleware(RequestLoggingMiddleware)
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# Include routers
app.include_router(auth.router)
app.include_router(metadata.router)
app.include_router(classify.router)


def _wait_for_db(max_retries: int = 10, retry_delay: int = 3) -> None:
    """Wait for the system database to become available with retry logic."""
    for attempt in range(1, max_retries + 1):
        try:
            with engine.connect() as conn:
                conn.execute(text("SELECT 1"))
            logger.info(f"System database connection established (attempt {attempt}/{max_retries})")
            return
        except Exception as e:
            if attempt == max_retries:
                logger.error(
                    f"Could not connect to system database after {max_retries} attempts: "
                    f"{type(e).__name__}: {e}"
                )
                raise
            logger.warning(
                f"System database not ready (attempt {attempt}/{max_retries}): "
                f"{type(e).__name__}. Retrying in {retry_delay}s..."
            )
            time.sleep(retry_delay)


@app.on_event("startup")
def on_startup():
    """Wait for database and create all system database tables on startup."""
    logger.info("Waiting for system database connection...")
    _wait_for_db()

    logger.info("Creating system database tables...")
    try:
        Base.metadata.create_all(bind=engine)
        logger.info("System database tables created successfully.")
    except Exception as e:
        logger.error(f"Failed to create system database tables: {e}")
        raise


@app.get("/", tags=["Health"])
def health_check():
    """Health check endpoint."""
    return {
        "status": "healthy",
        "service": "LLM-Based Database Data Discovery System",
        "version": "1.0.0",
    }
