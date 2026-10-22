package com.procuraflow.fournisseurs.exception;

import com.procuraflow.fournisseurs.dto.ApiErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(FournisseurNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleFournisseurNotFound(
            FournisseurNotFoundException ex,
            HttpServletRequest request
    ) {
        return buildResponse(
                HttpStatus.NOT_FOUND,
                "NOT_FOUND",
                ex.getMessage(),
                request.getRequestURI()
        );
    }

    @ExceptionHandler(DuplicateEmailException.class)
    public ResponseEntity<ApiErrorResponse> handleDuplicateEmail(
            DuplicateEmailException ex,
            HttpServletRequest request
    ) {
        return buildResponse(
                HttpStatus.CONFLICT,
                "CONFLICT",
                ex.getMessage(),
                request.getRequestURI()
        );
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleDataIntegrityViolation(
            DataIntegrityViolationException ex,
            HttpServletRequest request
    ) {
        String rootMessage = ex.getMostSpecificCause() != null
                ? ex.getMostSpecificCause().getMessage()
                : "";
        String lower = rootMessage != null ? rootMessage.toLowerCase(Locale.ROOT) : "";

        if (lower.contains("email") || lower.contains("unique") || lower.contains("fournisseurs_email_key")) {
            return buildResponse(
                    HttpStatus.CONFLICT,
                    "CONFLICT",
                    "Un fournisseur avec cette adresse e-mail existe déjà.",
                    request.getRequestURI()
            );
        }

        if (lower.contains("check") || lower.contains("nom")) {
            return buildResponse(
                    HttpStatus.BAD_REQUEST,
                    "BAD_REQUEST",
                    "Les données du fournisseur ne respectent pas les contraintes de validation.",
                    request.getRequestURI()
            );
        }

        log.warn("Database constraint violation on path {}: {}", request.getRequestURI(), ex.getClass().getSimpleName());
        return buildResponse(
                HttpStatus.CONFLICT,
                "CONFLICT",
                "Opération refusée en raison d'une contrainte d'intégrité des données.",
                request.getRequestURI()
        );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidationErrors(
            MethodArgumentNotValidException ex,
            HttpServletRequest request
    ) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            fieldErrors.putIfAbsent(fieldError.getField(), fieldError.getDefaultMessage());
        }

        String summaryMessage = fieldErrors.isEmpty()
                ? "Requête invalide : erreur de validation des champs."
                : fieldErrors.values().iterator().next();

        ApiErrorResponse body = ApiErrorResponse.ofValidation(
                HttpStatus.BAD_REQUEST.value(),
                "BAD_REQUEST",
                summaryMessage,
                request.getRequestURI(),
                fieldErrors
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleConstraintViolation(
            ConstraintViolationException ex,
            HttpServletRequest request
    ) {
        Map<String, String> violations = new LinkedHashMap<>();
        for (ConstraintViolation<?> violation : ex.getConstraintViolations()) {
            violations.put(violation.getPropertyPath().toString(), violation.getMessage());
        }

        ApiErrorResponse body = ApiErrorResponse.ofValidation(
                HttpStatus.BAD_REQUEST.value(),
                "BAD_REQUEST",
                "Paramètre de requête invalide.",
                request.getRequestURI(),
                violations
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiErrorResponse> handleTypeMismatch(
            MethodArgumentTypeMismatchException ex,
            HttpServletRequest request
    ) {
        String paramName = ex.getName();
        String message = "Format invalide pour le paramètre '" + paramName + "'.";
        return buildResponse(
                HttpStatus.BAD_REQUEST,
                "BAD_REQUEST",
                message,
                request.getRequestURI()
        );
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiErrorResponse> handleUnreadableBody(
            HttpMessageNotReadableException ex,
            HttpServletRequest request
    ) {
        return buildResponse(
                HttpStatus.BAD_REQUEST,
                "BAD_REQUEST",
                "Corps de requête JSON manquant ou mal formé.",
                request.getRequestURI()
        );
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiErrorResponse> handleMethodNotSupported(
            HttpRequestMethodNotSupportedException ex,
            HttpServletRequest request
    ) {
        return buildResponse(
                HttpStatus.METHOD_NOT_ALLOWED,
                "METHOD_NOT_ALLOWED",
                "Méthode HTTP non supportée pour cette ressource.",
                request.getRequestURI()
        );
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleNoResourceFound(
            NoResourceFoundException ex,
            HttpServletRequest request
    ) {
        return buildResponse(
                HttpStatus.NOT_FOUND,
                "NOT_FOUND",
                "Ressource introuvable.",
                request.getRequestURI()
        );
    }

    @ExceptionHandler(DownstreamServiceUnavailableException.class)
    public ResponseEntity<ApiErrorResponse> handleDownstreamUnavailable(
            DownstreamServiceUnavailableException ex,
            HttpServletRequest request
    ) {
        log.warn("Downstream service unavailable on path {}: {}", request.getRequestURI(), ex.getMessage());
        return buildResponse(
                HttpStatus.SERVICE_UNAVAILABLE,
                "SERVICE_UNAVAILABLE",
                ex.getMessage(),
                request.getRequestURI()
        );
    }

    @ExceptionHandler(DownstreamBadGatewayException.class)
    public ResponseEntity<ApiErrorResponse> handleDownstreamBadGateway(
            DownstreamBadGatewayException ex,
            HttpServletRequest request
    ) {
        log.warn("Downstream bad gateway on path {}: {}", request.getRequestURI(), ex.getMessage());
        return buildResponse(
                HttpStatus.BAD_GATEWAY,
                "BAD_GATEWAY",
                ex.getMessage(),
                request.getRequestURI()
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleUnexpectedException(
            Exception ex,
            HttpServletRequest request
    ) {
        log.error("Unexpected error while processing request {}", request.getRequestURI(), ex);
        return buildResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "INTERNAL_SERVER_ERROR",
                "Une erreur interne inattendue est survenue.",
                request.getRequestURI()
        );
    }

    private ResponseEntity<ApiErrorResponse> buildResponse(
            HttpStatus status,
            String errorCode,
            String message,
            String path
    ) {
        ApiErrorResponse body = ApiErrorResponse.of(
                status.value(),
                errorCode,
                message,
                path
        );
        return ResponseEntity.status(status).body(body);
    }
}
