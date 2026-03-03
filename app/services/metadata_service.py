import logging
from uuid import UUID
from typing import Optional

from sqlalchemy import text
from sqlalchemy.orm import Session

from app.database import create_target_engine
from app.models import DatabaseMetadata, TableMetadata, ColumnMetadata

logger = logging.getLogger("app.services.metadata")


class MetadataService:
    """Service for extracting and managing database metadata."""

    def __init__(self, db: Session):
        self.db = db

    def extract_and_store_metadata(
        self,
        host: str,
        port: int,
        database: str,
        username: str,
        password: str,
    ) -> DatabaseMetadata:
        """
        Connect to a target PostgreSQL database, extract table/column metadata,
        and store it in the system database.
        """
        # Security: mask sensitive info in logs
        masked_user = f"{username[:2]}***" if len(username) > 2 else "***"
        logger.info(f"Connecting to target database: {database}@{host}:{port} (user: {masked_user})")

        # Create connection to target database
        target_engine = create_target_engine(host, port, database, username, password)

        # Test connection
        try:
            with target_engine.connect() as conn:
                conn.execute(text("SELECT 1"))
            logger.info(f"Successfully connected to target database: {database}")
        except Exception as e:
            logger.error(f"Failed to connect to target database {database}@{host}:{port}: {type(e).__name__}")
            raise ConnectionError(
                f"Cannot connect to database '{database}' at {host}:{port}. "
                f"Please verify connection details. Error: {type(e).__name__}"
            )

        # Create database metadata record
        db_metadata = DatabaseMetadata(
            database_name=database,
            host=host,
            port=port,
            username=username,
            password=password,
        )
        self.db.add(db_metadata)
        self.db.flush()
        logger.info(f"Created metadata record: {db_metadata.id}")

        # Extract table and column information from information_schema
        try:
            with target_engine.connect() as conn:
                # Get all tables in public schema
                tables_result = conn.execute(
                    text("""
                        SELECT table_name 
                        FROM information_schema.tables 
                        WHERE table_schema = 'public' 
                          AND table_type = 'BASE TABLE'
                        ORDER BY table_name
                    """)
                )
                tables = tables_result.fetchall()
                logger.info(f"Found {len(tables)} tables in database '{database}'")

                for table_row in tables:
                    table_name = table_row[0]

                    # Create table metadata
                    table_metadata = TableMetadata(
                        database_id=db_metadata.id,
                        table_name=table_name,
                        schema_name="public",
                    )
                    self.db.add(table_metadata)
                    self.db.flush()

                    # Get column information for this table
                    columns_result = conn.execute(
                        text("""
                            SELECT 
                                column_name,
                                data_type,
                                is_nullable,
                                column_default,
                                ordinal_position
                            FROM information_schema.columns 
                            WHERE table_schema = 'public' 
                              AND table_name = :table_name
                            ORDER BY ordinal_position
                        """),
                        {"table_name": table_name},
                    )
                    columns = columns_result.fetchall()
                    logger.debug(f"  Table '{table_name}': {len(columns)} columns")

                    for col_row in columns:
                        column_metadata = ColumnMetadata(
                            table_id=table_metadata.id,
                            column_name=col_row[0],
                            data_type=col_row[1],
                            is_nullable=col_row[2],
                            column_default=col_row[3],
                            ordinal_position=col_row[4],
                        )
                        self.db.add(column_metadata)

        except Exception as e:
            logger.error(f"Failed to extract metadata from database '{database}': {e}")
            self.db.rollback()
            raise RuntimeError(
                f"Failed to extract metadata from database '{database}': {str(e)}"
            )

        self.db.commit()
        self.db.refresh(db_metadata)

        # Dispose the target engine connection
        target_engine.dispose()

        total_columns = sum(len(t.columns) for t in db_metadata.tables)
        logger.info(
            f"Metadata extraction complete: {db_metadata.table_count} tables, "
            f"{total_columns} columns stored with ID {db_metadata.id}"
        )

        return db_metadata

    def list_all_metadata(self) -> list[DatabaseMetadata]:
        """List all stored metadata records."""
        records = self.db.query(DatabaseMetadata).order_by(DatabaseMetadata.created_at.desc()).all()
        logger.info(f"Listed {len(records)} metadata records")
        return records

    def get_metadata_by_id(self, metadata_id: UUID) -> Optional[DatabaseMetadata]:
        """Get a specific metadata record by its ID."""
        record = self.db.query(DatabaseMetadata).filter(DatabaseMetadata.id == metadata_id).first()
        if record:
            logger.info(f"Found metadata record: {metadata_id}")
        else:
            logger.warning(f"Metadata record not found: {metadata_id}")
        return record

    def delete_metadata(self, metadata_id: UUID) -> bool:
        """Delete a metadata record and all associated data (cascading)."""
        record = self.db.query(DatabaseMetadata).filter(DatabaseMetadata.id == metadata_id).first()
        if not record:
            logger.warning(f"Cannot delete: metadata {metadata_id} not found")
            return False

        self.db.delete(record)
        self.db.commit()
        logger.info(f"Deleted metadata record: {metadata_id} (cascade: tables + columns)")
        return True
