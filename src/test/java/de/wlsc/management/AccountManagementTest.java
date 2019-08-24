package de.wlsc.management;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.wlsc.model.Account;
import de.wlsc.model.Customer;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.test.annotation.MicronautTest;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.inject.Inject;
import java.io.IOException;
import java.util.Currency;
import java.util.List;

import static de.wlsc.management.AccountManagement.CREATE_ACCOUNT;
import static de.wlsc.management.AccountManagement.LIST_ACCOUNTS;
import static de.wlsc.management.AccountManagement.REMOVE_ACCOUNTS;
import static io.micronaut.http.HttpRequest.DELETE;
import static io.micronaut.http.HttpRequest.GET;
import static io.micronaut.http.HttpRequest.PUT;
import static io.micronaut.http.HttpStatus.CREATED;
import static io.micronaut.http.HttpStatus.NOT_MODIFIED;
import static io.micronaut.http.HttpStatus.OK;
import static java.util.Locale.GERMANY;
import static org.assertj.core.api.Assertions.assertThat;

@MicronautTest
@Slf4j
class AccountManagementTest {

  private static final TypeReference<List<Account>> LIST_ACCOUNTS_REFERENCE = new TypeReference<>() {
  };

  @Inject
  @Client("/")
  private HttpClient client;

  @Inject
  private ObjectMapper objectMapper;

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

    Customer customer = Customer.builder()
            .id("cust1")
            .firstname("first")
            .lastname("last")
            .locale(GERMANY)
            .build();
    Account account = Account.builder()
            .id("acc1")
            .amount(500)
            .currency(Currency.getInstance(customer.getLocale()))
            .customer(customer)
            .build();
    String accountPayload = objectMapper.writeValueAsString(account);

    HttpResponse<String> actualResponse = client.toBlocking()
            .exchange(PUT(CREATE_ACCOUNT, accountPayload));

    assertThat(actualResponse.code()).isEqualTo(CREATED.getCode());
  }

  @Test
  @DisplayName("Register duplicate account")
  void registerDuplicateAccount() throws Exception {

    Customer customer = Customer.builder()
            .id("cust1")
            .firstname("first")
            .lastname("last")
            .locale(GERMANY)
            .build();
    Account account = Account.builder()
            .id("acc1")
            .amount(500)
            .currency(Currency.getInstance(customer.getLocale()))
            .customer(customer)
            .build();
    String accountPayload = objectMapper.writeValueAsString(account);

    client.toBlocking()
            .exchange(PUT(CREATE_ACCOUNT, accountPayload));
    HttpResponse<String> actualResponse = client.toBlocking()
            .exchange(PUT(CREATE_ACCOUNT, accountPayload));

    assertThat(actualResponse.code()).isEqualTo(NOT_MODIFIED.getCode());
  }

  @Test
  @DisplayName("Register unique accounts and remove them")
  void registerUniqueAccountsAndRemoveThem() throws Exception {

    Customer john = Customer.builder()
            .id("cust1")
            .firstname("first")
            .lastname("last")
            .locale(GERMANY)
            .build();
    Customer silver = Customer.builder()
            .id("cust2")
            .firstname("first")
            .lastname("last")
            .locale(GERMANY)
            .build();
    Account johnAccount = Account.builder()
            .id("acc1")
            .amount(500)
            .currency(Currency.getInstance(john.getLocale()))
            .customer(john)
            .build();
    Account silverAccount = Account.builder()
            .id("acc2")
            .amount(2000)
            .currency(Currency.getInstance(silver.getLocale()))
            .customer(silver)
            .build();

    client.toBlocking()
            .exchange(PUT(CREATE_ACCOUNT, objectMapper.writeValueAsString(johnAccount)));

    assertThat(requestAccountsList()).hasSize(1);

    client.toBlocking()
            .exchange(PUT(CREATE_ACCOUNT, objectMapper.writeValueAsString(silverAccount)));

    assertThat(requestAccountsList()).hasSize(2);

    HttpResponse<?> actualResponse = removeAllAccounts();
    assertThat(actualResponse.code()).isEqualTo(OK.getCode());
    assertThat(requestAccountsList()).isEmpty();
  }

  private List<Account> requestAccountsList() throws IOException {
    return objectMapper.readValue(client.toBlocking().retrieve(GET(LIST_ACCOUNTS)), LIST_ACCOUNTS_REFERENCE);
  }

  private HttpResponse<?> removeAllAccounts() {
    return client.toBlocking().exchange(DELETE(REMOVE_ACCOUNTS).body(""));
  }
}