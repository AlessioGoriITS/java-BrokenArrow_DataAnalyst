package it.alessiogori.battledebrief.common.exception;

import org.springframework.http.HttpStatus;

public class ResourceNotFoundException extends ApiException {

    public ResourceNotFoundException(String message) {
        super(message, HttpStatus.NOT_FOUND, ApiErrorCode.NOT_FOUND);
    }
}
