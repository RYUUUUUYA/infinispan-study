package demo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.MissingRequestValueException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@RestControllerAdvice
public class ApiExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(UnknownCacheException.class)
    public ResponseEntity<String> unknownCache(UnknownCacheException e) {
        return error(HttpStatus.BAD_REQUEST, "unknown_cache", e.getMessage());
    }

    @ExceptionHandler(InvalidJsonException.class)
    public ResponseEntity<String> invalidJson(InvalidJsonException e) {
        return error(HttpStatus.BAD_REQUEST, "invalid_json", e.getMessage());
    }

    @ExceptionHandler({
        HttpMediaTypeNotSupportedException.class,
        MissingRequestValueException.class,
        MethodArgumentTypeMismatchException.class,
        NoResourceFoundException.class
    })
    public ResponseEntity<String> badRequest(Exception e) {
        return error(HttpStatus.BAD_REQUEST, "bad_request", e.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<String> internalError(Exception e) {
        log.error("event=api_error result=internal_error", e);
        return error(HttpStatus.INTERNAL_SERVER_ERROR, "internal_error", "Internal server error");
    }

    private static ResponseEntity<String> error(HttpStatus status, String code, String message) {
        String escapedMessage = message == null ? "" : message.replace("\\", "\\\\").replace("\"", "\\\"");
        String body = "{\"error\":\"%s\",\"message\":\"%s\"}".formatted(code, escapedMessage);
        return ResponseEntity.status(status)
            .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .body(body);
    }
}
