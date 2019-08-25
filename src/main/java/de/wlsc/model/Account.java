package de.wlsc.model;

import java.util.Currency;
import lombok.Builder;
import lombok.Value;

@Value
@Builder(toBuilder = true)
public class Account {

  private final String id;
  private final long amount;
  private final Currency currency;
  private final Customer customer;
}
