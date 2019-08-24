package de.wlsc.model;

import lombok.Builder;
import lombok.Value;

import java.util.Locale;

@Value
@Builder
public class Customer {

  private final String id;
  private final String firstname;
  private final String lastname;
  private final Locale locale;
}
