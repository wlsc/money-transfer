package de.wlsc.management;

import static de.wlsc.management.AccountTransferCreator.createJohnsAccount;
import static de.wlsc.management.AccountTransferCreator.createMoneyTransfer;
import static de.wlsc.management.AccountTransferCreator.createSilversAccount;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.params.provider.Arguments.of;

import de.wlsc.model.Account;
import de.wlsc.model.MoneyTransfer;
import java.util.Collection;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@DisplayName("Account management")
class AccountManagementTest {

  private AccountManagement accountManagement;

  private static Iterable<Arguments> transferMoneySuccessfulScenarios() {
    return asList(
        of(createJohnsAccount(), createSilversAccount(), 25, asList(createJohnsAccount(), createSilversAccount())),
        of(createSilversAccount(), createJohnsAccount(), 99, asList(createJohnsAccount(), createSilversAccount())),
        of(createJohnsAccount(), createSilversAccount(), 100, asList(createJohnsAccount(), createSilversAccount()))
    );
  }

  private static Iterable<Arguments> transferMoneyFailedScenarios() {
    return asList(
        of(createJohnsAccount(), createSilversAccount(), 500000, asList(createJohnsAccount(), createSilversAccount()), IllegalArgumentException.class),
        of(createSilversAccount(), createJohnsAccount(), -1245, asList(createJohnsAccount(), createSilversAccount()), IllegalArgumentException.class),
        of(createJohnsAccount(), createSilversAccount(), 1, asList(createJohnsAccount()), IllegalArgumentException.class),
        of(createSilversAccount(), createJohnsAccount(), 1, asList(createSilversAccount()), IllegalArgumentException.class)
    );
  }

  @BeforeEach
  void setUp() {
    accountManagement = new AccountManagement();
  }

  @Test
  @DisplayName("List accounts when none is present, empty list returned")
  void listAccounts_when_noneIsPresent_then_emptyListReturned() {
    assertThat(accountManagement.listAccounts()).isEmpty();
  }

  @Test
  @DisplayName("Create and list accounts when 1 account is present, exactly that one is returned")
  void listAccounts_when_someIsPresent_then_theseAccountsReturned() {
    Account expectedAccount = Account.builder().id("1").amount(10).build();
    accountManagement.create(expectedAccount);

    Collection<Account> actualAccounts = accountManagement.listAccounts();

    assertThat(actualAccounts).hasSize(1);
    for (Account actualAccount : actualAccounts) {
      assertThat(actualAccount).isEqualTo(expectedAccount);
    }
  }

  @Test
  @DisplayName("Remove accounts leave none accounts present in system")
  void removeAccounts_when_someIsPresent_emptyListReturned() {
    accountManagement.removeAccounts();
    assertThat(accountManagement.listAccounts()).isEmpty();

    Account expectedAccount = Account.builder().id("1").amount(10).build();
    accountManagement.create(expectedAccount);
    assertThat(accountManagement.listAccounts()).isNotEmpty();
    accountManagement.removeAccounts();
    assertThat(accountManagement.listAccounts()).isEmpty();
  }

  @ParameterizedTest
  @MethodSource("transferMoneySuccessfulScenarios")
  @DisplayName("Transfer money is successful")
  void transferMoney_successful(final Account sourceAccount,
                                final Account destinationAccount,
                                final long expectedChangeOfAmount,
                                final List<Account> registeredAccounts) {

    for (Account registeredAccount : registeredAccounts) {
      accountManagement.create(registeredAccount);
    }

    MoneyTransfer moneyTransfer = createMoneyTransfer(sourceAccount, destinationAccount, expectedChangeOfAmount);

    accountManagement.transferMoney(moneyTransfer);

    for (Account changedAccount : accountManagement.listAccounts()) {
      if (sourceAccount.getId().equals(changedAccount.getId())) {
        assertThat(changedAccount.getAmount()).isEqualTo(sourceAccount.getAmount() - expectedChangeOfAmount);
      }
      if (destinationAccount.getId().equals(changedAccount.getId())) {
        assertThat(changedAccount.getAmount()).isEqualTo(destinationAccount.getAmount() + expectedChangeOfAmount);
      }
    }
  }

  @ParameterizedTest
  @MethodSource("transferMoneyFailedScenarios")
  @DisplayName("Transfer money has failed")
  void transferMoney_failed(final Account sourceAccount,
                            final Account destinationAccount,
                            final long expectedChangeOfAmount,
                            final List<Account> registeredAccounts,
                            final Class<?> expectedException) {

    for (Account registeredAccount : registeredAccounts) {
      accountManagement.create(registeredAccount);
    }

    MoneyTransfer moneyTransfer = createMoneyTransfer(sourceAccount, destinationAccount, expectedChangeOfAmount);

    assertThatThrownBy(() -> accountManagement.transferMoney(moneyTransfer))
        .isInstanceOf(expectedException);

    for (Account changedAccount : accountManagement.listAccounts()) {
      if (sourceAccount.getId().equals(changedAccount.getId())) {
        assertThat(changedAccount.getAmount()).isEqualTo(sourceAccount.getAmount());
      }
      if (destinationAccount.getId().equals(changedAccount.getId())) {
        assertThat(changedAccount.getAmount()).isEqualTo(destinationAccount.getAmount());
      }
    }
  }
}