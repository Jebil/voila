package net.jk.app.commons.cucumber;

import static net.jk.app.commons.cucumber.CucumberEnvironment.getApp;
import static net.jk.app.commons.cucumber.CucumberEnvironment.getAppForPath;
import static net.jk.app.commons.cucumber.HttpTestUtils.delete;
import static net.jk.app.commons.cucumber.HttpTestUtils.get;
import static net.jk.app.commons.cucumber.HttpTestUtils.options;
import static net.jk.app.commons.cucumber.HttpTestUtils.patch;
import static net.jk.app.commons.cucumber.HttpTestUtils.post;
import static net.jk.app.commons.cucumber.HttpTestUtils.put;
import static net.jk.app.commons.cucumber.JsonTestUtils.mergeJson;
import static org.junit.Assert.assertEquals;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.Option;
import com.jayway.jsonpath.PathNotFoundException;
import com.jayway.jsonpath.spi.json.JacksonJsonProvider;
import com.jayway.jsonpath.spi.json.JsonProvider;
import com.jayway.jsonpath.spi.mapper.JacksonMappingProvider;
import com.jayway.jsonpath.spi.mapper.MappingException;
import com.jayway.jsonpath.spi.mapper.MappingProvider;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.cucumber.datatable.DataTable;
import io.cucumber.java.Before;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import javax.mail.MessagingException;
import javax.ws.rs.HttpMethod;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriBuilder;
import junit.framework.AssertionFailedError;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.hamcrest.CoreMatchers;
import org.json.JSONException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.junit.Assert;

/** All Cucumber steps related to HTTP / REST */
@Slf4j
@SuppressFBWarnings("EXS")
public class HttpStepDefinitions {

  public static final String HEADER_AUTHORIZATION = "Authorization";
  public static final String METHOD_OVERRIDE = "X-HTTP-Method-Override";
  public static final String LOGIN_TOKEN = "SYSTEM_ADMIN_TOKEN";

  // extra multiple ${VARIABLE_NAME} from any String
  private static final Pattern CONTEXT_VARIABLE_PATTERN =
      Pattern.compile("\\$\\{([A-Z|0-9|_]+)\\}");

  // variables that are context-based when running in parallel, so need ThreadLocal
  private ThreadLocal<HttpResponse> lastResponse = new ThreadLocal<>();
  private ThreadLocal<Map<String, String>> onetimeHttpHeaders = new ThreadLocal<>();

  // globally shared across all threads
  private ConcurrentMap<String, Object> context = new ConcurrentHashMap<>();
  private ConcurrentMap<String, String> permanentHttpHeaders = new ConcurrentHashMap<>();
  private ConcurrentMap<String, String> permanentQueueHeaders = new ConcurrentHashMap<>();

  private ObjectMapper json = new ObjectMapper();

