package it.alessiogori.battledebrief.common.exception;

import org.springframework.http.HttpStatus;

public class ExternalProviderException extends ApiException {

    public ExternalProviderException(String message) {
        super(message, HttpStatus.BAD_GATEWAY, ApiErrorCode.EXTERNAL_PROVIDER_ERROR);
    }

    public ExternalProviderException(String message, Throwable cause) {
        super(
                message,
                cause,
                HttpStatus.BAD_GATEWAY,
                ApiErrorCode.EXTERNAL_PROVIDER_ERROR
        );
    }
}
