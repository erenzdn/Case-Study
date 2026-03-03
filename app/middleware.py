import logging
import time
import uuid

from fastapi import Request, Response
from starlette.middleware.base import BaseHTTPMiddleware

logger = logging.getLogger("app.middleware")


class RequestLoggingMiddleware(BaseHTTPMiddleware):
    """Middleware to log all incoming requests and outgoing responses."""

    async def dispatch(self, request: Request, call_next) -> Response:
        # Generate unique request ID
        request_id = str(uuid.uuid4())[:8]
        
        # Log request
        logger.info(
            f"[{request_id}] → {request.method} {request.url.path} "
            f"| Client: {request.client.host if request.client else 'unknown'}"
        )

        # Measure response time
        start_time = time.time()

        try:
            response = await call_next(request)
            duration = round((time.time() - start_time) * 1000, 2)

            # Log response
            logger.info(
                f"[{request_id}] ← {response.status_code} "
                f"| Duration: {duration}ms"
            )

            # Add request ID to response headers
            response.headers["X-Request-ID"] = request_id
            return response

        except Exception as e:
            duration = round((time.time() - start_time) * 1000, 2)
            logger.error(
                f"[{request_id}] ✗ Unhandled exception after {duration}ms: {str(e)}"
            )
            raise
