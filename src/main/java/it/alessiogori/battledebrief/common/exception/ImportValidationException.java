package it.alessiogori.battledebrief.common.exception;

import org.springframework.http.HttpStatus;

public class ImportValidationException extends ApiException {

    public ImportValidationException(String message) {
        super(
                message,
                HttpStatus.BAD_REQUEST,
                ApiErrorCode.IMPORT_VALIDATION_ERROR
        );
    }
}