  public HttpStepDefinitions() {
    json.registerModule(new JavaTimeModule());
    json.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
    LocalDateTimeSerializer serializer =
        new LocalDateTimeSerializer(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
    LocalDateTimeDeserializer deserializer =
        new LocalDateTimeDeserializer(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
    SimpleModule module = new SimpleModule();
    module.addSerializer(LocalDateTime.class, serializer);
    module.addDeserializer(LocalDateTime.class, deserializer);
    json.registerModule(module);

    Configuration.setDefaults(
        new Configuration.Defaults() {
          private final JsonProvider jsonProvider = new JacksonJsonProvider(json);
          private final MappingProvider mappingProvider = new JacksonMappingProvider(json);

          @Override
          public JsonProvider jsonProvider() {
            return jsonProvider;
          }

          @Override
          public MappingProvider mappingProvider() {
            return mappingProvider;
          }

          @Override
          public Set<Option> options() {
            return EnumSet.noneOf(Option.class);
          }
        });
  }

  @Before
  public void beforeScenario() {
    // clear permanent HTTP headers before every Scenario
    permanentHttpHeaders.clear();
    context.clear();

    lastResponse = new ThreadLocal<>();
    onetimeHttpHeaders = ThreadLocal.withInitial(() -> new HashMap<>());

    // fill the context with variables about the environment
    CucumberEnvironment.getApplications()
        .forEach(
            (app, info) -> {
              String APP_PREFIX = app.toUpperCase();
              Function<String, String> toKey = (suffix) -> APP_PREFIX + "_" + suffix;
              context.put(toKey.apply("HTTP_PORT"), info.getHttpPort());
              context.put(toKey.apply("HTTP_ADMIN_PORT"), info.getHttpAdminPort());
              context.put(toKey.apply("HTTP_HOST"), info.getHost());
              context.put(toKey.apply("HTTP_ADMIN_HOST"), info.getHost());
              context.put(toKey.apply("HTTP_URL"), info.getHttpUrl());
              context.put(toKey.apply("HTTP_ADMIN_URL"), info.getHttpAdminUrl());
            });
  }

  /**
   * ******************************************************** GIVEN
   * *******************************************************
   */
  @Given("^\"([^\"]*)\" demo data is created$")
  public void demo_data_is_created(String appName) {
    CucumberTestUtils.recreateDemoData(getApp(appName));
  }

  @Given("^permanent HTTP header \"([^\"]*):\\s*([^\"]*)\"$")
  public void permanent_HTTP_header(String name, String value) {
    permanentHttpHeaders.put(name, value);
  }

  // one time HTTP header
  @Given("^HTTP header \"([^\"]*):\\s*([^\"]*)\"$")
  public void http_header(String name, String value) {
    onetimeHttpHeaders.get().put(name, value);
  }

  @Given("^users logged into \"([^\"]*)\":$")
  @SuppressFBWarnings("EXS")
  public void logged_in(String appName, DataTable table) throws Exception {

    parallelProcess(
        table,
        (row) -> {
          Map<String, String> map = row.getData();
          LoginApplicationUserDto dto = new LoginApplicationUserDto();
          dto.setName(map.get("name"));
          dto.setPassword(map.get("password"));

          String jwtTokenName = map.get("jwtTokenName");

          try {
            anonymous_sends_POST_with_JSON(
                "/" + appName + "/public/login", json.writeValueAsString(dto));
            i_expect_HTTP_code(200);
            i_store_JSON_field_as("token", jwtTokenName);
          } catch (Exception e) {
            throw new RuntimeException(e);
          }
        });
  }

  @Given("^the following tenants exist in \"([^\"]*)\":$")
  @SuppressFBWarnings("EXS")
  public void tenants_exist(String appName, DataTable table) throws Exception {

    parallelProcess(
        table,
        (row) -> {
          Map<String, String> tenantMap = row.getData();

          loginSystemAdmin(appName);

          Map<String, Object> bodyMap = new HashMap<>();
          bodyMap.put("publicId", tenantMap.get("publicId"));
          bodyMap.put("tenantId", tenantMap.get("tenantId"));
          bodyMap.put("name", tenantMap.get("name"));
          bodyMap.put("features", StringUtils.split(tenantMap.get("features"), ","));

          String adminEmail = tenantMap.get("adminEmail");
          if (StringUtils.isNotBlank(adminEmail)) {
            String adminPassword = tenantMap.get("adminPassword");
            Assert.assertTrue(
                "adminPassword must not be null", StringUtils.isNotBlank(adminPassword));

            Map<String, Object> adminDto = new HashMap<>(3);
            adminDto.put("name", adminEmail);
            adminDto.put("email", adminEmail);
            adminDto.put("password", adminPassword);

            bodyMap.put("admin", adminDto);
          }

          try {
            String requestBody = json.writeValueAsString(bodyMap);
            String requestPath = "/" + appName + "/system/tenants";

            user_with_JWT_sends_POST_with_JSON(LOGIN_TOKEN, requestPath, requestBody);

          } catch (Exception e) {
            throw new RuntimeException(e);
          }

          i_expect_HTTP_code(201);
          i_expect_JSON_at_path_equals("publicId", tenantMap.get("publicId"));
          i_expect_JSON_at_path_equals("name", tenantMap.get("name"));

          Optional<String> storeKey =
              tenantMap.keySet().stream().filter((k) -> k.startsWith("store ")).findFirst();
          if (storeKey.isPresent()) {
            i_store_JSON_field_as("tenantId", tenantMap.get(storeKey.get()));
          }
        });
  }

  @Given(
      "^the role \"([^\"]*)\" exists for tenant \"([^\"]*)\" in \"([^\"]*)\" with no permissions$")
  public void role_exists_for_tenant_with_permissions(
      String roleName, String tenantPublicId, String appName) {

    int tenantId = getTenantIdFromPublicId(appName, tenantPublicId);

    int roleId = createRole(appName, roleName, tenantId, Collections.emptyList());
    context.put(roleName, roleId);
  }

  @Given("^the role \"([^\"]*)\" exists for tenant \"([^\"]*)\" in \"([^\"]*)\" with permissions:$")
  public void role_exists_for_tenant_with_permissions(
      String roleName, String tenantPublicId, String appName, DataTable table) {

    int tenantId = getTenantIdFromPublicId(appName, tenantPublicId);
    Set<String> permissionNames = getAllSystemPermissions(appName);
    Set<String> requestedPermissions = new HashSet<>(table.asList(String.class));

    Set<String> unknownPermissions =
        requestedPermissions
            .stream()
            .filter(requestedPerm -> !permissionNames.contains(requestedPerm))
            .collect(Collectors.toSet());

    Assert.assertTrue(
        "Invalid Permissions: " + unknownPermissions.toString(), unknownPermissions.isEmpty());

    int roleId = createRole(appName, roleName, tenantId, requestedPermissions);
    context.put(roleName, roleId);
  }

  @Given("^the super user role exist for tenant \"([^\"]*)\" in \"([^\"]*)\":$")
  public void role_exist_for_tenant(String tenantPublicId, String appName, DataTable table) {
    int tenantId = getTenantIdFromPublicId(appName, tenantPublicId);
    Set<String> permissionNames = getAllSystemPermissions(appName);

    parallelProcess(
        table,
        (row) -> {
          Map<String, String> roleMap = row.getData();

          long roleId = createRole(appName, roleMap.get("name"), tenantId, permissionNames);

          roleMap
              .keySet()
              .stream()
              .filter((k) -> k.startsWith("store "))
              .findFirst()
              .ifPresent(key -> context.put(roleMap.get(key), roleId));
        });
  }

  @Given("^the super user role exist in \"([^\"]*)\":$")
  public void roles_exist_for(String appName, DataTable table) {
    Set<String> permissionNames = getAllSystemPermissions(appName);

    parallelProcess(
        table,
        (row) -> {
          Map<String, String> roleMap = row.getData();

          int tenantId = Integer.parseInt(getParameterizedValue(roleMap.get("tenantId")));

          long roleId = createRole(appName, roleMap.get("name"), tenantId, permissionNames);

          roleMap
              .keySet()
              .stream()
              .filter((k) -> k.startsWith("store "))
              .findFirst()
              .ifPresent(key -> context.put(roleMap.get(key), roleId));
        });
  }

  private int createRole(
      String appName, String roleName, int tenantId, Collection<String> permissions) {

    Map<String, Object> bodyMap = new HashMap<>();
    bodyMap.put("name", roleName);
    bodyMap.put("tenantId", tenantId);
    bodyMap.put("permissions", permissions);

    sendRequestAsSystemAdmin(appName, "/secure/roles", HttpMethod.POST, bodyMap);
    i_expect_HTTP_code(201);

    return JsonTestUtils.getJsonAtPath(lastResponse.get().getBody(), "roleId");
  }

  @Given("^user with JWT \"([^\"]*)\" creates the tenant carriers in \"([^\"]*)\":$")
  public void tenantcarrier_exists_for(String jwtTokenName, String appName, DataTable table) {

    parallelProcess(
        table,
        (row) -> {
          Map<String, String> contextMap = row.getData();

          Map<String, Object> bodyMap = new HashMap<>();
          bodyMap.put("carrierCode", contextMap.get("carrierCode"));

          bodyMap.put("tenderNotificationMode", "EMAIL");
          bodyMap.put("tenderRecallMode", "EMAIL");
          bodyMap.put("shipmentStatusUpdateType", "PUSH");
          bodyMap.put("ratePerDistanceRoundingInterval", 10);
          bodyMap.put("fixedRateDistanceRoundingInterval", 10);
          bodyMap.put("freightAllKindsCode", 92.5);
          bodyMap.put("currency", "USD");

          user_with_JWT_sends_POST_with_content(
              jwtTokenName,
              "/" + appName + "/secure/carriers",
              MediaType.APPLICATION_JSON,
              bodyMap);
          i_expect_HTTP_code(201);
        });
  }

  @Given(
      "^user with JWT \"([^\"]*)\" creates datacontext for tenant \"([^\"]*)\" in \"([^\"]*)\":$")
  public void datacontext_exists_for(
      String jwtTokenName, String tenantPublicId, String appName, DataTable table) {
    int tenantId = getTenantIdFromPublicId(appName, tenantPublicId);

    parallelProcess(
        table,
        (row) -> {
          Map<String, String> contextMap = row.getData();

          Map<String, Object> bodyMap = new HashMap<>();
          bodyMap.put("name", contextMap.get("name"));
          bodyMap.put("tenantId", tenantId);

          List<Map<String, Object>> conditionList = new ArrayList<>();

          Map<String, Object> carrierMap = new HashMap<>();
          carrierMap.put("conditionType", "CARRIER");
          if (contextMap.containsKey("carrierCodes")) {
            String[] carrierCodes = contextMap.get("carrierCodes").split("\\s*,\\s*");
            carrierMap.put("conditionValues", carrierCodes);
            carrierMap.put("unrestricted", false);
          } else {
            carrierMap.put("unrestricted", true);
          }
          conditionList.add(carrierMap);

          Map<String, Object> partyMap = new HashMap<>();
          partyMap.put("conditionType", "PARTY");
          if (contextMap.containsKey("partyIds")) {
            String[] partyIds = contextMap.get("partyIds").split("\\s*,\\s*");
            partyMap.put("conditionValues", partyIds);
            partyMap.put("unrestricted", false);
          } else {
            partyMap.put("unrestricted", true);
          }
          conditionList.add(partyMap);

          Map<String, Object> regionMap = new HashMap<>();
          regionMap.put("conditionType", "REGION");
          if (contextMap.containsKey("regionIds")) {
            String[] regionIds = contextMap.get("regionIds").split("\\s*,\\s*");
            regionMap.put("conditionValues", regionIds);
            regionMap.put("unrestricted", false);
          } else {
            regionMap.put("unrestricted", true);
          }
          conditionList.add(regionMap);

          if (!conditionList.isEmpty()) {
            bodyMap.put("conditions", conditionList);
          }

          user_with_JWT_sends_POST_with_content(
              jwtTokenName,
              "/" + appName + "/secure/dataContexts",
              MediaType.APPLICATION_JSON,
              bodyMap);
          i_expect_HTTP_code(201);

          Optional<String> storeKey =
              contextMap.keySet().stream().filter((k) -> k.startsWith("store ")).findFirst();
          if (storeKey.isPresent()) {
            i_store_JSON_field_as("dataContextId", contextMap.get(storeKey.get()));
          }
        });
  }

  @Given("^user with JWT \"([^\"]*)\" create contacts and link to party in \"([^\"]*)\":$")
  public void create_contact_link_to_party(String jwtTokenName, String appName, DataTable table) {
    parallelProcess(
        table,
        row -> {
          Map<String, String> contextMap = row.getData();

          Map<String, Object> contactMap = new HashMap<>();
          contactMap.put("firstName", contextMap.get("lastName"));
          contactMap.put("lastName", contextMap.get("lastName"));
          contactMap.put("email", contextMap.get("email"));
          user_with_JWT_sends_POST_with_content(
              jwtTokenName,
              "/" + appName + "/secure/contacts",
              MediaType.APPLICATION_JSON,
              contactMap);
          i_expect_HTTP_code(201);
          String contactId = JsonTestUtils.getJsonAtPath(lastResponse.get().getBody(), "contactId");
          String partyId = contextMap.get("partyId");
          Assert.assertTrue(StringUtils.isNotBlank(partyId));
          user_with_JWT_sends_POST_with_content(
              jwtTokenName,
              "/" + appName + "/secure/parties/" + partyId + "/contacts/" + contactId,
              MediaType.APPLICATION_JSON,
              contactMap);
          i_expect_HTTP_code(200);
          Optional<String> storeKey =
              contextMap.keySet().stream().filter((k) -> k.startsWith("store ")).findFirst();
          if (storeKey.isPresent()) {
            i_store_JSON_field_as("contactId", contextMap.get(storeKey.get()));
          }
        });
  }

  @Given("^user with JWT \"([^\"]*)\" creates the regions in \"([^\"]*)\":$")
  public void region_exists_for_tenant_with_countries(
      String jwtTokenName, String appName, DataTable table) throws JsonProcessingException {

    parallelProcess(
        table,
        (row) -> {
          Map<String, String> contextMap = row.getData();

          String[] requestCountries = contextMap.get("countryCodes").split("\\s*,\\s*");
          Map<String, Object> bodyMap = new HashMap<>();
          bodyMap.put("name", contextMap.get("name"));
          bodyMap.put("countryCodes", requestCountries);

          try {
            user_with_JWT_sends_POST_with_content(
                jwtTokenName,
                "/" + appName + "/secure/regions",
                MediaType.APPLICATION_JSON,
                json.writeValueAsString(bodyMap));
          } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
          }
          i_expect_HTTP_code(201);

          Optional<String> storeKey =
              contextMap.keySet().stream().filter((k) -> k.startsWith("store ")).findFirst();
          if (storeKey.isPresent()) {
            i_store_JSON_field_as("regionId", contextMap.get(storeKey.get()));
          }
        });
  }

  @Given("^user with JWT \"([^\"]*)\" creates parties in \"([^\"]*)\":$")
  public void anonymous_creates_parties(String jwtTokenName, String appName, DataTable table)
      throws Exception {

    String path = "/" + appName + "/secure/parties";

    parallelProcess(
        table,
        (row) -> {
          Map<String, String> contextMap = row.getData();

          Map<String, Object> bodyMap = new HashMap<>();
          bodyMap.put("name", contextMap.get("name"));
          bodyMap.put("creationMode", "USER_OPERATION");
          String[] partyTypes = contextMap.get("partyTypes").split("\\s*,\\s*");
          bodyMap.put("partyTypes", partyTypes);
          bodyMap.put("source", contextMap.get("source"));
          user_with_JWT_sends_POST_with_content(
              jwtTokenName, path, MediaType.APPLICATION_JSON, bodyMap);
          i_expect_HTTP_code(201);

          Optional<String> storeKey =
              contextMap.keySet().stream().filter((k) -> k.startsWith("store ")).findFirst();
          if (storeKey.isPresent()) {
            i_store_JSON_field_as("partyId", contextMap.get(storeKey.get()));
          }
        });
  }

  @Given("^the following users exist for tenant \"([^\"]*)\" in \"([^\"]*)\":$")
  @SuppressFBWarnings("EXS")
  public void users_exist_for_tenant(String tenantPublicId, String appName, DataTable table) {

    parallelProcess(
        table,
        (row) -> {
          Map<String, String> userMap = row.getData();

          String restrictedCarrier = userMap.getOrDefault("carrierCode", "");
          if (restrictedCarrier.isEmpty()) {
            restrictedCarrier = null;
          }

          Map<String, Object> bodyMap = new HashMap<>(userMap.size());
          bodyMap.put("tenantPublicId", tenantPublicId);
          bodyMap.put("name", userMap.get("name"));
          bodyMap.put("password", userMap.get("password"));
          bodyMap.put("restrictedToCarrierCode", restrictedCarrier);
          bodyMap.put("roleId", userMap.get("roleId"));
          bodyMap.put("dataContextId", userMap.get("dataContextId"));

          String randomName = UUID.randomUUID().toString();

          bodyMap.put(
              "email", userMap.getOrDefault("address@email.com", randomName + "@mycompany.com"));
          bodyMap.put("firstName", userMap.getOrDefault("firstName", "F_" + randomName));
          bodyMap.put("lastName", userMap.getOrDefault("lastName", "L_" + randomName));
          bodyMap.put("active", Boolean.parseBoolean(userMap.getOrDefault("active", "true")));
          bodyMap.put("roles", userMap.getOrDefault("roles", "USER").split(","));
          bodyMap.put("dataContextId", userMap.getOrDefault("dataContextId", null));
          bodyMap.put(
              "tenantAdmin", Boolean.parseBoolean(userMap.getOrDefault("tenantAdmin", "false")));

          sendRequestAsSystemAdmin(appName, "/secure/users", HttpMethod.POST, bodyMap);
          i_expect_HTTP_code(201);
        });
  }

  /** Allow to create entities in a table as part of data setup */
  @Given("^user with JWT \"([^\"]*)\" sends POST \"([^\"]*)\" with JSON:$")
  public void user_with_JWT_sends_POST_with_JSON_table(
      String jwtTokenName, String path, DataTable table) {

    parallelProcess(
        table,
        Object.class,
        (row) -> {
          Map<String, Object> m = setEmptyStringsToNulls(row.getData());
          // send POST
          user_with_JWT_sends_POST_with_content(jwtTokenName, path, MediaType.APPLICATION_JSON, m);
          // verify we got 201 successful creation
          i_expect_HTTP_code(201);
        });
  }

  @Given(
      "^user with JWT \"([^\"]*)\" sends POST \"([^\"]*)\" with JSON and store field \"([^\"]*)\" with prefix \"([^\"]*)\" in the context:$")
  @SuppressFBWarnings("EXS")
  public void user_with_JWT_sends_POST_with_JSON_table_and_store_in_context(
      String jwtTokenName, String path, String fieldName, String prefix, DataTable table) {

    parallelProcess(
        table,
        Object.class,
        (row) -> {
          Map<String, Object> item = row.getData();

          item = setEmptyStringsToNulls(item);
          // send POST
          user_with_JWT_sends_POST_with_content(
              jwtTokenName, path, MediaType.APPLICATION_JSON, item);
          // verify we got 201 successful creation
          i_expect_HTTP_code(201);
          i_store_JSON_field_as(fieldName, prefix + (row.getIndex() + 1));
        });
  }

  /**
   * Step definition to upload tms specific documents and save the documentId in context with the
   * specified prefix.
   */
  @Given(
      "^user with JWT \"([^\"]*)\" uploads TMS documents and stores documentId with prefix \"([^\"]*)\" in the context:$")
  @SuppressFBWarnings("EXS")
  public void user_with_JWT_upload_document(String jwtTokenName, String prefix, DataTable table) {

    parallelProcess(
        table,
        Object.class,
        (row) -> {
          Map<String, Object> item = row.getData();

          item = setEmptyStringsToNulls(item);
          // send POST
          user_with_JWT_sends_POST_with_content(
              jwtTokenName, "/bff/secure/tms/documents", MediaType.APPLICATION_JSON, item);
          // verify we got 200
          i_expect_HTTP_code(200);
          i_store_JSON_field_as("documentId", prefix + (row.getIndex() + 1));
        });
  }

  /** */
  @Given(
      "^user with JWT \"([^\"]*)\" sends POST \"([^\"]*)\" with JSON using nested data table and store field \"([^\"]*)\" with prefix \"([^\"]*)\" in the context:$")
  @SuppressFBWarnings("EXS")
  public void user_with_JWT_create_charge_type_POST_with_JSON_table_and_store_in_context(
      String jwtTokenName, String path, String fieldName, String prefix, DataTable table) {

    parallelProcess(
        table,
        Object.class,
        (row) -> {
          Map<String, Object> item = row.getData();

          item = setEmptyStringsToNulls(item);
          for (Entry<String, Object> entry : item.entrySet()) {
            entry.setValue(toJsonNodeIfPossible(entry.getValue()));
          }
          // send POST
          user_with_JWT_sends_POST_with_content(
              jwtTokenName, path, MediaType.APPLICATION_JSON, item);
          // verify we got 201 successful creation
          i_expect_HTTP_code(201);
          i_store_JSON_field_as(fieldName, prefix + (row.getIndex() + 1));
        });
  }

  @Given("^user with JWT \"([^\"]*)\" sends PUT \"([^\"]*)\" with JSON:$")
  @SuppressFBWarnings("EXS")
  public void user_with_JWT_sends_PUT(String jwtTokenName, String path, DataTable table) {

    parallelProcess(
        table,
        Object.class,
        (row) -> {
          Map<String, Object> m = row.getData();

          m = setEmptyStringsToNulls(m);

          // send POST
          user_with_JWT_sends_PUT_with_content(jwtTokenName, path, MediaType.APPLICATION_JSON, m);
          // verify we got 201 successful creation
          i_expect_HTTP_code(204);
        });
  }

  @Given("^user with JWT \"([^\"]*)\" sends PUT \"([^\"]*)\" with file \"([^\"]*)\"")
  @SuppressFBWarnings("EXS")
  public void user_with_JWT_sends_PUT_with_file(String jwtTokenName, String path, String fileName) {
    path = getParameterizedValue(path);
    ApplicationInfo app = CucumberEnvironment.getAppForPath(path);
    String body = CucumberTestUtils.readResourceFile(fileName);

    MediaType mediaType = MediaType.valueOf("text/csv");
    onetimeHttpHeaders.get().put(HEADER_AUTHORIZATION, getJwtHeaderValue(jwtTokenName));
    lastResponse.set(
        put(app, false, path, body, permanentHttpHeaders, onetimeHttpHeaders.get(), mediaType));
  }

  @Given("^user sends PUT to the signed S3 url \"([^\"]*)\" with file \"([^\"]*)\"")
  @SuppressFBWarnings("EXS")
  public void user_with_JWT_sends_PUT_with_file_external(String path, String fileName) {
    path = getParameterizedValue(path);
    path = S3TestUtils.getTransformedSignedUrl(path);
    byte[] file = CucumberTestUtils.readFileAsBytes(fileName);

    lastResponse.set(HttpTestUtils.put(path, file, permanentHttpHeaders, onetimeHttpHeaders.get()));
  }

  @Given("^I wait (?:up to (\\d+) seconds )?for all of the queues to process$")
  public void wait_for_queues_to_drain(Integer numSeconds) throws InterruptedException {

    final int sleep_ms = 250;
    numSeconds = Optional.ofNullable(numSeconds).orElse(20);
    final int numAttempts = numSeconds * 1000 / sleep_ms;

    long startTime = System.currentTimeMillis();
    boolean success = false;
    for (int i = 0; i < numAttempts; i++) {
      Map<String, Integer> queueToMessageCount = SqsTestUtils.getMessageCountsOfActiveQueues();

      success = queueToMessageCount.values().stream().mapToInt(Integer::intValue).sum() == 0;

      /*
       * There is a race condition when we check the number of messages that remain in
       * the queues. It's possible that the processing of one queue enqueues a message
       * into another queue during that processing. If it turns out that it enqueues a
       * message into a queue we've already checked the message count for, it will
       * consider all of the messages as being processed. To combat this race
       * condition, we'll do another check. This second check is still subject to the
       * same race condition but at least reduces the probability that it occurs.
       */
      if (success) {
        queueToMessageCount = SqsTestUtils.getMessageCountsOfActiveQueues();
        success = queueToMessageCount.values().stream().mapToInt(Integer::intValue).sum() == 0;
      }

      if (!success) {
        StringBuilder msg = ThreadLocals.STRINGBUILDER.get();
        msg.append("Some queues still contain messages:").append(System.lineSeparator());
        for (Map.Entry<String, Integer> entry : queueToMessageCount.entrySet()) {
          String queueUrl = entry.getKey();
          int numMessages = entry.getValue();
          if (entry.getValue() > 0) {
            msg.append("Queue ")
                .append(queueUrl)
                .append(" still contains ")
                .append(numMessages)
                .append(" messages")
                .append(System.lineSeparator());
          }
        }
        msg.append("Sleeping ").append(sleep_ms).append(" ms until next attempt");
        log.info(msg.toString());
        Thread.sleep(sleep_ms);
      } else {
        break;
      }
    }

    if (success) {
      long endTime = System.currentTimeMillis();
      log.info("Completed queue processing after {} ms", endTime - startTime);
    }

    Assert.assertTrue(
        "Queue processing did not complete after " + numSeconds + " seconds", success);
  }

  @Given("^queue headers:$")
  public void queue_headers(DataTable table) {
    Map<String, String> tableMap = table.asMap(String.class, String.class);
    tableMap.entrySet().forEach(es -> permanentQueueHeaders.put(es.getKey(), es.getValue()));
  }

  @Given("^queue tenant \"([^\"]*)\" and message type \"([^\"]*)\"$")
  public void queue_tenant_and_message_type(String tenant, String messageType) {
    permanentQueueHeaders.put("VoilaTenant", tenant);
    permanentQueueHeaders.put("VoilaMessageType", messageType);
  }

  @Given("^I purge queue \"([^\"]*)\"$")
  public void i_purge_queue(String queueName) {
    SqsTestUtils.purgeQueue(queueName);
  }

  @Given("^I purge all queues")
  public void i_purge_all_queues() {
    SqsTestUtils.purgeAllQueues();
  }

  /**
   * ******************************************************** WHEN
   * *******************************************************
   */
  @When("^anonymous sends GET \"([^\"]*)\" on \"([^\"]*)\" admin port$")
  public void sends_GET_on_admin_port(String path, String appName) {
    path = getParameterizedUrl(path);

    ApplicationInfo app = getApp(appName);
    lastResponse.set(get(app, true, path, permanentHttpHeaders, onetimeHttpHeaders.get()));
  }

  @When("^anonymous sends POST \"([^\"]*)\" with JSON$")
  public void anonymous_sends_POST_with_JSON(String path, String body) {
    path = getParameterizedUrl(path);

    ApplicationInfo app = getAppForPath(path);
    lastResponse.set(
        post(
            app,
            false,
            path,
            body,
            permanentHttpHeaders,
            onetimeHttpHeaders.get(),
            MediaType.APPLICATION_JSON_TYPE));
  }

  @When("^anonymous sends PATCH \"([^\"]*)\" with JSON$")
  public void anonymous_sends_PATCH_with_JSON(String path, String body) {
    path = getParameterizedUrl(path);

    ApplicationInfo app = getAppForPath(path);
    lastResponse.set(patch(app, false, path, body, permanentHttpHeaders, onetimeHttpHeaders.get()));
  }

  @When("^anonymous sends GET \"([^\"]*)\"( without encoding the url)?$")
  public void anonymous_sends_GET(String path, String shouldNotEncode) {
    boolean encodeUrl = StringUtils.isEmpty(shouldNotEncode);
    path = getParameterizedValue(path, encodeUrl);

    ApplicationInfo app = getAppForPath(path);
    lastResponse.set(get(app, false, path, permanentHttpHeaders, onetimeHttpHeaders.get()));
  }

  @When("^user with JWT \"([^\"]*)\" sends GET \"([^\"]*)\"$")
  public void user_with_JWT_sends_GET(String jwtTokenName, String path) {
    path = getParameterizedUrl(path);

    ApplicationInfo app = getAppForPath(path);

    onetimeHttpHeaders.get().put(HEADER_AUTHORIZATION, getJwtHeaderValue(jwtTokenName));
    if (context.containsKey(path)) path = getFieldValue(path);
    lastResponse.set(get(app, false, path, permanentHttpHeaders, onetimeHttpHeaders.get()));
  }

  @When("^user with JWT \"([^\"]*)\" sends OPTIONS \"([^\"]*)\"$")
  public void user_with_JWT_sends_OPTIONS(String jwtTokenName, String path) {
    path = getParameterizedUrl(path);

    ApplicationInfo app = getAppForPath(path);

    onetimeHttpHeaders.get().put(HEADER_AUTHORIZATION, getJwtHeaderValue(jwtTokenName));
    if (context.containsKey(path)) path = getFieldValue(path);
    lastResponse.set(options(app, false, path, permanentHttpHeaders, onetimeHttpHeaders.get()));
  }

  @When("^user with JWT \"([^\"]*)\" sends GET \"([^\"]*)\" with query params$")
  public void user_with_JWT_sends_GET(String jwtTokenName, String path, DataTable queryParamTable) {
    path = getParameterizedUrl(path);

    Map<String, String> queryParams = new HashMap<>(queryParamTable.asLists().size());
    ApplicationInfo app = getAppForPath(path);
    for (List<String> row : queryParamTable.asLists()) {
      Assert.assertEquals("Query param rows must just be a key and value", 2, row.size());

      String key = row.get(0);
      String value = row.get(1);
      Assert.assertNull(
          "Duplicate query param \"" + key + "\" specified", queryParams.put(key, value));
    }

    UriBuilder builder = UriBuilder.fromUri(path);
    queryParams.forEach(builder::queryParam);

    path = builder.build().toString();
    log.debug(path);

    onetimeHttpHeaders.get().put(HEADER_AUTHORIZATION, getJwtHeaderValue(jwtTokenName));
    if (context.containsKey(path)) path = getFieldValue(path);
    lastResponse.set(get(app, false, path, permanentHttpHeaders, onetimeHttpHeaders.get()));
  }

  @SuppressFBWarnings("EXS")
  @When(
      "^user with JWT \"([^\"]*)\" sends GET \"([^\"]*)\"(?: up to (\\d+) times)? until the JSON at \"([^\"]*)\" equals$")
  public void user_with_JWT_sends_GET_until_path_equals_str(
      String jwtTokenName, String urlPath, Integer numAttempts, String jsonPath, String jsonBody) {

    Runnable assertFunc =
        () -> {
          try {
            i_expect_JSON_at_path_equals_JSON(jsonPath, jsonBody);
          } catch (IOException e) {
            throw new RuntimeException(e);
          }
        };

    sendGetUntilMatch(jwtTokenName, urlPath, numAttempts, jsonPath, assertFunc);
  }

  @When(
      "^user with JWT \"([^\"]*)\" sends GET \"([^\"]*)\"(?: up to (\\d+) times)? until the JSON at \"([^\"]*)\" (?:equals \"([^\"]*)\"|equals (\\d+)|has (\\d+) .*|contains \"([^\"]*)\")$")
  public void user_with_JWT_sends_GET_until_path_equals_str(
      String jwtTokenName,
      String urlPath,
      Integer numAttempts,
      String jsonPath,
      String matchingStr,
      Integer matchingInt,
      Integer arraySize,
      String containsStr) {

    Runnable assertFunc;
    if (matchingStr != null) {
      assertFunc = () -> i_expect_JSON_at_path_equals(jsonPath, matchingStr);
    } else if (matchingInt != null) {
      assertFunc = () -> i_expect_JSON_at_path_equals(jsonPath, matchingInt);
    } else if (arraySize != null) {
      assertFunc = () -> i_expect_JSON_at_path_has_n_objects(jsonPath, arraySize);
    } else {
      assertFunc = () -> i_expect_JSON_at_path_contains_string(jsonPath, containsStr);
    }

    sendGetUntilMatch(jwtTokenName, urlPath, numAttempts, jsonPath, assertFunc);
  }

  /**
   * Send GET requests as a user to a particular url until the response at the json path passes the
   * json assertion function or the requested number of attempts has been reached. An attempt sleeps
   * for 250 ms so we'll multiply the number of requested attempts by 4 to maintain the general
   * expected passage of time of a second.
   *
   * @param jwtTokenName The JWT of the user making the request
   * @param urlPath The url path to send GET requests to
   * @param numAttempts The number of times to attempt the request
   * @param jsonPath The path to the json that is going to be tested
   * @param jsonAssertFunc The assertion function that verifies the value at the path
   */
  private void sendGetUntilMatch(
      String jwtTokenName,
      String urlPath,
      Integer numAttempts,
      String jsonPath,
      Runnable jsonAssertFunc) {
    final int maxAttempts = Optional.ofNullable(numAttempts).orElse(5) * 4;
    final String attemptStr = "on attempt {} of {}";

    int i = 0;
    while (i++ < maxAttempts) {
      Throwable lastException = null;
      try {
        user_with_JWT_sends_GET(jwtTokenName, urlPath);
        jsonAssertFunc.run();
      } catch (PathNotFoundException ex) {
        log.info(
            "GET response from {} did not contain json path {} " + attemptStr,
            urlPath,
            jsonPath,
            i,
            maxAttempts);
        lastException = ex;
      } catch (AssertionError error) {
        log.info(
            "GET response from {} failed due to {} " + attemptStr,
            urlPath,
            error.getMessage(),
            i,
            maxAttempts);
        lastException = error;
      } catch (Exception ex) {
        log.info("Uncaught exception for GET {} " + attemptStr, urlPath, i, maxAttempts);
        lastException = ex;
      }

      if (lastException == null) {
        break;
      } else if (i >= maxAttempts) {
        throw new RuntimeException(lastException);
      }

      try {
        Thread.sleep(250);
      } catch (InterruptedException ex) {
      }
    }
  }

  // used for triggering system/admin functions
  @When("^user with JWT \"([^\"]*)\" sends POST \"([^\"]*)\"$")
  public void user_with_JWT_sends_POST(String jwtTokenName, String path) {
    user_with_JWT_sends_POST_with_content(jwtTokenName, path, MediaType.APPLICATION_JSON, "{}");
  }

  @When("^user with JWT \"([^\"]*)\" sends POST \"([^\"]*)\" with JSON$")
  public void user_with_JWT_sends_POST_with_JSON(String jwtTokenName, String path, String body) {
    user_with_JWT_sends_POST_with_content(jwtTokenName, path, MediaType.APPLICATION_JSON, body);
  }

  @When("^user with JWT \"([^\"]*)\" sends POST \"([^\"]*)\" with text$")
  public void user_with_JWT_sends_POST_with_text(String jwtTokenName, String path, String body) {
    user_with_JWT_sends_POST_with_content(jwtTokenName, path, MediaType.TEXT_PLAIN, body);
  }

  @When("^user with JWT \"([^\"]*)\" sends POST \"([^\"]*)\" with text file \"([^\"]+)\"$")
  public void user_with_JWT_sends_POST_with_text_file(
      String jwtTokenName, String path, String resourcePath) {
    String body = CucumberTestUtils.readResourceFile(resourcePath);

    user_with_JWT_sends_POST_with_content(jwtTokenName, path, MediaType.TEXT_PLAIN, body);
  }

  @When("^user with JWT \"([^\"]*)\" sends POST \"([^\"]*)\" with XML file \"([^\"]+)\"$")
  public void user_with_JWT_sends_POST_with_xml_file(
      String jwtTokenName, String path, String resourcePath) {
    String body = CucumberTestUtils.readResourceFile(resourcePath);

    user_with_JWT_sends_POST_with_content(jwtTokenName, path, MediaType.APPLICATION_XML, body);
  }

  @When("^user with JWT \"([^\"]*)\" sends PUT \"([^\"]*)\" with XML file \"([^\"]+)\"$")
  public void user_with_JWT_sends_PUT_with_xml_file(
      String jwtTokenName, String path, String resourcePath) {
    String body = CucumberTestUtils.readResourceFile(resourcePath);

    user_with_JWT_sends_PUT_with_content(jwtTokenName, path, MediaType.APPLICATION_XML, body);
  }

  @When("^user with JWT \"([^\"]*)\" sends POST \"([^\"]*)\" with \"([^\"]*)\"$")
  public void user_with_JWT_sends_POST_with_content(
      String jwtTokenName, String path, String contentType, String body) {
    path = getParameterizedUrl(path);
    body = getParameterizedValue(body);

    ApplicationInfo app = getAppForPath(path);
    MediaType mediaType = MediaType.valueOf(contentType);

    // Write code here that turns the phrase above into concrete actions
    onetimeHttpHeaders.get().put(HEADER_AUTHORIZATION, getJwtHeaderValue(jwtTokenName));
    lastResponse.set(
        post(app, false, path, body, permanentHttpHeaders, onetimeHttpHeaders.get(), mediaType));
  }

  // overloaded version that takes care of JSON serialization
  @SuppressFBWarnings("EXS")
  public void user_with_JWT_sends_POST_with_content(
      String jwtTokenName, String path, String contentType, Map<String, Object> body) {
    try {
      user_with_JWT_sends_POST_with_content(
          jwtTokenName, path, contentType, json.writeValueAsString(body));
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }
  }

  @SuppressFBWarnings("EXS")
  public void user_with_JWT_sends_POST_with_JSON_body(
      String jwtTokenName, String path, Object objectBody) {
    String body;
    try {
      body = json.writeValueAsString(objectBody);
    } catch (IOException ex) {
      throw new RuntimeException(ex);
    }
    user_with_JWT_sends_POST_with_JSON(jwtTokenName, path, body);
  }

  @SuppressFBWarnings("EXS")
  public void user_with_JWT_sends_PUT_with_JSON_body(
      String jwtTokenName, String path, Object objectBody) {
    String body;
    try {
      body = json.writeValueAsString(objectBody);
    } catch (IOException ex) {
      throw new RuntimeException(ex);
    }
    user_with_JWT_sends_PUT_with_JSON(jwtTokenName, path, body);
  }

  @When("^user with JWT \"([^\"]*)\" sends PATCH \"([^\"]*)\" with JSON$")
  public void user_with_JWT_sends_PATCH_with_JSON(String jwtTokenName, String path, String body) {
    path = getParameterizedUrl(path);
    body = getParameterizedValue(body);

    ApplicationInfo app = getAppForPath(path);

    // Write code here that turns the phrase above into concrete actions
    onetimeHttpHeaders.get().put(HEADER_AUTHORIZATION, getJwtHeaderValue(jwtTokenName));
    if (context.containsKey(path)) path = getFieldValue(path);
    lastResponse.set(patch(app, false, path, body, permanentHttpHeaders, onetimeHttpHeaders.get()));
  }

  @When(
      "^user with JWT \"([^\"]*)\" sends PATCH \"([^\"]*)\" after overriding following attributes in JSON stored as \"([^\"]*)\"$")
  public void user_with_JWT_send_PATCH_after_merging_json(
      String jwtTokenName, String path, String bodyJsonVariableName, String overrideJson) {
    String bodyJson = getParameterizedValue(bodyJsonVariableName);
    path = getParameterizedUrl(path);

    bodyJson = getParameterizedValue(bodyJson);
    overrideJson = getParameterizedValue(overrideJson);
    bodyJson = mergeJson(bodyJson, overrideJson);

    ApplicationInfo app = getAppForPath(path);

    // Write code here that turns the phrase above into concrete actions
    onetimeHttpHeaders.get().put(HEADER_AUTHORIZATION, getJwtHeaderValue(jwtTokenName));
    if (context.containsKey(path)) path = getFieldValue(path);
    lastResponse.set(
        patch(app, false, path, bodyJson, permanentHttpHeaders, onetimeHttpHeaders.get()));
  }

  @When("^user with JWT \"([^\"]*)\" sends PATCH \"([^\"]*)\"$")
  public void user_with_JWT_sends_PATCH(String jwtTokenName, String path) {
    path = getParameterizedUrl(path);

    ApplicationInfo app = getAppForPath(path);

    onetimeHttpHeaders.get().put(HEADER_AUTHORIZATION, getJwtHeaderValue(jwtTokenName));
    if (context.containsKey(path)) path = getFieldValue(path);
    lastResponse.set(patch(app, false, path, "", permanentHttpHeaders, onetimeHttpHeaders.get()));
  }

  @When("^user with JWT \"([^\"]*)\" sends PATCH as POST \"([^\"]*)\" with JSON$")
  public void user_with_JWT_sends_PATCH_as_POST_with_JSON(
      String jwtTokenName, String path, String body) {
    path = getParameterizedUrl(path);
    body = getParameterizedValue(body);

    ApplicationInfo app = getAppForPath(path);

    // Write code here that turns the phrase above into concrete actions
    onetimeHttpHeaders.get().put(HEADER_AUTHORIZATION, getJwtHeaderValue(jwtTokenName));
    onetimeHttpHeaders.get().put(METHOD_OVERRIDE, "PATCH");
    if (context.containsKey(path)) path = getFieldValue(path);
    lastResponse.set(
        post(
            app,
            false,
            path,
            body,
            permanentHttpHeaders,
            onetimeHttpHeaders.get(),
            MediaType.APPLICATION_JSON_TYPE));
  }

  @When("^user with JWT \"([^\"]*)\" sends PUT \"([^\"]*)\" with JSON$")
  public void user_with_JWT_sends_PUT_with_JSON(String jwtTokenName, String path, String body) {
    user_with_JWT_sends_PUT_with_content(jwtTokenName, path, MediaType.APPLICATION_JSON, body);
  }

  @When("^user with JWT \"([^\"]*)\" sends PUT \"([^\"]*)\" with \"([^\"]*)\"$")
  public void user_with_JWT_sends_PUT_with_content(
      String jwtTokenName, String path, String contentType, String body) {
    path = getParameterizedUrl(path);
    body = getParameterizedValue(body);

    ApplicationInfo app = getAppForPath(path);
    MediaType mediaType = MediaType.valueOf(contentType);

    // Write code here that turns the phrase above into concrete actions
    onetimeHttpHeaders.get().put(HEADER_AUTHORIZATION, getJwtHeaderValue(jwtTokenName));
    lastResponse.set(
        put(app, false, path, body, permanentHttpHeaders, onetimeHttpHeaders.get(), mediaType));
  }

  // overiden version that automatically converts to JSON
  @SuppressFBWarnings("EXS")
  public void user_with_JWT_sends_PUT_with_content(
      String jwtTokenName, String path, String contentType, Map<String, Object> body) {
    try {
      user_with_JWT_sends_PUT_with_content(
          jwtTokenName, path, contentType, json.writeValueAsString(body));
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }
  }

  @Given("^geocode lookups:$")
  public void i_insert_geocode_lookups(DataTable table) throws SQLException {

    Connection connection = CucumberEnvironment.getDatabaseConnection("SYSTEM");

    // do an upsert to update existing rows or insert new ones
    var sql =
        "INSERT INTO geocode_lookup (city_name, state_province, country, latitude, longitude, time_zone,  "
            + "geocode_quality, street_address, zip_postal, created_by) "
            + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, 'BDD' ) "
            + "ON CONFLICT (geocode_quality, street_address, city_name, state_province, zip_postal, country) "
            + "DO UPDATE  "
            + "SET latitude = EXCLUDED.latitude, longitude = EXCLUDED.longitude ";

    // use Batch to execute all updates in 1 SQL call
    var ps = connection.prepareStatement(sql);

    table
        .asMaps()
        .forEach(
            r -> {
              try {
                // INSERT
                ps.setString(1, getParameterizedValue(r.get("city_name")));
                ps.setString(2, getParameterizedValue(r.get("state_province")));
                ps.setString(3, getParameterizedValue(r.get("country")));
                ps.setBigDecimal(4, new BigDecimal(r.get("latitude")));
                ps.setBigDecimal(5, new BigDecimal(r.get("longitude")));
                ps.setString(6, getParameterizedValue(r.get("time_zone")));
                ps.setString(7, getParameterizedValue(r.get("geocode_quality")));
                ps.setString(8, getParameterizedValue(r.get("street_address")));
                ps.setString(9, getParameterizedValue(r.get("zip_postal")));

                ps.addBatch();

              } catch (SQLException e) {
                log.error("SQL error", e);
                throw new RuntimeException(e);
              }
            });

    // execute all as 1 batch for max speed
    ps.executeBatch();
    connection.commit();
  }

