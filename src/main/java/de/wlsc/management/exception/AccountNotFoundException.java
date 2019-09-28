package de.wlsc.management.exception;

public class AccountNotFoundException extends IllegalArgumentException {

  public AccountNotFoundException(final String message) {
    super(message);
  }
}
