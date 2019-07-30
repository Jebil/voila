package net.jk.app.commons.cucumber;

import com.google.common.collect.ImmutableMap;
import java.time.Instant;
import java.util.*;
import java.util.function.Supplier;
import javax.ws.rs.client.*;
import javax.ws.rs.core.MediaType;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.glassfish.jersey.client.ClientProperties;
import org.glassfish.jersey.client.HttpUrlConnectorProvider;

/** Wrapper around common HTTP methods */
@Slf4j
public class HttpTestUtils {

  public static final Client CLIENT =
      ClientBuilder.newClient()
          // required for Jersey to accept PATCH without exceptions
          .property(HttpUrlConnectorProvider.SET_METHOD_WORKAROUND, true);

  public static String getFullUrl(ApplicationInfo app, boolean admin, String path) {
    String baseUrl = app.getHttpUrl(admin);
    return ThreadLocals.STRINGBUILDER.get().append(baseUrl).append(path).toString();
  }

  private static final List<RequestLog> REQUEST_LOGS = new ArrayList<>();

  private static Map<String, String> getAllHeaders(
      Map<String, String> permanentHeaders, Map<String, String> oneTimeHeaders) {
    return new ImmutableMap.Builder<String, String>()
        .putAll(permanentHeaders)
        .putAll(oneTimeHeaders)
        .build();
  }

  private static Invocation.Builder getResource(
      ApplicationInfo app,
      boolean admin,
      String path,
      Map<String, String> permanentHeaders,
      Map<String, String> oneTimeHeaders) {

    WebTarget t =
        CLIENT
            .target(getFullUrl(app, admin, path))
            .property(ClientProperties.FOLLOW_REDIRECTS, Boolean.FALSE);

    Invocation.Builder bld = t.request().accept(MediaType.APPLICATION_JSON_TYPE);
    permanentHeaders.entrySet().forEach(es -> bld.header(es.getKey(), es.getValue()));
    oneTimeHeaders.entrySet().forEach(es -> bld.header(es.getKey(), es.getValue()));

    return bld;
  }

  /** Performs HTTP GET with HTTP Basic Auth */
  public static HttpResponse get(
      ApplicationInfo app,
      boolean admin,
      String path,
      Map<String, String> permanentHeaders,
      Map<String, String> oneTimeHeaders) {
    try {
      return runTimed(
          "GET",
          path,
          () ->
              HttpResponse.from(
                  getResource(app, admin, path, permanentHeaders, oneTimeHeaders).get()));
    } finally {
      log.debug("Sent GET to {}", path);
      oneTimeHeaders.clear();
    }
  }

  /** Performs HTTP POST of a JSON document */
  public static HttpResponse post(
      ApplicationInfo app,
      boolean admin,
      String path,
      String body,
      Map<String, String> permanentHeaders,
      Map<String, String> oneTimeHeaders,
      MediaType mediaType) {
    try {
      return runTimed(
          "POST",
          path,
          () ->
              HttpResponse.from(
                  getResource(app, admin, path, permanentHeaders, oneTimeHeaders)
                      .post(Entity.entity(body, mediaType))));
    } finally {
      log.debug("Sent POST to {}", path);
      oneTimeHeaders.clear();
    }
  }

  /** Performs HTTP PATCH of a JSON document */
  public static HttpResponse patch(
      ApplicationInfo app,
      boolean admin,
      String path,
      String body,
      Map<String, String> permanentHeaders,
      Map<String, String> oneTimeHeaders) {
    try {
      return runTimed(
          "PATCH",
          path,
          () ->
              HttpResponse.from(
                  getResource(app, admin, path, permanentHeaders, oneTimeHeaders)
                      .method("PATCH", Entity.entity(body, MediaType.APPLICATION_JSON_TYPE))));
    } finally {
      oneTimeHeaders.clear();
    }
  }

  /** Performs HTTP PUT of a JSON document */
  public static HttpResponse put(
      ApplicationInfo app,
      boolean admin,
      String path,
      String body,
      Map<String, String> permanentHeaders,
      Map<String, String> oneTimeHeaders,
      MediaType mediaType) {
    try {
      return runTimed(
          "PUT",
          path,
          () ->
              HttpResponse.from(
                  getResource(app, admin, path, permanentHeaders, oneTimeHeaders)
                      .put(Entity.entity(body, mediaType))));
    } finally {
      log.debug("Sent PUT to {}", path);
      oneTimeHeaders.clear();
    }
  }

  /** Performs HTTP DELETE */
  public static HttpResponse delete(
      ApplicationInfo app,
      boolean admin,
      String path,
      Map<String, String> permanentHeaders,
      Map<String, String> oneTimeHeaders) {
    try {
      return runTimed(
          "DELETE",
          path,
          () ->
              HttpResponse.from(
                  getResource(app, admin, path, permanentHeaders, oneTimeHeaders).delete()));
    } finally {
      log.debug("Sent DELETE to {}", path);
      oneTimeHeaders.clear();
    }
  }

