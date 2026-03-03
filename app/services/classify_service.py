import logging
from uuid import UUID

from sqlalchemy import text
from sqlalchemy.orm import Session

from app.database import create_target_engine
from app.models import ColumnMetadata, TableMetadata, DatabaseMetadata
from app.schemas import ClassifyResponse, ProbabilityItem
from app.services.llm_service import classify_with_llm

logger = logging.getLogger("app.services.classify")


class ClassifyService:
    """Service for LLM-based data classification."""

    def __init__(self, db: Session):
        self.db = db

    async def classify_column(self, column_id: UUID, sample_count: int = 10) -> ClassifyResponse:
        """
        Classify data in a specific column using LLM.
        
        1. Find metadata and connection info from Column ID
        2. Extract specified number of sample data
        3. Perform classification using LLM
        4. Return probability distributions
        """
        logger.info(f"Starting classification for column_id={column_id}, sample_count={sample_count}")

        # Step 1: Find column metadata with joins to table and database
        column = (
            self.db.query(ColumnMetadata)
            .filter(ColumnMetadata.id == column_id)
            .first()
        )

        if not column:
            logger.warning(f"Column not found: {column_id}")
            raise ValueError(f"Column with ID {column_id} not found")

        table = self.db.query(TableMetadata).filter(TableMetadata.id == column.table_id).first()
        if not table:
            logger.error(f"Table metadata not found for column {column_id}")
            raise ValueError(f"Table metadata not found for column {column_id}")

        database = self.db.query(DatabaseMetadata).filter(DatabaseMetadata.id == table.database_id).first()
        if not database:
            logger.error(f"Database metadata not found for column {column_id}")
            raise ValueError(f"Database metadata not found for column {column_id}")

        logger.info(
            f"Found column: {database.database_name}.{table.table_name}.{column.column_name} "
            f"(type: {column.data_type})"
        )

        # Step 2: Connect to target database and extract sample data
        logger.info(f"Connecting to target DB to extract {sample_count} samples...")
        target_engine = create_target_engine(
            host=database.host,
            port=database.port,
            database=database.database_name,
            username=database.username,
            password=database.password,
        )

        samples = []
        try:
            with target_engine.connect() as conn:
                # Use parameterized table/column names (safe since they come from information_schema)
                query = text(
                    f'SELECT "{column.column_name}" FROM "{table.schema_name}"."{table.table_name}" '
                    f"WHERE \"{column.column_name}\" IS NOT NULL LIMIT :limit"
                )
                result = conn.execute(query, {"limit": sample_count})
                samples = [str(row[0]) for row in result.fetchall()]
                logger.info(f"Extracted {len(samples)} samples from {table.table_name}.{column.column_name}")
        except Exception as e:
            logger.error(f"Failed to extract samples from target DB: {e}")
            raise RuntimeError(
                f"Failed to extract sample data from column '{column.column_name}' "
                f"in table '{table.table_name}': {str(e)}"
            )
        finally:
            target_engine.dispose()

        if not samples:
            logger.warning(f"No non-null data found in {table.table_name}.{column.column_name}")
            raise ValueError(
                f"No non-null data found in column '{column.column_name}' "
                f"of table '{table.table_name}'"
            )

        logger.debug(f"Sample data: {samples[:3]}{'...' if len(samples) > 3 else ''}")

        # Step 3: Classify using LLM
        logger.info(f"Sending {len(samples)} samples to LLM for classification...")
        try:
            classifications = await classify_with_llm(
                column_name=column.column_name,
                table_name=table.table_name,
                data_type=column.data_type,
                samples=samples,
            )
            top_class = classifications[0] if classifications else None
            logger.info(
                f"Classification complete: top category = '{top_class['category']}' "
                f"(probability: {top_class['probability']})" if top_class else "No classifications returned"
            )
        except Exception as e:
            logger.error(f"LLM classification failed for {table.table_name}.{column.column_name}: {e}")
            raise

        # Step 4: Build response
        return ClassifyResponse(
            column_id=column.id,
            column_name=column.column_name,
            table_name=table.table_name,
            database_name=database.database_name,
            sample_count=len(samples),
            samples=samples,
            classifications=[
                ProbabilityItem(
                    category=c["category"],
                    probability=c["probability"],
                )
                for c in classifications
            ],
        )
