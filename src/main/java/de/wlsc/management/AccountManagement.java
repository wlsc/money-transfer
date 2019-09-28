package de.wlsc.management;

import de.wlsc.management.exception.AccountAlreadyExistException;
import de.wlsc.management.exception.AccountNotFoundException;
import de.wlsc.management.exception.NegativeAmountTransferException;
import de.wlsc.management.exception.NotEnoughMoneyException;
import de.wlsc.model.Account;
import de.wlsc.model.MoneyTransfer;
import java.util.Collection;
import java.util.Currency;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import javax.inject.Singleton;

@Singleton
public class AccountManagement {

  private final Map<String, Account> IN_MEMORY_STORE_ACCOUNT_TO_ID = new ConcurrentHashMap<>();

  public Collection<Account> listAccounts() {
    return IN_MEMORY_STORE_ACCOUNT_TO_ID.values();
  }

  public void create(final Account account) {
    Account previousAccount = IN_MEMORY_STORE_ACCOUNT_TO_ID.putIfAbsent(account.getId(), account);
    if (previousAccount != null) {
      throw new AccountAlreadyExistException("Account does already exist");
    }
  }

  public void removeAccounts() {
    IN_MEMORY_STORE_ACCOUNT_TO_ID.clear();
  }

  public void transferMoney(final MoneyTransfer moneyTransfer) {

    if (moneyTransfer.getAmount() < 0) {
      throw new NegativeAmountTransferException("Negative amount of money is not accepted");
    }

    String sourceAccountId = moneyTransfer.getFromAccountId();
    String destinationAccountId = moneyTransfer.getToAccountId();

    if (isNotFound(sourceAccountId)) {
      throw new AccountNotFoundException("Source account not found");
    } else if (isNotFound(destinationAccountId)) {
      throw new AccountNotFoundException("Destination account not found");
    }

    Account source = IN_MEMORY_STORE_ACCOUNT_TO_ID.get(sourceAccountId);

    if ((source.getAmount() - moneyTransfer.getAmount()) < 0) {
      throw new NotEnoughMoneyException("Source account has not enough money to transfer");
    }

    Currency sourceCurrency = source.getCurrency();
    Account destination = IN_MEMORY_STORE_ACCOUNT_TO_ID.get(destinationAccountId);
    Currency destinationCurrency = destination.getCurrency();
    long transferAmount;

    if (Objects.equals(sourceCurrency, destinationCurrency)) {
      transferAmount = moneyTransfer.getAmount();
    } else {
      transferAmount = convertToDestinationCurrency(sourceCurrency, destinationCurrency, moneyTransfer.getAmount());
    }

    Account sourceAfterWithdraw = source.toBuilder()
        .amount(source.getAmount() - moneyTransfer.getAmount())
        .build();
    Account destinationAfterDeposit = destination.toBuilder()
        .amount(destination.getAmount() + transferAmount)
        .build();

    IN_MEMORY_STORE_ACCOUNT_TO_ID.put(sourceAfterWithdraw.getId(), sourceAfterWithdraw);
    IN_MEMORY_STORE_ACCOUNT_TO_ID.put(destinationAfterDeposit.getId(), destinationAfterDeposit);
  }

  private boolean isNotFound(final String accountId) {
    return !IN_MEMORY_STORE_ACCOUNT_TO_ID.containsKey(accountId);
  }

  private long convertToDestinationCurrency(final Currency sourceCurrency,
                                            final Currency destinationCurrency,
                                            final long transferAmount) {
    // #TODO: here implementation of conversion from source currency into destination's currency
    // for the sake of simplicity remains unimplemented
    return transferAmount;
  }
}
