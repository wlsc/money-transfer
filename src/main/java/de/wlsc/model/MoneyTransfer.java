package de.wlsc.model;

import lombok.Builder;
import lombok.Value;

@Value
@Builder(toBuilder = true)
public class MoneyTransfer {

  private final String id;
  private final String fromAccountId;
  private final String toAccountId;
  private final long amount;
}
