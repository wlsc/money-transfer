package de.wlsc.model;

import lombok.Builder;
import lombok.Value;

import java.util.Currency;

@Value
@Builder(toBuilder = true)
public class Account {

  private final String id;
  private final long amount;
  private final Currency currency;
  private final Customer customer;
}
