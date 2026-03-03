from datetime import datetime
from typing import Optional
from uuid import UUID

from pydantic import BaseModel, Field


# ==================== Auth Schemas ====================

class TokenResponse(BaseModel):
    access_token: str
    token_type: str = "bearer"


# ==================== Database Connection Schemas ====================

class DBConnectionRequest(BaseModel):
    host: str = Field(..., description="Database host address")
    port: int = Field(5432, description="Database port")
    database: str = Field(..., description="Database name")
    username: str = Field(..., description="Database username")
    password: str = Field(..., description="Database password")


# ==================== Column Schemas ====================

class ColumnInfo(BaseModel):
    column_id: UUID
    column_name: str
    data_type: str
    is_nullable: str
    ordinal_position: Optional[int] = None

    class Config:
        from_attributes = True


# ==================== Table Schemas ====================

class TableInfo(BaseModel):
    table_id: UUID
    table_name: str
    schema_name: str
    columns: list[ColumnInfo] = []

    class Config:
        from_attributes = True


# ==================== Metadata Schemas ====================

class MetadataListItem(BaseModel):
    metadata_id: UUID
    database_name: str
    created_at: datetime
    table_count: int

    class Config:
        from_attributes = True


class MetadataDetailResponse(BaseModel):
    metadata_id: UUID
    database_name: str
    created_at: datetime
    tables: list[TableInfo] = []

    class Config:
        from_attributes = True


class MetadataCreateResponse(BaseModel):
    metadata_id: UUID
    database_name: str
    table_count: int
    tables: list[TableInfo] = []


# ==================== Classification Schemas ====================

class ClassifyRequest(BaseModel):
    column_id: UUID = Field(..., description="Unique column identifier")
    sample_count: int = Field(10, description="Number of sample data to extract", ge=1, le=100)


class ProbabilityItem(BaseModel):
    category: str
    probability: float = Field(..., ge=0.0, le=1.0)


class ClassifyResponse(BaseModel):
    column_id: UUID
    column_name: str
    table_name: str
    database_name: str
    sample_count: int
    samples: list[str] = []
    classifications: list[ProbabilityItem] = []


# ==================== Common Schemas ====================

class MessageResponse(BaseModel):
    message: str
    status: str = "success"
