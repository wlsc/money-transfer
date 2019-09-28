package de.wlsc.management.exception;

public class NotEnoughMoneyException extends IllegalArgumentException {

  public NotEnoughMoneyException(final String message) {
    super(message);
  }
}
