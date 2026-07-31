package it.alessiogori.battledebrief.common.exception;

import org.springframework.http.HttpStatus;

public class InvalidRequestException extends ApiException {

    public InvalidRequestException(String message) {
        super(message, HttpStatus.BAD_REQUEST, ApiErrorCode.MALFORMED_REQUEST);
    }
}
