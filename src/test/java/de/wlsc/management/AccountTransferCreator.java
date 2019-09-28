package de.wlsc.management;

import static java.util.Locale.GERMANY;

import de.wlsc.model.Account;
import de.wlsc.model.Customer;
import de.wlsc.model.MoneyTransfer;
import java.util.Currency;
import java.util.UUID;

final class AccountTransferCreator {

  private AccountTransferCreator() {
    // utility
  }

  static Account createJohnsAccount() {
    Customer john = Customer.builder()
        .id("cust1")
        .firstname("first")
        .lastname("last")
        .locale(GERMANY)
        .build();
    return Account.builder()
        .id("acc1")
        .amount(500)
        .currency(Currency.getInstance(john.getLocale()))
        .customer(john)
        .build();
  }

  static Account createSilversAccount() {
    Customer silver = Customer.builder()
        .id("cust2")
        .firstname("first")
        .lastname("last")
        .locale(GERMANY)
        .build();
    return Account.builder()
        .id("acc2")
        .amount(2000)
        .currency(Currency.getInstance(silver.getLocale()))
        .customer(silver)
        .build();
  }

  static MoneyTransfer createMoneyTransfer(final Account fromAccount,
                                           final Account toAccount,
                                           final long amount) {
    return MoneyTransfer.builder()
        .id(UUID.randomUUID().toString())
        .fromAccountId(fromAccount.getId())
        .toAccountId(toAccount.getId())
        .amount(amount)
        .build();
  }
}
