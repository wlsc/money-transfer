package de.wlsc.model;

import java.util.Locale;
import lombok.Builder;
import lombok.Value;

@Value
@Builder(toBuilder = true)
public class Customer {

  private final String id;
  private final String firstname;
  private final String lastname;
  private final Locale locale;
}
