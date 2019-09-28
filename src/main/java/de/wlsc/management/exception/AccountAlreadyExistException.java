package de.wlsc.management.exception;

public class AccountAlreadyExistException extends IllegalArgumentException {

  public AccountAlreadyExistException(final String message) {
    super(message);
  }
}
