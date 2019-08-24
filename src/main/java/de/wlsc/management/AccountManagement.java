package de.wlsc.management;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.wlsc.model.Account;
import io.micronaut.core.version.annotation.Version;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Delete;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.Put;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static io.micronaut.http.HttpResponse.created;
import static io.micronaut.http.HttpResponse.notModified;
import static io.micronaut.http.HttpResponse.status;
import static io.micronaut.http.HttpStatus.OK;
import static io.micronaut.http.MediaType.APPLICATION_JSON;

@Controller
@Slf4j
public class AccountManagement {

  static final String LIST_ACCOUNTS = "/accounts";
  static final String CREATE_ACCOUNT = "/account/create";
  static final String REMOVE_ACCOUNTS = "/accounts/remove";

  private Map<String, Account> inMemoryStoreAccountToId = new ConcurrentHashMap<>();

  @Inject
  private ObjectMapper objectMapper;

  @Get(uri = LIST_ACCOUNTS, produces = APPLICATION_JSON)
  @Version("1")
  public String listAccounts() throws JsonProcessingException {
    return objectMapper.writeValueAsString(inMemoryStoreAccountToId.values());
  }

  @Put(uri = CREATE_ACCOUNT, consumes = APPLICATION_JSON)
  @Version("1")
  public HttpResponse<?> create(@Body final Account account) {

    Account previousAccount = inMemoryStoreAccountToId.putIfAbsent(account.getId(), account);

    if (previousAccount != null) {
      return notModified();
    }

    return created("/account/" + account.getId());
  }

  @Delete(REMOVE_ACCOUNTS)
  @Version("1")
  public HttpResponse<?> removeAccounts() {
    inMemoryStoreAccountToId.clear();
    return status(OK);
  }
}
