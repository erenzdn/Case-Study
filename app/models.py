import uuid
from datetime import datetime, timezone

from sqlalchemy import Column, String, Integer, DateTime, ForeignKey, Text, JSON
from sqlalchemy.orm import relationship
from sqlalchemy.dialects.postgresql import UUID

from app.database import Base


def _utcnow() -> datetime:
    """Return current UTC time as timezone-aware datetime."""
    return datetime.now(timezone.utc)


class DatabaseMetadata(Base):
    """Stores metadata about discovered databases."""
    __tablename__ = "database_metadata"

    id = Column(UUID(as_uuid=True), primary_key=True, default=uuid.uuid4)
    database_name = Column(String(255), nullable=False, index=True)
    host = Column(String(255), nullable=False)
    port = Column(Integer, nullable=False)
    username = Column(String(255), nullable=False)
    password = Column(Text, nullable=False)
    created_at = Column(DateTime(timezone=True), default=_utcnow)

    # Relationships
    tables = relationship(
        "TableMetadata",
        back_populates="database",
        cascade="all, delete-orphan",
        lazy="selectin",
    )

    @property
    def table_count(self) -> int:
        return len(self.tables) if self.tables else 0

    def __repr__(self) -> str:
        return f"<DatabaseMetadata(id={self.id}, name='{self.database_name}')>"


class TableMetadata(Base):
    """Stores metadata about tables within a database."""
    __tablename__ = "table_metadata"

    id = Column(UUID(as_uuid=True), primary_key=True, default=uuid.uuid4)
    database_id = Column(
        UUID(as_uuid=True),
        ForeignKey("database_metadata.id", ondelete="CASCADE"),
        nullable=False,
        index=True,
    )
    table_name = Column(String(255), nullable=False)
    schema_name = Column(String(255), default="public")
    created_at = Column(DateTime(timezone=True), default=_utcnow)

    # Relationships
    database = relationship("DatabaseMetadata", back_populates="tables")
    columns = relationship(
        "ColumnMetadata",
        back_populates="table",
        cascade="all, delete-orphan",
        lazy="selectin",
    )

    def __repr__(self) -> str:
        return f"<TableMetadata(id={self.id}, name='{self.table_name}')>"


class ColumnMetadata(Base):
    """Stores metadata about columns within a table."""
    __tablename__ = "column_metadata"

    id = Column(UUID(as_uuid=True), primary_key=True, default=uuid.uuid4)
    table_id = Column(
        UUID(as_uuid=True),
        ForeignKey("table_metadata.id", ondelete="CASCADE"),
        nullable=False,
        index=True,
    )
    column_name = Column(String(255), nullable=False)
    data_type = Column(String(100), nullable=False)
    is_nullable = Column(String(10), default="YES")
    column_default = Column(Text, nullable=True)
    ordinal_position = Column(Integer, nullable=True)
    created_at = Column(DateTime(timezone=True), default=_utcnow)

    # Relationships
    table = relationship("TableMetadata", back_populates="columns")
    classifications = relationship(
        "ClassificationResult",
        back_populates="column",
        cascade="all, delete-orphan",
        lazy="selectin",
    )

    def __repr__(self) -> str:
        return f"<ColumnMetadata(id={self.id}, name='{self.column_name}', type='{self.data_type}')>"


class ClassificationResult(Base):
    """Stores LLM classification results for columns."""
    __tablename__ = "classification_results"

    id = Column(UUID(as_uuid=True), primary_key=True, default=uuid.uuid4)
    column_id = Column(
        UUID(as_uuid=True),
        ForeignKey("column_metadata.id", ondelete="CASCADE"),
        nullable=False,
        index=True,
    )
    sample_count = Column(Integer, nullable=False)
    classification = Column(JSON, nullable=False)
    created_at = Column(DateTime(timezone=True), default=_utcnow)

    # Relationships
    column = relationship("ColumnMetadata", back_populates="classifications")

    def __repr__(self) -> str:
        return f"<ClassificationResult(id={self.id}, column_id={self.column_id})>"
