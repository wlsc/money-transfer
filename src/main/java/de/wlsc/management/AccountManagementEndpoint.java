package de.wlsc.management;

import static io.micronaut.http.HttpResponse.badRequest;
import static io.micronaut.http.HttpResponse.created;
import static io.micronaut.http.HttpResponse.notModified;
import static io.micronaut.http.HttpResponse.status;
import static io.micronaut.http.HttpStatus.OK;
import static io.micronaut.http.MediaType.APPLICATION_JSON;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.wlsc.management.exception.AccountAlreadyExistException;
import de.wlsc.management.exception.AccountNotFoundException;
import de.wlsc.management.exception.NegativeAmountTransferException;
import de.wlsc.management.exception.NotEnoughMoneyException;
import de.wlsc.model.Account;
import de.wlsc.model.MoneyTransfer;
import io.micronaut.core.version.annotation.Version;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Delete;
import io.micronaut.http.annotation.Error;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.Post;
import io.micronaut.http.annotation.Put;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;

@Controller
@Slf4j
public class AccountManagementEndpoint {

  static final String ACCOUNTS = "/accounts";
  static final String TRANSFER_MONEY_FROM_TO_ACCOUNT = "/accounts/transfer";

  private final AccountManagement accountManagement;
  private final ObjectMapper objectMapper;

  @Inject
  public AccountManagementEndpoint(final AccountManagement accountManagement, final ObjectMapper objectMapper) {
    this.accountManagement = accountManagement;
    this.objectMapper = objectMapper;
  }

  @Get(uri = ACCOUNTS, produces = APPLICATION_JSON)
  @Version("1")
  public String listAccounts() throws JsonProcessingException {
    return objectMapper.writeValueAsString(accountManagement.listAccounts());
  }

  @Put(uri = ACCOUNTS, consumes = APPLICATION_JSON)
  @Version("1")
  public HttpResponse<?> create(@Body final Account account) {
    log.info("Requested account creation");
    accountManagement.create(account);
    log.info("Account {} created", account.getId());
    return created("/account/" + account.getId());
  }

  @Delete(ACCOUNTS)
  @Version("1")
  public HttpResponse<?> removeAccounts() {
    log.info("Requested removal of all accounts");
    accountManagement.removeAccounts();
    log.info("All accounts were removed");
    return status(OK);
  }

  @Post(uri = TRANSFER_MONEY_FROM_TO_ACCOUNT, consumes = APPLICATION_JSON)
  @Version("1")
  public HttpResponse<?> transferMoney(@Body final MoneyTransfer moneyTransfer) {
    log.info("Initializing money transfer from {} to {} account...", moneyTransfer.getFromAccountId(), moneyTransfer.getToAccountId());
    accountManagement.transferMoney(moneyTransfer);
    log.info("Money transfer from {} to {} account was successful", moneyTransfer.getFromAccountId(), moneyTransfer.getToAccountId());
    return HttpResponse.ok();
  }

  @Error(AccountAlreadyExistException.class)
  public HttpResponse<?> onAccountNotFound(final AccountAlreadyExistException e) {
    log.info(e.getMessage());
    log.debug(e.getMessage(), e);
    return notModified();
  }

  @Error(NegativeAmountTransferException.class)
  public HttpResponse<?> onNegativeAmountTransfer(final NegativeAmountTransferException e) {
    return respondWithBadRequest(e);
  }

  @Error(AccountNotFoundException.class)
  public HttpResponse<?> onAccountNotFound(final AccountNotFoundException e) {
    return respondWithBadRequest(e);
  }

  @Error(NotEnoughMoneyException.class)
  public HttpResponse<?> onNotEnoughMoney(final NotEnoughMoneyException e) {
    return respondWithBadRequest(e);
  }

  private HttpResponse<?> respondWithBadRequest(final Exception e) {
    log.info(e.getMessage());
    log.debug(e.getMessage(), e);
    return badRequest(e.getMessage());
  }
}
