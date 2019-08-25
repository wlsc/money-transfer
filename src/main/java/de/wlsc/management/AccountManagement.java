package de.wlsc.management;

import static io.micronaut.http.HttpResponse.badRequest;
import static io.micronaut.http.HttpResponse.created;
import static io.micronaut.http.HttpResponse.notModified;
import static io.micronaut.http.HttpResponse.status;
import static io.micronaut.http.HttpStatus.OK;
import static io.micronaut.http.MediaType.APPLICATION_JSON;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.wlsc.model.Account;
import de.wlsc.model.MoneyTransfer;
import io.micronaut.core.version.annotation.Version;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Delete;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.Post;
import io.micronaut.http.annotation.Put;
import java.util.Currency;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;

@Controller
@Slf4j
@SuppressWarnings("PMD.BeanMembersShouldSerialize")
public class AccountManagement {

  static final String LIST_ACCOUNTS = "/accounts";
  static final String CREATE_ACCOUNT = "/account/create";
  static final String REMOVE_ACCOUNTS = "/accounts/remove";
  static final String TRANSFER_MONEY_FROM_TO_ACCOUNT = "/accounts/transfer";

  private static final Map<String, Account> IN_MEMORY_STORE_ACCOUNT_TO_ID = new ConcurrentHashMap<>();

  private final ObjectMapper objectMapper;

  @Inject
  public AccountManagement(final ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  @Get(uri = LIST_ACCOUNTS, produces = APPLICATION_JSON)
  @Version("1")
  public String listAccounts() throws JsonProcessingException {
    return objectMapper.writeValueAsString(IN_MEMORY_STORE_ACCOUNT_TO_ID.values());
  }

  @Put(uri = CREATE_ACCOUNT, consumes = APPLICATION_JSON)
  @Version("1")
  public HttpResponse<?> create(@Body final Account account) {

    Account previousAccount = IN_MEMORY_STORE_ACCOUNT_TO_ID.putIfAbsent(account.getId(), account);

    if (previousAccount != null) {
      return notModified();
    }

    return created("/account/" + account.getId());
  }

  @Delete(REMOVE_ACCOUNTS)
  @Version("1")
  public HttpResponse<?> removeAccounts() {
    IN_MEMORY_STORE_ACCOUNT_TO_ID.clear();
    return status(OK);
  }

  @Post(uri = TRANSFER_MONEY_FROM_TO_ACCOUNT, consumes = APPLICATION_JSON)
  @Version("1")
  synchronized public HttpResponse<?> transferMoneyFromToAccount(@Body final MoneyTransfer moneyTransfer) {

    if (moneyTransfer.getAmount() < 0) {
      return badRequest("we are not accepting negative money amounts");
    }

    String sourceAccountId = moneyTransfer.getFromAccountId();

    if (!IN_MEMORY_STORE_ACCOUNT_TO_ID.containsKey(sourceAccountId)) {
      return badRequest("no such account " + sourceAccountId + " found");
    }

    String destinationAccountId = moneyTransfer.getToAccountId();

    if (!IN_MEMORY_STORE_ACCOUNT_TO_ID.containsKey(destinationAccountId)) {
      return badRequest("no such account " + destinationAccountId + " found");
    }

    Account source = IN_MEMORY_STORE_ACCOUNT_TO_ID.get(sourceAccountId);

    if ((source.getAmount() - moneyTransfer.getAmount()) < 0) {
      return badRequest("Source account has not enough money to transfer");
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

    return HttpResponse.ok();
  }

  private long convertToDestinationCurrency(final Currency sourceCurrency,
                                            final Currency destinationCurrency,
                                            final long transferAmount) {
    // #TODO: here implementation of conversion from source currency into destination's currency 
    // for the sake of simplicity remains unimplemented
    return transferAmount;
  }
}
