package de.wlsc.management;

import static de.wlsc.management.AccountManagementEndpoint.ACCOUNTS;
import static de.wlsc.management.AccountManagementEndpoint.TRANSFER_MONEY_FROM_TO_ACCOUNT;
import static de.wlsc.management.AccountTransferCreator.createJohnsAccount;
import static de.wlsc.management.AccountTransferCreator.createMoneyTransfer;
import static de.wlsc.management.AccountTransferCreator.createSilversAccount;
import static io.micronaut.http.HttpRequest.DELETE;
import static io.micronaut.http.HttpRequest.GET;
import static io.micronaut.http.HttpRequest.POST;
import static io.micronaut.http.HttpRequest.PUT;
import static io.micronaut.http.HttpStatus.BAD_REQUEST;
import static io.micronaut.http.HttpStatus.CREATED;
import static io.micronaut.http.HttpStatus.NOT_MODIFIED;
import static io.micronaut.http.HttpStatus.OK;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.junit.jupiter.params.provider.Arguments.of;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.wlsc.model.Account;
import de.wlsc.model.MoneyTransfer;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.http.client.exceptions.HttpClientResponseException;
import io.micronaut.test.annotation.MicronautTest;
import java.io.IOException;
import java.util.List;
import java.util.function.Supplier;
import javax.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@MicronautTest
@DisplayName("Account management endpoint")
class AccountManagementEndpointTest {

  private static final TypeReference<List<Account>> LIST_ACCOUNTS_REFERENCE = new TypeReference<>() {
  };

  @Inject
  @Client("/")
  private HttpClient client;

  @Inject
  private ObjectMapper objectMapper;

  private static Iterable<Arguments> transferMoney() {
    return asList(
        of(createJohnsAccount(), createSilversAccount(), 25, OK, asList(createJohnsAccount(), createSilversAccount())),
        of(createSilversAccount(), createJohnsAccount(), 99, OK, asList(createJohnsAccount(), createSilversAccount())),
        of(createJohnsAccount(), createSilversAccount(), 100, OK, asList(createJohnsAccount(), createSilversAccount())),
        of(createJohnsAccount(), createSilversAccount(), 500000, BAD_REQUEST, asList(createJohnsAccount(), createSilversAccount())),
        of(createSilversAccount(), createJohnsAccount(), -1245, BAD_REQUEST, asList(createJohnsAccount(), createSilversAccount())),
        of(createJohnsAccount(), createSilversAccount(), 1, BAD_REQUEST, asList(createJohnsAccount())),
        of(createSilversAccount(), createJohnsAccount(), 1, BAD_REQUEST, asList(createSilversAccount()))
    );
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
        .exchange(PUT(ACCOUNTS, accountPayload));

    assertThat(actualResponse.code()).isEqualTo(CREATED.getCode());
  }

  @Test
  @DisplayName("Register duplicate account")
  void registerDuplicateAccount() throws Exception {

    Account johnsAccount = createJohnsAccount();
    String accountPayload = objectMapper.writeValueAsString(johnsAccount);

    client.toBlocking()
        .exchange(PUT(ACCOUNTS, accountPayload));
    HttpResponse<String> actualResponse = client.toBlocking()
        .exchange(PUT(ACCOUNTS, accountPayload));

    assertThat(actualResponse.code()).isEqualTo(NOT_MODIFIED.getCode());
  }

  @Test
  @DisplayName("Register unique accounts and remove them")
  void registerUniqueAccountsAndRemoveThem() throws Exception {

    Account johnsAccount = createJohnsAccount();
    Account silversAccount = createSilversAccount();

    client.toBlocking()
        .exchange(PUT(ACCOUNTS, objectMapper.writeValueAsString(johnsAccount)));

    assertThat(requestAccountsList()).hasSize(1);

    client.toBlocking()
        .exchange(PUT(ACCOUNTS, objectMapper.writeValueAsString(silversAccount)));

    assertThat(requestAccountsList()).hasSize(2);

    HttpResponse<?> actualResponse = removeAllAccounts();
    assertThat(actualResponse.code()).isEqualTo(OK.getCode());
    assertThat(requestAccountsList()).isEmpty();
  }

  @ParameterizedTest
  @MethodSource("transferMoney")
  @DisplayName("Transfer money")
  void transferMoney(final Account sourceAccount,
                     final Account destinationAccount,
                     final long expectedChangeOfAmount,
                     final HttpStatus expectedHttpStatus,
                     final List<Account> registeredAccounts) throws Exception {

    registerExpectedAccounts(registeredAccounts);

    MoneyTransfer moneyTransfer = createMoneyTransfer(sourceAccount, destinationAccount, expectedChangeOfAmount);
    String payload = objectMapper.writeValueAsString(moneyTransfer);

    HttpResponse<?> actualResponse = null;
    try {
      actualResponse = client.toBlocking()
          .exchange(POST(TRANSFER_MONEY_FROM_TO_ACCOUNT, payload));

      assertAccountHaveRightAmountOfMoney(sourceAccount, destinationAccount,
          () -> sourceAccount.getAmount() - expectedChangeOfAmount,
          () -> destinationAccount.getAmount() + expectedChangeOfAmount);

    } catch (HttpClientResponseException responseException) {

      actualResponse = responseException.getResponse();
      assertAccountHaveRightAmountOfMoney(sourceAccount, destinationAccount,
          sourceAccount::getAmount,
          destinationAccount::getAmount);

    } catch (Exception e) {
      fail("This line shouldn't be executed");
    }

    assertThat(actualResponse).isNotNull();
    assertThat(actualResponse.code()).isEqualTo(expectedHttpStatus.getCode());
  }

  private void assertAccountHaveRightAmountOfMoney(
      final Account sourceAccount,
      final Account destinationAccount,
      final Supplier<Long> expectedSourceChangeOfMoney,
      final Supplier<Long> expectedDestinationChangeOfMoney) throws Exception {

    for (Account account : requestAccountsList()) {
      if (sourceAccount.getId().equals(account.getId())) {
        assertThat(account.getAmount()).isEqualTo(expectedSourceChangeOfMoney.get());
      }
      if (destinationAccount.getId().equals(account.getId())) {
        assertThat(account.getAmount()).isEqualTo(expectedDestinationChangeOfMoney.get());
      }
    }
  }

  private void registerExpectedAccounts(final List<Account> accounts) throws Exception {
    for (Account account : accounts) {
      client.toBlocking().exchange(PUT(ACCOUNTS, objectMapper.writeValueAsString(account)));
    }
  }

  private List<Account> requestAccountsList() throws IOException {
    return objectMapper.readValue(client.toBlocking()
        .retrieve(GET(ACCOUNTS)), LIST_ACCOUNTS_REFERENCE);
  }

  private HttpResponse<?> removeAllAccounts() {
    return client.toBlocking().exchange(DELETE(ACCOUNTS).body(""));
  }
}