  /** Performs HTTP OPTIONS with HTTP Basic Auth */
  public static HttpResponse options(
      ApplicationInfo app,
      boolean admin,
      String path,
      Map<String, String> permanentHeaders,
      Map<String, String> oneTimeHeaders) {
    try {
      return runTimed(
          "OPTIONS",
          path,
          () ->
              HttpResponse.from(
                  getResource(app, admin, path, permanentHeaders, oneTimeHeaders).options()));
    } finally {
      log.debug("Sent OPTIONS to {}", path);
      oneTimeHeaders.clear();
    }
  }

  /** External URL */
  public static HttpResponse put(
      String path,
      String body,
      Map<String, String> permanentHttpHeaders,
      Map<String, String> onetimeHttpHeaders,
      MediaType mediaType) {
    try {
      return HttpResponse.from(
          getResource(path, permanentHttpHeaders, onetimeHttpHeaders)
              .put(Entity.entity(body, mediaType)));
    } finally {
      log.debug("Sent PUT to {}", path);
      onetimeHttpHeaders.clear();
    }
  }

  public static HttpResponse put(
      String path,
      byte[] file,
      Map<String, String> permanentHttpHeaders,
      Map<String, String> onetimeHttpHeaders) {
    try {
      return HttpResponse.from(
          getResource(path, permanentHttpHeaders, onetimeHttpHeaders)
              .put(Entity.entity(file, MediaType.APPLICATION_OCTET_STREAM)));
    } finally {
      log.debug("Sent PUT to {}", path);
      onetimeHttpHeaders.clear();
    }
  }

  private static Invocation.Builder getResource(
      String path, Map<String, String> permanentHeaders, Map<String, String> oneTimeHeaders) {
    WebTarget t = CLIENT.target(path).property(ClientProperties.FOLLOW_REDIRECTS, Boolean.FALSE);

    Invocation.Builder bld = t.request().accept(MediaType.APPLICATION_JSON_TYPE);
    permanentHeaders.entrySet().forEach(es -> bld.header(es.getKey(), es.getValue()));
    oneTimeHeaders.entrySet().forEach(es -> bld.header(es.getKey(), es.getValue()));

    return bld;
  }

  // runs a request and keeps a log of how long it took
  // for statistics report at end
  private static HttpResponse runTimed(String method, String path, Supplier<HttpResponse> request) {
    Instant before = Instant.now();
    HttpResponse response = request.get();
    Instant after = Instant.now();

    REQUEST_LOGS.add(new RequestLog(method, path, (after.toEpochMilli() - before.toEpochMilli())));
    return response;
  }

  /**
   * Dumps to console a simple report with stats on which URLs took most time and were invoked most
   */
  public static void printRequestLogSummary() {
    SortedMap<String, RequestLogSummary> summaries = new TreeMap<>();

    REQUEST_LOGS.forEach(
        rs -> {
          String key =
              ThreadLocals.STRINGBUILDER
                  .get()
                  .append(rs.method)
                  .append(" ")
                  .append(rs.path)
                  .toString();
          RequestLogSummary summary = summaries.get(key);
          if (summary == null) {
            summaries.put(key, summary = new RequestLogSummary(rs.method, rs.path));
          }
          summary.add(rs);
        });

    // print report
    StringBuilder sb = ThreadLocals.STRINGBUILDER.get();
    sb.append(
        "-----------------------------------------------------------------------------------------------------------\n");
    sb.append("| Total (ms) | Count | Avg (ms) | Request\n");
    sb.append(
        "-----------------------------------------------------------------------------------------------------------");
    summaries.values().stream().sorted().forEach(s -> sb.append("\n").append(s));
    sb.append(
        "-----------------------------------------------------------------------------------------------------------");

    log.info("HTTP Request summary\n{}", sb.toString());
  }

  @Value
  private static final class RequestLog {
    private String method;
    private String path;
    private long millis;
  }

  @Data
  @RequiredArgsConstructor
  private static final class RequestLogSummary implements Comparable<RequestLogSummary> {

    private static final Comparator<RequestLogSummary> COMPARATOR =
        Comparator.comparing(RequestLogSummary::getTotalMillis)
            .thenComparing(RequestLogSummary::getAveragePerRequest)
            .thenComparing(RequestLogSummary::getCount)
            .thenComparing(RequestLogSummary::getPath)
            .thenComparing(RequestLogSummary::getMethod)
            .reversed();

    private final String method;
    private final String path;
    private long count;
    private long totalMillis;

    private long getAveragePerRequest() {
      return totalMillis / count;
    }

    void add(RequestLog log) {
      count++;
      totalMillis += log.millis;
    }

    @Override
    public int compareTo(RequestLogSummary o) {
      return COMPARATOR.compare(this, o);
    }

    public String toString() {
      return String.format(
          "| %1$10d | %2$5d | %3$8d | %4$6s %5$1s ",
          totalMillis, count, getAveragePerRequest(), method, path);
    }
  }
}
