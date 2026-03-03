from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy.orm import Session

from app.auth import verify_credentials
from app.database import get_db
from app.schemas import ClassifyRequest, ClassifyResponse
from app.services.classify_service import ClassifyService

router = APIRouter(tags=["Classification"])


@router.post("/classify", response_model=ClassifyResponse)
async def classify_column(
    request: ClassifyRequest,
    username: str = Depends(verify_credentials),
    db: Session = Depends(get_db),
):
    """
    Classify data in a specific column using LLM.
    
    - Finds metadata and connection info from Column ID
    - Extracts specified number of sample data
    - Performs PII classification using LLM
    - Returns probability distributions from predefined classes
    """
    service = ClassifyService(db)

    try:
        result = await service.classify_column(
            column_id=request.column_id,
            sample_count=request.sample_count,
        )
    except ValueError as e:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail=str(e),
        )
    except Exception as e:
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=f"Classification failed: {str(e)}",
        )

    return result