  @When("^user with JWT \"([^\"]*)\" sends PUT \"([^\"]*)\" with CSV$")
  public void user_with_JWT_sends_PUT_with_csv_content(
      String jwtTokenName, String path, String body) {
    path = getParameterizedUrl(path);
    body = getParameterizedValue(body);

    ApplicationInfo app = getAppForPath(path);
    MediaType mediaType = MediaType.valueOf("text/csv");

    // Write code here that turns the phrase above into concrete actions
    onetimeHttpHeaders.get().put(HEADER_AUTHORIZATION, getJwtHeaderValue(jwtTokenName));
    lastResponse.set(
        put(app, false, path, body, permanentHttpHeaders, onetimeHttpHeaders.get(), mediaType));
  }

  @When("^user with JWT \"([^\"]*)\" sends PUT \"([^\"]*)\" with CSV table$")
  public void user_with_JWT_sends_PUT_with_csv_content(
      String jwtTokenName, String path, DataTable table) {

    StringBuilder csvBodyBuilder = new StringBuilder(500);
    for (List<String> tableRow : table.asLists()) {
      String csvRow =
          tableRow
              .stream()
              .map(StringUtils::strip)
              .map(this::getParameterizedValue)
              .collect(Collectors.joining(","));

      csvBodyBuilder.append(csvRow).append("\n");
    }

    String body = csvBodyBuilder.toString();
    path = getParameterizedUrl(path);

    ApplicationInfo app = getAppForPath(path);
    MediaType mediaType = MediaType.valueOf("text/csv");

    onetimeHttpHeaders.get().put(HEADER_AUTHORIZATION, getJwtHeaderValue(jwtTokenName));
    lastResponse.set(
        put(app, false, path, body, permanentHttpHeaders, onetimeHttpHeaders.get(), mediaType));
  }

