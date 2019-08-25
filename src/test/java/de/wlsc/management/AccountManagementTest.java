package de.wlsc.management;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.wlsc.model.Account;
import de.wlsc.model.Customer;
import de.wlsc.model.MoneyTransfer;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.test.annotation.MicronautTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import javax.inject.Inject;
import java.io.IOException;
import java.util.Currency;
import java.util.List;
import java.util.UUID;

import static de.wlsc.management.AccountManagement.CREATE_ACCOUNT;
import static de.wlsc.management.AccountManagement.LIST_ACCOUNTS;
import static de.wlsc.management.AccountManagement.REMOVE_ACCOUNTS;
import static de.wlsc.management.AccountManagement.TRANSFER_MONEY_FROM_TO_ACCOUNT;
import static io.micronaut.http.HttpRequest.DELETE;
import static io.micronaut.http.HttpRequest.GET;
import static io.micronaut.http.HttpRequest.POST;
import static io.micronaut.http.HttpRequest.PUT;
import static io.micronaut.http.HttpStatus.CREATED;
import static io.micronaut.http.HttpStatus.NOT_MODIFIED;
import static io.micronaut.http.HttpStatus.OK;
import static java.util.Arrays.asList;
import static java.util.Locale.GERMANY;
import static org.assertj.core.api.Assertions.assertThat;

@MicronautTest
class AccountManagementTest {

  private static final TypeReference<List<Account>> LIST_ACCOUNTS_REFERENCE = new TypeReference<>() {
  };

  @Inject
  @Client("/")
  private HttpClient client;

  @Inject
  private ObjectMapper objectMapper;

  private static Iterable<Arguments> transferAccounts() {
    return asList(
            Arguments.of(createJohnsAccount(), createSilversAccount(), 25),
            Arguments.of(createSilversAccount(), createJohnsAccount(), 99),
            Arguments.of(createJohnsAccount(), createSilversAccount(), 100)
    );
  }

  private static Account createJohnsAccount() {
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

  private static Account createSilversAccount() {
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

  @BeforeEach
  void setUp() {
    removeAllAccounts();
  }

  @Test
  @DisplayName("Initial list of all accounts should return no accounts present")
  void invokeListOfAccountsNoAccountsShouldBePresent() throws IOException {
    assertThat(requestAccountsList()).isEmpty();
  }

  @Test
  @DisplayName("Register unique account")
  void registerUniqueAccount() throws Exception {

    Account johnsAccount = createJohnsAccount();
    String accountPayload = objectMapper.writeValueAsString(johnsAccount);

    HttpResponse<String> actualResponse = client.toBlocking()
            .exchange(PUT(CREATE_ACCOUNT, accountPayload));

    assertThat(actualResponse.code()).isEqualTo(CREATED.getCode());
  }

  @Test
  @DisplayName("Register duplicate account")
  void registerDuplicateAccount() throws Exception {

    Account johnsAccount = createJohnsAccount();
    String accountPayload = objectMapper.writeValueAsString(johnsAccount);

    client.toBlocking()
            .exchange(PUT(CREATE_ACCOUNT, accountPayload));
    HttpResponse<String> actualResponse = client.toBlocking()
            .exchange(PUT(CREATE_ACCOUNT, accountPayload));

    assertThat(actualResponse.code()).isEqualTo(NOT_MODIFIED.getCode());
  }

  @Test
  @DisplayName("Register unique accounts and remove them")
  void registerUniqueAccountsAndRemoveThem() throws Exception {

    Account johnsAccount = createJohnsAccount();
    Account silversAccount = createSilversAccount();

    client.toBlocking()
            .exchange(PUT(CREATE_ACCOUNT, objectMapper.writeValueAsString(johnsAccount)));

    assertThat(requestAccountsList()).hasSize(1);

    client.toBlocking()
            .exchange(PUT(CREATE_ACCOUNT, objectMapper.writeValueAsString(silversAccount)));

    assertThat(requestAccountsList()).hasSize(2);

    HttpResponse<?> actualResponse = removeAllAccounts();
    assertThat(actualResponse.code()).isEqualTo(OK.getCode());
    assertThat(requestAccountsList()).isEmpty();
  }

  @ParameterizedTest
  @MethodSource("transferAccounts")
  @DisplayName("Transfer money with no issues")
  void transferMoneyWithNoIssues(final Account sourceAccount,
                                 final Account destinationAccount,
                                 final long expectedChangeOfAmount) throws Exception {

    registerExpectedAccounts(sourceAccount, destinationAccount);

    MoneyTransfer moneyTransfer = createMoneyTransfer(sourceAccount, destinationAccount, expectedChangeOfAmount);
    String payload = objectMapper.writeValueAsString(moneyTransfer);
    HttpResponse<?> actualResponse = client.toBlocking()
            .exchange(POST(TRANSFER_MONEY_FROM_TO_ACCOUNT, payload));

    assertThat(actualResponse.code()).isEqualTo(OK.getCode());
    assertAccountHaveRightAmountOfMoney(sourceAccount, destinationAccount, expectedChangeOfAmount);
  }

  private void assertAccountHaveRightAmountOfMoney(final Account sourceAccount,
                                                   final Account destinationAccount,
                                                   long expectedChangeOfAmount) throws Exception {

    for (Account account : requestAccountsList()) {
      if (sourceAccount.getId().equals(account.getId())) {
        assertThat(account.getAmount()).isEqualTo(sourceAccount.getAmount() - expectedChangeOfAmount);
      }
      if (destinationAccount.getId().equals(account.getId())) {
        assertThat(account.getAmount()).isEqualTo(destinationAccount.getAmount() + expectedChangeOfAmount);
      }
    }
  }

  private void registerExpectedAccounts(Account sourceAccount, Account destinationAccount) throws Exception {
    client.toBlocking()
            .exchange(PUT(CREATE_ACCOUNT, objectMapper.writeValueAsString(sourceAccount)));
    client.toBlocking()
            .exchange(PUT(CREATE_ACCOUNT, objectMapper.writeValueAsString(destinationAccount)));
  }

  private List<Account> requestAccountsList() throws IOException {
    return objectMapper.readValue(client.toBlocking().retrieve(GET(LIST_ACCOUNTS)), LIST_ACCOUNTS_REFERENCE);
  }

  private HttpResponse<?> removeAllAccounts() {
    return client.toBlocking().exchange(DELETE(REMOVE_ACCOUNTS).body(""));
  }

  private MoneyTransfer createMoneyTransfer(final Account fromAccount,
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