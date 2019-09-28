package de.wlsc.management.exception;

public class NegativeAmountTransferException extends IllegalArgumentException {

  public NegativeAmountTransferException(final String message) {
    super(message);
  }
}