  @When("^user with JWT \"([^\"]*)\" sends DELETE \"([^\"]*)\"$")
  public void user_with_JWT_sends_DELETE(String jwtTokenName, String path) {
    path = getParameterizedUrl(path);

    ApplicationInfo app = getAppForPath(path);

    onetimeHttpHeaders.get().put(HEADER_AUTHORIZATION, getJwtHeaderValue(jwtTokenName));
    if (context.containsKey(path)) path = getFieldValue(path);
    lastResponse.set(delete(app, false, path, permanentHttpHeaders, onetimeHttpHeaders.get()));
  }

  @When("^the JSON message is placed on the queue \"([^\"]*)\"(?: grouped by \"([^\"]*)\")?$")
  public void message_is_placed_on_queue(String queueName, String groupId, String json) {
    json = getParameterizedValue(json);
    SqsTestUtils.sendMessage(queueName, groupId, json, permanentQueueHeaders);
  }

  @When(
      "^the \"([^\"]+)\" file \"([^\"]+)\" is placed in the s3 bucket \"([^\"]*)\" with key \"([^\"]*)\"$")
  public void text_file_placed_in_s3(
      String charsetStr, String resourcePath, String s3Bucket, String key) {
    Charset charset = Charset.forName(charsetStr);
    String body = CucumberTestUtils.readResourceFile(resourcePath, charset);
    text_body_placed_in_s3(s3Bucket, key, body);
  }

  @When(
      "^the text file \"([^\"]+)\" is placed in the s3 bucket \"([^\"]*)\" with key \"([^\"]*)\"$")
  public void text_file_placed_in_s3(String resourcePath, String s3Bucket, String key) {
    String body = CucumberTestUtils.readResourceFile(resourcePath);
    text_body_placed_in_s3(s3Bucket, key, body);
  }

  @When("^the text is placed in the s3 bucket \"([^\"]*)\" with key \"([^\"]*)\"$")
  public void text_body_placed_in_s3(String s3Bucket, String key, String body) {
    s3Bucket = getParameterizedValue(s3Bucket);
    key = getParameterizedValue(key);

    S3TestUtils.createBucket(s3Bucket);
    S3TestUtils.putTextInBucket(s3Bucket, key, body);
  }

  /**
   * ******************************************************** THEN
   * *******************************************************
   */
  @Then("^I expect HTTP status (\\d+)$")
  public void i_expect_HTTP_code(int status) {
    Assert.assertEquals(
        "unexpected status:\n" + lastResponse.get().getBody(),
        status,
        lastResponse.get().getStatus());
  }

  @Then("^I expect the JSON at \"([^\"]+)\" to equal (\\d+)$")
  public void i_expect_JSON_at_path_equals(String path, int expected) {
    int actual = JsonPath.parse(lastResponse.get().getBody()).read(path, int.class);
    Assert.assertEquals(expected, actual);
  }

  @Then("^I expect the JSON at \"([^\"]+)\" to equal (\\d+\\.\\d+)$")
  public void i_expect_JSON_at_path_equals_double(String path, String expected) {
    BigDecimal actual = JsonPath.parse(lastResponse.get().getBody()).read(path, BigDecimal.class);
    Assert.assertTrue(new BigDecimal(expected).compareTo(actual) == 0);
  }

  @Then("^I expect the JSON at \"([^\"]+)\" to equal (.+) with delta (.+)$")
  public void i_expect_JSON_at_path_equals_approx(String path, double approximate, double delta) {
    double actual = JsonPath.parse(lastResponse.get().getBody()).read(path, double.class);
    Assert.assertEquals(approximate, actual, delta);
  }

  @Then("^I expect the JSON at \"([^\"]+)\" to equal \"([^\"]*)\"$")
  public void i_expect_JSON_at_path_equals(String path, String expected) {
    expected = getParameterizedValue(expected);
    String actual = JsonTestUtils.getJsonAtPath(lastResponse.get().getBody(), path, String.class);
    Assert.assertEquals(expected, actual);
  }

