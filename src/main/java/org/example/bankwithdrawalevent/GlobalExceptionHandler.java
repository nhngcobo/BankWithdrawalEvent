package org.example.bankwithdrawalevent;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class GlobalExceptionHandler extends RuntimeException {
  @ExceptionHandler(NumberFormatException.class)
  public ResponseEntity<String> handleNumberFormatException(NumberFormatException e) {
    return ResponseEntity.badRequest().body("Invalid input: account ID and Amount must be numeric.");
  }

  @ExceptionHandler(AccountNotFoundException.class)
  public ResponseEntity<String> handleAccountNotFound(AccountNotFoundException e) {
    return ResponseEntity.status(404).body(e.getMessage());
  }

  @ExceptionHandler(InsufficientFundsException.class)
  public ResponseEntity<String> handleInsufficientFunds(InsufficientFundsException e) {
    return ResponseEntity.status(400).body(e.getMessage());
  }

  @ExceptionHandler(InvalidRequestException.class)
  public ResponseEntity<String> handleInvalidRequest(InvalidRequestException e) {
    return ResponseEntity.badRequest().body(e.getMessage());
  }

  @ExceptionHandler(InvalidAccountException.class)
  public ResponseEntity<String> handleInvalidAccount(InvalidAccountException ex) {
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex.getMessage());
  }

  //This is for when "amount" is set to a non-numeric value. Because it was defined as a Big Decimal on the DTO it was breaking even before hitting the controller
  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ResponseEntity<String> handleDeserializationError(HttpMessageNotReadableException ex) {
    return ResponseEntity.badRequest().body("Invalid input: account ID and Amount must be numeric.");
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<String> handleGenericException(Exception ex) {
    // For unexpected internal errors
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body("An internal error occurred. Please try again later.");
  }
}
