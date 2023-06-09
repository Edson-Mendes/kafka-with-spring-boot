package com.emendes.controller;

import com.emendes.exception.InvalidLibraryEventId;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@ControllerAdvice
public class LibraryEventControllerAdvice {

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<String> handleRequestBody(MethodArgumentNotValidException ex) {
    List<FieldError> fieldErrors = ex.getBindingResult().getFieldErrors();
    String errorMessage = fieldErrors
        .stream().map(fe -> fe.getField() + " - " + fe.getDefaultMessage())
        .sorted()
        .collect(Collectors.joining(", "));

    log.info("errorMessage : {}", errorMessage);

    return ResponseEntity.badRequest().body(errorMessage);
  }

  @ExceptionHandler(InvalidLibraryEventId.class)
  public ResponseEntity<String> handleInvalidLibraryEventId(InvalidLibraryEventId ex) {
    String errorMessage = ex.getMessage();

    log.info("errorMessage : {}", errorMessage);

    return ResponseEntity.badRequest().body(errorMessage);
  }

}
