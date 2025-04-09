package org.example.bankwithdrawalevent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class GlobalExceptionHandler extends RuntimeException {

  private final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

  // This is for handling an invalid amount or accountId
  @ExceptionHandler(NumberFormatException.class)
  public ResponseEntity<String> handleNumberFormatException(NumberFormatException e) {
    logger.error("Invalid input: account ID and Amount must be numeric. {}", e.getMessage());
    return ResponseEntity.status(400).body("Invalid input: account ID and Amount must be numeric.");
  }

  // This is for when there was an unexpected response with fetching the account balance
  @ExceptionHandler(ProcessingRequestFailed.class)
  public ResponseEntity<String> handleProcessingRequest(ProcessingRequestFailed e) {
    logger.error("Something went wrong with processing your request: {}", e.getMessage());
    return ResponseEntity.status(400).body(e.getMessage());
  }

  // This is for when there were Zero rows returned from the DB for the account
  @ExceptionHandler(AccountNotFoundException.class)
  public ResponseEntity<String> handleAccountNotFound(AccountNotFoundException e) {
    logger.error("Account not found: {}", e.getMessage());
    return ResponseEntity.status(404).body(e.getMessage());
  }

  // This is for when the amount requested for withdrawal > balance
  @ExceptionHandler(InsufficientFundsException.class)
  public ResponseEntity<String> handleInsufficientFunds(InsufficientFundsException e) {
    logger.error("Insufficient funds for withdrawal. {}", e.getMessage());
    return ResponseEntity.status(400).body(e.getMessage());
  }

  // This is for validating the request  JSON body
  @ExceptionHandler(InvalidRequestException.class)
  public ResponseEntity<String> handleInvalidRequest(InvalidRequestException e) {
    logger.error("Invalid account ID or amount. {}", e.getMessage());
    return ResponseEntity.status(400).body(e.getMessage());
  }


  //This is for when "amount" is set to a non-numeric value. Because it was defined as a Big Decimal on the DTO it was breaking even before hitting the controller
  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ResponseEntity<String> handleDeserializationError(HttpMessageNotReadableException ex) {
    return ResponseEntity.status(400).body("Invalid input: account ID and Amount must be numeric.");
  }

  // General Exception
  @ExceptionHandler(Exception.class)
  public ResponseEntity<String> handleGenericException(Exception ex) {
    logger.error("An internal error occurred. Please try again later.: {}", ex.getMessage());
    // For unexpected internal errors
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body("An internal error occurred. Please try again later.");
  }
}