  @Then("^I expect the JSON at \"([^\"]+)\" to not equal \"([^\"]*)\"$")
  public void i_expect_JSON_at_path_not_equals(String path, String expected) {
    expected = getParameterizedValue(expected);
    String actual = JsonTestUtils.getJsonAtPath(lastResponse.get().getBody(), path, String.class);
    Assert.assertNotEquals(expected, actual);
  }

  @Then("^I expect the JSON at \"([^\"]+)\" to equal null$")
  public void i_expect_JSON_at_path_equals_null(String path) {
    String actual = JsonTestUtils.getJsonAtPath(lastResponse.get().getBody(), path, String.class);
    Assert.assertEquals(null, actual);
  }

  @Then("^I expect the JSON at \"([^\"]+)\" to not equal null$")
  public void i_expect_JSON_at_path_not_equals_null(String path) {
    String actual = JsonTestUtils.getJsonAtPath(lastResponse.get().getBody(), path, String.class);
    Assert.assertNotEquals(null, actual);
  }

  @Then("^I expect the JSON at \"([^\"]+)\" starts with \"([^\"]*)\"$")
  public void i_expect_JSON_at_path_starts_with(String path, String expected) {
    expected = getParameterizedValue(expected);
    String actual = JsonTestUtils.getJsonAtPath(lastResponse.get().getBody(), path, String.class);
    Assert.assertTrue(
        "Actual " + actual + " does not start with " + expected, actual.startsWith(expected));
  }

  @Then("^I expect the JSON at \"([^\"]+)\" is not present")
  public void i_expect_JSON_at_path_not_present(String path) {
    JsonTestUtils.getJsonAtPath(lastResponse.get().getBody(), path);
    Assert.assertFalse(
        "Expected " + path + " to be not present in the JSON. But, found it in the response.",
        true);
  }

  @Then("^I expect the JSON at \"([^\"]+)\" to equal$")
  public void i_expect_JSON_at_path_equals_JSON(String path, String expected) throws IOException {
    expected = getParameterizedValue(expected);
    Object actualAtPath = JsonTestUtils.getJsonAtPath(lastResponse.get().getBody(), path);
    String actualJson = json.writeValueAsString(actualAtPath);
    JsonTestUtils.assertEqualJson(expected, actualJson);
  }

  @Then("^I expect the JSON at \"([^\"]+)\" to be equivalent to$")
  public void i_expect_JSON_at_path_equivalent_to(String path, String expected) throws IOException {
    expected = getParameterizedValue(expected);
    expected = getParameterizedValue(expected);
    Object actualAtPath = JsonTestUtils.getJsonAtPath(lastResponse.get().getBody(), path);
    String actualJson = json.writeValueAsString(actualAtPath);
    JsonTestUtils.assertEquivalentJson(expected, actualJson);
  }

  @Then("^I expect the JSON at \"([^\"]+)\" does not contain \"([^\"]*)\"$")
  public void i_expect_JSON_at_path_not_contains_string(String path, String expected)
      throws IOException {
    expected = getParameterizedValue(expected);
    List<String> actualAtPath = JsonTestUtils.getJsonListAtPath(lastResponse.get().getBody(), path);

    Assert.assertThat(actualAtPath, CoreMatchers.not(CoreMatchers.hasItem(expected)));
  }

  @Then("^I expect the JSON at \"([^\"]+)\" matches regex \"([^\"]*)\"$")
  public void i_expect_JSON_at_path_matches_regex(String path, String regex) throws IOException {

    String actualAtPath = JsonTestUtils.getJsonAtPath(lastResponse.get().getBody(), path);

    Assert.assertTrue(actualAtPath + " does not match regex " + regex, actualAtPath.matches(regex));
  }

  @Then("^I expect the JSON at \"([^\"]+)\" contains \"([^\"]*)\"$")
  public void i_expect_JSON_at_path_contains_string(String path, String expected) {
    expected = getParameterizedValue(expected);
    try {
      List<String> actualAtPath =
          JsonTestUtils.getJsonListAtPath(lastResponse.get().getBody(), path);

      Assert.assertThat(actualAtPath, CoreMatchers.hasItem(expected));
    } catch (MappingException ex) {
      log.info("JSON at path {} is not a list, attempting to do contains check as string", path);

      String actualAtPath =
          JsonTestUtils.getJsonAtPath(lastResponse.get().getBody(), path, String.class);

      Assert.assertThat(actualAtPath, CoreMatchers.containsString(expected));
    }
  }

  @Then(
      "^I sort the JSON at \"([^\"]+)\"(?: by \"([^\"]*)\")?(?: in (ascending|descending) order)?$")
  public void i_sort_JSON_at_path_by(String path, String sortPath, String sortOrderStr) {

    boolean ascending = !"descending".equals(sortOrderStr);
    sortPath = StringUtils.isEmpty(sortPath) ? "$" : sortPath;
    String sortedJson =
        JsonTestUtils.sortJsonAtPath(lastResponse.get().getBody(), path, sortPath, ascending);

    lastResponse.get().setBody(sortedJson);
  }

  @Then("^I expect the JSON(?: at \"([^\"]+)\")? has (\\d+) .*$")
  public void i_expect_JSON_at_path_has_n_objects(String path, int numResults) {
    if (StringUtils.isEmpty(path)) {
      path = "$";
    }

    JsonTestUtils.assertArraySizeAtPath(lastResponse.get().getBody(), path, numResults);
  }

  @Then("^I expect the JSON at \"([^\"]+)\" has at least (\\d+) elements.*$")
  public void i_expect_JSON_at_path_has_atleast_n_objects(String path, int numResults) {
    JsonTestUtils.assertArraySizeAtPathIsAtLeast(lastResponse.get().getBody(), path, numResults);
  }

  @Then("^I expect an empty response$")
  public void i_expect_empty_response() {
    Assert.assertEquals("", lastResponse.get().getBody());
  }

  @Then("^I expect JSON equivalent to$")
  public void i_expect_JSON_equivalent_to(String expected) {
    expected = getParameterizedValue(expected);
    JsonTestUtils.assertEquivalentJson(expected, lastResponse.get().getBody());
  }

  @Then("^I expect JSON equivalent to \\$\\{(\\w+)}$")
  public void i_expect_JSON_equivalent_to_var(String expectedVar) {
    String expected = context.get(expectedVar).toString();
    JsonTestUtils.assertEquivalentJson(expected, lastResponse.get().getBody());
  }

  @Then("^I expect JSON equal to$")
  public void i_expect_JSON_equal_to(String expected) {
    expected = getParameterizedValue(expected);
    JsonTestUtils.assertEqualJson(expected, lastResponse.get().getBody());
  }

  @Then("^I expect JSON containing field \"([^\"]*)\"$")
  public void i_expect_JSON_containing_field(String field) throws Exception {
    Map<String, Object> doc = JsonTestUtils.parseMap(lastResponse.get().getBody());
    Assert.assertTrue("field is not present in JSON document: " + field, doc.containsKey(field));
  }

  @Then("^I expect text equivalent to$")
  public void i_expect_text_equivalent_to(String expected) {
    // Compress all whitespace sequences to a single space.
    expected = getParameterizedValue(expected);
    Assert.assertEquals(
        expected.trim().replaceAll("\\s+", " "),
        lastResponse.get().getBody().trim().replaceAll("\\s+", " "));
    // If you want to preserve newlines, you can instead try something like:
    // myString.replaceAll("[ |\\t]+", " ").replaceAll("\\r\\n", "\n").replaceAll("\\r", "\n")
  }

  @Given(
      "^I store the date and time (\\d+) (milli|second|minute|hour|day)s? (before|after) now in format \"([^\"]*)\"(?: at timezone \"(.+)\")? as \"([^\"]*)\"(?: in \"([^\"]*)\" case)?$")
  public void i_store_relative_time_in_custom_format(
      int amount,
      String unit,
      String relative,
      String formatPattern,
      String zoneStr,
      String variableName,
      String caseInfo) {

    if ("before".equals(relative)) {
      amount = amount * -1;
    }
    unit = unit + "s";

    DateTimeFormatter formatter = DateTimeFormatter.ofPattern(formatPattern);

    Predicate<String> hasValue = (str) -> str != null && !str.isEmpty();
    ZoneId zoneId =
        Optional.ofNullable(zoneStr)
            .filter(hasValue)
            .map(ZoneId::of)
            .orElse(ZoneId.systemDefault());

    ChronoUnit timeUnit = ChronoUnit.valueOf(unit.toUpperCase());
    ZonedDateTime day = ZonedDateTime.now(zoneId).plus(amount, timeUnit);

    String dateTime = day.format(formatter);
    if (StringUtils.isNotBlank(caseInfo)) {
      if (caseInfo.equalsIgnoreCase("UPPER")) {
        dateTime = dateTime.toUpperCase();
      } else {
        dateTime = dateTime.toLowerCase();
      }
    }
    log.info("Storing date string \"{}\" in variable {}", dateTime, variableName);
    context.put(variableName, dateTime);
  }

  @Given(
      "^I store the (date|date and time|time) (\\d+) (milli|second|minute|hour|day)s? (before|after) now in ISO8601 (local|offset|UTC) format(?: at timezone \"(.+)\")? as \"([^\"]*)\"$")
  public void i_store_relative_time_in_format(
      String dateType,
      int amount,
      String unit,
      String relative,
      String format,
      String zoneStr,
      String variableName) {

    if ("before".equals(relative)) {
      amount = amount * -1;
    }
    unit = unit + "s";

    DateTimeFormatter formatter = getFormatter(format, dateType);

    ZoneId zoneId =
        Optional.ofNullable(zoneStr)
            .filter(StringUtils::isNotEmpty)
            .map(ZoneId::of)
            .orElse(ZoneId.systemDefault());

    ChronoUnit timeUnit = ChronoUnit.valueOf(unit.toUpperCase());
    ZonedDateTime day =
        ZonedDateTime.now(zoneId).truncatedTo(ChronoUnit.MICROS).plus(amount, timeUnit);

    String dateTime = day.format(formatter);
    context.put(variableName, dateTime);
  }

  @Given(
      "^I store (yesterday's|today's|tomorrow's) (date|date and time|time) in ISO8601 (local|offset|UTC) format(?: at timezone \"(.+)\")? as \"([^\"]*)\"$")
  public void i_store_relative_day_time_in_format(
      String dayStr, String dateType, String format, String zoneStr, String variableName) {

    switch (dayStr) {
      case "yesterday's":
        i_store_relative_time_in_format(
            dateType, 1, "day", "before", format, zoneStr, variableName);
        break;
      case "tomorrow's":
        i_store_relative_time_in_format(dateType, 1, "day", "after", format, zoneStr, variableName);
        break;
      default:
        i_store_relative_time_in_format(dateType, 0, "day", "after", format, zoneStr, variableName);
        break;
    }
  }

  @Given("^I store (\\d+) hours (before|after) in \"([^\"]*)\" format as \"([^\"]*)\"$")
  public void i_store_hours_before_in_format_as(
      int hours, String beforeAfter, String format, String variableName) {
    int multiplier = ("before".equals(beforeAfter)) ? -1 : 1;
    int hoursOffset = hours * multiplier;

    OffsetDateTime time =
        OffsetDateTime.now().truncatedTo(ChronoUnit.MICROS).plus(hoursOffset, ChronoUnit.HOURS);

    String dateTime = DateTimeFormatter.ofPattern(format).format(time);

    context.put(variableName, dateTime);
  }

  @Given(
      "^I store (\\d+) hours (before|after) in \"([^\"]*)\" format at timezone \"([^\"]*)\" as \"([^\"]*)\"$")
  public void i_store_hours_before_in_format_at_timezone_as(
      int hours, String beforeAfter, String format, String zoneId, String variableName) {
    int multiplier = ("before".equals(beforeAfter)) ? -1 : 1;
    int hoursOffset = hours * multiplier;

    OffsetDateTime time =
        OffsetDateTime.now(ZoneId.of(zoneId))
            .truncatedTo(ChronoUnit.MICROS)
            .plus(hoursOffset, ChronoUnit.HOURS);

    String dateTime = DateTimeFormatter.ofPattern(format).format(time);

    context.put(variableName, dateTime);
  }

  @Given(
      "^I store (\\d+) hours (before|after) in ISO8601 (local|offset|UTC) format as \"([^\"]*)\"$")
  public void i_store_hours_before_in_iso8601_format_as(
      int hours, String beforeAfter, String formatType, String variableName) {
    int multiplier = ("before".equals(beforeAfter)) ? -1 : 1;
    int hoursOffset = hours * multiplier;

    ZoneId zoneId = "UTC".equals(formatType) ? ZoneId.of("UTC") : ZoneId.systemDefault();
    ZonedDateTime time =
        ZonedDateTime.now(zoneId)
            .truncatedTo(ChronoUnit.MICROS)
            .plus(hoursOffset, ChronoUnit.HOURS);

    DateTimeFormatter formatter = FORMATTER_TABLE.get("date and time", formatType);
    String dateTime = formatter.format(time);

    context.put(variableName, dateTime);
  }

  @Given(
      "^I store a (same|different) day time range at least (\\d+) days from now in ISO8601 (local|offset|UTC) format as \"([^\"]*)\" and \"([^\"]*)\"$")
  public void i_store_time_range_in_iso8601_format_as(
      String dayStr, int days, String format, String start, String end) {

    String dateType = "date and time";
    LocalDateTime startTime;
    LocalDateTime endTime;

    DateTimeFormatter formatter = getFormatter(format, dateType);
    if ("same".equals(dayStr)) {
      startTime = LocalDate.now().plusDays(days).atStartOfDay().plusHours(1);
      endTime = startTime.plusHours(1);
    } else {
      startTime = LocalDate.now().plusDays(days + 1).atStartOfDay().minusMinutes(30);
      endTime = startTime.plusHours(1);
    }

    String startDateTime = formatter.format(startTime);
    String endDateTime = formatter.format(endTime);

    context.put(start, startDateTime);
    context.put(end, endDateTime);
  }

