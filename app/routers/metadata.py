from uuid import UUID

from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy.orm import Session

from app.auth import verify_credentials
from app.database import get_db
from app.schemas import (
    DBConnectionRequest,
    MetadataCreateResponse,
    MetadataListItem,
    MetadataDetailResponse,
    TableInfo,
    ColumnInfo,
    MessageResponse,
)
from app.services.metadata_service import MetadataService

router = APIRouter(tags=["Metadata"])


@router.post("/db/metadata", response_model=MetadataCreateResponse, status_code=status.HTTP_201_CREATED)
def extract_metadata(
    request: DBConnectionRequest,
    username: str = Depends(verify_credentials),
    db: Session = Depends(get_db),
):
    """
    Connect to a PostgreSQL database, extract metadata (tables, columns),
    and store it in the system database.
    """
    service = MetadataService(db)
    try:
        db_metadata = service.extract_and_store_metadata(
            host=request.host,
            port=request.port,
            database=request.database,
            username=request.username,
            password=request.password,
        )
    except Exception as e:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail=f"Failed to connect or extract metadata: {str(e)}",
        )

    return MetadataCreateResponse(
        metadata_id=db_metadata.id,
        database_name=db_metadata.database_name,
        table_count=db_metadata.table_count,
        tables=[
            TableInfo(
                table_id=t.id,
                table_name=t.table_name,
                schema_name=t.schema_name,
                columns=[
                    ColumnInfo(
                        column_id=c.id,
                        column_name=c.column_name,
                        data_type=c.data_type,
                        is_nullable=c.is_nullable,
                        ordinal_position=c.ordinal_position,
                    )
                    for c in t.columns
                ],
            )
            for t in db_metadata.tables
        ],
    )


@router.get("/metadata", response_model=list[MetadataListItem])
def list_metadata(
    username: str = Depends(verify_credentials),
    db: Session = Depends(get_db),
):
    """
    List all stored metadata records.
    Returns metadata IDs, database names, creation dates, and table counts.
    """
    service = MetadataService(db)
    records = service.list_all_metadata()

    return [
        MetadataListItem(
            metadata_id=r.id,
            database_name=r.database_name,
            created_at=r.created_at,
            table_count=r.table_count,
        )
        for r in records
    ]


@router.get("/metadata/{metadata_id}", response_model=MetadataDetailResponse)
def get_metadata_detail(
    metadata_id: UUID,
    username: str = Depends(verify_credentials),
    db: Session = Depends(get_db),
):
    """
    Retrieve details of a specific metadata record.
    Returns table list, column details, column IDs, and data types.
    """
    service = MetadataService(db)
    record = service.get_metadata_by_id(metadata_id)

    if not record:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail=f"Metadata with ID {metadata_id} not found",
        )

    return MetadataDetailResponse(
        metadata_id=record.id,
        database_name=record.database_name,
        created_at=record.created_at,
        tables=[
            TableInfo(
                table_id=t.id,
                table_name=t.table_name,
                schema_name=t.schema_name,
                columns=[
                    ColumnInfo(
                        column_id=c.id,
                        column_name=c.column_name,
                        data_type=c.data_type,
                        is_nullable=c.is_nullable,
                        ordinal_position=c.ordinal_position,
                    )
                    for c in t.columns
                ],
            )
            for t in record.tables
        ],
    )


@router.delete("/metadata/{metadata_id}", response_model=MessageResponse)
def delete_metadata(
    metadata_id: UUID,
    username: str = Depends(verify_credentials),
    db: Session = Depends(get_db),
):
    """
    Delete a stored metadata record and all associated data.
    """
    service = MetadataService(db)
    deleted = service.delete_metadata(metadata_id)

    if not deleted:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail=f"Metadata with ID {metadata_id} not found",
        )

    return MessageResponse(
        message=f"Metadata {metadata_id} deleted successfully",
        status="success",
    )
