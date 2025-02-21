package ru.practicum.exception;

import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class ErrorHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException ex) {
        List<String> errors = ex.getBindingResult().getFieldErrors()
                .stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.toList());

        ErrorResponse errorResponse = ErrorResponse.builder()
                .status(HttpStatus.BAD_REQUEST)
                .reason("Некорректный запрос")
                .message("Ошибка валидации параметров")
                .errors(errors)
                .timestamp(LocalDateTime.now())
                .build();

        log.warn("Validation error: {}", errors);
        return ResponseEntity.badRequest().body(errorResponse);
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ErrorResponse> handleBadRequestException(BadRequestException ex) {
        ErrorResponse errorResponse = ErrorResponse.builder()
                .status(HttpStatus.BAD_REQUEST)
                .reason("Некорректный запрос")
                .message(ex.getMessage())
                .timestamp(LocalDateTime.now())
                .build();

        log.warn("Bad request error: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<ErrorResponse> handleForbiddenException(ForbiddenException ex) {
        ErrorResponse errorResponse = ErrorResponse.builder()
                .status(HttpStatus.FORBIDDEN)
                .reason("Время на которые намечено событие несоответствует требованиям.")
                .message(ex.getMessage())
                .timestamp(LocalDateTime.now())
                .build();

        log.warn("Forbidden error: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse);
    }

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFoundException(EntityNotFoundException ex) {
        ErrorResponse errorResponse = ErrorResponse.builder()
                .message(ex.getMessage())
                .reason("Объект не найден")
                .status(HttpStatus.NOT_FOUND)
                .timestamp(LocalDateTime.now())
                .build();

        log.warn("Not found: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolationException(ConstraintViolationException ex) {
        ErrorResponse errorResponse = ErrorResponse.builder()
                .status(HttpStatus.CONFLICT)
                .reason("Нарушение целостности данных")
                .message("Дубликат или нарушение ограничения БД")
                .timestamp(LocalDateTime.now())
                .build();

        log.warn("Constraint violation error: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse);
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ErrorResponse> handleConflictException(ConflictException ex) {
        ErrorResponse errorResponse = ErrorResponse.builder()
                .status(HttpStatus.CONFLICT)
                .reason("Нарушение условий")
                .message(ex.getMessage())
                .timestamp(LocalDateTime.now())
                .build();

        log.warn("Conflict error: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrityViolationException(DataIntegrityViolationException ex) {
        ErrorResponse errorResponse = ErrorResponse.builder()
                .status(HttpStatus.CONFLICT)
                .reason("Нарушение уникальности данных")
                .message("Объект с такими данными уже существует")
                .timestamp(LocalDateTime.now())
                .build();

        log.warn("Data integrity violation: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingParamException(MissingServletRequestParameterException ex) {
        ErrorResponse errorResponse = ErrorResponse.builder()
                .status(HttpStatus.BAD_REQUEST)
                .reason("Отсутствует обязательный параметр запроса")
                .message(ex.getMessage())
                .timestamp(LocalDateTime.now())
                .build();

        log.warn("Missing parameter: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    @ExceptionHandler(Throwable.class)
    public ResponseEntity<ErrorResponse> handleThrowable(Throwable ex) {
        ErrorResponse errorResponse = ErrorResponse.builder()
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .reason("Внутренняя ошибка сервера")
                .message(ex.getMessage())
                .timestamp(LocalDateTime.now())
                .build();

        log.error("Internal server error:", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }
}