  /**
   * This method is to format an existing dateTime variable in the context and save as a different
   * variable. The dateAndTime parameter should be an existing variable name.
   */
  @Given("^I store the date and time \"([^\"]*)\" in \"([^\"]*)\" format as \"([^\"]*)\"$")
  public void i_store_the_date_and_time_in_format_as(
      String dateAndTime, String format, String variableName) {

    String dateTime = context.get(dateAndTime).toString();
    String formattedString =
        DateTimeFormatter.ofPattern(format)
            .format(LocalDateTime.parse(dateTime, DateTimeFormatter.ISO_LOCAL_DATE_TIME));
    context.put(variableName, formattedString);
  }

  @Given("^I store the date and time at path \"([^\"]*)\" in \"([^\"]*)\" format as \"([^\"]*)\"$")
  public void i_store_the_offset_date_and_time_in_format_as(
      String path, String format, String variableName) {
    String dateTime = JsonTestUtils.getJsonAtPath(lastResponse.get().getBody(), path);
    String formattedString =
        DateTimeFormatter.ofPattern(format)
            .format(OffsetDateTime.parse(dateTime, DateTimeFormatter.ISO_OFFSET_DATE_TIME));
    context.put(variableName, formattedString);
  }

  /**
   * This method is to take an existing dateTime variable in the context, round the minutes portion
   * down to the nearest multiple given, and save as a different variable. The dateAndTime parameter
   * should be an existing variable name. For example, setting the multiple as 15 will round the
   * minutes down to :00, :15, :30, or :45.
   */
  @Given(
      "^I store the date and time \"([^\"]*)\" in \"([^\"]*)\" format as \"([^\"]*)\" rounded down to a multiple of (\\d+) minutes$")
  public void i_store_the_date_and_time_in_format_with_minutes_rounded_down_as(
      String dateAndTime, String format, String variableName, int minuteMultiple) {

    String dateTime = context.get(dateAndTime).toString();
    LocalDateTime localDateTime =
        LocalDateTime.parse(dateTime, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    int minutes = localDateTime.getMinute() - (localDateTime.getMinute() % minuteMultiple);
    localDateTime = localDateTime.withMinute(minutes);
    String formattedString = DateTimeFormatter.ofPattern(format).format(localDateTime);
    context.put(variableName, formattedString);
  }

  private static final Table<String, String, DateTimeFormatter> FORMATTER_TABLE =
      HashBasedTable.create(3, 3);

  static {
    FORMATTER_TABLE.put("date and time", "local", DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    FORMATTER_TABLE.put("date and time", "offset", DateTimeFormatter.ISO_OFFSET_DATE_TIME);
    FORMATTER_TABLE.put("date and time", "UTC", DateTimeFormatter.ISO_INSTANT);

    FORMATTER_TABLE.put("date", "local", DateTimeFormatter.ISO_LOCAL_DATE);
    FORMATTER_TABLE.put("date", "offset", DateTimeFormatter.ISO_OFFSET_DATE);
    FORMATTER_TABLE.put("date", "UTC", DateTimeFormatter.ISO_DATE);

    FORMATTER_TABLE.put("time", "local", DateTimeFormatter.ISO_LOCAL_TIME);
    FORMATTER_TABLE.put("time", "offset", DateTimeFormatter.ISO_OFFSET_TIME);
    FORMATTER_TABLE.put("time", "UTC", DateTimeFormatter.ISO_TIME);
  }

  private static DateTimeFormatter getFormatter(String format, String dateType) {
    DateTimeFormatter formatter = FORMATTER_TABLE.get(dateType, format);

    if (formatter == null) {
      throw new IllegalArgumentException(
          "No formatter exists for date type: " + dateType + "and format: " + format);
    }

    return formatter;
  }

  @Given("^I store the value \"([^\"]*)\" as \"([^\"]*)\"$")
  public void i_store_value_as(String value, String variableName) {
    context.put(variableName, value);
  }

  @Given("^I store the value (\\d+) as \"([^\"]*)\"$")
  public void i_store_value_as(int value, String variableName) {
    context.put(variableName, value);
  }

  @Given("^I store the following JSON as \"([^\"]*)\":$")
  public void i_store_JSON_as(String variableName, String jsonBody) {
    context.put(variableName, getParameterizedValue(jsonBody));
  }

  @Given("^mail box of user \"([^\"]*)\" is cleared")
  public void mail_box_is_cleared(String emailId) throws MessagingException {
    MailTestUtils.clearMailBox(getParameterizedValue(emailId));
  }

  @Given("^mail box of users is cleared:$")
  public void mail_box_is_cleared_multiple(DataTable table) {
    parallelProcess(
        table,
        row -> {
          try {
            Map<String, String> data = row.getData();
            mail_box_is_cleared(data.get("email"));
          } catch (MessagingException e) {
            throw new RuntimeException(e);
          }
        });
  }

  @Then("^I expect the user with email id \"([^\"]*)\" has following mails$")
  public void i_expect_the_followig_mail_in_the_mailbox(String emailId, String expected)
      throws IOException, MessagingException, JSONException {
    expected = getParameterizedValue(expected);
    expected = MailTestUtils.extractEmailWithHtml(expected);
    JsonTestUtils.assertEquivalentJson(
        expected, MailTestUtils.getMessagesAsJSON(getParameterizedValue(emailId), json));
  }

  @Then("^I expect the first message in the inbox of \"([^\"]*)\" to contain \"([^\"]*)\"$")
  public void i_expect_first_message_in_inbox_to_contain(String emailAddress, String expected) {

    expected = getParameterizedValue(expected);
    Optional<String> htmlContent = MailTestUtils.getHtmlContentOfFirstInboxMessage(emailAddress);

    if (htmlContent.isEmpty()) {
      Assert.fail("The first message in the inbox of " + emailAddress + " had no html content");
    } else {
      Assert.assertThat(htmlContent.get(), CoreMatchers.containsString(expected));
    }
  }

  @Given("^I store the relative portion from the url \"([^\"]*)\" as \"([^\"]*)\"$")
  public void i_strip_hostname_and_port_from_variable(String fullUrlString, String var)
      throws MalformedURLException {
    fullUrlString = getParameterizedValue(fullUrlString);

    URL url = new URL(fullUrlString);

    // https://stackoverflow.com/questions/11928199/whats-the-difference-between-url-getfile-and-getpath
    String relativePath = url.getFile();

    context.put(var, relativePath);
  }

  @Given(
      "^I store the html element (?:attribute \"([^\"]*)\"|content) at path \"([^\"]*)\" of the first message in the inbox of \"([^\"]*)\" as \"([^\"]*)\"$")
  public void i_store_xpath_in_first_message_as(
      String attr, String xpath, String emailAddress, String var) {

    Optional<String> htmlContent = MailTestUtils.getHtmlContentOfFirstInboxMessage(emailAddress);

    if (htmlContent.isEmpty()) {
      Assert.fail("The first message in the inbox of " + emailAddress + " had no html content");
    } else {
      Document htmlDoc = Jsoup.parse(htmlContent.get());
      Element element = htmlDoc.selectFirst(xpath);
      String contentToStore;
      if (!StringUtils.isEmpty(attr)) {
        contentToStore = element.attr(attr);
      } else {
        contentToStore = element.text();
      }
      context.put(var, contentToStore);
    }
  }

  @Given("^I store the rendered template \"([^\"]*)\" as \"([^\"]*)\" using params$")
  public void i_store_the_rendered_template_as_using_params(
      String templateName, String variableName, DataTable paramsDataTable) {
    String htmlContent =
        CucumberTestUtils.readResourceFile(templateName).replaceAll("(\r\n|\n)", "\r\n");
    htmlContent = resolvePlaceHolders(paramsDataTable, htmlContent);
    context.put(variableName, htmlContent);
  }

  @Given("^the HTML content of the pdf returned by \"([^\"]*)\" equals \"([^\"]*)\"$")
  public void the_HTML_content_of_the_pdf_returned_by_equals(String url, String expected)
      throws IOException {
    url = getParameterizedValue(url, false);
    String pdfHtml = PdfTestUtils.getPdfAsHTML(url);
    expected = getParameterizedValue(expected);
    assertEquals(expected, pdfHtml);
  }

  @Then("^I expect the following users to have the mail content:$")
  public void i_expect_the_followig_mail_in_the_mailbox_multiple(DataTable table) {
    parallelProcess(
        table,
        row -> {
          Map<String, String> contextMap = row.getData();
          try {
            i_expect_the_followig_mail_in_the_mailbox(
                contextMap.get("email"), contextMap.get("content"));
          } catch (IOException | MessagingException | JSONException e) {
            throw new RuntimeException(e);
          }
        });
  }

  @Then(
      "^I store JSON field at \"([^\"]*)\" rounded to (floor|ceiling|halfup|halfdown) with scale (\\d+) as \"([^\"]*)\"$")
  public void i_store_JSON_field_at_path_rounded_to_as(
      String path, String mode, int scale, String variableName) {
    BigDecimal value =
        BigDecimal.valueOf(
            (Double) JsonTestUtils.getJsonAtPath(lastResponse.get().getBody(), path));
    int roundMode = 0;
    switch (mode) {
      case "floor":
        roundMode = BigDecimal.ROUND_FLOOR;
        break;
      case "ceiling":
        roundMode = BigDecimal.ROUND_CEILING;
        break;
      case "halfup":
        roundMode = BigDecimal.ROUND_HALF_UP;
        break;
      case "halfdown":
        roundMode = BigDecimal.ROUND_HALF_DOWN;
        break;
    }
    context.put(variableName, value.setScale(scale, roundMode));
  }

  @Then("^I store JSON field \"([^\"]*)\" as \"([^\"]*)\"$")
  public void i_store_JSON_field_as(String field, String variableName) {
    i_store_JSON_field_at_path_as(field, variableName);
  }

  @Then("^I store JSON field at \"([^\"]*)\" as \"([^\"]*)\"$")
  public void i_store_JSON_field_at_path_as(String path, String variableName) {

    Object objectAtPath = JsonTestUtils.getJsonAtPath(lastResponse.get().getBody(), path);
    context.put(variableName, objectAtPath);
  }

  @Then("^I expect HTTP header \"([^\"]*)\" contains \"([^\"]*)\"$")
  public void i_expect_HTTP_header_contains(String header, String content) {
    content = getParameterizedValue(content);
    Assert.assertNotNull(
        "HTTP header " + header + " is not present", lastResponse.get().getHeaders().get(header));

    String value = lastResponse.get().getHeaders().get(header).iterator().next();
    Assert.assertTrue(
        "HTTP header " + header + " with content '" + value + "' does not contain: " + content,
        value.contains(content));
  }

  @Then("^I expect HTTP header \"([^\"]*)\" contains UUID \"([^\"]*)\"$")
  public void i_expect_HTTP_header_contains_UUID(String header, String content) {

    Assert.assertNotNull(
        "HTTP header " + header + " is not present", lastResponse.get().getHeaders().get(header));

    String value = lastResponse.get().getHeaders().get(header).iterator().next();
    Assert.assertTrue(
        "HTTP header "
            + header
            + " with content '"
            + value
            + "' does not contain: "
            + getFieldValue(content),
        value.contains(getFieldValue(content)));
  }

  @Then("^I expect a list of (\\d+) JSON entities$")
  public void i_expect_a_list_of_JSON_entities(int size) {
    JsonTestUtils.assertArraySizeAtPath(lastResponse.get().getBody(), "$", size);
  }

  @Then("^I expect the JSON at \"([^\"]+)\" is not empty")
  public void i_expect_JSON_at_path_is_not_empty(String path) {
    String actual = JsonTestUtils.getJsonAtPath(lastResponse.get().getBody(), path, String.class);
    Assert.assertTrue(StringUtils.isNotBlank(actual));
  }

  @Then("^I expect the JSON at \"([^\"]+)\" is not null")
  public void i_expect_JSON_at_path_is_not_null(String path) {
    Object actual = JsonTestUtils.getJsonAtPath(lastResponse.get().getBody(), path, Object.class);
    Assert.assertTrue(actual != null);
  }

  @Then("^I expect the date at \"([^\"]+)\" is today's date")
  public void i_expect_DateTime_at_path_is_on_today(String path) {
    String actual = JsonTestUtils.getJsonAtPath(lastResponse.get().getBody(), path, String.class);
    Assert.assertTrue(isToday(actual));
  }

  @Then("^I expect (\\d+) messages on queue \"([^\"]*)\"$")
  public void i_expect_messages_on_queue(int count, String queueName) {
    int pendingCount = SqsTestUtils.getPendingMessageCountForQueueName(queueName);
    Assert.assertEquals(count, pendingCount);
  }

  @Then("^I wait (?:up to (\\d+) seconds )?for (\\d+) messages on queue \"([^\"]*)\"$")
  public void i_wait_for_messages_on_queue(Integer numSeconds, int count, String queueName)
      throws InterruptedException {

    final int sleep_ms = 250;
    numSeconds = Optional.ofNullable(numSeconds).orElse(20);
    final int numAttempts = numSeconds * 1000 / sleep_ms;

    int pendingCount = SqsTestUtils.getPendingMessageCountForQueueName(queueName);
    for (int i = 0; i < numAttempts && pendingCount != count; i++) {
      StringBuilder msg = ThreadLocals.STRINGBUILDER.get();
      msg.append("\tQueue ")
          .append(queueName)
          .append(" contains ")
          .append(pendingCount)
          .append(" messages.  Waiting for ")
          .append(count)
          .append(".\n");
      msg.append("Sleeping ").append(sleep_ms).append(" ms until next attempt");
      log.info("{}", msg);
      Thread.sleep(sleep_ms);
      pendingCount = SqsTestUtils.getPendingMessageCountForQueueName(queueName);
    }
    if (pendingCount == count) {
      Assert.assertEquals(count, pendingCount);
    } else {
      Assert.assertTrue(
          "Queue processing did not complete after " + numSeconds + " seconds", false);
    }
  }

  @Then("^I expect equivalent JSON messages on queue \"([^\"]*)\"$")
  public void i_expect_equivalent_JSON_messages_on_queue(String queueName, String messages) {
    messages = getParameterizedValue(messages);
    List<SqsTestUtils.SqsTestMessage> pending = SqsTestUtils.getPendingMessages(queueName);
    String pendingJson = JsonTestUtils.toJson(pending);

    JsonTestUtils.assertEqualJson(messages, pendingJson);
  }

  @Given("^I verify the key \"([^\"]*)\" exists in the S3 bucket \"([^\"]*)\"$")
  public void i_verify_the_key_exists_in_the_S_bucket(String key, String bucketName) {
    key = getParameterizedValue(key);
    bucketName = getParameterizedValue(bucketName);

    boolean objectExists = S3TestUtils.doesObjectExist(bucketName, key);
    String message = "Expected object s3://" + bucketName + "/" + key + " to exist but it does not";
    Assert.assertTrue(message, objectExists);
  }

  @Given("^I verify the key \"([^\"]*)\" does not exist in the S3 bucket \"([^\"]*)\"$")
  public void i_verify_the_key_does_not_exist_in_the_s3_bucket(String key, String bucketName) {
    key = getParameterizedValue(key);
    bucketName = getParameterizedValue(bucketName);

    boolean objectExists = S3TestUtils.doesObjectExist(bucketName, key);
    String message = "Expected object s3://" + bucketName + "/" + key + " to not exist but it does";
    Assert.assertFalse(message, objectExists);
  }

  @Given("^the super user datacontext exists for tenant \"([^\"]*)\" in \"([^\"]*)\":$")
  public void super_datacontext_exists_for(String tenantPublicId, String appName, DataTable table)
      throws Exception {
    int tenantId = getTenantIdFromPublicId(appName, tenantPublicId);

    parallelProcess(
        table,
        (row) -> {
          Map<String, String> contextMap = row.getData();

          Map<String, Object> bodyMap = new HashMap<>();
          bodyMap.put("name", contextMap.get("name"));
          bodyMap.put("tenantId", tenantId);

          List<Map<String, Object>> conditionList = new ArrayList<>();

          Map<String, Object> carrierMap = new HashMap<>();
          carrierMap.put("conditionType", "CARRIER");
          carrierMap.put("unrestricted", true);
          carrierMap.put("conditionValues", Collections.EMPTY_LIST);
          conditionList.add(carrierMap);

          Map<String, Object> partyMap = new HashMap<>();
          partyMap.put("conditionType", "PARTY");
          partyMap.put("unrestricted", true);
          partyMap.put("conditionValues", Collections.EMPTY_LIST);
          conditionList.add(partyMap);

          Map<String, Object> regionMap = new HashMap<>();
          regionMap.put("conditionType", "REGION");
          regionMap.put("unrestricted", true);
          regionMap.put("conditionValues", Collections.EMPTY_LIST);
          conditionList.add(regionMap);

          if (!conditionList.isEmpty()) {
            bodyMap.put("conditions", conditionList);
          }

          sendRequestAsSystemAdmin(appName, "/secure/dataContexts", HttpMethod.POST, bodyMap);
          i_expect_HTTP_code(201);

          Optional<String> storeKey =
              contextMap.keySet().stream().filter((k) -> k.startsWith("store ")).findFirst();
          if (storeKey.isPresent()) {
            i_store_JSON_field_as("dataContextId", contextMap.get(storeKey.get()));
          }
        });
  }

  /** BEGIN TMS SPECIFIC STEPS */
  //  @Given("^user with JWT \"([^\"]*)\" creates the following contacts$")
  //  public void create_contacts(String jwtToken, DataTable contactsTable) {
  //    Optional<String> storeKeyHeader = getStoreKeyHeader(contactsTable);
  //    String requestUrl = "/bff/secure/contacts";
  //
  //    contactsTable
  //        .asMaps(String.class, String.class)
  //        .forEach(
  //            (row) -> {
  //              Set<ContactTag> tags =
  //                  Optional.ofNullable(row.get("tags"))
  //                      .map(csv -> csv.split(","))
  //                      .map(Arrays::asList)
  //                      .stream()
  //                      .flatMap(List::stream)
  //                      .filter(StringUtils::isNotEmpty)
  //                      .map(ContactTag::valueOf)
  //                      .collect(Collectors.toSet());
  //
  //              ContactDto dto =
  //                  ContactDto.builder()
  //                      .firstName(Optional.ofNullable(row.get("firstName")).orElse(""))
  //                      .middleName(row.get("middleName"))
  //                      .lastName(Optional.ofNullable(row.get("lastName")).orElse(""))
  //                      .email(Optional.ofNullable(row.get("email")).orElse(""))
  //                      .phoneNumber(Optional.ofNullable(row.get("phoneNumber")).orElse(""))
  //                      .tags(tags)
  //                      .build();
  //
  //              user_with_JWT_sends_POST_with_JSON_body(jwtToken, requestUrl, dto);
  //              i_expect_HTTP_code(201);
  //
  //              storeKeyHeader.ifPresent(key -> i_store_JSON_field_as("contactId", row.get(key)));
  //            });
  //  }
  //
  //  @Given("^the following EIA area diesel prices are set$")
  //  public void create_diesel_price_config(DataTable dieselPriceTable) {
  //
  //    String url = "/bff/system/test/eiaDieselPrice/dieselPriceArea";
  //    loginSystemAdmin("intake");
  //
  //    parallelProcess(
  //        dieselPriceTable,
  //        String.class,
  //        row -> {
  //          String code = row.get("code").orElseThrow();
  //          String name = row.get("name").orElse(code);
  //          BigDecimal pricePerGallon = row.get("price").map(BigDecimal::new).orElseThrow();
  //          String countryCode = row.get("countryCode").orElse("US");
  //
  //          DieselPriceByAreaDto priceByAreaDto = new DieselPriceByAreaDto();
  //          priceByAreaDto.setPrice(pricePerGallon);
  //          priceByAreaDto.setLastUpdated(LocalDate.now());
  //
  //          DieselPriceAreaDto dto = new DieselPriceAreaDto();
  //          dto.setAreaCode(code);
  //          dto.setAreaName(name);
  //          dto.setCountryCode(countryCode);
  //          dto.setPriceMetric(DieselPriceMetric.DOLLARS_PER_GALLON);
  //          dto.setPriceByAreaList(Collections.singletonList(priceByAreaDto));
  //
  //          user_with_JWT_sends_PUT_with_JSON_body(LOGIN_TOKEN, url, dto);
  //          i_expect_HTTP_code(204);
  //        });
  //  }
  //
  //  @Given("^user with JWT \"([^\"]*)\" creates the following equipment types$")
  //  public void create_equipment_types(String jwtToken, DataTable equipmentTypeTable) {
  //
  //    Optional<String> storeKeyHeader = getStoreKeyHeader(equipmentTypeTable);
  //    String requestUrl = "/bff/secure/tms/equipmentTypes";
  //
  //    parallelProcess(
  //        equipmentTypeTable,
  //        (row) -> {
  //          Map<String, String> data = row.getData();
  //
  //          EquipmentTypeDto dto = new EquipmentTypeDto();
  //          dto.setCode(data.get("code"));
  //          dto.setDescription(data.get("description"));
  //
  //          user_with_JWT_sends_POST_with_JSON_body(jwtToken, requestUrl, dto);
  //          i_expect_HTTP_code(201);
  //
  //          storeKeyHeader.ifPresent(key -> i_store_JSON_field_as("equipmentTypeId",
  // data.get(key)));
  //        });
  //  }
  //
  //  @Given(
  //      "^user with JWT \"([^\"]*)\" associates the following equipment types with carrier
  // \"([^\"]*)\"$")
  //  public void associate_equip_types_with_carrier(
  //      String jwtToken, String carrierCode, DataTable equipmentTypeTable) {
  //
  //    Optional<String> storeKeyHeader = getStoreKeyHeader(equipmentTypeTable);
  //    carrierCode = getParameterizedValue(carrierCode);
  //    String requestUrl = "/bff/secure/tms/carriers/" + carrierCode + "/carrierEquipmentTypes";
  //
  //    parallelProcess(
  //        equipmentTypeTable,
  //        (row) -> {
  //          Map<String, String> data = row.getData();
  //
  //          String equipmentTypeId = getParameterizedValue(data.get("equipmentTypeId"));
  //          CarrierEquipmentTypeDto dto = new CarrierEquipmentTypeDto();
  //          dto.setEquipmentTypeId(Long.parseLong(equipmentTypeId));
  //
  //          user_with_JWT_sends_POST_with_JSON_body(jwtToken, requestUrl, dto);
  //          i_expect_HTTP_code(201);
  //
  //          storeKeyHeader.ifPresent(
  //              key -> i_store_JSON_field_as("carrierEquipmentTypeId", data.get(key)));
  //        });
  //  }
  //
  //  @Given("^user with JWT \"([^\"]*)\" creates the following urgency codes$")
  //  public void create_urgency_codes(String jwtToken, DataTable urgencyCodeTable) {
  //
  //    Optional<String> storeKeyHeader = getStoreKeyHeader(urgencyCodeTable);
  //    String requestUrl = "/bff/secure/tms/urgencyCodes";
  //
  //    parallelProcess(
  //        urgencyCodeTable,
  //        (row) -> {
  //          Map<String, String> data = row.getData();
  //
  //          UrgencyCodeDto dto = new UrgencyCodeDto();
  //          dto.setCode(data.get("code"));
  //          dto.setDescription(data.get("description"));
  //
  //          user_with_JWT_sends_POST_with_JSON_body(jwtToken, requestUrl, dto);
  //          i_expect_HTTP_code(201);
  //
  //          storeKeyHeader.ifPresent(key -> i_store_JSON_field_as("urgencyCodeId",
  // data.get(key)));
  //        });
  //  }
  //
  //  @Given("^user with JWT \"([^\"]*)\" creates the following transportation order types$")
  //  public void create_transportation_order_types(String jwtToken, DataTable orderTypeTable) {
  //
  //    Optional<String> storeKeyHeader = getStoreKeyHeader(orderTypeTable);
  //    String requestUrl = "/bff/secure/tms/transportationOrderTypes";
  //
  //    parallelProcess(
  //        orderTypeTable,
  //        (row) -> {
  //          Map<String, String> data = row.getData();
  //
  //          TransportationOrderTypeDto dto = new TransportationOrderTypeDto();
  //          dto.setCode(data.get("code"));
  //          dto.setDescription(data.get("description"));
  //
  //          user_with_JWT_sends_POST_with_JSON_body(jwtToken, requestUrl, dto);
  //          i_expect_HTTP_code(201);
  //
  //          storeKeyHeader.ifPresent(key -> i_store_JSON_field_as("orderTypeId", data.get(key)));
  //        });
  //  }
  //
  //  @Given(
  //      "^user with JWT \"([^\"]*)\" creates the following accessorial charges with a single
  // component$")
  //  public void createAccessorialCharge(String jwtToken, DataTable componentTable) {
  //
  //    Optional<String> storeKeyHeader = getStoreKeyHeader(componentTable);
  //
  //    parallelProcess(
  //        componentTable,
  //        (row) -> {
  //          Map<String, String> data = setEmptyStringsToNulls(row.getData());
  //
  //          CarrierAccessorialChargeDto dto = new CarrierAccessorialChargeDto();
  //          dto.setDescription(data.get("description"));
  //          dto.setAccessorialNameId(
  //              Long.parseLong(getParameterizedValue(data.get("accessorialNameId"))));
  //          dto.setCarrierEquipmentTypeId(
  //              isNotBlank(data.get("carrierEquipmentTypeId"))
  //                  ? Long.parseLong(getParameterizedValue(data.get("carrierEquipmentTypeId")))
  //                  : null);
  //          dto.setComponents(Arrays.asList(createComponentDto(data)));
  //
  //          user_with_JWT_sends_POST_with_JSON_body(
  //              jwtToken,
  //              String.format(
  //                  "/bff/secure/tms/carriers/%s/accessorialCharges",
  //                  getParameterizedValue(data.get("carrierCode"))),
  //              dto);
  //          i_expect_HTTP_code(201);
  //
  //          storeKeyHeader.ifPresent(
  //              key -> i_store_JSON_field_as("carrierAccessorialChargeId", data.get(key)));
  //        });
  //  }
  //
  //  private CarrierAccessorialChargeComponentDto createComponentDto(Map<String, String> row) {
  //    AccessorialChargeTypeRequirement type =
  //        AccessorialChargeTypeRequirement.valueOf(row.get("componentType"));
  //    BigDecimal charge = isNotBlank(row.get("charge")) ? new BigDecimal(row.get("charge")) :
  // null;
  //    BigDecimal minimumCharge =
  //        isNotBlank(row.get("minimumCharge")) ? new BigDecimal(row.get("minimumCharge")) : null;
  //    Integer minimumHours =
  //        isNotBlank(row.get("minimumHours")) ? Integer.parseInt(row.get("minimumHours")) : null;
  //    String distanceUomCode =
  //        isNotBlank(row.get("distanceUomCode")) ? row.get("distanceUomCode") : "MI";
  //    CarrierAccessorialChargeComponentDto dto = null;
  //    switch (type) {
  //      case HOURS:
  //        HourlyAccessorialComponentDto hourlyDto = new HourlyAccessorialComponentDto();
  //        hourlyDto.setRatePerHour(charge);
  //        hourlyDto.setMinimumHours(minimumHours);
  //        dto = hourlyDto;
  //        break;
  //      case INPUT_DISTANCE:
  //        DistanceBasedAccessorialComponentDto distanceDto =
  //            new InputDistanceAccessorialComponentDto();
  //        distanceDto.setDistanceUomCode(distanceUomCode);
  //        distanceDto.setRatePerDistanceUom(charge);
  //        distanceDto.setMinimumCharge(minimumCharge);
  //        dto = distanceDto;
  //        break;
  //      case TOTAL_DISTANCE:
  //        TotalDistanceAccessorialComponentDto totalDistanceDto =
  //            new TotalDistanceAccessorialComponentDto();
  //        totalDistanceDto.setDistanceUomCode(distanceUomCode);
  //        totalDistanceDto.setRatePerDistanceUom(charge);
  //        totalDistanceDto.setMinimumCharge(minimumCharge);
  //        dto = totalDistanceDto;
  //        break;
  //      case MILES:
  //        DistanceBasedAccessorialComponentDto milesDto = new MilesAccessorialComponentDto();
  //        milesDto.setDistanceUomCode(distanceUomCode);
  //        milesDto.setRatePerDistanceUom(charge);
  //        milesDto.setMinimumCharge(minimumCharge);
  //        dto = milesDto;
  //        break;
  //      case LINE_HAUL_RATE:
  //        LinehaulRatePercentageAccessorialComponentDto lhDto =
  //            new LinehaulRatePercentageAccessorialComponentDto();
  //        lhDto.setLinehaulRatePercentage(charge);
  //        lhDto.setMinimumCharge(minimumCharge);
  //        dto = lhDto;
  //        break;
  //      case PASS_COST:
  //        PassCostAccessorialComponentDto pcDto = new PassCostAccessorialComponentDto();
  //        pcDto.setMinimumCharge(minimumCharge);
  //        dto = pcDto;
  //        break;
  //      case PERCENT_COST:
  //        PercentCostAccessorialComponentDto percentCostDto =
  //            new PercentCostAccessorialComponentDto();
  //        percentCostDto.setPercentageOfCost(charge);
  //        percentCostDto.setMinimumCharge(minimumCharge);
  //        dto = percentCostDto;
  //        break;
  //      case STRAIGHT_FEE:
  //        StraightFeeAccessorialComponentDto sfDto = new StraightFeeAccessorialComponentDto();
  //        sfDto.setStraightFee(charge);
  //        dto = sfDto;
  //        break;
  //      default:
  //        throw new RuntimeException("Unsupported component type:" + type);
  //    }
  //    dto.setType(type);
  //    dto.setName(
  //        getParameterizedValue(
  //            isNotBlank(row.get("componentName"))
  //                ? row.get("componentName")
  //                : row.get("accessorialNameId")));
  //    dto.setDescription(row.get("componentDescription"));
  //    dto.setFuelSurchargeApplicable(
  //        isNotBlank(row.get("fuelSurchargeApplicable"))
  //            ? Boolean.parseBoolean(row.get("fuelSurchargeApplicable"))
  //            : false);
  //    return dto;
  //  }

  @Given(
      "^I store the date and time (\\d+) (milli|second|minute|hour|day)s? (before|after) \"([^\"]*)\" in ISO8601 offset format as \"([^\"]*)\""
          + " in ISO8601 (local|offset|UTC) format"
          + "(?: at timezone \\\"(.+)\\\")?$")
  public void i_store_the_relative_date_and_time_as(
      int amount,
      String unit,
      String relative,
      String dateAndTime,
      String variableName,
      String targerFormat,
      String timeZ) {
    if ("before".equals(relative)) {
      amount = amount * -1;
    }
    unit = unit + "s";
    ChronoUnit timeUnit = ChronoUnit.valueOf(unit.toUpperCase());
    String dateTime = context.get(dateAndTime).toString();
    DateTimeFormatter targetFormatter = getFormatter(targerFormat, "date and time");
    ZonedDateTime source = ZonedDateTime.parse(dateTime, DateTimeFormatter.ISO_OFFSET_DATE_TIME);
    if (StringUtils.isNotBlank(timeZ)) {
      ZoneId zoneId = ZoneId.of(timeZ);
      source = source.withZoneSameInstant(zoneId);
    }
    String formattedString = targetFormatter.format(source.plus(amount, timeUnit));
    context.put(variableName, formattedString);
  }

  /** END TMS SPECIFIC STEPS */

  /**
   * Retrieve the column header that is used to reference the context variable to store variables in
   */
  private Optional<String> getStoreKeyHeader(DataTable table) {
    return table.row(0).stream().filter(k -> k.startsWith("store")).findFirst();
  }

  private boolean isToday(String timeString) {
    OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
    OffsetDateTime time = OffsetDateTime.parse(timeString).withOffsetSameInstant(ZoneOffset.UTC);
    return now.truncatedTo(ChronoUnit.DAYS).isEqual(time.truncatedTo(ChronoUnit.DAYS));
  }

  // returns the full JWT Authorization header content
  private String getJwtHeaderValue(String tokenName) {
    if (context.containsKey(tokenName)) {
      return ThreadLocals.STRINGBUILDER
          .get()
          .append("Bearer ")
          .append(context.get(tokenName))
          .toString();
    } else {
      throw new RuntimeException(
          "Variable " + tokenName + " not found in current context: " + context);
    }
  }

  // returns the field value in header content
  private String getFieldValue(String tokenName) {
    if (context.containsKey(tokenName)) {
      return ThreadLocals.STRINGBUILDER.get().append(context.get(tokenName)).toString();
    } else {
      throw new RuntimeException(
          "Variable " + tokenName + " not found in current context: " + context);
    }
  }

  private String getParameterizedUrl(String url) {
    return getParameterizedValue(url, true);
  }

  /** @see #getParameterizedValue(String, boolean) */
  private String getParameterizedValue(String value) {
    return getParameterizedValue(value, false);
  }

  /**
   * Replaces any ${PARAM} values in a URL path with a value from the current context Useful for
   * passing auto-generated fields (e.g. GUID Id) into REST paths or JSON with references to
   * auto-generated fields
   *
   * @param value The string to replace parameterized values within
   * @param urlEncoded whether or not the value should be url encoded within the string
   */
  @SuppressFBWarnings("EXS")
  private String getParameterizedValue(String value, boolean urlEncoded) {
    Matcher m = CONTEXT_VARIABLE_PATTERN.matcher(value);
    while (m.find()) {
      String variableName = m.group(1);
      Object variableValue = context.get(variableName);

      if (variableValue == null) {
        throw new AssertionFailedError(
            "Unable to find variable " + variableName + " in test context");
      }

      String replacementValue = String.valueOf(variableValue);
      if (urlEncoded) {
        try {
          replacementValue = URLEncoder.encode(replacementValue, StandardCharsets.UTF_8.name());
        } catch (UnsupportedEncodingException e) {
          throw new RuntimeException(e);
        }
      }

      value = StringUtils.replace(value, m.group(0), replacementValue);
    }
    return value;
  }

  // workaround for default behaviour of Cucumber JVM
  // where empty cells in tables get put in as empt strings
  private <T> Map<String, T> setEmptyStringsToNulls(Map<String, T> data) {
    // convert unmodifiable collection into
    Map<String, T> newMap = new HashMap<>(data);

    newMap
        .entrySet()
        .stream()
        .filter(es -> StringUtils.isEmpty(String.valueOf(es.getValue())))
        .forEach(es -> es.setValue(null));

    return newMap;
  }

  /** Logs in the system admin and stores the */
  public synchronized void loginSystemAdmin(String appName) {

    if (!context.containsKey(LOGIN_TOKEN)) {

      // log in system admin once and store token
      LoginApplicationUserDto loginDto = new LoginApplicationUserDto();
      loginDto.setName("admin@voila.com");
      loginDto.setPassword("systemsystem");
      String loginUrl = "/" + appName + "/public/login";

      try {
        anonymous_sends_POST_with_JSON(loginUrl, json.writeValueAsString(loginDto));
      } catch (JsonProcessingException e) {
        throw new RuntimeException(e);
      }
      i_expect_HTTP_code(200);
      i_store_JSON_field_as("token", LOGIN_TOKEN);
    }
  }

  @SuppressFBWarnings("EXS")
  public HttpResponse sendRequestAsSystemAdmin(
      String appName, String url, String method, @Nullable Object body) {

    loginSystemAdmin(appName);

    try {

      String requestBody = body == null ? null : json.writeValueAsString(body);
      String requestPath = "/" + appName + url;
      switch (method) {
        case HttpMethod.POST:
          user_with_JWT_sends_POST_with_JSON(LOGIN_TOKEN, requestPath, requestBody);
          break;
        case HttpMethod.GET:
          user_with_JWT_sends_GET(LOGIN_TOKEN, requestPath);
          break;
        case HttpMethod.PATCH:
          user_with_JWT_sends_PATCH_with_JSON(LOGIN_TOKEN, requestPath, requestBody);
          break;
        case HttpMethod.PUT:
          user_with_JWT_sends_PUT_with_JSON(LOGIN_TOKEN, requestPath, requestBody);
          break;
        default:
          throw new IllegalArgumentException("Invalid HTTP method " + method);
      }

      return lastResponse.get();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private int getTenantIdFromPublicId(String appName, String tenantPublicId) {
    sendRequestAsSystemAdmin(appName, "/system/tenants/" + tenantPublicId, HttpMethod.GET, null);
    Map<String, Object> tenant = JsonTestUtils.getJsonAtPath(lastResponse.get().getBody(), "$");
    return (int) tenant.get("tenantId");
  }

  private String getTenantPublicIdFromId(String appName, int tenantId) {
    sendRequestAsSystemAdmin(appName, "/system/tenants", HttpMethod.GET, null);
    List<Map<String, Object>> tenants =
        JsonTestUtils.getJsonListAtPath(lastResponse.get().getBody(), "$");

    return tenants
        .stream()
        .filter(tenantMap -> tenantMap.get("tenantId").equals(tenantId))
        .map(tenantMap -> tenantMap.get("publicId").toString())
        .findFirst()
        .orElseThrow(() -> new IllegalArgumentException("no tenant exists with id " + tenantId));
  }

  private Set<String> getAllSystemPermissions(String appName) {
    sendRequestAsSystemAdmin(appName, "/secure/permissions", HttpMethod.GET, null);
    List<Map<String, Object>> permissions =
        JsonTestUtils.getJsonListAtPath(lastResponse.get().getBody(), "$");
    Set<String> permissionNames =
        permissions.stream().map(perm -> perm.get("name").toString()).collect(Collectors.toSet());
    return permissionNames;
  }

  private Object toJsonNodeIfPossible(Object value) {
    try {
      return json.reader().readTree(value.toString());
    } catch (IOException e) {
      return value;
    }
  }

  // processes table rows in parallel, for faster BDD setup
  // assumes Strings are used for values in each row
  @SuppressFBWarnings("EXS")
  private void parallelProcess(DataTable table, Consumer<TableRow<String>> lambda) {
    parallelProcess(table, String.class, lambda);
  }

  // processes table rows in parallel, for faster BDD setup
  private <V> void parallelProcess(
      DataTable table, Class<V> valueClass, Consumer<TableRow<V>> lambda) {
    List<TableRow<V>> rows = getTableRows(table, valueClass);

    rows.parallelStream().forEach(lambda);
  }

  // gets the table rows, while keeping track of their index
  // (which  is required
  private <V> List<TableRow<V>> getTableRows(DataTable table, Class<V> valueClass) {
    List<Map<String, V>> maps = table.asMaps(String.class, valueClass);
    List<TableRow<V>> rows = new ArrayList<>();

    for (int i = 0; i < maps.size(); i++) {
      rows.add(new TableRow(i, maps.get(i)));
    }

    return rows;
  }

  private String resolvePlaceHolders(DataTable paramsDataTable, String htmlContent) {
    Map<String, String> paramMap = paramsDataTable.asMap(String.class, String.class);
    for (Entry<String, String> entry : paramMap.entrySet()) {
      htmlContent = htmlContent.replaceAll(entry.getKey(), getParameterizedValue(entry.getValue()));
    }
    return htmlContent;
  }

  @Value
  private static class TableRow<V> {
    int index;
    Map<String, V> data;

    Optional<V> get(String col) {
      return Optional.ofNullable(data.get(col));
    }
  }
}
