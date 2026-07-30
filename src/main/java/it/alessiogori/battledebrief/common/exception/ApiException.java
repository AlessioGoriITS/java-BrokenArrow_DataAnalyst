package it.alessiogori.battledebrief.common.exception;

import org.springframework.http.HttpStatus;

public abstract class ApiException extends RuntimeException {

    private final HttpStatus status;
    private final ApiErrorCode errorCode;

    protected ApiException(
            String message,
            HttpStatus status,
            ApiErrorCode errorCode
    ) {
        super(message);
        this.status = status;
        this.errorCode = errorCode;
    }

    protected ApiException(
            String message,
            Throwable cause,
            HttpStatus status,
            ApiErrorCode errorCode
    ) {
        super(message, cause);
        this.status = status;
        this.errorCode = errorCode;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public ApiErrorCode getErrorCode() {
        return errorCode;
    }
}
