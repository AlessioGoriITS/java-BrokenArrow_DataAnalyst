package it.alessiogori.battledebrief.common.exception;

import org.springframework.http.HttpStatus;

public class InvalidCredentialsException extends ApiException {

    public InvalidCredentialsException(String message) {
        super(message, HttpStatus.UNAUTHORIZED, ApiErrorCode.INVALID_CREDENTIALS);
    }
}
