package com.omuiotlab.wildlifemediaservice.exception;

import com.omuiotlab.wildlifemediaservice.dto.ApiErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

import java.time.Instant;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler({MediaNotFoundException.class, CameraNotFoundException.class})
    public ResponseEntity<ApiErrorResponse> handleNotFound(
            RuntimeException exception,
            HttpServletRequest request
    ) {
        return build(HttpStatus.NOT_FOUND, exception.getMessage(), request);
    }

    @ExceptionHandler(DuplicateCameraCodeException.class)
    public ResponseEntity<ApiErrorResponse> handleConflict(
            DuplicateCameraCodeException exception,
            HttpServletRequest request
    ) {
        return build(HttpStatus.CONFLICT, exception.getMessage(), request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidation(
            MethodArgumentNotValidException exception,
            HttpServletRequest request
    ) {
        String message = exception.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> error.getDefaultMessage() == null
                        ? error.getField() + " geçersiz."
                        : error.getDefaultMessage())
                .distinct()
                .collect(Collectors.joining(" "));

        return build(HttpStatus.BAD_REQUEST, message, request);
    }

    @ExceptionHandler({
            MediaStorageException.class,
            ConstraintViolationException.class,
            MissingServletRequestParameterException.class,
            MissingServletRequestPartException.class,
            HttpMessageNotReadableException.class,
            IllegalArgumentException.class
    })
    public ResponseEntity<ApiErrorResponse> handleBadRequest(
            Exception exception,
            HttpServletRequest request
    ) {
        String message = exception instanceof HttpMessageNotReadableException
                ? "İstek gövdesi okunamadı veya enum değeri geçersiz."
                : exception.getMessage();

        return build(HttpStatus.BAD_REQUEST, message, request);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiErrorResponse> handleMaxUploadSize(
            MaxUploadSizeExceededException exception,
            HttpServletRequest request
    ) {
        return build(
                HttpStatus.PAYLOAD_TOO_LARGE,
                "Dosya izin verilen maksimum boyutu aşıyor.",
                request
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleUnexpected(
            Exception exception,
            HttpServletRequest request
    ) {
        return build(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Beklenmeyen bir sunucu hatası oluştu.",
                request
        );
    }

    private ResponseEntity<ApiErrorResponse> build(
            HttpStatus status,
            String message,
            HttpServletRequest request
    ) {
        ApiErrorResponse error = new ApiErrorResponse(
                Instant.now(),
                status.value(),
                status.getReasonPhrase(),
                message,
                request.getRequestURI()
        );

        return ResponseEntity.status(status).body(error);
    }
}
