package it.alessiogori.battledebrief.common.exception;

import org.springframework.http.HttpStatus;

public class DuplicateResourceException extends ApiException {

    public DuplicateResourceException(String message) {
        super(message, HttpStatus.CONFLICT, ApiErrorCode.DUPLICATE_RESOURCE);
    }
}
