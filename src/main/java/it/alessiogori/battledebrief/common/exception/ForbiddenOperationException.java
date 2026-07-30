package it.alessiogori.battledebrief.common.exception;

import org.springframework.http.HttpStatus;

public class ForbiddenOperationException extends ApiException {

    public ForbiddenOperationException(String message) {
        super(message, HttpStatus.FORBIDDEN, ApiErrorCode.FORBIDDEN_OPERATION);
    }
}
